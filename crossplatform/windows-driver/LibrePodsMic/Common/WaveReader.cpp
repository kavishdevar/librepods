/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:
    WaveReader.cpp

Abstract:
    Implementation of wave file reader for ACX sample drivers.

    To read data from disk, this class maintains a circular data buffer. This buffer is segmented into multiple chunks of 
    big buffer (Though we only need two, so it is set to two now). Initially we fill first two chunks and once a chunk gets emptied 
    by OS, we schedule a workitem to fill the next available chunk.


--*/

#pragma warning (disable : 4127)
#pragma warning (disable : 26165)

#include "private.h"
#include <devguid.h>
#include <ks.h>
#include <mmsystem.h>
#include <ksmedia.h>
#include "WaveReader.h"

#define FILE_NAME_BUFFER_TAG        'WRT1'
#define WAVE_DATA_BUFFER_TAG        'WRT2'
#define WORK_ITEM_BUFFER_TAG        'WRT3'

#define MAX_READ_WORKER_ITEM_COUNT       15

#define IF_FAILED_JUMP(result, tag) do {if (!NT_SUCCESS(result)) {goto tag;}} while(false)
#define IF_TRUE_JUMP(result, tag) do {if (result) {goto tag;}} while(false)
#define IF_TRUE_ACTION_JUMP(result, action, tag) do {if (result) {action; goto tag;}} while(false)

PREADWORKER_PARAM       CWaveReader::m_pWorkItems = NULL;
PDEVICE_OBJECT          CWaveReader::m_pDeviceObject = NULL;


/*++

Routine Description:
    Ctor: basic init.

--*/

_Use_decl_annotations_
PAGED_CODE_SEG
CWaveReader::CWaveReader()
: m_ChannelCount(0),
  m_BitsPerSample(0),
  m_SamplesPerSecond(0),
  m_Mute(false),
  m_FileHandle(NULL)
{
    PAGED_CODE();
    m_WaveDataQueue.pWavData = NULL;
    KeInitializeMutex(&m_FileSync, 0);
}

/*++

Routine Description:
    Dtor: free resources.

--*/
_Use_decl_annotations_
PAGED_CODE_SEG
CWaveReader::~CWaveReader()
{
    PAGED_CODE();
    if (STATUS_SUCCESS == KeWaitForSingleObject
    (
        &m_FileSync,
        Executive,
        KernelMode,
        FALSE,
        NULL
    ))
    {
        if (m_WaveDataQueue.pWavData != NULL)
        {
            ExFreePoolWithTag(m_WaveDataQueue.pWavData, WAVE_DATA_BUFFER_TAG);
            m_WaveDataQueue.pWavData = NULL;
        }

        FileClose();
        KeReleaseMutex(&m_FileSync, FALSE);
    }
    
}

/*++

Routine Description:
    - Initializing the workitems. These workitems will be scheduled asynchronously by the OS. 
    - When these work items will be scheduled the wave file will be read and the data 
    - will be put inside the big chunks.

Arguments:
    Device object

Return Value:
    NT status code.

--*/

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS CWaveReader::InitializeWorkItems(_In_ PDEVICE_OBJECT DeviceObject)
{
    PAGED_CODE();

    ASSERT(DeviceObject);

    NTSTATUS                    ntStatus = STATUS_SUCCESS;

    if (m_pWorkItems != NULL)
    {
        return ntStatus;
    }

    m_pWorkItems = (PREADWORKER_PARAM)
        ExAllocatePool2
        (
            POOL_FLAG_NON_PAGED,
            sizeof(READWORKER_PARAM) * MAX_READ_WORKER_ITEM_COUNT,
            'RDPT'
        );
    if (m_pWorkItems)
    {
        for (int i = 0; i < MAX_READ_WORKER_ITEM_COUNT; i++)
        {

            m_pWorkItems[i].WorkItem = IoAllocateWorkItem(DeviceObject);
            if (m_pWorkItems[i].WorkItem == NULL)
            {
                return STATUS_INSUFFICIENT_RESOURCES;
            }
            KeInitializeEvent
            (
                &m_pWorkItems[i].EventDone,
                NotificationEvent,
                TRUE
            );
        }
    }
    else
    {
        ntStatus = STATUS_INSUFFICIENT_RESOURCES;
    }

    return ntStatus;
} 

