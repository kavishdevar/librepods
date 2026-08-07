/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    CircuitHelper.h

Abstract:

   This module contains helper functions for endpoints.

Environment:

    Kernel mode

--*/

// size_t
// __inline
// CODEC_ALIGN_SIZE_DOWN_CONSTANT(
//     IN size_t Length,
//     IN size_t AlignTo
//     )
#define CODEC_ALIGN_SIZE_DOWN_CONSTANT(Length, AlignTo) ((Length) & ~((AlignTo)-1))

#define CODEC_ALIGN_SIZE_DOWN CODEC_ALIGN_SIZE_DOWN_CONSTANT

// size_t
// __inline
// CODEC_ALIGN_SIZE_UP_CONSTANT(
//     IN size_t Length,
//     IN size_t AlignTo
//     )
#define CODEC_ALIGN_SIZE_UP_CONSTANT(Length, AlignTo) CODEC_ALIGN_SIZE_DOWN_CONSTANT((Length) + (AlignTo)-1, (AlignTo))

#define CODEC_ALIGN_SIZE_UP CODEC_ALIGN_SIZE_UP_CONSTANT

PAGED_CODE_SEG
NTSTATUS
Codec_GetModeFromAttributeList(
    _In_    const PKSMULTIPLE_ITEM  Attributes,
    _In_    ULONG                   AttributesSize,
    _Out_   GUID                  * SignalProcessingMode
    );

//
// Enumeration visitor callback.
//
typedef
NTSTATUS
(EVT_KSATTRIBUTES_VISITOR)(
    _In_  PKSATTRIBUTE  AttributeHeader,
    _In_  PVOID         Context,
    _Out_ BOOLEAN     * bContinue
    );

typedef EVT_KSATTRIBUTES_VISITOR *PFN_KSATTRIBUTES_VISITOR;

static
PAGED_CODE_SEG
EVT_KSATTRIBUTES_VISITOR FindKsAttributeByIdVisitor;

PAGED_CODE_SEG
NTSTATUS
FindKsAttributeById(
    _In_  const PKSMULTIPLE_ITEM Attributes,
    _In_  ULONG                  AttributesSize,
    _In_  const _GUID *          AttributeId,
    _In_  ULONG                  AttributeSize,
    _Out_ PKSATTRIBUTE         * AttributeHeader
    );

PAGED_CODE_SEG
NTSTATUS
TraverseKsAttributeList(
    _In_ const PKSMULTIPLE_ITEM         Attributes,
    _In_ ULONG                          AttributesSize,
    _In_ PFN_KSATTRIBUTES_VISITOR       Visitor,
    _In_ PVOID                          Context
    );

PAGED_CODE_SEG
NTSTATUS AllocateFormat(
    _In_ KSDATAFORMAT_WAVEFORMATEXTENSIBLE      WaveFormat,
    _In_ ACXCIRCUIT                             Circuit,
    _In_ WDFDEVICE                              Device,
    _Out_ ACXDATAFORMAT*                        Format
);

PAGED_CODE_SEG
NTSTATUS CreateStreamBridge(
    _In_ ACX_STREAM_BRIDGE_CONFIG                   StreamCfg,
    _In_ ACXCIRCUIT                                 Circuit,
    _In_ ACXPIN                                     Pin,
    _In_ DSP_PIN_CONTEXT*                           PinCtx,
    _In_ BOOL                                       Render
);

PAGED_CODE_SEG
NTSTATUS ConnectRenderCircuitElements(
    _In_ ACXAUDIOENGINE                          AudioEngineElement,
    _In_ ACXCIRCUIT                              Circuit
);

PAGED_CODE_SEG
NTSTATUS CreateAudioJack(
    _In_ ULONG                          ChannelMapping,
    _In_ ULONG                          Color,
    _In_ ACX_JACK_CONNECTION_TYPE       ConnectionType,
    _In_ ACX_JACK_GEO_LOCATION          GeoLocation,
    _In_ ACX_JACK_GEN_LOCATION          GenLocation,
    _In_ ACX_JACK_PORT_CONNECTION       PortConnection,
    _In_ ULONG                          Flags,
    _In_ ACXPIN                         BridgePin
);

PAGED_CODE_SEG
NTSTATUS EvtJackRetrievePresence(
    _In_    ACXJACK     Jack,
    _In_    PBOOLEAN    IsConnected
);

PAGED_CODE_SEG
VOID CpuResourcesCallbackHelper( 
    _In_    WDFOBJECT    Object,
    _In_    WDFREQUEST   Request,
    _In_    ACXELEMENT   Element
);

