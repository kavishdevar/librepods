/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    CircuitHelper.cpp

Abstract:

   This module contains helper functions for circuits.

Environment:

    Kernel mode

--*/

#include "private.h"
#include "public.h"
#include "CircuitHelper.h"

#ifndef __INTELLISENSE__
#include "CircuitHelper.tmh"
#endif

const ULONG _DSP_STREAM_PROPERTY_UI4_VALUE = 1;

PAGED_CODE_SEG
NTSTATUS AllocateFormat(
    _In_ KSDATAFORMAT_WAVEFORMATEXTENSIBLE      WaveFormat,
    _In_ ACXCIRCUIT                             Circuit,
    _In_ WDFDEVICE                              Device,
    _Out_ ACXDATAFORMAT*                        Format
)
{
    PAGED_CODE();

    NTSTATUS status = STATUS_SUCCESS;

    WDF_OBJECT_ATTRIBUTES attributes;
    WDF_OBJECT_ATTRIBUTES_INIT(&attributes);

    ACX_DATAFORMAT_CONFIG formatCfg;
    ACX_DATAFORMAT_CONFIG_INIT_KS(&formatCfg, &WaveFormat);
    WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, FORMAT_CONTEXT);
    attributes.ParentObject = Circuit;

    //
    // Creates an ACXDATAFORMAT handle for the given wave format.
    //
    RETURN_NTSTATUS_IF_FAILED(AcxDataFormatCreate(Device, &attributes, &formatCfg, Format));

    ASSERT((*Format) != NULL);
    FORMAT_CONTEXT* formatCtx;
    formatCtx = GetFormatContext(*Format);
    ASSERT(formatCtx);
    UNREFERENCED_PARAMETER(formatCtx);

    return status;
}

PAGED_CODE_SEG
NTSTATUS CreateStreamBridge(
    _In_ ACX_STREAM_BRIDGE_CONFIG StreamCfg,
    _In_ ACXCIRCUIT Circuit,
    _In_ ACXPIN Pin,
    _In_ DSP_PIN_CONTEXT* PinCtx,
    _In_ BOOL Render)
{
    PAGED_CODE();

    NTSTATUS status = STATUS_SUCCESS;

    WDF_OBJECT_ATTRIBUTES attributes;
    WDF_OBJECT_ATTRIBUTES_INIT(&attributes);

    attributes.ParentObject = Pin;

    ACX_OBJECTBAG_CONFIG objBagCfg;
    ACXOBJECTBAG objBag = NULL;
    ACX_OBJECTBAG_CONFIG_INIT(&objBagCfg);
    WDF_OBJECT_ATTRIBUTES_INIT(&attributes);
    attributes.ParentObject = Circuit;
    RETURN_NTSTATUS_IF_FAILED(AcxObjectBagCreate(&attributes, &objBagCfg, &objBag));

    DECLARE_CONST_ACXOBJECTBAG_DRIVER_PROPERTY_NAME(msft, TestUI4);
    RETURN_NTSTATUS_IF_FAILED(AcxObjectBagAddUI4(objBag, &TestUI4, _DSP_STREAM_PROPERTY_UI4_VALUE));

    //
    // Add a stream BRIDGE.
    //
    PCGUID inModes[] = {
        &AUDIO_SIGNALPROCESSINGMODE_RAW,
        &AUDIO_SIGNALPROCESSINGMODE_DEFAULT,
    };

    if (Render)
    {
        StreamCfg.InModesCount = SIZEOF_ARRAY(inModes);
        StreamCfg.InModes = inModes;
    }

    //
    // Do not specify InModes for capture - this will prevent the ACX framework from adding created streams to this stream
    // bridge automatically. We want to add the stream bridges manually since we don't want KWS streams added.
    //
    StreamCfg.OutMode = &AUDIO_SIGNALPROCESSINGMODE_RAW;
    StreamCfg.OutStreamVarArguments = objBag;

    //
    // Uncomment this line to reverse the change-state sequence notifications.
    //
    // streamCfg.Flags       |= AcxStreamBridgeInvertChangeStateSequence;

    ACXSTREAMBRIDGE streamBridge = NULL;
    RETURN_NTSTATUS_IF_FAILED(AcxStreamBridgeCreate(Circuit, &attributes, &StreamCfg, &streamBridge));

    if (!Render)
    {
        PinCtx->HostStreamBridge = streamBridge;
    }

    RETURN_NTSTATUS_IF_FAILED(AcxPinAddStreamBridges(Pin, &streamBridge, 1));

    return status;
}