/*++

Routine Description:
- Wait for all the scheduled workitems to finish.

--*/


//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
void CWaveReader::WaitAllWorkItems()
{
    PAGED_CODE();

    for (int i = 0; i < MAX_READ_WORKER_ITEM_COUNT; i++)
    {
        KeWaitForSingleObject
        (
            &(m_pWorkItems[i].EventDone),
            Executive,
            KernelMode,
            FALSE,
            NULL
        );
    }
} 

/*++

Routine Description:
    - Deallocating the workitems. 

--*/


_Use_decl_annotations_
PAGED_CODE_SEG
VOID CWaveReader::DestroyWorkItems()
{
    PAGED_CODE();

    if (m_pWorkItems)
    {
        for (int i = 0; i < MAX_READ_WORKER_ITEM_COUNT; i++)
        {
            if (m_pWorkItems[i].WorkItem != NULL)
            {
                IoFreeWorkItem(m_pWorkItems[i].WorkItem);
                m_pWorkItems[i].WorkItem = NULL;
            }
        }
        ExFreePoolWithTag(m_pWorkItems, WORK_ITEM_BUFFER_TAG);
        m_pWorkItems = NULL;
    }
} 

/*++

Routine Description:
    - Get a free work item to schedule a file read operation.

--*/
_Use_decl_annotations_
PREADWORKER_PARAM CWaveReader::GetNewWorkItem()
{
    LARGE_INTEGER               timeOut = { 0 };
    NTSTATUS                    ntStatus;

    for (int i = 0; i < MAX_READ_WORKER_ITEM_COUNT; i++)
    {
        ntStatus =
            KeWaitForSingleObject
            (
                &m_pWorkItems[i].EventDone,
                Executive,
                KernelMode,
                FALSE,
                &timeOut
            );
        if (STATUS_SUCCESS == ntStatus)
        {
            if (m_pWorkItems[i].WorkItem)
                return &(m_pWorkItems[i]);
            else
                return NULL;
        }
    }

    return NULL;
} 

/*++
Routine Description:
- This routine will enqueue a workitem for reading wave file and putting 
- the data into the chunk buffer.

Arguments:
    Chunk descriptor for the chunk to be filled.
--*/

_Use_decl_annotations_
VOID CWaveReader::ReadWavChunk(PCHUNKDESCRIPTOR pChunkDescriptor)
{
    PREADWORKER_PARAM           pParam = NULL;

    pParam = GetNewWorkItem();
    if (pParam)
    {
        pParam->PtrWaveReader = this;
        pParam->PtrChunkDescriptor = pChunkDescriptor;
        KeResetEvent(&pParam->EventDone);
        IoQueueWorkItem(pParam->WorkItem, ReadFrameWorkerCallback,
            DelayedWorkQueue, (PVOID)pParam);
    }
} 

_Use_decl_annotations_
PAGED_CODE_SEG
IO_WORKITEM_ROUTINE ReadFrameWorkerCallback;
/*
Routine Description:
- This routine will be called by the OS. It will fill the chunk buffer, defined by the chunk descriptor
- If end of file is reached it will mark the end of file as true.

Arguments:
    pDeviceObject - Device object
    Context       - pointer to reader worker params
*/

_Use_decl_annotations_
PAGED_CODE_SEG
VOID ReadFrameWorkerCallback
(
    _In_        PDEVICE_OBJECT         pDeviceObject,
    _In_opt_    PVOID                  Context
)
{
    PAGED_CODE();
    UNREFERENCED_PARAMETER(pDeviceObject);
    pWaveReader                 pWavRd;
    PREADWORKER_PARAM           pParam = (PREADWORKER_PARAM)Context;

    if (NULL == pParam)
    {
        // This is completely unexpected, assert here.
        //
        ASSERT(pParam);
        goto exit;
    }

    pWavRd = pParam->PtrWaveReader;

    if (pWavRd == NULL)
    {
        goto exit;
    }
    if (STATUS_SUCCESS == KeWaitForSingleObject
    (
        &pWavRd->m_FileSync,
        Executive,
        KernelMode,
        FALSE,
        NULL
    )) 
    {

        NTSTATUS                    ntStatus = STATUS_SUCCESS;

        ASSERT(Context);
        
        IO_STATUS_BLOCK             ioStatusBlock;

        if (pParam->WorkItem)
        {
            if (pWavRd->m_WaveDataQueue.bEofReached || pWavRd->m_WaveDataQueue.pWavData == NULL)
            {
                KeReleaseMutex(&pWavRd->m_FileSync, FALSE);
                goto exit;
            }

            if (pParam->PtrChunkDescriptor->pStartAddress != NULL)
            {
                ntStatus = ZwReadFile(pWavRd->m_FileHandle,
                    NULL,
                    NULL,
                    NULL,
                    &ioStatusBlock,
                    pParam->PtrChunkDescriptor->pStartAddress,
                    pParam->PtrChunkDescriptor->ulChunkLength,
                    NULL,
                    NULL);

                pParam->PtrChunkDescriptor->bIsChunkEmpty = false;

                if (ioStatusBlock.Information != pParam->PtrChunkDescriptor->ulChunkLength)
                {
                    pWavRd->m_WaveDataQueue.bEofReached = true;
                }
            }
        }

        KeReleaseMutex(&pWavRd->m_FileSync, FALSE);
    }

exit:
    KeSetEvent(&pParam->EventDone, 0, FALSE);
}

