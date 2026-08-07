/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    RenderCircuit.cpp

Abstract:

    Render Circuit. This file contains routines to create and handle
    render circuit with no offload.

Environment:

    Kernel mode

--*/

#include "private.h"
#include "public.h"
#include <ks.h>
#include <mmsystem.h>
#include <ksmedia.h>
#include "AudioFormats.h"
#include "streamengine.h"
#include "cpp_utils.h"
#include "circuithelper.h"

#ifndef __INTELLISENSE__
#include "renderCircuit.tmh"
#endif

PAGED_CODE_SEG
NTSTATUS
CodecR_EvtAcxPinSetDataFormat(
    _In_    ACXPIN          Pin,
    _In_    ACXDATAFORMAT   DataFormat
)
/*++

Routine Description:

    This ACX pin callback sets the device/mixed format. 

Return Value:

    NTSTATUS

--*/
{
    UNREFERENCED_PARAMETER(Pin);
    UNREFERENCED_PARAMETER(DataFormat);

    PAGED_CODE();

    // NOTE: update device/mixed format here.

    return STATUS_NOT_SUPPORTED;
}

///////////////////////////////////////////////////////////
//
// For more information on mute element see: https://docs.microsoft.com/en-us/windows-hardware/drivers/audio/ksnodetype-mute
//
_Use_decl_annotations_
NTSTATUS
NTAPI
CodecR_EvtMuteAssignState(
    _In_ ACXMUTE    Mute,
    _In_ ULONG      Channel,
    _In_ ULONG      State
)
{
    PMUTE_ELEMENT_CONTEXT muteCtx;
    ULONG                       i;

    PAGED_CODE();

    muteCtx = GetMuteElementContext(Mute);
    ASSERT(muteCtx);

    //
    // Use first channel for all channels setting.
    //
    if (Channel != ALL_CHANNELS_ID)
    {
        muteCtx->MuteState[Channel] = State;
    }
    else
    {
        for (i = 0; i < MAX_CHANNELS; ++i)
        {
            muteCtx->MuteState[i] = State;
        }
    }

    return STATUS_SUCCESS;
}

_Use_decl_annotations_
NTSTATUS
NTAPI
CodecR_EvtMuteRetrieveState(
    _In_  ACXMUTE   Mute,
    _In_  ULONG     Channel,
    _Out_ ULONG *   State
)
{
    PMUTE_ELEMENT_CONTEXT muteCtx;

    PAGED_CODE();

    muteCtx = GetMuteElementContext(Mute);
    ASSERT(muteCtx);

    //
    // Use first channel for all channels setting.
    //
    if (Channel != ALL_CHANNELS_ID)
    {
        *State = muteCtx->MuteState[Channel];
    }
    else
    {
        *State = muteCtx->MuteState[0];
    }

    return STATUS_SUCCESS;
}

///////////////////////////////////////////////////////////
//
// For more information on volume element see: https://docs.microsoft.com/en-us/windows-hardware/drivers/audio/ksnodetype-volume
//
_Use_decl_annotations_
NTSTATUS
NTAPI
CodecR_EvtRampedVolumeAssignLevel(
    _In_ ACXVOLUME              Volume,
    _In_ ULONG                  Channel,
    _In_ LONG                   VolumeLevel,
    _In_ ACX_VOLUME_CURVE_TYPE  CurveType,
    _In_ ULONGLONG              CurveDuration
)
{
    PVOLUME_ELEMENT_CONTEXT         volumeCtx;
    ULONG                           i;

    PAGED_CODE();

    UNREFERENCED_PARAMETER(CurveType);
    UNREFERENCED_PARAMETER(CurveDuration);

    volumeCtx = GetVolumeElementContext(Volume);
    ASSERT(volumeCtx);

    if (Channel != ALL_CHANNELS_ID)
    {
        volumeCtx->VolumeLevel[Channel] = VolumeLevel;
    }
    else
    {
        for (i = 0; i < MAX_CHANNELS; ++i)
        {
            volumeCtx->VolumeLevel[i] = VolumeLevel;
        }
    }

    return STATUS_SUCCESS;
}