PAGED_CODE_SEG
NTSTATUS ConnectRenderCircuitElements(
    _In_ ACXAUDIOENGINE                          AudioEngineElement,
    _In_ ACXCIRCUIT                              Circuit
)
{
    PAGED_CODE();

    NTSTATUS status = STATUS_SUCCESS;

    //
    // Explicitly connect the circuit/elements. Note that driver doesn't 
    // need to perform this step when circuit/elements are connected in the 
    // same order as they were added to the circuit. By default ACX connects
    // the elements starting from the sink circuit pin and ending with the 
    // source circuit pin for both render and capture devices.
    //
    //                           Circuit layout
    //              -----------------------------------------
    //              |                                       |
    //              |        --------------------           |
    //     Host -0->|-----1->|                  |-0-------->|-3-> Bridge Pin
    //              |        |  Audio Engine    |           |
    //  Offload -1->|-----2->|      Node        |-3--|      |
    //              |        |------------------|    |      |
    //              |                                |      |
    // Loopback <-2-|<------------------------------ |      |
    //              |                                       |
    //              |                                       |
    //              |---------------------------------------|
    //

    ACX_CONNECTION connections[4];

    ACX_CONNECTION_INIT(&connections[0], Circuit, AudioEngineElement);
    connections[0].FromPin.Id = RenderHostPin;
    connections[0].ToPin.Id = 1;

    ACX_CONNECTION_INIT(&connections[1], Circuit, AudioEngineElement);
    connections[1].FromPin.Id = RenderOffloadPin;
    connections[1].ToPin.Id = 2;

    ACX_CONNECTION_INIT(&connections[2], AudioEngineElement, Circuit);
    connections[2].ToPin.Id = RenderLoopbackPin;
    connections[2].FromPin.Id = 3;

    ACX_CONNECTION_INIT(&connections[3], AudioEngineElement, Circuit);
    connections[3].ToPin.Id = RenderBridgePin;
    connections[3].FromPin.Id = 0;

    //
    // Add the connections linking circuit to elements.
    //
    RETURN_NTSTATUS_IF_FAILED(AcxCircuitAddConnections(Circuit, connections, SIZEOF_ARRAY(connections)));

    return status;

}

PAGED_CODE_SEG
NTSTATUS
Codec_GetModeFromAttributeList(
    _In_    const PKSMULTIPLE_ITEM  Attributes,
    _In_    ULONG                   AttributesSize,
    _Out_   GUID                  * SignalProcessingMode
    )
{
    NTSTATUS        status          = STATUS_NO_MORE_ENTRIES;
    PKSATTRIBUTE    attributeHeader = NULL;
    PAGED_CODE();
    *SignalProcessingMode = AUDIO_SIGNALPROCESSINGMODE_DEFAULT;
    
    status = FindKsAttributeById(Attributes, 
                                 AttributesSize, 
                                 &KSATTRIBUTEID_AUDIOSIGNALPROCESSING_MODE,
                                 sizeof(KSATTRIBUTE_AUDIOSIGNALPROCESSING_MODE),
                                 &attributeHeader);
    if (!NT_SUCCESS(status))
    {
        goto exit;
    }
   
    ASSERT(attributeHeader->Attribute == KSATTRIBUTEID_AUDIOSIGNALPROCESSING_MODE);
    ASSERT(attributeHeader->Size == sizeof(KSATTRIBUTE_AUDIOSIGNALPROCESSING_MODE));
    
    KSATTRIBUTE_AUDIOSIGNALPROCESSING_MODE* signalProcessingModeAttribute;
    signalProcessingModeAttribute = (KSATTRIBUTE_AUDIOSIGNALPROCESSING_MODE*)attributeHeader;
    *SignalProcessingMode = signalProcessingModeAttribute->SignalProcessingMode;
    status = STATUS_SUCCESS;
exit:
    return status;
}

struct AFX_FIND_KSATTRIBUTE_BY_ID
{
    const _GUID *   Id;
    ULONG           Size;
    PKSATTRIBUTE    Attribute;
};