/*++

Routine Description:
-   If all the chunks are empty this resturn true.

--*/

_Use_decl_annotations_
bool CWaveReader::IsAllChunkEmpty()
{
    for (int i = 0; i < NUM_OF_CHUNK_FOR_FILE_READ; i++)
    {
        if (!m_WaveDataQueue.sChunkDescriptor[i].bIsChunkEmpty)
        {
            return false;
        }
    }
    return true;
}

/*++
Routine Description:
    - This routine does the actual copy of data from the chunk buffer to the buffer provided by OS.
    - If it empties the current chunk buffer, then it sets it state to empty and then enqueue a workitem 
    - to read data from the wave file and put it to the next available chunk buffer. 

Arguments:
    Buffer - Pointer to the OS buffer
    BufferLength - Length of the data to be filled (in bytes)

--*/
_Use_decl_annotations_
VOID CWaveReader::CopyDataFromRingBuffer
(
    _Out_writes_bytes_(BufferLength) BYTE       *Buffer,
    _In_                             ULONG      BufferLength
)
{
    if (IsAllChunkEmpty())
    {
        RtlZeroMemory(Buffer, BufferLength);
    }
    else
    {
        ULONG prevChunk = (m_WaveDataQueue.ulReadPtr*NUM_OF_CHUNK_FOR_FILE_READ )/ m_WaveDataQueue.ulLength;

        /////////////////
        BYTE       *currentBuf = Buffer;
        ULONG       length = BufferLength;
        while (length > 0)
        {
            ULONG runWrite = min(length, m_WaveDataQueue.ulLength - m_WaveDataQueue.ulReadPtr);
            
            // Copy the wave buffer data to OS buffer
            RtlCopyMemory(currentBuf, m_WaveDataQueue.pWavData + m_WaveDataQueue.ulReadPtr, runWrite);
            // Zero out the wave buffer, so that if wave end of file is reached we should copy only zeros
            RtlZeroMemory(m_WaveDataQueue.pWavData + m_WaveDataQueue.ulReadPtr, runWrite);
            // Update the read pointer
            m_WaveDataQueue.ulReadPtr = (m_WaveDataQueue.ulReadPtr  + runWrite) % m_WaveDataQueue.ulLength;
            currentBuf += runWrite;
            length = length - runWrite;
        }

        ULONG curChunk = (m_WaveDataQueue.ulReadPtr*NUM_OF_CHUNK_FOR_FILE_READ) / m_WaveDataQueue.ulLength;

        if (curChunk != prevChunk)
        {
            m_WaveDataQueue.currentExecutedChunk++;
            // Schedule a workitem to read data from the wave file
            ULONG chunkNo = m_WaveDataQueue.currentExecutedChunk % NUM_OF_CHUNK_FOR_FILE_READ;
            m_WaveDataQueue.sChunkDescriptor[chunkNo].bIsChunkEmpty = true;
            if (!m_WaveDataQueue.bEofReached)
            {
                ReadWavChunk(&m_WaveDataQueue.sChunkDescriptor[chunkNo]);
            }
        }
    }
}

/*++
Routine Description:
    - Just a high level read buffer call.

 Arguments:
     Buffer - Pointer to the OS buffer
     BufferLength - Length of the data to be filled (in bytes)
--*/