_Use_decl_annotations_
NTSTATUS
NTAPI
CodecR_EvtVolumeRetrieveLevel(
    _In_  ACXVOLUME Volume,
    _In_  ULONG     Channel,
    _Out_ LONG *    VolumeLevel
)
{
    PVOLUME_ELEMENT_CONTEXT   volumeCtx;

    PAGED_CODE();

    volumeCtx = GetVolumeElementContext(Volume);
    ASSERT(volumeCtx);

    if (Channel != ALL_CHANNELS_ID)
    {
        *VolumeLevel = volumeCtx->VolumeLevel[Channel];
    }
    else
    {
        *VolumeLevel = volumeCtx->VolumeLevel[0];
    }

    return STATUS_SUCCESS;
}

VOID
CodecR_EvtPinContextCleanup(
    _In_ WDFOBJECT      WdfPin
)
/*++

Routine Description:

    In this callback, it cleans up pin context.

Arguments:

    WdfDevice - WDF device object

Return Value:

    nullptr

--*/
{

    UNREFERENCED_PARAMETER(WdfPin);
}

PAGED_CODE_SEG
NTSTATUS
CodecR_AddStaticRender(
    _In_ WDFDEVICE              Device,
    _In_ const GUID *           ComponentGuid,
    _In_ const UNICODE_STRING * CircuitName
)
/*++

Routine Description:

    Creates the static render circuit (pictured below) and
    adds it to the device context. This is called when a
    new device is detected and the AddDevice call is made
    by the pnp manager.

    ***************************************************************************
    * Render Circuit                                                          *
    *                                                                         *
    *              +--------------------------------------------+             *
    *              |                                            |             *
    *              |    +-------------+      +-------------+    |             *
    * Host  ------>|    | Volume Node |      |  Mute Node  |    |---> Bridge  *
    * Pin          |    +-------------+      +-------------+    |      Pin    *
    *              |                                            |             *
    *              +--------------------------------------------+             *
    *                                                                         *
    ***************************************************************************

Return Value:

    NTSTATUS

--*/
{
    NTSTATUS                        status = STATUS_SUCCESS;
    PCODEC_DEVICE_CONTEXT           devCtx;
    PRENDER_DEVICE_CONTEXT          renderDevCtx;
    ACXCIRCUIT                      renderCircuit = nullptr;
    WDF_OBJECT_ATTRIBUTES           attributes;

    PAGED_CODE();

    devCtx = GetCodecDeviceContext(Device);
    ASSERT(devCtx != nullptr);

    //
    // Alloc audio context to current device.
    //
    WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, RENDER_DEVICE_CONTEXT);
    RETURN_NTSTATUS_IF_FAILED(WdfObjectAllocateContext(Device, &attributes, (PVOID*)&renderDevCtx));
    ASSERT(renderDevCtx);

    //
    // Create a render circuit associated with this child device.
    //
    RETURN_NTSTATUS_IF_FAILED(CodecR_CreateRenderCircuit(Device, ComponentGuid, CircuitName, &renderCircuit));

    devCtx->Render = renderCircuit;

    return status;
}

