/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    ToneGenerator.h

Abstract:

    Declaration of sine wave generator for ACX driver samples.


--*/
#pragma once

#define _USE_MATH_DEFINES
#include <math.h>
#include <limits.h>

class ToneGenerator
{
public:
    DWORD           m_Frequency; 
    WORD            m_ChannelCount; 
    WORD            m_BitsPerSample;
    DWORD           m_SamplesPerSecond;
    double          m_Theta;
    double          m_SampleIncrement;  
    bool            m_Mute;
    BYTE*           m_PartialFrame;
    DWORD           m_PartialFrameBytes;
    DWORD           m_FrameSize;

public:
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    ToneGenerator();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    ~ToneGenerator();
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    Init
    (
        _In_    DWORD                   ToneFrequency, 
        _In_    PWAVEFORMATEXTENSIBLE   WfExt
    );
    
    __drv_maxIRQL(DISPATCH_LEVEL)
    VOID
    GenerateSine
    (
        _Out_writes_bytes_(BufferLength) BYTE       *Buffer, 
        _In_                             size_t      BufferLength
    );

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    VOID
    SetMute
    (
        _In_ bool Value
    )
    {
        PAGED_CODE();

        m_Mute = Value;
    }

private:
    __drv_maxIRQL(DISPATCH_LEVEL)
    VOID InitNewFrame
    (
        _Out_writes_bytes_(FrameSize)   BYTE*  Frame, 
        _In_                            DWORD  FrameSize
    );
};