_Use_decl_annotations_
VOID CWaveReader::ReadWaveData
(
    _Out_writes_bytes_(BufferLength) BYTE       *Buffer, 
    _In_                             ULONG      BufferLength
)
{
    if (m_Mute)
    {
        RtlZeroMemory(Buffer, BufferLength);
    }
    else
    {
        CopyDataFromRingBuffer(Buffer, BufferLength);
    }
}

/*++
Routine Description:
- initialization for the wavereader member variables, 
- Allocating memory for the 1 second buffer
- Preread the one second buffer data, so that when OS comes to read the data we have it available in the memory.

Arguments:
    WfExt - Format which should be used for capture
    fileNameString - name of the file to be read

Return:
    NTStatus
--*/
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS CWaveReader::Init
(
    _In_    PWAVEFORMATEXTENSIBLE   WfExt,
    _In_    PUNICODE_STRING puiFileName
)
{
    PAGED_CODE();
    NTSTATUS        ntStatus = STATUS_SUCCESS;
    KFLOATING_SAVE  saveData;
    
    // Save floating state (just in case).
    ntStatus = KeSaveFloatingPointState(&saveData);
    if (!NT_SUCCESS(ntStatus))
    {
        return ntStatus;
    }

    //
    // This sample supports PCM 16bit formats only. 
    //
    if ((WfExt->Format.wFormatTag != WAVE_FORMAT_PCM &&
        !(WfExt->Format.wFormatTag == WAVE_FORMAT_EXTENSIBLE &&
          IsEqualGUIDAligned(WfExt->SubFormat, KSDATAFORMAT_SUBTYPE_PCM))) ||
        (WfExt->Format.wBitsPerSample != 16 &&
         WfExt->Format.wBitsPerSample != 8))
    {
        ntStatus = STATUS_NOT_SUPPORTED;
    }
    IF_FAILED_JUMP(ntStatus, Done);

    // Basic init.
    m_ChannelCount      = WfExt->Format.nChannels;      // # channels.
    m_BitsPerSample     = WfExt->Format.wBitsPerSample; // bits per sample.
    m_SamplesPerSecond  = WfExt->Format.nSamplesPerSec; // samples per sec.
    m_Mute              = false;

    // Wave data queue initialization
    m_WaveDataQueue.ulLength = WfExt->Format.nAvgBytesPerSec;
    m_WaveDataQueue.bEofReached = false;
    m_WaveDataQueue.ulReadPtr = 0;

    // Mark all the chunk empty
    for (int i = 0; i < NUM_OF_CHUNK_FOR_FILE_READ; i++)
    {
        m_WaveDataQueue.sChunkDescriptor[i].bIsChunkEmpty = true;
    }

    ntStatus = OpenWaveFile(puiFileName);
    IF_FAILED_JUMP(ntStatus, Done);

    ntStatus = AllocateBigBuffer();
    IF_FAILED_JUMP(ntStatus, Done);

    ntStatus = ReadHeaderAndFillBuffer();

Done:
    (void)KeRestoreFloatingPointState(&saveData);
    return ntStatus;
}

/*++
Routine Description:
    This function read the wave header file and compare the header info with the
    stream info. Currently we are using only number of channel, sampling frequency 
    and bits per sample as the primary parameters for the wave file to compare against
    stream params. If the params don't match we return success but streams zeros.

Return:
    NTStatus
--*/

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS CWaveReader::ReadHeaderAndFillBuffer()
{
    PAGED_CODE();
    NTSTATUS    ntStatus = STATUS_SUCCESS;
    ntStatus = FileReadHeader();

    if(NT_SUCCESS(ntStatus))
    {
        if (m_WaveHeader.numChannels != m_ChannelCount ||
            m_WaveHeader.bitsPerSample != m_BitsPerSample ||
            m_WaveHeader.sampleRate != m_SamplesPerSecond)
        {
            // If the wave file format don't match we wont treat this as error
            // and we will stream zeros. So we return from here and will not read the 
            // wave file and wont fill the buffers.
            return STATUS_SUCCESS;
        }
    }

    if (NT_SUCCESS(ntStatus))
    {
        // If the wave file format is same as the stream format we will stream the data 
        //  else we will just stream zeros.
        ReadWavChunk(&m_WaveDataQueue.sChunkDescriptor[0]); // Fill the first chunk
        ReadWavChunk(&m_WaveDataQueue.sChunkDescriptor[1]);  // Fill the second chunk
        // Set the current executed chunk to 1. Once OS finishs the data for the first chunk
        // use the currentExecutedChunk to find the next chunk and schedule a workitem to fill the 
        // data into the next chunk
        m_WaveDataQueue.currentExecutedChunk = 1;   
    }

    return ntStatus;
}