PAGED_CODE_SEG
NTSTATUS
Render_AllocateSupportedFormats(
    _In_                                  WDFDEVICE        Device,
    _In_reads_bytes_(CodecRenderPinCount) ACXPIN           Pin[],
    _In_                                  ACXCIRCUIT       Circuit,
    _In_                                  size_t           CodecRenderPinCount
)
{
    UNREFERENCED_PARAMETER(CodecRenderPinCount);

    NTSTATUS status = STATUS_SUCCESS;
    ACXDATAFORMAT formatPcm44100c2;
    ACXDATAFORMAT formatPcm48000c2;
    ACXDATAFORMATLIST formatList;

    WDF_OBJECT_ATTRIBUTES attributes;
    WDF_OBJECT_ATTRIBUTES_INIT(&attributes);

    ///////////////////////////////////////////////////////////
    //
    // Allocate the formats this circuit supports.
    //

    RETURN_NTSTATUS_IF_FAILED(AllocateFormat(Pcm44100c2, Circuit, Device, &formatPcm44100c2));
    RETURN_NTSTATUS_IF_FAILED(AllocateFormat(Pcm48000c2, Circuit, Device, &formatPcm48000c2));

    ///////////////////////////////////////////////////////////
    //
    // Define supported formats for the host pin.
    //

    //
    // The raw processing mode list is associated with each single circuit
    // by ACX. The driver uses this DDI to retrieve the built-in raw
    // data-format list.
    //
    RETURN_NTSTATUS_IF_TRUE(CodecRenderHostPin >= CodecRenderPinCount, STATUS_INVALID_PARAMETER);
    formatList = AcxPinGetRawDataFormatList(Pin[CodecRenderHostPin]);
    RETURN_NTSTATUS_IF_TRUE(formatList == nullptr, STATUS_INSUFFICIENT_RESOURCES);

    //
    // The driver uses this DDI to add data formats to the raw
    // processing mode list associated with the current circuit.
    //
    RETURN_NTSTATUS_IF_FAILED(AcxDataFormatListAddDataFormat(formatList, formatPcm44100c2));
    RETURN_NTSTATUS_IF_FAILED(AcxDataFormatListAddDataFormat(formatList, formatPcm48000c2));

    return status;
}

