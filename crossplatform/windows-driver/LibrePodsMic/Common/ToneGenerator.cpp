/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    ToneGenerator.cpp

Abstract:

    Implementation of sine wave generator for ACX driver samples


--*/
#include "private.h"
#include "public.h"
#include <devguid.h>
#include <ks.h>
#include <mmsystem.h>
#include <ksmedia.h>
#include "ToneGenerator.h"

#define TONEGENERATOR_POOLTAG  'rGnT'

const double TONE_AMPLITUDE = 0.5;  // Scalar value, should be between 0.0 - 1.0
const double TWO_PI = M_PI * 2;

DWORD g_DisableToneGenerator = 0;  // default is to generate tones.

#define IF_FAILED_JUMP(result, tag) do {if (!NT_SUCCESS(result)) {goto tag;}} while(false)
#define IF_TRUE_JUMP(result, tag) do {if (result) {goto tag;}} while(false)
#define IF_TRUE_ACTION_JUMP(result, action, tag) do {if (result) {action; goto tag;}} while(false)

//
// Double to short conversion.
//
__drv_maxIRQL(DISPATCH_LEVEL)
short ConvertToShort(double Value)
{
    return (short)(Value * _I16_MAX);
};

//
// Double to char conversion.
//
__drv_maxIRQL(DISPATCH_LEVEL)
unsigned char ConvertToUChar(double Value)
{
    const double F_127_5 = 127.5;
    return (unsigned char)(Value * F_127_5 + F_127_5);
};

//
// Ctor: basic init.
//
_Use_decl_annotations_
PAGED_CODE_SEG
ToneGenerator::ToneGenerator()
: m_Frequency(0),
  m_ChannelCount(0),
  m_BitsPerSample(0),
  m_SamplesPerSecond(0),
  m_Mute(false),
  m_PartialFrame(NULL),
  m_PartialFrameBytes(0),
  m_FrameSize(0)
{
    PAGED_CODE();

    // Theta (double) and SampleIncrement (double) are init in the Init() method 
    // after saving the floating point state. 
}

//
// Dtor: free resources.
//
_Use_decl_annotations_
PAGED_CODE_SEG
ToneGenerator::~ToneGenerator()
{
    PAGED_CODE();

    if (m_PartialFrame)
    {
        ExFreePoolWithTag(m_PartialFrame, TONEGENERATOR_POOLTAG);
        m_PartialFrame = NULL;
        m_PartialFrameBytes = 0;
    }
}

// 
// Init a new frame. 
// Note: caller will save and restore the floatingpoint state.
//
#pragma warning(push)
// Caller wraps this routine between KeSaveFloatingPointState/KeRestoreFloatingPointState calls.
#pragma warning(disable: 28110)

_Use_decl_annotations_
VOID ToneGenerator::InitNewFrame
(
    _Out_writes_bytes_(FrameSize)    BYTE*  Frame, 
    _In_                             DWORD  FrameSize
)
{
    double sinValue = TONE_AMPLITUDE * sin( m_Theta );

    if (FrameSize != (DWORD)m_ChannelCount * m_BitsPerSample/8)
    {
        ASSERT(FALSE);
        RtlZeroMemory(Frame, FrameSize);
        return;
    }
    
    for(ULONG i = 0; i < m_ChannelCount; ++i)
    {
        if (m_BitsPerSample == 8)
        {
            unsigned char *dataBuffer = reinterpret_cast<unsigned char *>(Frame);
             dataBuffer[i] = ConvertToUChar(sinValue);
        }
        else // 16 bits per sample
        {
            short *dataBuffer = reinterpret_cast<short *>(Frame);
            dataBuffer[i] = ConvertToShort(sinValue);
        }
    }

    m_Theta += m_SampleIncrement;
    if (m_Theta >= TWO_PI)
    {
        m_Theta -= TWO_PI;
    }
}
#pragma warning(pop)