PAGED_CODE_SEG
NTSTATUS
FindKsAttributeByIdVisitor(
    _In_  PKSATTRIBUTE  AttributeHeader,
    _In_  PVOID         Context,
    _Out_ BOOLEAN     * bContinue
    )
{
    NTSTATUS status = STATUS_SUCCESS;
    AFX_FIND_KSATTRIBUTE_BY_ID * ctx = (AFX_FIND_KSATTRIBUTE_BY_ID *)Context;
    PAGED_CODE();
    // Default: continue searching.
    *bContinue = TRUE;
    
    if (IsEqualGUIDAligned(AttributeHeader->Attribute, *ctx->Id))
    {
        // Validate its size. 
        if (AttributeHeader->Size < ctx->Size)
        {
            status = STATUS_INVALID_PARAMETER;
            goto exit;
        }
            
        ctx->Attribute = AttributeHeader; 
        *bContinue = FALSE;
    }
exit:
    return status;
}

PAGED_CODE_SEG
NTSTATUS
FindKsAttributeById(
    _In_  const PKSMULTIPLE_ITEM Attributes,
    _In_  ULONG                  AttributesSize,
    _In_  const _GUID *          AttributeId,
    _In_  ULONG                  AttributeSize,
    _Out_ PKSATTRIBUTE         * AttributeHeader
    )
{
    PAGED_CODE();
    NTSTATUS status;
    AFX_FIND_KSATTRIBUTE_BY_ID ctx = {0}; 
    *AttributeHeader = NULL;
    ctx.Id          = AttributeId;
    ctx.Size        = AttributeSize;
    status = TraverseKsAttributeList(Attributes, AttributesSize, FindKsAttributeByIdVisitor, &ctx);
    if (!NT_SUCCESS(status))
    {
        goto exit;
    }
    if (ctx.Attribute == NULL)
    {
        status = STATUS_NO_MORE_ENTRIES;
        goto exit;
    }
    *AttributeHeader = ctx.Attribute;
exit:
    return status;
}

PAGED_CODE_SEG
NTSTATUS
TraverseKsAttributeList(
    _In_ const PKSMULTIPLE_ITEM         Attributes,
    _In_ ULONG                          AttributesSize,
    _In_ PFN_KSATTRIBUTES_VISITOR       Visitor,
    _In_ PVOID                          Context
    )
{
    PAGED_CODE();
    NTSTATUS        status          = STATUS_SUCCESS;
    ULONG           cbRemaining     = 0;
    PKSATTRIBUTE    attributeHeader = NULL;
    ASSERT(Visitor);
    //
    // Note: multiple-item ptr must have a FILE_QUAD_ALIGNMENT only in relation to the Pin's format size.
    // I.e., it is not guaranteed that the attributes are FILE_QUAD_ALIGNMENT in memory, unless the caller reallocates
    // them before calling this function (which is currently not done by anyone in any driver/sample).
    // 
    //ASSERT((ULONG_PTR)Attributes == (((ULONG_PTR)Attributes + FILE_QUAD_ALIGNMENT) & ~FILE_QUAD_ALIGNMENT));
    if (AttributesSize < sizeof(KSMULTIPLE_ITEM))
    {
        status = STATUS_INVALID_PARAMETER;
        goto exit;
    }
    if (Attributes->Size < sizeof(KSMULTIPLE_ITEM))
    {
        status = STATUS_INVALID_PARAMETER;
        goto exit;
    }
    if (AttributesSize < Attributes->Size)
    {
        status = STATUS_INVALID_PARAMETER;
        goto exit;
    }
    
    cbRemaining = Attributes->Size;
    //
    // Init ptr / size of attributes of list.
    //
    cbRemaining -= sizeof(KSMULTIPLE_ITEM);
    attributeHeader = (PKSATTRIBUTE)(Attributes + 1);
    for (ULONG i = 0; i < Attributes->Count; i++)
    {
        BOOLEAN bContinue   = TRUE;
        size_t  cbAttribute = 0;
        
        if (cbRemaining < sizeof(KSATTRIBUTE))
        {
            status = STATUS_INVALID_PARAMETER;
            goto exit;
        }
        if (attributeHeader->Size < sizeof(KSATTRIBUTE))
        {
            status = STATUS_INVALID_PARAMETER;
            goto exit;
        }
        if (cbRemaining < attributeHeader->Size)
        {
            status = STATUS_INVALID_PARAMETER;
            goto exit;
        }
        //
        // Invoke the descriptor enumeration routine.
        //
        status = Visitor(attributeHeader, Context, &bContinue);
        if (!NT_SUCCESS(status))
        {
            goto exit;
        }
        
        if (!bContinue)
        {
            break;
        }
        //
        // Adjust pointer and buffer size to next attribute (QWORD aligned)
        //
        cbAttribute = CODEC_ALIGN_SIZE_UP_CONSTANT(attributeHeader->Size, FILE_QUAD_ALIGNMENT);
        if (cbRemaining < cbAttribute)
        {
            //
            // Out of buffer, check if this was the last attribute.
            //
            if (i + 1 != Attributes->Count)
            {
                //
                // This was not the last attribute, nevertheless there is no more buffer, error out.
                //
                status = STATUS_INVALID_PARAMETER;
                goto exit;
            }
            // This was the final attribute. Exit loop with success status.
            break;
        }
        // Check next attribute.
        attributeHeader = (PKSATTRIBUTE)(((PBYTE)attributeHeader) + cbAttribute);
        cbRemaining -= (ULONG)cbAttribute;
    }
    // Normalize success code.
    status = STATUS_SUCCESS;
exit:
    return status;
}