PAGED_CODE_SEG
NTSTATUS
CodecR_CreateRenderCircuit(
    _In_     WDFDEVICE              Device,
    _In_     const GUID *           ComponentGuid,
    _In_     const UNICODE_STRING * CircuitName,
    _Out_    ACXCIRCUIT*            Circuit
)
/*++

Routine Description:

    This routine builds the CODEC render circuit.

Return Value:

    NT status value

--*/
{
    NTSTATUS                        status = STATUS_SUCCESS;
    WDF_OBJECT_ATTRIBUTES           attributes;
    ACXCIRCUIT                      circuit;
    CODEC_RENDER_CIRCUIT_CONTEXT*   circuitCtx;
    ACXPIN                          pin[CodecRenderPinCount];

    PAGED_CODE();

    //
    // Init output value.
    //
    *Circuit = nullptr;

    ///////////////////////////////////////////////////////////
    //
    // Create a circuit.
    //
    {
        PACXCIRCUIT_INIT                circuitInit = nullptr;
        ACX_CIRCUIT_PNPPOWER_CALLBACKS  powerCallbacks;

        //
        // The driver uses this DDI to allocate an ACXCIRCUIT_INIT
        // structure. This opaque structure is used when creating
        // a standalone audio circuit representing an audio device. 
        //
        circuitInit = AcxCircuitInitAllocate(Device);

        //
        // The driver uses this DDI to free the allocated
        // ACXCIRCUIT_INIT structure when an error is detected.
        // Normally the structures is deleted/cleared by ACX when
        // an ACX circuit is created successfully.
        //
        auto circuitInitScope = scope_exit([&circuitInit]() {
            if (circuitInit) {
                AcxCircuitInitFree(circuitInit);
            }
            });

        //
        // The driver uses this DDI to specify the Component ID
        // of the ACX circuit. This ID is a guid that uniquely
        // identifies the circuit instance (vendor specific).
        //
        AcxCircuitInitSetComponentId(circuitInit, ComponentGuid);

        //
        // The driver uses this DDI to specify the circuit name.
        // For standalone circuits, this is the audio device name
        // which is used by clients to open handles to the audio devices.
        //
        (VOID)AcxCircuitInitAssignName(circuitInit, CircuitName);

        //
        // The driver uses this DDI to specify the circuit type. The
        // circuit type can be AcxCircuitTypeRender, AcxCircuitTypeCapture,
        // AcxCircuitTypeOther, or AcxCircuitTypeMaximum (for validation). 
        //
        AcxCircuitInitSetCircuitType(circuitInit, AcxCircuitTypeRender);

        //
        // The driver uses this DDI to assign its (if any) power callbacks.
        //
        ACX_CIRCUIT_PNPPOWER_CALLBACKS_INIT(&powerCallbacks);
        powerCallbacks.EvtAcxCircuitPowerUp = CodecR_EvtCircuitPowerUp;
        powerCallbacks.EvtAcxCircuitPowerDown = CodecR_EvtCircuitPowerDown;
        AcxCircuitInitSetAcxCircuitPnpPowerCallbacks(circuitInit, &powerCallbacks);

        //
        // The driver uses this DDI to register for a stream-create callback.
        //
        RETURN_NTSTATUS_IF_FAILED(AcxCircuitInitAssignAcxCreateStreamCallback(circuitInit, CodecR_EvtCircuitCreateStream));

        //
        // The driver uses this DDI to create a new ACX circuit.
        //
        WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, CODEC_RENDER_CIRCUIT_CONTEXT);
        RETURN_NTSTATUS_IF_FAILED(AcxCircuitCreate(Device, &attributes, &circuitInit, &circuit));

        circuitInitScope.release();

        circuitCtx = GetRenderCircuitContext(circuit);
        ASSERT(circuitCtx);
    }

    //
    // Post circuit creation initialization.
    //

    ///////////////////////////////////////////////////////////
    //
    // Create mute and volume elements.
    //
    {
        ACXELEMENT elements[RenderElementCount] = { 0 };

        //
        // The driver uses this DDI to assign its volume element callbacks.
        //
        ACX_VOLUME_CALLBACKS volumeCallbacks;
        ACX_VOLUME_CALLBACKS_INIT(&volumeCallbacks);
        volumeCallbacks.EvtAcxRampedVolumeAssignLevel = CodecR_EvtRampedVolumeAssignLevel;
        volumeCallbacks.EvtAcxVolumeRetrieveLevel = CodecR_EvtVolumeRetrieveLevel;

        //
        // Create Volume element
        //
        ACX_VOLUME_CONFIG volumeCfg;
        ACX_VOLUME_CONFIG_INIT(&volumeCfg);
        volumeCfg.ChannelsCount = MAX_CHANNELS;
        volumeCfg.Minimum = VOLUME_LEVEL_MINIMUM;
        volumeCfg.Maximum = VOLUME_LEVEL_MAXIMUM;
        volumeCfg.SteppingDelta = VOLUME_STEPPING;
        volumeCfg.Name = &KSAUDFNAME_VOLUME_CONTROL;
        volumeCfg.Callbacks = &volumeCallbacks;

        WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, VOLUME_ELEMENT_CONTEXT);
        attributes.ParentObject = circuit;

        RETURN_NTSTATUS_IF_FAILED(AcxVolumeCreate(circuit, &attributes, &volumeCfg, (ACXVOLUME*)&elements[RenderVolumeIndex]));

        //
        // The driver uses this DDI to assign its mute element callbacks.
        //
        ACX_MUTE_CALLBACKS muteCallbacks;
        ACX_MUTE_CALLBACKS_INIT(&muteCallbacks);
        muteCallbacks.EvtAcxMuteAssignState = CodecR_EvtMuteAssignState;
        muteCallbacks.EvtAcxMuteRetrieveState = CodecR_EvtMuteRetrieveState;

        //
        // Create Mute element
        //
        ACX_MUTE_CONFIG muteCfg;
        ACX_MUTE_CONFIG_INIT(&muteCfg);
        muteCfg.ChannelsCount = MAX_CHANNELS;
        muteCfg.Name = &KSAUDFNAME_WAVE_MUTE;
        muteCfg.Callbacks = &muteCallbacks;

        WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, MUTE_ELEMENT_CONTEXT);
        attributes.ParentObject = circuit;

        RETURN_NTSTATUS_IF_FAILED(AcxMuteCreate(circuit, &attributes, &muteCfg, (ACXMUTE*)&elements[RenderMuteIndex]));

        //
        // Saving the volume and mute elements in the circuit context.
        //
        circuitCtx->VolumeElement = (ACXVOLUME)elements[RenderVolumeIndex];
        circuitCtx->MuteElement = (ACXMUTE)elements[RenderMuteIndex];

        //
        // The driver uses this DDI post circuit creation to add ACXELEMENTs.
        //
        RETURN_NTSTATUS_IF_FAILED(AcxCircuitAddElements(circuit, elements, SIZEOF_ARRAY(elements)));
    } 

    ///////////////////////////////////////////////////////////
    //
    // Create the pins for the circuit. 
    //
    {
        ACX_PIN_CONFIG                  pinCfg;
        CODEC_PIN_CONTEXT*              pinCtx;
        ACX_PIN_CALLBACKS               pinCallbacks;

        ///////////////////////////////////////////////////////////
        //
        // Create Render Pin. 
        //

        ACX_PIN_CALLBACKS_INIT(&pinCallbacks);
        pinCallbacks.EvtAcxPinSetDataFormat = CodecR_EvtAcxPinSetDataFormat;

        ACX_PIN_CONFIG_INIT(&pinCfg);
        pinCfg.Type = AcxPinTypeSink;
        pinCfg.Communication = AcxPinCommunicationSink;
        pinCfg.Category = &KSCATEGORY_AUDIO;
        pinCfg.PinCallbacks = &pinCallbacks;

        WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, CODEC_PIN_CONTEXT);
        attributes.EvtCleanupCallback = CodecR_EvtPinContextCleanup;
        attributes.ParentObject = circuit;

        //
        // The driver uses this DDI to create one or more pins on the circuits.
        //
        RETURN_NTSTATUS_IF_FAILED(AcxPinCreate(circuit, &attributes, &pinCfg, &pin[CodecRenderHostPin]));

        ASSERT(pin[CodecRenderHostPin] != nullptr);
        pinCtx = GetCodecPinContext(pin[CodecRenderHostPin]);
        ASSERT(pinCtx);
        pinCtx->CodecPinType = CodecPinTypeHost;

        ///////////////////////////////////////////////////////////
        //
        // Create Device Bridge Pin.
        //

        ACX_PIN_CONFIG_INIT(&pinCfg);
        pinCfg.Type = AcxPinTypeSource;
        pinCfg.Communication = AcxPinCommunicationNone;
        pinCfg.Category = &KSNODETYPE_SPEAKER;

        WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, CODEC_PIN_CONTEXT);
        attributes.EvtCleanupCallback = CodecR_EvtPinContextCleanup;
        attributes.ParentObject = circuit;

        //
        // The driver uses this DDI to create one or more pins on the circuits.
        //
        RETURN_NTSTATUS_IF_FAILED(AcxPinCreate(circuit, &attributes, &pinCfg, &pin[CodecRenderBridgePin]));

        ASSERT(pin[CodecRenderBridgePin] != nullptr);
        pinCtx = GetCodecPinContext(pin[CodecRenderBridgePin]);
        ASSERT(pinCtx);
        pinCtx->CodecPinType = CodecPinTypeDevice;
    }

    ///////////////////////////////////////////////////////////
    //
    // Add audio jack to bridge pin. 
    // For more information on audio jack see: https://docs.microsoft.com/en-us/windows/win32/api/devicetopology/ns-devicetopology-ksjack_description
    //
    {
        ACX_JACK_CONFIG                 jackCfg;
        ACXJACK                         jack;
        PJACK_CONTEXT                   jackCtx;

        ACX_JACK_CONFIG_INIT(&jackCfg);
        jackCfg.Description.ChannelMapping = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT;
        jackCfg.Description.Color = RGB(0, 0, 0);
        jackCfg.Description.ConnectionType = AcxConnTypeAtapiInternal;
        jackCfg.Description.GeoLocation = AcxGeoLocFront;
        jackCfg.Description.GenLocation = AcxGenLocPrimaryBox;
        jackCfg.Description.PortConnection = AcxPortConnIntegratedDevice;

        WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, JACK_CONTEXT);
        attributes.ParentObject = pin[CodecRenderBridgePin];

        RETURN_NTSTATUS_IF_FAILED(AcxJackCreate(pin[CodecRenderBridgePin], &attributes, &jackCfg, &jack));

        ASSERT(jack != nullptr);

        jackCtx = GetJackContext(jack);
        ASSERT(jackCtx);
        jackCtx->Dummy = 0;

        RETURN_NTSTATUS_IF_FAILED(AcxPinAddJacks(pin[CodecRenderBridgePin], &jack, 1));
    }

    RETURN_NTSTATUS_IF_FAILED(Render_AllocateSupportedFormats(Device, pin, circuit, CodecRenderPinCount));

    ///////////////////////////////////////////////////////////
    //
    // The driver uses this DDI post circuit creation to add ACXPINs.
    //
    RETURN_NTSTATUS_IF_FAILED(AcxCircuitAddPins(circuit, pin, CodecRenderPinCount));

    //
    // Set output value.
    //
    *Circuit = circuit;

    //
    // Done. 
    //
    status = STATUS_SUCCESS;

    return status;
}

