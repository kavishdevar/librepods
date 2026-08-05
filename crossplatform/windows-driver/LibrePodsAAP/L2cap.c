/*++
    L2cap.c - the L2CAP channel operations, implemented with Bluetooth Request
    Blocks (BRBs) submitted to the stack via IOCTL_INTERNAL_BTH_SUBMIT_BRB.
--*/

#include "LibrePodsAAP.h"

//
// Submit a BRB synchronously to the Bluetooth stack (our parent I/O target).
//
NTSTATUS
LpSubmitBrbSync(
    _In_    PDEVICE_CONTEXT Ctx,
    _Inout_ PBRB            Brb
)
{
    NTSTATUS                 status;
    WDFREQUEST               request;
    PIRP                     irp;
    PIO_STACK_LOCATION       stack;
    WDF_REQUEST_SEND_OPTIONS options;

    if (Ctx->IoTarget == NULL) {
        return STATUS_DEVICE_NOT_READY;
    }

    status = WdfRequestCreate(WDF_NO_OBJECT_ATTRIBUTES, Ctx->IoTarget, &request);
    if (!NT_SUCCESS(status)) {
        return status;
    }

    // bthport reads the BRB pointer from Parameters.Others.Argument1 of the
    // IOCTL_INTERNAL_BTH_SUBMIT_BRB IRP. Set it explicitly on the next stack
    // location (the WDF memory-descriptor path left Argument1 NULL, so bthport
    // dereferenced a NULL BRB -> bugcheck 0x3B in BTHport!IOTracing::TraceStart).
    irp = WdfRequestWdmGetIrp(request);
    stack = IoGetNextIrpStackLocation(irp);
    stack->MajorFunction = IRP_MJ_INTERNAL_DEVICE_CONTROL;
    stack->MinorFunction = 0;
    stack->Parameters.DeviceIoControl.IoControlCode = IOCTL_INTERNAL_BTH_SUBMIT_BRB;
    stack->Parameters.Others.Argument1 = Brb;

    WDF_REQUEST_SEND_OPTIONS_INIT(
        &options, WDF_REQUEST_SEND_OPTION_SYNCHRONOUS | WDF_REQUEST_SEND_OPTION_TIMEOUT);
    WDF_REQUEST_SEND_OPTIONS_SET_TIMEOUT(&options, WDF_REL_TIMEOUT_IN_SEC(10));

    if (WdfRequestSend(request, Ctx->IoTarget, &options)) {
        status = WdfRequestGetStatus(request);
    } else {
        status = WdfRequestGetStatus(request);
    }

    WdfObjectDelete(request);
    return status;
}

//
// Open an outbound L2CAP channel to the remote device on the given PSM.
//
NTSTATUS
LpConnect(
    _In_ PDEVICE_CONTEXT Ctx,
    _In_ BTH_ADDR        Address,
    _In_ USHORT          Psm
)
{
    NTSTATUS                       status;
    struct _BRB_L2CA_OPEN_CHANNEL* brb;

    if (!Ctx->HasBthInterface) {
        return STATUS_DEVICE_NOT_READY;
    }

    if (Ctx->State == LpConnected) {
        if (Ctx->RemoteAddress == Address && Ctx->Psm == Psm) {
            return STATUS_SUCCESS;
        }
        LpDisconnect(Ctx);
    }

    brb = (struct _BRB_L2CA_OPEN_CHANNEL*)
        Ctx->BthInterface.BthAllocateBrb(BRB_L2CA_OPEN_CHANNEL, LP_POOL_TAG);
    if (brb == NULL) {
        return STATUS_INSUFFICIENT_RESOURCES;
    }

    brb->BtAddress    = Address;
    brb->Psm          = Psm;
    brb->ChannelFlags = CF_ROLE_EITHER;

    // Flags == 0 => let the stack negotiate default MTU/flush/QoS.
    brb->ConfigOut.Flags    = 0;
    brb->ConfigIn.Flags     = 0;
    brb->IncomingQueueDepth = 10; // MS-recommended default

    // Be notified when the remote tears the channel down.
    brb->CallbackFlags   = CALLBACK_DISCONNECT;
    brb->Callback        = LpIndicationCallback;
    brb->CallbackContext = Ctx;
    brb->ReferenceObject = Ctx->WdmDeviceObject;

    WdfSpinLockAcquire(Ctx->Lock);
    Ctx->State         = LpConnecting;
    Ctx->RemoteAddress = Address;
    Ctx->Psm           = Psm;
    WdfSpinLockRelease(Ctx->Lock);

    status = LpSubmitBrbSync(Ctx, (PBRB)brb);

    if (NT_SUCCESS(status)) {
        WdfSpinLockAcquire(Ctx->Lock);
        Ctx->ChannelHandle = brb->ChannelHandle;
        Ctx->State         = LpConnected;
        WdfSpinLockRelease(Ctx->Lock);
        KdPrint(("LibrePodsAAP: L2CAP connected (handle=%p)\n", brb->ChannelHandle));
    } else {
        WdfSpinLockAcquire(Ctx->Lock);
        Ctx->State         = LpDisconnected;
        Ctx->ChannelHandle = NULL;
        WdfSpinLockRelease(Ctx->Lock);
        KdPrint(("LibrePodsAAP: L2CAP connect failed 0x%08X\n", status));
    }

    Ctx->BthInterface.BthFreeBrb((PBRB)brb);
    return status;
}