//
// GenerateSamples()
//
//  Generate a sine wave that fits into the specified buffer.
//
//  Buffer - Buffer to hold the samples
//  BufferLength - Length of the buffer.
//
//  Note: this function supports 16bit and 8bit samples only.
//
_Use_decl_annotations_
void ToneGenerator::GenerateSine
(
    _Out_writes_bytes_(BufferLength) BYTE       *Buffer, 
    _In_                             size_t      BufferLength
)
{
    NTSTATUS        status;
    KFLOATING_SAVE  saveData;
    BYTE *          buffer;
    size_t          length;
    size_t          copyBytes;

    // if muted, or tone generator disabled via registry,
    // we deliver silence.
    if (m_Mute || g_DisableToneGenerator)
    {
        goto ZeroBuffer;
    }
    
    status = KeSaveFloatingPointState(&saveData);
    if (!NT_SUCCESS(status))
    {
        goto ZeroBuffer;
    }

    buffer = Buffer;
    length = BufferLength;

    //
    // Check if we have any residual frame bytes from the last time.
    //
    if (m_PartialFrameBytes)
    {
        ASSERT(m_FrameSize > m_PartialFrameBytes);
        DWORD offset = m_FrameSize - m_PartialFrameBytes;
        copyBytes = MIN(m_PartialFrameBytes, length);
        RtlCopyMemory(buffer, m_PartialFrame + offset, copyBytes);
        RtlZeroMemory(m_PartialFrame + offset, copyBytes);
        length -= copyBytes;
        buffer += copyBytes;
        m_PartialFrameBytes = 0;
    }
    
    IF_TRUE_JUMP(length == 0, Done);

    //
    // Copy all the aligned frames.
    // 

    size_t frames = length/m_FrameSize;

    for (size_t i = 0; i < frames; ++i)
    {
        InitNewFrame(buffer, m_FrameSize);
        buffer += m_FrameSize;
        length -= m_FrameSize;
    }

    IF_TRUE_JUMP(length == 0, Done);
    
    //
    // Copy any partial frame at the end.
    //
    ASSERT(m_FrameSize > length);
    InitNewFrame(m_PartialFrame, m_FrameSize);
    RtlCopyMemory(buffer, m_PartialFrame, length);
    RtlZeroMemory(m_PartialFrame, length);
    m_PartialFrameBytes = m_FrameSize - (DWORD)length;    
    
Done:
    KeRestoreFloatingPointState(&saveData);
    return;

ZeroBuffer:
    RtlZeroMemory(Buffer, BufferLength);
    return;
}

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS ToneGenerator::Init
(
    _In_    DWORD                   ToneFrequency, 
    _In_    PWAVEFORMATEXTENSIBLE   WfExt
)
{
    NTSTATUS        status      = STATUS_SUCCESS;
    KFLOATING_SAVE  saveData;
    
    PAGED_CODE();

    //
    // This sample supports PCM 16bit formats only. 
    //
    if ((WfExt->Format.wFormatTag != WAVE_FORMAT_PCM &&
        !(WfExt->Format.wFormatTag == WAVE_FORMAT_EXTENSIBLE &&
          IsEqualGUIDAligned(WfExt->SubFormat, KSDATAFORMAT_SUBTYPE_PCM))) ||
        (WfExt->Format.wBitsPerSample != 16 &&
         WfExt->Format.wBitsPerSample != 8))
    {
        status = STATUS_NOT_SUPPORTED;
    }
    IF_FAILED_JUMP(status, Done);

    //
    // Save floating state (just in case).
    //
    status = KeSaveFloatingPointState(&saveData);
    IF_FAILED_JUMP(status, Done);

    //
    // Basic init.
    //
    RtlZeroMemory(&m_Theta, sizeof(m_Theta));
    m_Frequency         = ToneFrequency;
    m_ChannelCount      = WfExt->Format.nChannels;      // # channels.
    m_BitsPerSample     = WfExt->Format.wBitsPerSample; // bits per sample.
    m_SamplesPerSecond  = WfExt->Format.nSamplesPerSec; // samples per sec.
    m_Mute              = false;
    m_SampleIncrement   = (m_Frequency * TWO_PI) / (double)m_SamplesPerSecond;
    m_FrameSize         = (DWORD)m_ChannelCount * m_BitsPerSample/8;
    ASSERT(m_FrameSize == WfExt->Format.nBlockAlign);
    
    //
    // Restore floating state.
    //
    KeRestoreFloatingPointState(&saveData);

    // 
    // Allocate a buffer to hold a partial frame.
    //
    m_PartialFrame = (BYTE*)ExAllocatePool2(
                                    POOL_FLAG_NON_PAGED,
                                    m_FrameSize,
                                    TONEGENERATOR_POOLTAG);

    IF_TRUE_ACTION_JUMP(m_PartialFrame == NULL, status = STATUS_INSUFFICIENT_RESOURCES, Done);
    
    status = STATUS_SUCCESS;

Done:
    return status;
}


