/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    Private.h

Abstract:

    Contains structure definitions and function prototypes private to
    the Common library.

Environment:

    Kernel mode

--*/

#ifndef _PRIVATE_H_
#define _PRIVATE_H_


#include <wdm.h>
#include <windef.h>
#include "cpp_utils.h"
#include <mmsystem.h>
#include <ks.h>
#include <ksmedia.h>
#include "NewDelete.h"

/* make prototypes usable from C++ */
#ifdef __cplusplus
extern "C" {
#endif

#include <initguid.h>
#include <ntddk.h>
#include <ntstrsafe.h>
#include <ntintsafe.h>
#include "Trace.h"

#include <wdf.h>
#include <acx.h>

#define PAGED_CODE_SEG __declspec(code_seg("PAGE"))
#define INIT_CODE_SEG __declspec(code_seg("INIT"))

extern const GUID DSP_CIRCUIT_SPEAKER_GUID;
extern const GUID DSP_CIRCUIT_MICROPHONE_GUID;
extern const GUID DSP_CIRCUIT_UNIVERSALJACK_RENDER_GUID;
extern const GUID DSP_CIRCUIT_UNIVERSALJACK_CAPTURE_GUID;

/////////////////////////////////////////////////////////
//
// Driver wide definitions
//

// Copied from cfgmgr32.h
#if !defined(MAX_DEVICE_ID_LEN)
#define MAX_DEVICE_ID_LEN 200
#endif

// Number of millisecs per sec. 
#define MS_PER_SEC 1000

// Number of hundred nanosecs per sec. 
#define HNS_PER_SEC 10000000

#define REQUEST_TIMEOUT_SECONDS 5

#undef MIN
#undef MAX
#define MIN(a,b) ((a) > (b) ? (b) : (a))
#define MAX(a,b) ((a) > (b) ? (a) : (b))

#ifndef BOOL
typedef int BOOL;
#endif

#ifndef SIZEOF_ARRAY
#define SIZEOF_ARRAY(ar)        (sizeof(ar)/sizeof((ar)[0]))
#endif // !defined(SIZEOF_ARRAY)

#ifndef RGB
#define RGB(r, g, b) (DWORD)(r << 16 | g << 8 | b)
#endif

#define ALL_CHANNELS_ID             UINT32_MAX
#define MAX_CHANNELS                2
    
//
// Ks support.
//
#define KSPROPERTY_TYPE_ALL         KSPROPERTY_TYPE_BASICSUPPORT | \
                                        KSPROPERTY_TYPE_GET | \
                                        KSPROPERTY_TYPE_SET

//
// Define struct to hold signal processing mode and corresponding
// list of supported formats. 
//
typedef struct
{
    GUID SignalProcessingMode;
    KSDATAFORMAT_WAVEFORMATEXTENSIBLE* FormatList;
    ULONG FormatListCount;
} SUPPORTED_FORMATS_LIST;

//
// Define CAPTURE device context.
//
typedef struct _CAPTURE_DEVICE_CONTEXT {
    ACXCIRCUIT      Circuit;
    BOOLEAN         FirstTimePrepareHardware;
} CAPTURE_DEVICE_CONTEXT, * PCAPTURE_DEVICE_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(CAPTURE_DEVICE_CONTEXT, GetCaptureDeviceContext)

//
// Define RENDER device context.
//
typedef struct _RENDER_DEVICE_CONTEXT {
    ACXCIRCUIT      Circuit;
    BOOLEAN         FirstTimePrepareHardware;
} RENDER_DEVICE_CONTEXT, * PRENDER_DEVICE_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(RENDER_DEVICE_CONTEXT, GetRenderDeviceContext)

//
// Define circuit/stream element context.
//
typedef struct _ELEMENT_CONTEXT {
    BOOLEAN         Dummy;
} ELEMENT_CONTEXT, *PELEMENT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(ELEMENT_CONTEXT, GetElementContext)
    
//
// Define circuit/stream element context.
//
typedef struct _MUTE_ELEMENT_CONTEXT {
    BOOL            MuteState[MAX_CHANNELS]; 
} MUTE_ELEMENT_CONTEXT, *PMUTE_ELEMENT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(MUTE_ELEMENT_CONTEXT, GetMuteElementContext)

//
// Define circuit/stream element context.
//
typedef struct _VOLUME_ELEMENT_CONTEXT {
    LONG            VolumeLevel[MAX_CHANNELS];
} VOLUME_ELEMENT_CONTEXT, *PVOLUME_ELEMENT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(VOLUME_ELEMENT_CONTEXT, GetVolumeElementContext)

#define VOLUME_STEPPING         0x8000
#define VOLUME_LEVEL_MAXIMUM    0x00000000
#define VOLUME_LEVEL_MINIMUM    (-96 * 0x10000)
    
//
// Define mute timer context.
//
typedef struct _MUTE_TIMER_CONTEXT {
    ACXELEMENT      MuteElement;
    ACXEVENT        Event;
} MUTE_TIMER_CONTEXT, *PMUTE_TIMER_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(MUTE_TIMER_CONTEXT, GetMuteTimerContext)

//
// Define format context.
//
typedef struct _FORMAT_CONTEXT {
    BOOLEAN         Dummy;
} FORMAT_CONTEXT, *PFORMAT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(FORMAT_CONTEXT, GetFormatContext)

//
// Define jack context.
//
typedef struct _JACK_CONTEXT {
    ULONG Dummy;
} JACK_CONTEXT, * PJACK_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(JACK_CONTEXT, GetJackContext)

//
// Define audio engine context.
//
typedef struct _ENGINE_CONTEXT {
    ACXDATAFORMAT   MixFormat;
} ENGINE_CONTEXT, * PENGINE_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(ENGINE_CONTEXT, GetEngineContext)

//
// Define stream audio engine context.
//
typedef struct _STREAMAUDIOENGINE_CONTEXT {
    BOOLEAN         Dummy;
} STREAMAUDIOENGINE_CONTEXT, * PSTREAMAUDIOENGINE_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(STREAMAUDIOENGINE_CONTEXT, GetStreamAudioEngineContext)

//
// Define keyword spotter context
//
typedef struct _KEYWORDSPOTTER_CONTEXT {
    ACXPNPEVENT Event;
    PVOID       KeywordDetector;
} KEYWORDSPOTTER_CONTEXT, * PKEYWORDSPOTTER_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(KEYWORDSPOTTER_CONTEXT, GetKeywordSpotterContext)

//
// Define pnp event context.
//
typedef struct _PNPEVENT_CONTEXT {
    BOOLEAN         Dummy;
} PNPEVENT_CONTEXT, * PPNPEVENT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(PNPEVENT_CONTEXT, GetPnpEventContext)

//
// Define peakmeter element context.
//
typedef struct _PEAKMETER_ELEMENT_CONTEXT {
    LONG            PeakMeterLevel[MAX_CHANNELS];
} PEAKMETER_ELEMENT_CONTEXT, * PPEAKMETER_ELEMENT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(PEAKMETER_ELEMENT_CONTEXT, GetPeakMeterElementContext)

//
// Define DSP circuit's peakmeter element context.
//
typedef struct _DSP_PEAKMETER_ELEMENT_CONTEXT
{
    PVOID peakMeter;
} DSP_PEAKMETER_ELEMENT_CONTEXT, *PDSP_PEAKMETER_ELEMENT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(DSP_PEAKMETER_ELEMENT_CONTEXT, GetDspPeakMeterElementContext)

//
// Define stream engine context.
//
typedef struct _STREAMENGINE_CONTEXT {
    PVOID                   StreamEngine;
} STREAMENGINE_CONTEXT, * PSTREAMENGINE_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(STREAMENGINE_CONTEXT, GetStreamEngineContext)

#define PEAKMETER_STEPPING_DELTA    0x1000
#define PEAKMETER_MAXIMUM           LONG_MAX
#define PEAKMETER_MINIMUM           LONG_MIN

/////////////////////////////////////////////////////////////
// Codec driver defintions
//

typedef enum _CODEC_PIN_TYPE {
    CodecPinTypeHost,
    CodecPinTypeOffload,
    CodecPinTypeLoopback,
    CodecPinTypeKeyword,
    CodecPinTypeDevice
} CODEC_PIN_TYPE, * PCODEC_PIN_TYPE;

typedef struct _CODEC_PIN_CONTEXT {
    CODEC_PIN_TYPE  CodecPinType;
} CODEC_PIN_CONTEXT, * PCODEC_PIN_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(CODEC_PIN_CONTEXT, GetCodecPinContext)

///////////////////////////////////////////////////////////
// Dsp driver definitions
//

typedef enum {
    CaptureHostPin = 0,
    CaptureBridgePin = 1,
    CaptureKWSPin = 2,
    CapturePinCount = 3
} CAPTURE_PIN_TYPE;

typedef enum {
    RenderHostPin = 0,
    RenderOffloadPin = 1,
    RenderLoopbackPin = 2,
    RenderBridgePin = 3,
    RenderPinCount = 4
} RENDER_PIN_TYPE;

typedef struct _DSP_PIN_CONTEXT {
    ACXTARGETCIRCUIT        TargetCircuit;
    ULONG                   TargetPinId;
    RENDER_PIN_TYPE         RenderPinType;
    CAPTURE_PIN_TYPE        CapturePinType;

    // The stream bridge below will only be valid for the Capture circuit Bridge Pin

    // Host stream bridge will be used to ensure host stream creations are passed
    // to the downlevel circuits. Since the HostStreamBridge won't have InModes set,
    // the ACX framework will not add streams automatically. We will add streams for
    // non KWS pin.
    ACXSTREAMBRIDGE         HostStreamBridge;
} DSP_PIN_CONTEXT, * PDSP_PIN_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(DSP_PIN_CONTEXT, GetDspPinContext)

/////////////////////////////////////////////////////////////
// Multicircuit Dsp driver definitions
//

//
// Circuit context for multi circuit dsp circuits.
//
typedef struct _DSP_CIRCUIT_CONTEXT
{
    ACXAUDIOENGINE AudioEngineElement;
    ACXPEAKMETER PeakMeterElement;
    PVOID peakMeter;
    ACXKEYWORDSPOTTER KeywordSpotter;
    BOOLEAN IsRenderCircuit;
} DSP_CIRCUIT_CONTEXT, *PDSP_CIRCUIT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(DSP_CIRCUIT_CONTEXT, GetDspCircuitContext)

typedef struct _DSP_ENGINE_CONTEXT
{
    ACXDATAFORMAT MixFormat;
    BOOLEAN GFxEnabled;
} DSP_ENGINE_CONTEXT, *PDSP_ENGINE_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(DSP_ENGINE_CONTEXT, GetDspEngineContext)

typedef struct _DSP_STREAMAUDIOENGINE_CONTEXT
{
    BOOLEAN LFxEnabled;
} DSP_STREAMAUDIOENGINE_CONTEXT, *PDSP_STREAMAUDIOENGINE_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(DSP_STREAMAUDIOENGINE_CONTEXT, GetDspStreamAudioEngineContext)

/////////////////////////////////////////////////////////
//
// Codec Render (speaker) definitions
//

//
// Define render circuit context.
//
typedef struct _CODEC_RENDER_CIRCUIT_CONTEXT {
    ACXVOLUME       VolumeElement;
    ACXMUTE         MuteElement;
    ACXPEAKMETER    PeakMeterElement;
    ACXAUDIOENGINE  AudioEngineElement;
} CODEC_RENDER_CIRCUIT_CONTEXT, * PCODEC_RENDER_CIRCUIT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(CODEC_RENDER_CIRCUIT_CONTEXT, GetRenderCircuitContext)

typedef enum {
    CodecRenderHostPin = 0,
    CodecRenderBridgePin = 1,
    CodecRenderPinCount = 2
} CODEC_RENDER_PINS;

typedef enum {
    RenderVolumeIndex = 0,
    RenderMuteIndex = 1,
    RenderElementCount = 2
} CODEC_RENDER_ELEMENTS;

// Render callbacks.

EVT_ACX_CIRCUIT_CREATE_STREAM       CodecR_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            CodecR_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          CodecR_EvtCircuitPowerDown;
EVT_ACX_STREAM_SET_RENDER_PACKET    CodecR_EvtStreamSetRenderPacket;
EVT_ACX_STREAM_GET_CAPTURE_PACKET   CodecR_EvtStreamGetLoopbackPacket;
EVT_ACX_PIN_SET_DATAFORMAT          CodecR_EvtAcxPinSetDataFormat;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      CodecR_EvtPinContextCleanup;
EVT_ACX_MUTE_ASSIGN_STATE           CodecR_EvtMuteAssignState;
EVT_ACX_MUTE_RETRIEVE_STATE         CodecR_EvtMuteRetrieveState;
EVT_ACX_VOLUME_ASSIGN_LEVEL         CodecR_EvtVolumeAssignLevel;
EVT_ACX_VOLUME_RETRIEVE_LEVEL       CodecR_EvtVolumeRetrieveLevel;
EVT_ACX_PEAKMETER_RETRIEVE_LEVEL    CodecR_EvtPeakMeterRetrieveLevelCallback;
EVT_ACX_RAMPED_VOLUME_ASSIGN_LEVEL              CodecR_EvtRampedVolumeAssignLevel;
EVT_ACX_AUDIOENGINE_RETRIEVE_BUFFER_SIZE_LIMITS CodecR_EvtAcxAudioEngineRetrieveBufferSizeLimits;
EVT_ACX_AUDIOENGINE_RETRIEVE_EFFECTS_STATE      CodecR_EvtAcxAudioEngineRetrieveEffectsState;
EVT_ACX_AUDIOENGINE_ASSIGN_EFFECTS_STATE        CodecR_EvtAcxAudioEngineAssignEffectsState;
EVT_ACX_AUDIOENGINE_ASSIGN_ENGINE_FORMAT        CodecR_EvtAcxAudioEngineAssignEngineDeviceFormat;
EVT_ACX_AUDIOENGINE_RETRIEVE_ENGINE_FORMAT      CodecR_EvtAcxAudioEngineRetrieveEngineMixFormat;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_EFFECTS_STATE            CodecR_EvtAcxStreamAudioEngineRetrieveEffectsState;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_EFFECTS_STATE              CodecR_EvtAcxStreamAudioEngineAssignEffectsState;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_PRESENTATION_POSITION    CodecR_EvtAcxStreamAudioEngineRetrievePresentationPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_CURRENT_WRITE_POSITION     CodecR_EvtAcxStreamAudioEngineAssignCurrentWritePosition;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_LINEAR_BUFFER_POSITION   CodecR_EvtAcxStreamAudioEngineRetrieveLinearBufferPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_LAST_BUFFER_POSITION       CodecR_EvtAcxStreamAudioEngineAssignLastBufferPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_LOOPBACK_PROTECTION        CodecR_EvtAcxStreamAudioEngineAssignLoopbackProtection;

PAGED_CODE_SEG
NTSTATUS
CodecR_CreateRenderCircuit(
    _In_     WDFDEVICE              Device,
    _In_     const GUID *           ComponentGuid,
    _In_     const UNICODE_STRING * CircuitName,
    _Out_    ACXCIRCUIT *           Circuit
);

/////////////////////////////////////////////////////////
//
// Codec Capture (microphone) definitions
//

//
// Define capture circuit context.
//
typedef struct _CODEC_CAPTURE_CIRCUIT_CONTEXT {
    ACXVOLUME           BoostElement;
    ACXVOLUME           VolumeElement;
    ACXKEYWORDSPOTTER   KeywordSpotter;
} CODEC_CAPTURE_CIRCUIT_CONTEXT, * PCODEC_CAPTURE_CIRCUIT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(CODEC_CAPTURE_CIRCUIT_CONTEXT, GetCaptureCircuitContext)

typedef enum {
    CodecCaptureHostPin = 0,
    CodecCaptureBridgePin = 1,
    CodecCapturePinCount = 2
} CODEC_CAPTURE_PINS;

typedef enum {
    CaptureVolumeIndex = 0,
    CaptureElementCount = 1
} CAPTURE_ELEMENTS;

// Capture callbacks.

EVT_ACX_CIRCUIT_CREATE_STREAM       CodecC_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            CodecC_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          CodecC_EvtCircuitPowerDown;
EVT_ACX_VOLUME_ASSIGN_LEVEL         CodecC_EvtVolumeAssignLevelCallback;
EVT_ACX_VOLUME_RETRIEVE_LEVEL       CodecC_EvtVolumeRetrieveLevelCallback;
EVT_ACX_VOLUME_ASSIGN_LEVEL         CodecC_EvtBoostAssignLevelCallback;
EVT_ACX_VOLUME_RETRIEVE_LEVEL       CodecC_EvtBoostRetrieveLevelCallback;
EVT_ACX_STREAM_GET_CAPTURE_PACKET   CodecC_EvtStreamGetCapturePacket;
EVT_ACX_PIN_SET_DATAFORMAT          CodecC_EvtAcxPinSetDataFormat;
EVT_ACX_PIN_RETRIEVE_NAME           CodecC_EvtAcxPinRetrieveName;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      CodecC_EvtPinContextCleanup;
EVT_ACX_KEYWORDSPOTTER_RETRIEVE_ARM     CodecC_EvtAcxKeywordSpotterRetrieveArm;
EVT_ACX_KEYWORDSPOTTER_ASSIGN_ARM       CodecC_EvtAcxKeywordSpotterAssignArm;
EVT_ACX_KEYWORDSPOTTER_ASSIGN_PATTERNS  CodecC_EvtAcxKeywordSpotterAssignPatterns;
EVT_ACX_KEYWORDSPOTTER_ASSIGN_RESET     CodecC_EvtAcxKeywordSpotterAssignReset;

PAGED_CODE_SEG
NTSTATUS
CodecC_CreateCaptureCircuit(
    _In_     WDFDEVICE              Device,
    _In_     const GUID *           ComponentGuid,
    _In_     const GUID *           MicCustomName,
    _In_     const UNICODE_STRING * CircuitName,
    _Out_    ACXCIRCUIT *           Circuit
);

/////////////////////////////////////////////////////////
//
// MicArray definitions
//

//
// Define MicArray circuit context.
//
typedef struct _MICARRAY_CIRCUIT_CONTEXT {
    ACXMUTE             MuteElement;
    ACXVOLUME           VolumeElement;
    ACXPEAKMETER        PeakMeterElement;
    ACXKEYWORDSPOTTER   KeywordSpotter;
} MICARRAY_CIRCUIT_CONTEXT, * PMICARRAY_CIRCUIT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(MICARRAY_CIRCUIT_CONTEXT, GetMicArrayCircuitContext)

typedef enum {
    MicArrayVolumeIndex = 0,
    MicArrayMuteIndex = 1,
    MicArrayPeakmeterIndex = 2,
    MicArrayKWSIndex = 3,
    MicArrayElementCount = 4
} MICARRAY_ELEMENTS;

// MicArray callbacks.

EVT_ACX_MUTE_ASSIGN_STATE           MicArray_EvtMuteAssignState;
EVT_ACX_MUTE_RETRIEVE_STATE         MicArray_EvtMuteRetrieveState;
EVT_ACX_PEAKMETER_RETRIEVE_LEVEL    MicArray_EvtPeakMeterRetrieveLevelCallback;
EVT_ACX_CIRCUIT_CREATE_STREAM       MicArray_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            MicArray_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          MicArray_EvtCircuitPowerDown;
EVT_ACX_VOLUME_ASSIGN_LEVEL         MicArray_EvtVolumeAssignLevelCallback;
EVT_ACX_VOLUME_RETRIEVE_LEVEL       MicArray_EvtVolumeRetrieveLevelCallback;
EVT_ACX_STREAM_GET_CAPTURE_PACKET   MicArray_EvtStreamGetCapturePacket;
EVT_ACX_PIN_SET_DATAFORMAT          MicArray_EvtAcxPinSetDataFormat;
EVT_ACX_PIN_RETRIEVE_NAME           MicArray_EvtAcxPinRetrieveName;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      MicArray_EvtPinContextCleanup;
EVT_ACX_KEYWORDSPOTTER_RETRIEVE_ARM     MicArray_EvtAcxKeywordSpotterRetrieveArm;
EVT_ACX_KEYWORDSPOTTER_ASSIGN_ARM       MicArray_EvtAcxKeywordSpotterAssignArm;
EVT_ACX_KEYWORDSPOTTER_ASSIGN_PATTERNS  MicArray_EvtAcxKeywordSpotterAssignPatterns;
EVT_ACX_KEYWORDSPOTTER_ASSIGN_RESET     MicArray_EvtAcxKeywordSpotterAssignReset;
EVT_ACX_PIN_CONNECTED                   MicArray_EvtPinConnected;
EVT_ACX_PIN_DISCONNECTED                MicArray_EvtPinDisconnected;
EVT_WDF_DEVICE_CONTEXT_CLEANUP          MicArray_EvtDeviceContextCleanup;
EVT_ACX_OBJECT_PROCESS_REQUEST          MicArray_EvtPinProposeDataFormat2Callback;
EVT_ACX_OBJECT_PREPROCESS_REQUEST       MicArray_EvtAudioCpuResourcesCallback;

PAGED_CODE_SEG
NTSTATUS
MicArray_CreateCaptureCircuit(
    _In_     WDFDEVICE        Device,
    _In_     GUID             ComponentGuid,
    _In_     GUID             MicCustomName,
    _In_     UNICODE_STRING   CircuitName,
    _Out_    ACXCIRCUIT *     Circuit
);

PAGED_CODE_SEG
NTSTATUS
MicArray_SetPowerPolicy(
    _In_ WDFDEVICE            Device
);

/////////////////////////////////////////////////////////
//
// Microphone (external: headphone) definitions
//

//
// Define MicrophoneHp circuit context.
//
typedef struct _MICROPHONEHP_CIRCUIT_CONTEXT {
    ACXMUTE             MuteElement;
    ACXVOLUME           VolumeElement;
    ACXPEAKMETER        PeakMeterElement;
} MICROPHONEHP_CIRCUIT_CONTEXT, * PMICROPHONEHP_CIRCUIT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(MICROPHONEHP_CIRCUIT_CONTEXT, GetMicrophoneHpCircuitContext)

typedef enum {
    MicrophoneHpVolumeIndex = 0,
    MicrophoneHpMuteIndex = 1,
    MicrophoneHpPeakmeterIndex = 2,
    MicrophoneHpElementCount = 3
} MICROPHONEHP_ELEMENTS;

// MicrophoneHp callbacks.

EVT_ACX_MUTE_ASSIGN_STATE           MicrophoneHp_EvtMuteAssignState;
EVT_ACX_MUTE_RETRIEVE_STATE         MicrophoneHp_EvtMuteRetrieveState;
EVT_ACX_PEAKMETER_RETRIEVE_LEVEL    MicrophoneHp_EvtPeakMeterRetrieveLevelCallback;
EVT_ACX_CIRCUIT_CREATE_STREAM       MicrophoneHp_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            MicrophoneHp_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          MicrophoneHp_EvtCircuitPowerDown;
EVT_ACX_VOLUME_ASSIGN_LEVEL         MicrophoneHp_EvtVolumeAssignLevelCallback;
EVT_ACX_VOLUME_RETRIEVE_LEVEL       MicrophoneHp_EvtVolumeRetrieveLevelCallback;
EVT_ACX_STREAM_GET_CAPTURE_PACKET   MicrophoneHp_EvtStreamGetCapturePacket;
EVT_ACX_PIN_SET_DATAFORMAT          MicrophoneHp_EvtAcxPinSetDataFormat;
EVT_ACX_PIN_RETRIEVE_NAME           MicrophoneHp_EvtAcxPinRetrieveName;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      MicrophoneHp_EvtPinContextCleanup;
EVT_ACX_PIN_CONNECTED               MicrophoneHp_EvtPinConnected;
EVT_ACX_PIN_DISCONNECTED            MicrophoneHp_EvtPinDisconnected;
EVT_ACX_OBJECT_PREPROCESS_REQUEST   MicrophoneHp_EvtAudioCpuResourcesCallback;

PAGED_CODE_SEG
NTSTATUS
MicrophoneHp_CreateCaptureCircuit(
    _In_     WDFDEVICE        Device,
    _In_     GUID             ComponentGuid,
    _In_     GUID             MicCustomName,
    _In_     UNICODE_STRING   CircuitName,
    _Out_    ACXCIRCUIT *     Circuit
);

PAGED_CODE_SEG
NTSTATUS
MicrophoneHp_SetPowerPolicy(
    _In_ WDFDEVICE            Device
);

/////////////////////////////////////////////////////////
//
// Speaker definitions
//

//
// Define circuit context for Speaker and SpeakerHp. 
//
typedef struct _DSP_RENDER_CIRCUIT_CONTEXT {
    ACXPEAKMETER    PeakMeterElement;
    ACXAUDIOENGINE  AudioEngineElement;
    PVOID           PeakMeter;
} DSP_RENDER_CIRCUIT_CONTEXT, * PDSP_RENDER_CIRCUIT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(DSP_RENDER_CIRCUIT_CONTEXT, GetSpeakerCircuitContext)

// Speaker callbacks.

EVT_ACX_CIRCUIT_CREATE_STREAM       Speaker_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            Speaker_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          Speaker_EvtCircuitPowerDown;
EVT_ACX_STREAM_SET_RENDER_PACKET    Speaker_EvtStreamSetRenderPacket;
EVT_ACX_STREAM_GET_CAPTURE_PACKET   Speaker_EvtStreamGetLoopbackPacket;
EVT_ACX_PIN_SET_DATAFORMAT          Speaker_EvtAcxPinSetDataFormat;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      Speaker_EvtPinContextCleanup;
EVT_ACX_PIN_CONNECTED               Speaker_EvtPinConnected;
EVT_ACX_PIN_DISCONNECTED            Speaker_EvtPinDisconnected;

//Render Audio Engine callbacks.

EVT_ACX_MUTE_ASSIGN_STATE           Speaker_EvtMuteAssignState;
EVT_ACX_MUTE_RETRIEVE_STATE         Speaker_EvtMuteRetrieveState;
EVT_ACX_VOLUME_RETRIEVE_LEVEL       Speaker_EvtVolumeRetrieveLevel;
EVT_ACX_PEAKMETER_RETRIEVE_LEVEL    Speaker_EvtPeakMeterRetrieveLevelCallback;
EVT_ACX_RAMPED_VOLUME_ASSIGN_LEVEL              Speaker_EvtRampedVolumeAssignLevel;
EVT_ACX_AUDIOENGINE_RETRIEVE_BUFFER_SIZE_LIMITS Speaker_EvtAcxAudioEngineRetrieveBufferSizeLimits;
EVT_ACX_AUDIOENGINE_RETRIEVE_EFFECTS_STATE      Speaker_EvtAcxAudioEngineRetrieveEffectsState;
EVT_ACX_AUDIOENGINE_ASSIGN_EFFECTS_STATE        Speaker_EvtAcxAudioEngineAssignEffectsState;
EVT_ACX_AUDIOENGINE_ASSIGN_ENGINE_FORMAT        Speaker_EvtAcxAudioEngineAssignEngineDeviceFormat;
EVT_ACX_AUDIOENGINE_RETRIEVE_ENGINE_FORMAT      Speaker_EvtAcxAudioEngineRetrieveEngineMixFormat;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_EFFECTS_STATE            Speaker_EvtAcxStreamAudioEngineRetrieveEffectsState;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_EFFECTS_STATE              Speaker_EvtAcxStreamAudioEngineAssignEffectsState;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_PRESENTATION_POSITION    Speaker_EvtAcxStreamAudioEngineRetrievePresentationPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_CURRENT_WRITE_POSITION     Speaker_EvtAcxStreamAudioEngineAssignCurrentWritePosition;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_LINEAR_BUFFER_POSITION   Speaker_EvtAcxStreamAudioEngineRetrieveLinearBufferPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_LAST_BUFFER_POSITION       Speaker_EvtAcxStreamAudioEngineAssignLastBufferPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_LOOPBACK_PROTECTION        Speaker_EvtAcxStreamAudioEngineAssignLoopbackProtection;

PAGED_CODE_SEG
NTSTATUS
Speaker_SetPowerPolicy(
    _In_ WDFDEVICE      Device
);

PAGED_CODE_SEG
NTSTATUS
Speaker_CreateRenderCircuit(
    _In_     WDFDEVICE        Device,
    _In_     GUID             ComponentGuid,
    _In_     UNICODE_STRING   CircuitName,
    _In_     BOOLEAN          IsHeadphones,
    _Out_    ACXCIRCUIT *     Circuit
);

/////////////////////////////////////////////////////////
//
// HDMI definitions
//

//
// Define HDMI circuit context.
//
typedef struct _DSP_HDMI_CIRCUIT_CONTEXT {
    ACXVOLUME           VolumeElement;
    ACXMUTE             MuteElement;
    ACXPEAKMETER        PeakmeterElement;
} DSP_HDMI_CIRCUIT_CONTEXT, * PDSP_HDMI_CIRCUIT_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(DSP_HDMI_CIRCUIT_CONTEXT, GetHDMICircuitContext)

typedef enum {
    HDMIVolumeIndex = 0,
    HDMIMuteIndex = 1,
    HDMIPeakmeterIndex = 2,
    HDMIElementCount = 3
} HDMI_ELEMENTS;

typedef enum {
    HDMIHostPin = 0,
    HDMILoopbackPin = 1,
    HDMIBridgePin = 2,
    HDMIPinCount = 3
} HDMI_PIN_TYPE;

typedef struct _HDMI_PIN_CONTEXT {
    ACXTARGETCIRCUIT        TargetCircuit;
    ULONG                   TargetPinId;
    HDMI_PIN_TYPE           PinType;
    CAPTURE_PIN_TYPE        CapturePinType;
} HDMI_PIN_CONTEXT, * PHDMI_PIN_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(HDMI_PIN_CONTEXT, GetHDMIPinContext)

typedef struct _HDMI_STREAM_CONTEXT {
    HDMI_PIN_TYPE           PinType;
    CAPTURE_PIN_TYPE        CapturePinType;
} HDMI_STREAM_CONTEXT, *    PHDMI_STREAM_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(HDMI_STREAM_CONTEXT, GetHDMIStreamContext)

// HDMI callbacks.
EVT_ACX_CIRCUIT_CREATE_STREAM       HDMI_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            HDMI_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          HDMI_EvtCircuitPowerDown;
EVT_ACX_STREAM_SET_RENDER_PACKET    HDMI_EvtStreamSetRenderPacket;
EVT_ACX_STREAM_GET_CAPTURE_PACKET   HDMI_EvtStreamGetLoopbackPacket;
EVT_ACX_PIN_SET_DATAFORMAT          HDMI_EvtAcxPinSetDataFormat;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      HDMI_EvtPinContextCleanup;
EVT_ACX_PIN_CONNECTED               HDMI_EvtPinConnected;
EVT_ACX_PIN_DISCONNECTED            HDMI_EvtPinDisconnected;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      HDMI_EvtDeviceContextCleanup;
EVT_ACX_MUTE_ASSIGN_STATE           HDMI_EvtMuteAssignState;
EVT_ACX_MUTE_RETRIEVE_STATE         HDMI_EvtMuteRetrieveState;
EVT_ACX_VOLUME_ASSIGN_LEVEL         HDMI_EvtRampedVolumeAssignLevel;
EVT_ACX_VOLUME_RETRIEVE_LEVEL       HDMI_EvtVolumeRetrieveLevel;
EVT_ACX_PEAKMETER_RETRIEVE_LEVEL    HDMI_EvtPeakMeterRetrieveLevelCallback;
EVT_ACX_OBJECT_PREPROCESS_REQUEST   HDMI_EvtAudioCpuResourcesCallback;

PAGED_CODE_SEG
NTSTATUS
HDMI_SetPowerPolicy(
    _In_ WDFDEVICE      Device
);

PAGED_CODE_SEG
NTSTATUS
HDMI_CreateRenderCircuit(
    _In_     WDFDEVICE        Device,
    _In_     GUID             ComponentGuid,
    _In_     UNICODE_STRING   CircuitName,
    _Out_    ACXCIRCUIT *     Circuit
);

/////////////////////////////////////////////////////////
//
// Multi circuit codec Render (speaker) definitions
//

// Uses the same circuit context, render pin, and render element definitions as Codec Render.

// Render callbacks.
EVT_ACX_CIRCUIT_CREATE_STREAM       RenderMC_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            RenderMC_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          RenderMC_EvtCircuitPowerDown;
EVT_ACX_PIN_SET_DATAFORMAT          RenderMC_EvtAcxPinSetDataFormat;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      RenderMC_EvtPinContextCleanup;
EVT_ACX_MUTE_ASSIGN_STATE           RenderMC_EvtMuteAssignState;
EVT_ACX_MUTE_RETRIEVE_STATE         RenderMC_EvtMuteRetrieveState;
EVT_ACX_VOLUME_RETRIEVE_LEVEL       RenderMC_EvtVolumeRetrieveLevel;
EVT_ACX_RAMPED_VOLUME_ASSIGN_LEVEL  RenderMC_EvtRampedVolumeAssignLevel;

PAGED_CODE_SEG
NTSTATUS
RenderMC_CreateRenderCircuit(
    _In_     WDFDEVICE              Device,
    _In_     const GUID *           ComponentGuid,
    _In_     const UNICODE_STRING * CircuitName,
    _In_     const UNICODE_STRING * Uri,
    _Out_    ACXCIRCUIT *           Circuit
);

/////////////////////////////////////////////////////////
//
// Multi circuit codec Capture (microphone) definitions
//

// Uses the same circuit context, render pin, and render element definitions as Codec Capture.

// Capture callbacks.
EVT_ACX_CIRCUIT_CREATE_STREAM       CaptureMC_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            CaptureMC_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          CaptureMC_EvtCircuitPowerDown;
EVT_ACX_STREAM_GET_CAPTURE_PACKET   CaptureMC_EvtStreamGetCapturePacket;
EVT_ACX_PIN_SET_DATAFORMAT          CaptureMC_EvtAcxPinSetDataFormat;
EVT_ACX_PIN_RETRIEVE_NAME           CaptureMC_EvtAcxPinRetrieveName;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      CaptureMC_EvtPinContextCleanup;

PAGED_CODE_SEG
NTSTATUS
CaptureMC_CreateCaptureCircuit(
    _In_     WDFDEVICE              Device,
    _In_     const GUID *           ComponentGuid,
    _In_     const GUID *           MicCustomName,
    _In_     const UNICODE_STRING * CircuitName,
    _In_     const UNICODE_STRING * Uri,
    _Out_    ACXCIRCUIT *           Circuit
);

/////////////////////////////////////////////////////////
//
// Multi circuit dsp Render (speaker) definitions
//

// Uses the same circuit context, render pin, and render element definitions as Codec Render.

// Render callbacks.
EVT_ACX_CIRCUIT_COMPOSITE_CIRCUIT_INITIALIZE RenderMCDsp_EvtCircuitCompositeCircuitInitialize;
EVT_ACX_CIRCUIT_COMPOSITE_INITIALIZE RenderMCDsp_EvtCircuitCompositeInitialize;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      RenderMCDsp_EvtCircuitContextCleanup;
EVT_WDF_DEVICE_PREPARE_HARDWARE     RenderMCDsp_EvtDevicePrepareHardware;
EVT_WDF_DEVICE_RELEASE_HARDWARE     RenderMCDsp_EvtDeviceReleaseHardware;
EVT_WDF_DEVICE_SELF_MANAGED_IO_INIT RenderMCDsp_EvtDeviceSelfManagedIoInit;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      RenderMCDsp_EvtDeviceContextCleanup;
EVT_ACX_CIRCUIT_CREATE_STREAM       RenderMCDsp_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP            RenderMCDsp_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN          RenderMCDsp_EvtCircuitPowerDown;
EVT_ACX_STREAM_SET_RENDER_PACKET    RenderMCDsp_EvtStreamSetRenderPacket;
EVT_ACX_PIN_SET_DATAFORMAT          RenderMCDsp_EvtAcxPinSetDataFormat;
EVT_WDF_DEVICE_CONTEXT_CLEANUP      RenderMCDsp_EvtPinContextCleanup;
EVT_ACX_PIN_CONNECTED               RenderMCDsp_EvtPinConnected;
EVT_ACX_PIN_DISCONNECTED            RenderMCDsp_EvtPinDisconnected;

// Render Audio Engine
EVT_ACX_MUTE_ASSIGN_STATE DspR_EvtMuteAssignState;
EVT_ACX_MUTE_RETRIEVE_STATE DspR_EvtMuteRetrieveState;
EVT_ACX_VOLUME_RETRIEVE_LEVEL DspR_EvtVolumeRetrieveLevel;
EVT_ACX_PEAKMETER_RETRIEVE_LEVEL DspR_EvtPeakMeterRetrieveLevelCallback;
EVT_ACX_RAMPED_VOLUME_ASSIGN_LEVEL DspR_EvtRampedVolumeAssignLevel;
EVT_ACX_AUDIOENGINE_RETRIEVE_BUFFER_SIZE_LIMITS DspR_EvtAcxAudioEngineRetrieveBufferSizeLimits;
EVT_ACX_AUDIOENGINE_RETRIEVE_EFFECTS_STATE DspR_EvtAcxAudioEngineRetrieveEffectsState;
EVT_ACX_AUDIOENGINE_ASSIGN_EFFECTS_STATE DspR_EvtAcxAudioEngineAssignEffectsState;
EVT_ACX_AUDIOENGINE_RETRIEVE_ENGINE_FORMAT DspR_EvtAcxAudioEngineRetrieveEngineMixFormat;
EVT_ACX_AUDIOENGINE_ASSIGN_ENGINE_FORMAT DspR_EvtAcxAudioEngineAssignEngineDeviceFormat;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_EFFECTS_STATE DspR_EvtAcxStreamAudioEngineRetrieveEffectsState;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_EFFECTS_STATE DspR_EvtAcxStreamAudioEngineAssignEffectsState;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_PRESENTATION_POSITION DspR_EvtAcxStreamAudioEngineRetrievePresentationPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_CURRENT_WRITE_POSITION DspR_EvtAcxStreamAudioEngineAssignCurrentWritePosition;
EVT_ACX_STREAMAUDIOENGINE_RETRIEVE_LINEAR_BUFFER_POSITION DspR_EvtAcxStreamAudioEngineRetrieveLinearBufferPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_LAST_BUFFER_POSITION DspR_EvtAcxStreamAudioEngineAssignLastBufferPosition;
EVT_ACX_STREAMAUDIOENGINE_ASSIGN_LOOPBACK_PROTECTION DspR_EvtAcxStreamAudioEngineAssignLoopbackProtection;

PAGED_CODE_SEG
NTSTATUS
RenderMCDsp_SetPowerPolicy(
    _In_ WDFDEVICE            Device
);

PAGED_CODE_SEG
NTSTATUS CreateRenderCircuit(
    _In_ PACXCIRCUIT_INIT                   CircuitInit,
    _In_ UNICODE_STRING                     CircuitName,
    _In_ WDFDEVICE                          Device,
    _Out_ ACXCIRCUIT*                       Circuit
);

PAGED_CODE_SEG
NTSTATUS CreateRenderMCDspPin(
    _In_ ACX_PIN_TYPE                           PinType,
    _In_ ACXCIRCUIT                             Circuit,
    _In_ ACX_PIN_COMMUNICATION                  Communication,
    _In_ const GUID*                            Category,
    _In_ ACX_PIN_CALLBACKS*                     PinCallbacks,
    _In_ bool                                   Mic,
    _Out_ ACXPIN*                               Pin
);

PAGED_CODE_SEG
NTSTATUS CreateAudioEngine(
    _In_ ACXCIRCUIT                             Circuit,
    _In_reads_(PinCount) ACXPIN*                Pins,
    _In_ ULONG                                  PinCount,
    _Out_ ACXAUDIOENGINE*                       AudioEngineElement
);

/////////////////////////////////////////////////////////
//
// Multi circuit dsp Capture (microphone) definitions
//

// Uses the same circuit context, capture pin and capture element definitions as Codec Capture.

// Capture callbacks.
EVT_ACX_CIRCUIT_COMPOSITE_CIRCUIT_INITIALIZE CaptureMCDsp_EvtCircuitCompositeCircuitInitialize;
EVT_ACX_CIRCUIT_COMPOSITE_INITIALIZE CaptureMCDsp_EvtCircuitCompositeInitialize;
EVT_WDF_DEVICE_PREPARE_HARDWARE         CaptureMCDsp_EvtDevicePrepareHardware;
EVT_WDF_DEVICE_RELEASE_HARDWARE         CaptureMCDsp_EvtDeviceReleaseHardware;
EVT_WDF_DEVICE_SELF_MANAGED_IO_INIT     CaptureMCDsp_EvtDeviceSelfManagedIoInit;
EVT_WDF_DEVICE_CONTEXT_CLEANUP          CaptureMCDsp_EvtDeviceContextCleanup;
EVT_ACX_CIRCUIT_CREATE_STREAM           CaptureMCDsp_EvtCircuitCreateStream;
EVT_ACX_CIRCUIT_POWER_UP                CaptureMCDsp_EvtCircuitPowerUp;
EVT_ACX_CIRCUIT_POWER_DOWN              CaptureMCDsp_EvtCircuitPowerDown;
EVT_ACX_STREAM_GET_CAPTURE_PACKET       CaptureMCDsp_EvtStreamGetCapturePacket;
EVT_ACX_PIN_SET_DATAFORMAT              CaptureMCDsp_EvtAcxPinSetDataFormat;
EVT_WDF_DEVICE_CONTEXT_CLEANUP          CaptureMCDsp_EvtPinContextCleanup;
EVT_ACX_PIN_CONNECTED                   CaptureMCDsp_EvtPinConnected;
EVT_ACX_PIN_DISCONNECTED                CaptureMCDsp_EvtPinDisconnected;

PAGED_CODE_SEG
NTSTATUS
CaptureMCDsp_SetPowerPolicy(
    _In_ WDFDEVICE            Device
);

PAGED_CODE_SEG
NTSTATUS CreateCaptureCircuit(
    _In_ PACXCIRCUIT_INIT                   CircuitInit,
    _In_ UNICODE_STRING                     CircuitName,
    _In_ WDFDEVICE                          Device,
    _Out_ ACXCIRCUIT*                       Circuit
);

PAGED_CODE_SEG
NTSTATUS CreateCaptureMCDspPin(
    _In_ ACX_PIN_TYPE                           PinType,
    _In_ ACXCIRCUIT                             Circuit,
    _In_ ACX_PIN_COMMUNICATION                  Communication,
    _In_ const GUID*                            Category,
    _In_ ACX_PIN_CALLBACKS*                     PinCallbacks,
    _In_ bool                                   Mic,
    _Out_ ACXPIN*                               Pin
);

/* make internal prototypes usable from C++ */
#ifdef __cplusplus
}
#endif



#endif // _PRIVATE_H_