PAGED_CODE_SEG
NTSTATUS
EvtJackRetrievePresence(
    _In_    ACXJACK     Jack,
    _In_    PBOOLEAN    IsConnected
)
{
    PAGED_CODE();

    UNREFERENCED_PARAMETER(Jack);
    UNREFERENCED_PARAMETER(IsConnected);

    NTSTATUS                status = STATUS_SUCCESS;

    //
    // Because this is a sample we always return true (jack is present). A real driver should check
    // if the device is actually present before returning true. 
    //
    *IsConnected = true;

    return status;
}

PAGED_CODE_SEG
NTSTATUS
CreateAudioJack(
    _In_ ULONG                          ChannelMapping,
    _In_ ULONG                          Color,
    _In_ ACX_JACK_CONNECTION_TYPE       ConnectionType,
    _In_ ACX_JACK_GEO_LOCATION          GeoLocation,
    _In_ ACX_JACK_GEN_LOCATION          GenLocation,
    _In_ ACX_JACK_PORT_CONNECTION       PortConnection,
    _In_ ULONG                          Flags,
    _In_ ACXPIN                         BridgePin
    )
{
    PAGED_CODE();

    NTSTATUS status = STATUS_SUCCESS;
    ACX_JACK_CONFIG jackCfg;
    ACXJACK jack;
    PJACK_CONTEXT jackCtx;
    ACX_JACK_CALLBACKS jackCallbacks;

    WDF_OBJECT_ATTRIBUTES attributes;
    WDF_OBJECT_ATTRIBUTES_INIT(&attributes);

    ACX_JACK_CONFIG_INIT(&jackCfg);
    jackCfg.Description.ChannelMapping = ChannelMapping;
    jackCfg.Description.Color = Color;
    jackCfg.Description.ConnectionType = ConnectionType;
    jackCfg.Description.GeoLocation = GeoLocation;
    jackCfg.Description.GenLocation = GenLocation;
    jackCfg.Description.PortConnection = PortConnection;
    jackCfg.Flags = Flags;

    ACX_JACK_CALLBACKS_INIT(&jackCallbacks);
    jackCallbacks.EvtAcxJackRetrievePresenceState = EvtJackRetrievePresence;
    jackCfg.Callbacks = &jackCallbacks;

    WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&attributes, JACK_CONTEXT);
    attributes.ParentObject = BridgePin;

    status = AcxJackCreate(BridgePin, &attributes, &jackCfg, &jack);
    if (!NT_SUCCESS(status))
    {
        goto exit;
    }

    ASSERT(jack != nullptr);

    jackCtx = GetJackContext(jack);
    ASSERT(jackCtx);
    jackCtx->Dummy = 0;

    status = AcxPinAddJacks(BridgePin, &jack, 1);

exit:
    return status;
}

