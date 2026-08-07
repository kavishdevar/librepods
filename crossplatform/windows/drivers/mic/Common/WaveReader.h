/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    WaveReader.h

Abstract:

    Declaration of wave reader for ACX sample drivers.


--*/
#pragma once

#define _USE_MATH_DEFINES
#include <math.h>
#include <limits.h>

#define NUM_OF_CHUNK_FOR_FILE_READ 2

class CWaveReader;

// Wave header structure decleration 
typedef CWaveReader *pWaveReader;
typedef struct _WAVEHEADER
{
    BYTE chunkId[4];
    ULONG chunkSize;
    BYTE format[4];
    BYTE subChunkId[4];
    ULONG subChunkSize;
    WORD audioFormat;
    WORD numChannels;
    ULONG sampleRate;
    ULONG bytesPerSecond;
    WORD blockAlign;
    WORD bitsPerSample;
    BYTE dataChunkId[4];
    ULONG dataSize;
}WAVEHEADER;

typedef struct _CHUNKDESCRIPTOR
{
    PBYTE pStartAddress;        // Starting address of the chunk
    ULONG ulChunkLength;        // Length of the chunk
    bool  bIsChunkEmpty;        // If the chunk is empty
}CHUNKDESCRIPTOR;
typedef CHUNKDESCRIPTOR *PCHUNKDESCRIPTOR;

/*
    The idea here is to allocate one second long worth of buffer and divide it into NUM_OF_CHUNK_FOR_FILE_READ chunks.
    In one file read operation we read and fill one chunk data . The chunk will be emptied every 10 ms by OS.
    Once the OS empties one chunk data we schedule a workitem to read and fill next available chunk. 
*/

typedef struct _WAVEDATAQUEUE
{
    PBYTE pWavData;     // Pointer to the temporary buffer for reading one second worth of data from wave file
    ULONG ulLength;     // length of pWavData in bytes
    ULONG ulReadPtr;    // current reading position in pWavData in bytes
    bool  bEofReached;  // This will be set once the eof is reached.
    WORD  currentExecutedChunk;
    CHUNKDESCRIPTOR sChunkDescriptor[NUM_OF_CHUNK_FOR_FILE_READ];
}WAVEDATAQUEUE;
typedef WAVEDATAQUEUE *PWAVEDATAQUEUE;

// Parameter to workitem.
#include <pshpack1.h>
typedef struct _READWORKER_PARAM {
    PIO_WORKITEM     WorkItem;      // Pointer to the workitem
    KEVENT           EventDone;     // Used for synchronizing a workitem for scheduling. 
    pWaveReader      PtrWaveReader;   // pointer to the wavereader class.
    PCHUNKDESCRIPTOR PtrChunkDescriptor;   // chunk descriptor for the chunk, which needs to be filled after file read
} READWORKER_PARAM;
typedef READWORKER_PARAM *PREADWORKER_PARAM;
#include <poppack.h>

__drv_maxIRQL(PASSIVE_LEVEL)
PAGED_CODE_SEG
IO_WORKITEM_ROUTINE ReadFrameWorkerCallback;

// Wave Reader class

class CWaveReader
{

public:
    HANDLE                      m_FileHandle;       // Wave File handle.
    WORD                        m_ChannelCount;     // Number of Channels for the stream during stream init
    WORD                        m_BitsPerSample;    // Number of Bits per sample for the stream during stream init
    DWORD                       m_SamplesPerSecond; // Number of Sample per second for the stream during stream init
    bool                        m_Mute;             // Capture Zero buffer if mute
    OBJECT_ATTRIBUTES           m_objectAttributes; // Used for opening file.
    WAVEDATAQUEUE               m_WaveDataQueue;    // Big buffer data object and its current state
    KMUTEX                      m_FileSync;         // Synchronizes file access
    WAVEHEADER                  m_WaveHeader;

public:
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    CWaveReader();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    ~CWaveReader();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS Init
    (
        _In_ PWAVEFORMATEXTENSIBLE WfExt,
        _In_ PUNICODE_STRING puiFileName
    );

    __drv_maxIRQL(DISPATCH_LEVEL)
    VOID ReadWaveData
    (
        _Out_writes_bytes_(BufferLength) BYTE  *Buffer,
        _In_ ULONG      BufferLength
    );

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    VOID SetMute(_In_ bool Value)
    {
        PAGED_CODE();

        m_Mute = Value;
    }

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    void WaitAllWorkItems();

    // Static allocation totally related to the workitems for reading data from wavefile and putting it to chunk buffer
    static PDEVICE_OBJECT       m_pDeviceObject;
    static PREADWORKER_PARAM    m_pWorkItems;
    PAGED_CODE_SEG
    static NTSTATUS  InitializeWorkItems(_In_ PDEVICE_OBJECT  DeviceObject);

    __drv_maxIRQL(DISPATCH_LEVEL)
    static PREADWORKER_PARAM GetNewWorkItem();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    static VOID DestroyWorkItems();

private:
    __drv_maxIRQL(DISPATCH_LEVEL)
    VOID ReadWavChunk(PCHUNKDESCRIPTOR PtrChunkDescriptor);

    __drv_maxIRQL(DISPATCH_LEVEL)
    bool IsAllChunkEmpty();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS OpenWaveFile(PUNICODE_STRING puiFileName);

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS FileClose();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS FileReadHeader();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS FileOpen();

    __drv_maxIRQL(DISPATCH_LEVEL)
    VOID CopyDataFromRingBuffer
    (
        _Out_writes_bytes_(BufferLength) BYTE       *Buffer,
        _In_                             ULONG      BufferLength
    );

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS AllocateBigBuffer();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS ReadHeaderAndFillBuffer();

    friend IO_WORKITEM_ROUTINE ReadFrameWorkerCallback;
};