/*++
Routine Description:
    This function allocates 1 second buffer.
    Segments the buffer into multiple (currently two) chunks. Assigns the start pointer and length
    for each chunk.

Return:
    NTStatus
--*/

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS CWaveReader::AllocateBigBuffer()
{
    PAGED_CODE();
    NTSTATUS    ntStatus = STATUS_SUCCESS;

    m_WaveDataQueue.pWavData = (PBYTE)
        ExAllocatePool2
        (
            POOL_FLAG_NON_PAGED,
            m_WaveDataQueue.ulLength,
            WAVE_DATA_BUFFER_TAG
        );
    if (!m_WaveDataQueue.pWavData)
    {
        ntStatus = STATUS_INSUFFICIENT_RESOURCES;
    }
    else
    {
        ULONG chunklLength = m_WaveDataQueue.ulLength / NUM_OF_CHUNK_FOR_FILE_READ;
        for (int i = 0; i < NUM_OF_CHUNK_FOR_FILE_READ; i++)
        {
            m_WaveDataQueue.sChunkDescriptor[i].pStartAddress = m_WaveDataQueue.pWavData + chunklLength*i;
            m_WaveDataQueue.sChunkDescriptor[i].ulChunkLength = chunklLength;
        }
    }
    return ntStatus;
}

/*++
Routine Description:
    This function opens wave file.

Arguments:
    fileNameString - Name of the wave file

Return:
    NTStatus
--*/

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS CWaveReader::OpenWaveFile(PUNICODE_STRING puiFileName)
{
    PAGED_CODE();
    NTSTATUS    ntStatus = STATUS_SUCCESS;

    if (NT_SUCCESS(ntStatus) && puiFileName->Buffer != NULL)
    {
        // Create data file.
        InitializeObjectAttributes
        (
            &m_objectAttributes,
            puiFileName,
            OBJ_CASE_INSENSITIVE | OBJ_KERNEL_HANDLE,
            NULL,
            NULL
        );

        // Open Wave File
        ntStatus = FileOpen();
    }

    return ntStatus;
}

/*++
Routine Description:
    This function closes wave file handle.

Return:
    NTStatus
--*/

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS CWaveReader::FileClose()
{
    PAGED_CODE();

    NTSTATUS                    ntStatus = STATUS_SUCCESS;

    if (m_FileHandle)
    {
        ntStatus = ZwClose(m_FileHandle);
        m_FileHandle = NULL;
    }

    return ntStatus;
} 

/*++
Routine Description:
    Reads the wave file file header information

Return:
    NTStatus
--*/

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS CWaveReader::FileReadHeader()
{
    PAGED_CODE();
    NTSTATUS                    ntStatus = STATUS_SUCCESS;
    IO_STATUS_BLOCK             ioStatusBlock;


    ntStatus = ZwReadFile(m_FileHandle,
                        NULL, 
                        NULL, 
                        NULL,
                        &ioStatusBlock,
                        &m_WaveHeader,
                        sizeof(WAVEHEADER), 
                        NULL, 
                        NULL);

    return ntStatus;
}

/*++
Routine Description:
    This function opens wave file.

Return:
    NTStatus
--*/

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS CWaveReader::FileOpen()
{
 
    PAGED_CODE();
    NTSTATUS                    ntStatus = STATUS_SUCCESS;
    IO_STATUS_BLOCK             ioStatusBlock;

    if (!m_FileHandle)
    {
        ntStatus =
            ZwCreateFile
            (
                &m_FileHandle,
                GENERIC_READ,
                &m_objectAttributes,
                &ioStatusBlock,
                NULL,
                FILE_ATTRIBUTE_NORMAL,
                FILE_SHARE_READ,
                FILE_OPEN,
                FILE_SYNCHRONOUS_IO_NONALERT,
                NULL,
                0
            );
    }

    return ntStatus;
} 

