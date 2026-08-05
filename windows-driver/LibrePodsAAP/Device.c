/*++
    Device.c - PnP: obtain the Bluetooth profile driver interface + I/O target
    from our BTHENUM parent, and tear down the connection on removal.
--*/

#include "LibrePodsAAP.h"

NTSTATUS
LpEvtDevicePrepareHardware(
    _In_ WDFDEVICE    Device,
    _In_ WDFCMRESLIST ResourcesRaw,
    _In_ WDFCMRESLIST ResourcesTranslated
)
{
    NTSTATUS        status;
    PDEVICE_CONTEXT ctx;

    UNREFERENCED_PARAMETER(ResourcesRaw);
    UNREFERENCED_PARAMETER(ResourcesTranslated);

    ctx = DeviceGetContext(Device);

    // Default I/O target = our parent = the Bluetooth stack. BRBs submitted here
    // reach bthport (this is exactly what the Root\ install of other drivers
    // gets wrong).
    ctx->IoTarget = WdfDeviceGetIoTarget(Device);

    // Ask the BTHENUM parent for BthAllocateBrb/BthFreeBrb/... Because we are a
    // real profile driver bound to the AAP service PDO, this succeeds.
    ctx->BthInterface.Interface.Size    = sizeof(BTH_PROFILE_DRIVER_INTERFACE);
    ctx->BthInterface.Interface.Version = BTHDDI_PROFILE_DRIVER_INTERFACE_VERSION_FOR_QI;

    status = WdfFdoQueryForInterface(
        Device,
        &GUID_BTHDDI_PROFILE_DRIVER_INTERFACE,
        (PINTERFACE)&ctx->BthInterface.Interface,
        sizeof(BTH_PROFILE_DRIVER_INTERFACE),
        BTHDDI_PROFILE_DRIVER_INTERFACE_VERSION_FOR_QI,
        NULL);

    if (!NT_SUCCESS(status)) {
        KdPrint(("LibrePodsAAP: QueryInterface(PROFILE_DRIVER_INTERFACE) failed 0x%08X\n", status));
        ctx->HasBthInterface = FALSE;
        return status; // fatal: without it we cannot allocate/submit BRBs
    }

    ctx->HasBthInterface = TRUE;
    KdPrint(("LibrePodsAAP: acquired BTH profile interface\n"));
    return STATUS_SUCCESS;
}

NTSTATUS
LpEvtDeviceReleaseHardware(
    _In_ WDFDEVICE    Device,
    _In_ WDFCMRESLIST ResourcesTranslated
)
{
    PDEVICE_CONTEXT ctx = DeviceGetContext(Device);
    UNREFERENCED_PARAMETER(ResourcesTranslated);

    if (ctx->State != LpDisconnected) {
        LpDisconnect(ctx);
    }
    return STATUS_SUCCESS;
}

VOID
LpEvtDeviceContextCleanup(
    _In_ WDFOBJECT Object
)
{
    PDEVICE_CONTEXT ctx = DeviceGetContext((WDFDEVICE)Object);

    if (ctx->State != LpDisconnected) {
        LpDisconnect(ctx);
    }
}