_Use_decl_annotations_
NTSTATUS
CodecR_EvtCircuitPowerUp(
    _In_ WDFDEVICE  Device,
    _In_ ACXCIRCUIT Circuit,
    _In_ WDF_POWER_DEVICE_STATE PreviousState
)
{
    UNREFERENCED_PARAMETER(Device);
    UNREFERENCED_PARAMETER(Circuit);
    UNREFERENCED_PARAMETER(PreviousState);

    PAGED_CODE();

    return STATUS_SUCCESS;
}

_Use_decl_annotations_
NTSTATUS
CodecR_EvtCircuitPowerDown(
    _In_ WDFDEVICE  Device,
    _In_ ACXCIRCUIT Circuit,
    _In_ WDF_POWER_DEVICE_STATE TargetState
)
{
    UNREFERENCED_PARAMETER(Device);
    UNREFERENCED_PARAMETER(Circuit);
    UNREFERENCED_PARAMETER(TargetState);

    PAGED_CODE();

    return STATUS_SUCCESS;
}

PAGED_CODE_SEG
NTSTATUS
CodecR_EvtCircuitCreateStream(
    _In_    WDFDEVICE       Device,
    _In_    ACXCIRCUIT      Circuit,
    _In_    ACXPIN          Pin,
    _In_    PACXSTREAM_INIT StreamInit,
    _In_    ACXDATAFORMAT   StreamFormat,
    _In_    const GUID*     SignalProcessingMode,
    _In_    ACXOBJECTBAG    VarArguments
)
/*++

Routine Description:

    This routine creates a stream for the specified circuit.

Return Value:

    NT status value

--*/
{
    NTSTATUS                        status;
    PRENDER_DEVICE_CONTEXT          devCtx;
    WDF_OBJECT_ATTRIBUTES           attributes;
    ACXSTREAM                       stream;
    STREAMENGINE_CONTEXT *          streamCtx;
    ACX_STREAM_CALLBACKS            streamCallbacks;
    ACX_RT_STREAM_CALLBACKS         rtCallbacks;
    CRenderStreamEngine*            renderStreamEngine = nullptr;
    CODEC_PIN_TYPE                  codecPinType;
    PCODEC_PIN_CONTEXT              pinCtx;
    ACX_PIN_TYPE                    pinType;
    CODEC_RENDER_CIRCUIT_CONTEXT*   circuitCtx;

    auto streamEngineScope = scope_exit([&renderStreamEngine]() {

        delete renderStreamEngine;

        });

    PAGED_CODE();
    UNREFERENCED_PARAMETER(SignalProcessingMode);
    UNREFERENCED_PARAMETER(VarArguments);

    ASSERT(IsEqualGUID(*SignalProcessingMode, AUDIO_SIGNALPROCESSINGMODE_RAW));

    ASSERT(Circuit != nullptr);
    circuitCtx = GetRenderCircuitContext(Circuit);
    ASSERT(circuitCtx);

    devCtx = GetRenderDeviceContext(Device);
    ASSERT(devCtx != nullptr);
    UNREFERENCED_PARAMETER(devCtx);

    pinCtx = GetCodecPinContext(Pin);
    codecPinType = pinCtx->CodecPinType;

    pinType = AcxPinGetType(Pin);

    //
    // Init streaming callbacks.
    //
    ACX_STREAM_CALLBACKS_INIT(&streamCallbacks);
    streamCallbacks.EvtAcxStreamPrepareHardware = EvtStreamPrepareHardware;
    streamCallbacks.EvtAcxStreamReleaseHardware = EvtStreamReleaseHardware;
    streamCallbacks.EvtAcxStreamRun = EvtStreamRun;
    streamCallbacks.EvtAcxStreamPause = EvtStreamPause;

    RETURN_NTSTATUS_IF_FAILED(AcxStreamInitAssignAcxStreamCallbacks(StreamInit, &streamCallbacks));

    //
    // Init RT streaming callbacks.
    //
    ACX_RT_STREAM_CALLBACKS_INIT(&rtCallbacks);
    rtCallbacks.EvtAcxStreamGetHwLatency = EvtStreamGetHwLatency;
    rtCallbacks.EvtAcxStreamAllocateRtPackets = EvtStreamAllocateRtPackets;
    rtCallbacks.EvtAcxStreamFreeRtPackets = EvtStreamFreeRtPackets;
    rtCallbacks.EvtAcxStreamSetRenderPacket = CodecR_EvtStreamSetRenderPacket;
    rtCallbacks.EvtAcxStreamGetCurrentPacket = EvtStreamGetCurrentPacket;
    rtCallbacks.EvtAcxStreamGetPresentationPosition = EvtStreamGetPresentationPosition;

    RETURN_NTSTATUS_IF_FAILED(AcxStreamInitAssignAcxRtStreamCallbacks(StreamInit, &rtCallbacks));

    //
    // Buffer notifications are supported.
    //
    AcxStreamInitSetAcxRtStreamSupportsNotifications(StreamInit);

    //
    // Create the stream.
    //
    WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, STREAMENGINE_CONTEXT);
    attributes.EvtDestroyCallback = EvtStreamDestroy;
    RETURN_NTSTATUS_IF_FAILED(AcxRtStreamCreate(Device, Circuit, &attributes, &StreamInit, &stream));

    //
    // Create the virtual streaming engine which will control
    // streaming logic for the render circuit.
    //
    renderStreamEngine = new (POOL_FLAG_NON_PAGED, DeviceDriverTag) CRenderStreamEngine(stream, StreamFormat, FALSE, NULL);
    RETURN_NTSTATUS_IF_TRUE(renderStreamEngine == nullptr, STATUS_INSUFFICIENT_RESOURCES);

    streamCtx = GetStreamEngineContext(stream);
    ASSERT(streamCtx);
    streamCtx->StreamEngine = (PVOID)renderStreamEngine;

    renderStreamEngine = nullptr;

    //
    // Done. 
    //
    status = STATUS_SUCCESS;

    return status;
}

PAGED_CODE_SEG
NTSTATUS
CodecR_EvtStreamSetRenderPacket(
    _In_ ACXSTREAM          Stream,
    _In_ ULONG              Packet,
    _In_ ULONG              Flags,
    _In_ ULONG              EosPacketLength
)
{
    PSTREAMENGINE_CONTEXT ctx;
    CRenderStreamEngine* streamEngine = nullptr;

    PAGED_CODE();

    ctx = GetStreamEngineContext(Stream);

    streamEngine = static_cast<CRenderStreamEngine*>(ctx->StreamEngine);

    return streamEngine->SetRenderPacket(Packet, Flags, EosPacketLength);
}