//
// Close the channel (best-effort).
//
NTSTATUS
LpDisconnect(
    _In_ PDEVICE_CONTEXT Ctx
)
{
    struct _BRB_L2CA_CLOSE_CHANNEL* brb;
    L2CAP_CHANNEL_HANDLE            handle;
    BTH_ADDR                        addr;

    WdfSpinLockAcquire(Ctx->Lock);
    handle             = Ctx->ChannelHandle;
    addr               = Ctx->RemoteAddress;
    Ctx->State         = LpDisconnected;
    Ctx->ChannelHandle = NULL;
    WdfSpinLockRelease(Ctx->Lock);

    if (handle == NULL || !Ctx->HasBthInterface) {
        return STATUS_SUCCESS;
    }

    brb = (struct _BRB_L2CA_CLOSE_CHANNEL*)
        Ctx->BthInterface.BthAllocateBrb(BRB_L2CA_CLOSE_CHANNEL, LP_POOL_TAG);
    if (brb == NULL) {
        return STATUS_INSUFFICIENT_RESOURCES;
    }

    brb->BtAddress     = addr;
    brb->ChannelHandle = handle;
    (VOID)LpSubmitBrbSync(Ctx, (PBRB)brb);
    Ctx->BthInterface.BthFreeBrb((PBRB)brb);

    KdPrint(("LibrePodsAAP: disconnected\n"));
    return STATUS_SUCCESS;
}

//
// Write bytes to the channel.
//
NTSTATUS
LpSend(
    _In_ PDEVICE_CONTEXT Ctx,
    _In_ PVOID           Buffer,
    _In_ ULONG           Length
)
{
    NTSTATUS                       status;
    struct _BRB_L2CA_ACL_TRANSFER* brb;

    if (Ctx->State != LpConnected || Ctx->ChannelHandle == NULL) {
        return STATUS_DEVICE_NOT_CONNECTED;
    }
    if (Buffer == NULL || Length == 0) {
        return STATUS_INVALID_PARAMETER;
    }

    brb = (struct _BRB_L2CA_ACL_TRANSFER*)
        Ctx->BthInterface.BthAllocateBrb(BRB_L2CA_ACL_TRANSFER, LP_POOL_TAG);
    if (brb == NULL) {
        return STATUS_INSUFFICIENT_RESOURCES;
    }

    brb->BtAddress     = Ctx->RemoteAddress;
    brb->ChannelHandle = Ctx->ChannelHandle;
    brb->TransferFlags = ACL_TRANSFER_DIRECTION_OUT;
    brb->Buffer        = Buffer;
    brb->BufferMDL     = NULL;
    brb->BufferSize    = Length;
    brb->Timeout       = 0;

    status = LpSubmitBrbSync(Ctx, (PBRB)brb);
    Ctx->BthInterface.BthFreeBrb((PBRB)brb);
    return status;
}

//
// Read bytes from the channel (blocking up to TimeoutMs).
//
NTSTATUS
LpReceive(
    _In_  PDEVICE_CONTEXT Ctx,
    _Out_ PVOID           Buffer,
    _In_  ULONG           BufferLen,
    _Out_ PULONG          BytesRead,
    _In_  ULONG           TimeoutMs
)
{
    NTSTATUS                       status;
    struct _BRB_L2CA_ACL_TRANSFER* brb;

    *BytesRead = 0;

    if (Ctx->State != LpConnected || Ctx->ChannelHandle == NULL) {
        return STATUS_DEVICE_NOT_CONNECTED;
    }
    if (Buffer == NULL || BufferLen == 0) {
        return STATUS_INVALID_PARAMETER;
    }

    brb = (struct _BRB_L2CA_ACL_TRANSFER*)
        Ctx->BthInterface.BthAllocateBrb(BRB_L2CA_ACL_TRANSFER, LP_POOL_TAG);
    if (brb == NULL) {
        return STATUS_INSUFFICIENT_RESOURCES;
    }

    brb->BtAddress     = Ctx->RemoteAddress;
    brb->ChannelHandle = Ctx->ChannelHandle;
    brb->TransferFlags = ACL_TRANSFER_DIRECTION_IN | ACL_SHORT_TRANSFER_OK | ACL_TRANSFER_TIMEOUT;
    brb->Buffer        = Buffer;
    brb->BufferMDL     = NULL;
    brb->BufferSize    = BufferLen;
    brb->Timeout       = (LONGLONG)(TimeoutMs ? TimeoutMs : LP_RECV_TIMEOUT_MS);

    status = LpSubmitBrbSync(Ctx, (PBRB)brb);
    if (NT_SUCCESS(status)) {
        *BytesRead = brb->BufferSize; // updated with bytes actually read
    }

    Ctx->BthInterface.BthFreeBrb((PBRB)brb);
    return status;
}

//
// Notifications from the stack (we only care about remote disconnect).
//
VOID
LpIndicationCallback(
    _In_opt_ PVOID                 Context,
    _In_     INDICATION_CODE       Indication,
    _In_     PINDICATION_PARAMETERS Parameters
)
{
    PDEVICE_CONTEXT ctx = (PDEVICE_CONTEXT)Context;

    UNREFERENCED_PARAMETER(Parameters);

    if (ctx == NULL) {
        return;
    }

    switch (Indication) {
    case IndicationRemoteDisconnect:
        WdfSpinLockAcquire(ctx->Lock);
        ctx->State         = LpDisconnected;
        ctx->ChannelHandle = NULL;
        WdfSpinLockRelease(ctx->Lock);
        KdPrint(("LibrePodsAAP: remote disconnected the channel\n"));
        break;
    default:
        break;
    }
}