PAGED_CODE_SEG
VOID
CpuResourcesCallbackHelper
( 
    _In_    WDFOBJECT    Object,
    _In_    WDFREQUEST   Request,
    _In_    ACXELEMENT   Element
)
{
    NTSTATUS                    ntStatus = STATUS_NOT_SUPPORTED;
    ULONG_PTR                   outDataCb = 0;
    ACX_REQUEST_PARAMETERS      params;
    ULONG                       minSize = sizeof(ULONG);

    PAGED_CODE();

    ACX_REQUEST_PARAMETERS_INIT(&params);
    AcxRequestGetParameters(Request, &params);

    if ((params.Type != AcxRequestTypeProperty) ||
        (params.Parameters.Property.ItemType != AcxItemTypeElement))
    {
        // Return to acx
        (VOID) AcxCircuitDispatchAcxRequest((ACXCIRCUIT)Object, Request);
        Request = NULL;
        goto exit; 
    }

    if (Element == NULL)
    {
        ntStatus = STATUS_NOT_SUPPORTED;
        goto exit;
    }

    ULONG elementId = params.Parameters.Property.ItemId;
    ULONG currentElementId = AcxElementGetId(Element);
    ULONG valueCb = params.Parameters.Property.ValueCb;

    if (valueCb != 0)
    {
        if (params.Parameters.Property.Value == NULL)
        {
            ntStatus = STATUS_BUFFER_TOO_SMALL;
            goto exit;
        }
    }

    //
    // Check to see if the current node is the peakmeter node, if not then return the call to ACX
    //
    if (elementId != currentElementId)
    {
        (VOID) AcxCircuitDispatchAcxRequest((ACXCIRCUIT)Object, Request);
        Request = NULL;
        goto exit; 
    }

    if (params.Parameters.Property.Verb == AcxPropertyVerbGet)
    {

        if (valueCb == 0)
        {
            outDataCb = minSize;
            ntStatus = STATUS_BUFFER_OVERFLOW;
            goto exit;
        }
        else if (valueCb < minSize)
        {
            outDataCb = 0;
            ntStatus = STATUS_BUFFER_TOO_SMALL;
            goto exit;
        }
        else
        {
            *((PULONG)params.Parameters.Property.Value) = KSAUDIO_CPU_RESOURCES_NOT_HOST_CPU;
            params.Parameters.Property.ValueCb = sizeof(ULONG);
            outDataCb = params.Parameters.Property.ValueCb;
            ntStatus = STATUS_SUCCESS;
        }
    }
    else if (params.Parameters.Property.Verb == AcxPropertyVerbBasicSupport)
    {
        if ((valueCb != sizeof(ULONG)) && (valueCb != sizeof(KSPROPERTY_DESCRIPTION)))
        {
            outDataCb = minSize;
            ntStatus = STATUS_BUFFER_OVERFLOW;
            goto exit;
        }

        if (valueCb >= sizeof(KSPROPERTY_DESCRIPTION))
        {
            // if return buffer can hold a KSPROPERTY_DESCRIPTION, return it
            //
            PKSPROPERTY_DESCRIPTION PropDesc = (PKSPROPERTY_DESCRIPTION)params.Parameters.Property.Value;

            PropDesc->AccessFlags = KSPROPERTY_TYPE_BASICSUPPORT | KSPROPERTY_TYPE_GET;
            PropDesc->DescriptionSize = sizeof(KSPROPERTY_DESCRIPTION);
            PropDesc->PropTypeSet.Set = KSPROPTYPESETID_General;
            PropDesc->PropTypeSet.Id = VT_UI4;
            PropDesc->PropTypeSet.Flags = 0;
            PropDesc->MembersListCount = 0;
            PropDesc->Reserved = 0;
            outDataCb = sizeof(KSPROPERTY_DESCRIPTION);
            ntStatus = STATUS_SUCCESS;
        }
        else if (valueCb >= sizeof(ULONG))
        {
            // if return buffer can hold a ULONG, return the access flags
            //
            *((PULONG)params.Parameters.Property.Value) = KSPROPERTY_TYPE_BASICSUPPORT | KSPROPERTY_TYPE_GET;
            outDataCb = minSize;
            ntStatus = STATUS_SUCCESS; 
        }
        else if (valueCb > 0)
        {
            outDataCb = 0;
            ntStatus = STATUS_BUFFER_TOO_SMALL;
        }
        else
        {
            outDataCb = minSize;
            ntStatus = STATUS_BUFFER_OVERFLOW;
        }
    }
    else
    {
        //
        // Just give it back to ACX. After this call the request is gone.
        //
        (VOID) AcxCircuitDispatchAcxRequest((ACXCIRCUIT)Object, Request);
        Request = NULL;
        goto exit; 
    }

exit:
    if (Request != NULL)
    {
        WdfRequestCompleteWithInformation(Request, ntStatus, outDataCb);
    }
} // EvtAudioCpuResourcesCallback
