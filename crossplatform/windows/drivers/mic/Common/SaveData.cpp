/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    SaveData.cpp

Abstract:

    Implementation of data saving class for ACX driver samples.

    To save the playback data to disk, this class maintains a circular data
    buffer, associated frame structures and worker items to save frames to
    disk.
    Each frame structure represents a portion of buffer. When that portion
    of frame is full, a workitem is scheduled to save it to disk.



--*/
#pragma warning (disable : 4127)
#pragma warning (disable : 26165)


#include "private.h"
#include <devguid.h>
#include <ks.h>
#include <mmsystem.h>
#include <ksmedia.h>
#include "savedata.h"
#include <ntstrsafe.h>   // This is for using RtlStringCbPrintf

#define SAVEDATA_POOLTAG  'TDVS'
#define SAVEDATA_POOLTAG1 '1DVS'
#define SAVEDATA_POOLTAG2 '2DVS'
#define SAVEDATA_POOLTAG3 '3DVS'
#define SAVEDATA_POOLTAG4 '4DVS'
#define SAVEDATA_POOLTAG5 '5DVS'
#define SAVEDATA_POOLTAG6 '6DVS'
#define SAVEDATA_POOLTAG7 '7DVS'

//=============================================================================
// Defines
//=============================================================================
#define RIFF_TAG                    0x46464952;
#define WAVE_TAG                    0x45564157;
#define FMT__TAG                    0x20746D66;
#define DATA_TAG                    0x61746164;

#define DEFAULT_FRAME_COUNT         4
#define DEFAULT_FRAME_SIZE          PAGE_SIZE * 4 
#define DEFAULT_BUFFER_SIZE         DEFAULT_FRAME_SIZE * DEFAULT_FRAME_COUNT

#define DEFAULT_FILE_FOLDER1        L"\\DriverData\\Audio_Samples"
#define DEFAULT_FILE_FOLDER2        L"\\DriverData\\Audio_Samples\\AudioCodec"
#define DEFAULT_FILE_NAME           L"\\DriverData\\Audio_Samples\\AudioCodec\\STREAM"
#define OFFLOAD_FILE_NAME           L"OFFLOAD"
#define HOST_FILE_NAME              L"HOST"

#define MAX_WORKER_ITEM_COUNT       15


PSAVEWORKER_PARAM       CSaveData::m_pWorkItems = NULL;
PDEVICE_OBJECT          CSaveData::m_pDeviceObject = NULL;

//=============================================================================
// Statics
//=============================================================================
ULONG CSaveData::m_ulStreamId = 0;
ULONG CSaveData::m_ulOffloadStreamId = 0;

//=============================================================================
// CSaveData
//=============================================================================

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
CSaveData::CSaveData()
:   m_pDataBuffer(NULL),
    m_FileHandle(NULL),
    m_ulFrameCount(DEFAULT_FRAME_COUNT),
    m_ulBufferSize(DEFAULT_BUFFER_SIZE),
    m_ulFrameSize(DEFAULT_FRAME_SIZE),
    m_ulBufferOffset(0),
    m_ulFrameIndex(0),
    m_fFrameUsed(NULL),
    m_waveFormat(NULL),
    m_pFilePtr(NULL),
    m_fWriteDisabled(FALSE),
    m_bInitialized(FALSE)
{
    PAGED_CODE();

    m_FileHeader.dwRiff           = RIFF_TAG;
    m_FileHeader.dwFileSize       = 0;
    m_FileHeader.dwWave           = WAVE_TAG;
    m_FileHeader.dwFormat         = FMT__TAG;
    m_FileHeader.dwFormatLength   = sizeof(WAVEFORMATEX);

    m_DataHeader.dwData           = DATA_TAG;
    m_DataHeader.dwDataLength     = 0;

    RtlZeroMemory(&m_objectAttributes, sizeof(m_objectAttributes));
} // CSaveData

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
CSaveData::~CSaveData()
{
    PAGED_CODE();
    Cleanup();
} // CSaveData

void
_Use_decl_annotations_
PAGED_CODE_SEG
CSaveData::Cleanup
(
    void
)
{
    PAGED_CODE();

    // Update the wave header in data file with real file size.
    //
    if(m_pFilePtr)
    {
        // RIFF header, whose size is the whole file size minus RIFF header.
        m_FileHeader.dwFileSize =
            (DWORD)m_pFilePtr->QuadPart - 2 * sizeof(DWORD);
        // The data length is the size of all the audio that was written.
        // It gets calculated by taking:
        m_DataHeader.dwDataLength = (DWORD)m_pFilePtr->QuadPart -   // the whole file size,
            sizeof(m_FileHeader) -                                  // minus the file header,
            m_FileHeader.dwFormatLength -                           // minus the format,
            sizeof(m_DataHeader);                                   // minus the data header itself.

        if (STATUS_SUCCESS == KeWaitForSingleObject
            (
                &m_FileSync,
                Executive,
                KernelMode,
                FALSE,
                NULL
            ))
        {
            if (NT_SUCCESS(FileOpen(FALSE)))
            {
                FileWriteHeader();

                FileClose();
            }

            KeReleaseMutex(&m_FileSync, FALSE);
        }

        m_FileHeader.dwRiff           = RIFF_TAG;
        m_FileHeader.dwFileSize       = 0;
        m_FileHeader.dwWave           = WAVE_TAG;
        m_FileHeader.dwFormat         = FMT__TAG;
        m_FileHeader.dwFormatLength   = sizeof(WAVEFORMATEX);
        
        m_DataHeader.dwData           = DATA_TAG;
        m_DataHeader.dwDataLength     = 0;
        m_pFilePtr = NULL;
    }

    if (m_waveFormat)
    {
        ExFreePoolWithTag(m_waveFormat, SAVEDATA_POOLTAG1);
        m_waveFormat = NULL;
    }

    if (m_fFrameUsed)
    {
        ExFreePoolWithTag(m_fFrameUsed, SAVEDATA_POOLTAG2);
        m_fFrameUsed = NULL;
    }

    if (m_FileName.Buffer)
    {
        ExFreePoolWithTag(m_FileName.Buffer, SAVEDATA_POOLTAG3);
        m_FileName.Buffer = NULL;
    }

    if (m_pDataBuffer)
    {
        ExFreePoolWithTag(m_pDataBuffer, SAVEDATA_POOLTAG4);
        m_pDataBuffer = NULL;
    }
}

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
void
CSaveData::DestroyWorkItems
(
    void
)
{
    PAGED_CODE();

    if (m_pWorkItems)
    {
        for (int i = 0; i < MAX_WORKER_ITEM_COUNT; i++)
        {
            if (m_pWorkItems[i].WorkItem!=NULL)
            {
                IoFreeWorkItem(m_pWorkItems[i].WorkItem);
                m_pWorkItems[i].WorkItem = NULL;
            }
        }
        ExFreePoolWithTag(m_pWorkItems, SAVEDATA_POOLTAG);
        m_pWorkItems = NULL;
    }

} // DestroyWorkItems

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
void
CSaveData::Disable
(
    _In_ BOOL                  fDisable
)
{
    PAGED_CODE();

    m_fWriteDisabled = fDisable;
} // Disable

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::FileClose(void)
{
    PAGED_CODE();

    NTSTATUS                    ntStatus = STATUS_SUCCESS;

    if (m_FileHandle)
    {
        ntStatus = ZwClose(m_FileHandle);
        m_FileHandle = NULL;
    }

    return ntStatus;
} // FileClose

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::FileOpen
(
    _In_  BOOL                  fOverWrite
)
{
    PAGED_CODE();

    NTSTATUS                    ntStatus = STATUS_SUCCESS;
    IO_STATUS_BLOCK             ioStatusBlock;

    if( FALSE == m_bInitialized )
    {
        return STATUS_UNSUCCESSFUL;
    }

    if(!m_FileHandle)
    {
        ntStatus =
            ZwCreateFile
            (
                &m_FileHandle,
                GENERIC_WRITE | SYNCHRONIZE,
                &m_objectAttributes,
                &ioStatusBlock,
                NULL,
                FILE_ATTRIBUTE_NORMAL,
                0,
                fOverWrite ? FILE_OVERWRITE_IF : FILE_OPEN_IF,
                FILE_NON_DIRECTORY_FILE | FILE_SYNCHRONOUS_IO_NONALERT,
                NULL,
                0
            );
    }

    return ntStatus;
} // FileOpen

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::FileWrite
(
    _In_reads_bytes_(ulDataSize)    PBYTE   pData,
    _In_                            ULONG   ulDataSize
)
{
    PAGED_CODE();

    ASSERT(pData);
    ASSERT(m_pFilePtr);

    NTSTATUS                    ntStatus;

    if (m_FileHandle)
    {
        IO_STATUS_BLOCK         ioStatusBlock;

        ntStatus = ZwWriteFile( m_FileHandle,
                                NULL,
                                NULL,
                                NULL,
                                &ioStatusBlock,
                                pData,
                                ulDataSize,
                                m_pFilePtr,
                                NULL);

        if (NT_SUCCESS(ntStatus))
        {
            ASSERT(ioStatusBlock.Information == ulDataSize);

            m_pFilePtr->QuadPart += ulDataSize;
        }
    }
    else
    {
        ntStatus = STATUS_INVALID_HANDLE;
    }

    return ntStatus;
} // FileWrite

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::FileWriteHeader(void)
{
    PAGED_CODE();

    NTSTATUS                    ntStatus;

    if (m_FileHandle && m_waveFormat)
    {
        IO_STATUS_BLOCK         ioStatusBlock;

        m_pFilePtr->QuadPart = 0;

        m_FileHeader.dwFormatLength = (m_waveFormat->wFormatTag == WAVE_FORMAT_PCM) ?
                                        sizeof( PCMWAVEFORMAT ) :
                                        sizeof( WAVEFORMATEX ) + m_waveFormat->cbSize;

        ntStatus = ZwWriteFile( m_FileHandle,
                                NULL,
                                NULL,
                                NULL,
                                &ioStatusBlock,
                                &m_FileHeader,
                                sizeof(m_FileHeader),
                                m_pFilePtr,
                                NULL);

        if (NT_SUCCESS(ntStatus))
        {
            m_pFilePtr->QuadPart += sizeof(m_FileHeader);

            ntStatus = ZwWriteFile( m_FileHandle,
                                    NULL,
                                    NULL,
                                    NULL,
                                    &ioStatusBlock,
                                    m_waveFormat,
                                    m_FileHeader.dwFormatLength,
                                    m_pFilePtr,
                                    NULL);
        }

        if (NT_SUCCESS(ntStatus))
        {
            m_pFilePtr->QuadPart += m_FileHeader.dwFormatLength;

            ntStatus = ZwWriteFile( m_FileHandle,
                                    NULL,
                                    NULL,
                                    NULL,
                                    &ioStatusBlock,
                                    &m_DataHeader,
                                    sizeof(m_DataHeader),
                                    m_pFilePtr,
                                    NULL);
        }

        if (NT_SUCCESS(ntStatus))
        {
            m_pFilePtr->QuadPart += sizeof(m_DataHeader);
        }
    }
    else
    {
        ntStatus = STATUS_INVALID_HANDLE;
    }


    return ntStatus;
} // FileWriteHeader

_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::SetDeviceObject
(
    _In_  PDEVICE_OBJECT        DeviceObject
)
{
    PAGED_CODE();

    ASSERT(DeviceObject);

    NTSTATUS                    ntStatus = STATUS_SUCCESS;
    
    m_pDeviceObject = DeviceObject;
    return ntStatus;
}

_Use_decl_annotations_
PAGED_CODE_SEG
PDEVICE_OBJECT
CSaveData::GetDeviceObject
(
    void
)
{
    PAGED_CODE();

    return m_pDeviceObject;
}

//=============================================================================
_Use_decl_annotations_
PSAVEWORKER_PARAM
CSaveData::GetNewWorkItem
(
    void
)
{
    LARGE_INTEGER               timeOut = { 0 };
    NTSTATUS                    ntStatus;

    for (int i = 0; i < MAX_WORKER_ITEM_COUNT; i++)
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
} // GetNewWorkItem

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::Initialize
(
    _In_ BOOL       _bOffloaded
)
{
    PAGED_CODE();

    NTSTATUS            ntStatus = STATUS_SUCCESS;
    WCHAR               szTemp[MAX_PATH];
    size_t              cLen = 0;
    IO_STATUS_BLOCK     ioStatusBlock = {0};
    HANDLE              fileHandle;
    OBJECT_ATTRIBUTES   objectAttributes;
    UNICODE_STRING      fileName;

    if (_bOffloaded)
    {
        m_ulOffloadStreamId++;
    }
    else
    {
        m_ulStreamId++;
    }

    RtlInitUnicodeString(&fileName, DEFAULT_FILE_FOLDER1);
    InitializeObjectAttributes(
            &objectAttributes,
            &fileName,
            OBJ_KERNEL_HANDLE | OBJ_CASE_INSENSITIVE,
            NULL,
            NULL);

    // Create the folder.
    ntStatus = ZwCreateFile(
            &fileHandle,
            0,
            &objectAttributes,
            &ioStatusBlock,
            NULL,
            FILE_ATTRIBUTE_NORMAL,
            0,
            FILE_OPEN_IF,
            FILE_DIRECTORY_FILE | FILE_SYNCHRONOUS_IO_NONALERT,
            NULL,
            0);

    if (NT_SUCCESS(ntStatus))
    {
        ZwClose(fileHandle);
        fileHandle = NULL;

        RtlInitUnicodeString(&fileName, DEFAULT_FILE_FOLDER2);
        InitializeObjectAttributes(
                &objectAttributes,
                &fileName,
                OBJ_KERNEL_HANDLE | OBJ_CASE_INSENSITIVE,
                NULL,
                NULL);

        // Create the folder.
        ntStatus = ZwCreateFile(
                &fileHandle,
                0,
                &objectAttributes,
                &ioStatusBlock,
                NULL,
                FILE_ATTRIBUTE_NORMAL,
                0,
                FILE_OPEN_IF,
                FILE_DIRECTORY_FILE | FILE_SYNCHRONOUS_IO_NONALERT,
                NULL,
                0);

        if (NT_SUCCESS(ntStatus))
        {
            ZwClose(fileHandle);
            fileHandle = NULL;
        }
    }

    if (NT_SUCCESS(ntStatus))
    {
        // Allocate data file name.
        //
        RtlStringCchPrintfW(szTemp, MAX_PATH, L"%s_%s_%d.wav", DEFAULT_FILE_NAME, _bOffloaded ? OFFLOAD_FILE_NAME : HOST_FILE_NAME, _bOffloaded ? m_ulOffloadStreamId : m_ulStreamId);
        m_FileName.Length = 0;
        ntStatus = RtlStringCchLengthW (szTemp, sizeof(szTemp)/sizeof(szTemp[0]), &cLen);
    }

    if (NT_SUCCESS(ntStatus))
    {
        m_FileName.MaximumLength = (USHORT)((cLen * sizeof(WCHAR)) + sizeof(WCHAR));//convert to wchar and add room for NULL
        m_FileName.Buffer = (PWSTR)
            ExAllocatePool2
            (
                POOL_FLAG_PAGED,
                m_FileName.MaximumLength,
                SAVEDATA_POOLTAG3
            );
        if (!m_FileName.Buffer)
        {
            ntStatus = STATUS_INSUFFICIENT_RESOURCES;
        }
    }

    // Allocate memory for data buffer.
    //
    if (NT_SUCCESS(ntStatus))
    {
        RtlStringCbCopyW(m_FileName.Buffer, m_FileName.MaximumLength, szTemp);
        m_FileName.Length = (USHORT)wcslen(m_FileName.Buffer) * sizeof(WCHAR);

        m_pDataBuffer = (PBYTE)
            ExAllocatePool2
            (
                POOL_FLAG_NON_PAGED,
                m_ulBufferSize,
                SAVEDATA_POOLTAG4
            );
        if (!m_pDataBuffer)
        {
            ntStatus = STATUS_INSUFFICIENT_RESOURCES;
        }
    }

    // Allocate memory for frame usage flags and m_pFilePtr.
    //
    if (NT_SUCCESS(ntStatus))
    {
        m_fFrameUsed = (PBOOL)
            ExAllocatePool2
            (
                POOL_FLAG_NON_PAGED,
                m_ulFrameCount * sizeof(BOOL) +
                sizeof(LARGE_INTEGER),
                SAVEDATA_POOLTAG2
            );
        if (!m_fFrameUsed)
        {
            ntStatus = STATUS_INSUFFICIENT_RESOURCES;
        }
    }

    // Initialize the spinlock to synchronize access to the frames
    //
    KeInitializeSpinLock ( &m_FrameInUseSpinLock ) ;

    // Initialize the file mutex
    //
    KeInitializeMutex( &m_FileSync, 1 ) ;

    // Open the data file.
    //
    if (NT_SUCCESS(ntStatus))
    {
        // m_fFrameUsed has additional memory to hold m_pFilePtr
        //
        m_pFilePtr = (PLARGE_INTEGER)
            (((PBYTE) m_fFrameUsed) + m_ulFrameCount * sizeof(BOOL));

        // Create data file.
        InitializeObjectAttributes
        (
            &m_objectAttributes,
            &m_FileName,
            OBJ_CASE_INSENSITIVE | OBJ_KERNEL_HANDLE,
            NULL,
            NULL
        );

        m_bInitialized = TRUE;

        // Write wave header information to data file.
        ntStatus = KeWaitForSingleObject
            (
                &m_FileSync,
                Executive,
                KernelMode,
                FALSE,
                NULL
            );

        if (STATUS_SUCCESS == ntStatus)
        {
            ntStatus = FileOpen(TRUE);
            if (NT_SUCCESS(ntStatus))
            {
                ntStatus = FileWriteHeader();

                FileClose();
            }

            KeReleaseMutex( &m_FileSync, FALSE );
        }
    }

    return ntStatus;
} // Initialize

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::InitializeWorkItems
(
    _In_  PDEVICE_OBJECT        DeviceObject
)
{
    PAGED_CODE();

    ASSERT(DeviceObject);

    NTSTATUS                    ntStatus = STATUS_SUCCESS;

    if (m_pWorkItems != NULL)
    {
        return ntStatus;
    }

    m_pWorkItems = (PSAVEWORKER_PARAM)
        ExAllocatePool2
        (
            POOL_FLAG_NON_PAGED,
            sizeof(SAVEWORKER_PARAM) * MAX_WORKER_ITEM_COUNT,
            SAVEDATA_POOLTAG
        );
    if (m_pWorkItems)
    {
        for (int i = 0; i < MAX_WORKER_ITEM_COUNT; i++)
        {

            m_pWorkItems[i].WorkItem = IoAllocateWorkItem(DeviceObject);
            if(m_pWorkItems[i].WorkItem == NULL)
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
} // InitializeWorkItems

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
VOID
SaveFrameWorkerCallback
(
    _In_        PDEVICE_OBJECT         pDeviceObject, 
    _In_opt_    PVOID                  Context
)
{
    UNREFERENCED_PARAMETER(pDeviceObject);

    PAGED_CODE();

    ASSERT(Context);

    PSAVEWORKER_PARAM           pParam = (PSAVEWORKER_PARAM) Context;
    PCSaveData                  pSaveData;
    
    if (NULL == pParam)
    {
        // This is completely unexpected, assert here.
        //
        ASSERT(pParam);
        return;
    }

    ASSERT(pParam->pSaveData);
    ASSERT(pParam->pSaveData->m_fFrameUsed);

    if (pParam->WorkItem)
    {
        pSaveData = pParam->pSaveData;

        if (STATUS_SUCCESS == KeWaitForSingleObject
            (
                &pSaveData->m_FileSync,
                Executive,
                KernelMode,
                FALSE,
                NULL
            ))
        {
            if (NT_SUCCESS(pSaveData->FileOpen(FALSE)))
            { 
                pSaveData->FileWrite(pParam->pData, pParam->ulDataSize);
                pSaveData->FileClose();
            }
            InterlockedExchange( (LONG *)&(pSaveData->m_fFrameUsed[pParam->ulFrameNo]), FALSE );

            KeReleaseMutex( &pSaveData->m_FileSync, FALSE );
        }
    }

    KeSetEvent(&pParam->EventDone, 0, FALSE);
} // SaveFrameWorkerCallback

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::SetDataFormat
(
    _In_ PKSDATAFORMAT          pDataFormat
)
{
    PAGED_CODE();
    NTSTATUS                    ntStatus = STATUS_SUCCESS;
 
    ASSERT(pDataFormat);

    PWAVEFORMATEX pwfx = NULL;

    if (IsEqualGUIDAligned(pDataFormat->Specifier,
        KSDATAFORMAT_SPECIFIER_DSOUND))
    {
        pwfx =
            &(((PKSDATAFORMAT_DSOUND) pDataFormat)->BufferDesc.WaveFormatEx);
    }
    else if (IsEqualGUIDAligned(pDataFormat->Specifier,
        KSDATAFORMAT_SPECIFIER_WAVEFORMATEX))
    {
        pwfx = &((PKSDATAFORMAT_WAVEFORMATEX) pDataFormat)->WaveFormatEx;
    }

    if (pwfx)
    {
        // Free the previously allocated waveformat
        if (m_waveFormat)
        {
            ExFreePoolWithTag(m_waveFormat, SAVEDATA_POOLTAG1);
        }

        m_waveFormat = (PWAVEFORMATEX)
            ExAllocatePool2
            (
                POOL_FLAG_NON_PAGED,
                (pwfx->wFormatTag == WAVE_FORMAT_PCM) ?
                sizeof( PCMWAVEFORMAT ) :
                sizeof( WAVEFORMATEX ) + pwfx->cbSize,
                SAVEDATA_POOLTAG1
            );

        if(m_waveFormat)
        {
            RtlCopyMemory( m_waveFormat,
                           pwfx,
                           (pwfx->wFormatTag == WAVE_FORMAT_PCM) ?
                           sizeof( PCMWAVEFORMAT ) :
                           sizeof( WAVEFORMATEX ) + pwfx->cbSize);
        }
        else
        {
            ntStatus = STATUS_INSUFFICIENT_RESOURCES;
        }
    }
    return ntStatus;
} // SetDataFormat

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
NTSTATUS
CSaveData::SetMaxWriteSize
(
    _In_ ULONG ulMaxWriteSize
)
{
    PAGED_CODE();
    
    NTSTATUS    ntStatus    = STATUS_SUCCESS;
    ULONG       bufferSize  = 0;
    PBYTE       buffer      = NULL;
 
    // 
    // Compute new buffer size.
    //
    ntStatus = RtlULongMult(ulMaxWriteSize, DEFAULT_FRAME_COUNT, &bufferSize);
    if (!NT_SUCCESS(ntStatus))
    {
        ntStatus = STATUS_INSUFFICIENT_RESOURCES;
        goto Done;
    }

    //
    // Alloc memory for buffer.
    //
    buffer = (PBYTE)
        ExAllocatePool2
        (
            POOL_FLAG_NON_PAGED,
            bufferSize,
            SAVEDATA_POOLTAG4
        );
    if (!buffer)
    {
        ntStatus = STATUS_INSUFFICIENT_RESOURCES;
        goto Done;
    }

    //
    // Free old one.
    //
    if (m_pDataBuffer)
    {
        ExFreePoolWithTag(m_pDataBuffer, SAVEDATA_POOLTAG4);
        m_pDataBuffer = NULL;
    }

    //
    // Init new buffer settings.
    //
    m_pDataBuffer  = buffer;
    m_ulBufferSize = bufferSize;
    m_ulFrameSize  = ulMaxWriteSize;
    
    ntStatus = STATUS_SUCCESS;

Done:
    return ntStatus;
} // SetDataFormat

//=============================================================================
_Use_decl_annotations_
PAGED_CODE_SEG
void
CSaveData::ReadData
(
    _Inout_updates_bytes_all_(ulByteCount)  PBYTE   pBuffer,
    _In_                                    ULONG   ulByteCount
)
{
    UNREFERENCED_PARAMETER(pBuffer);
    UNREFERENCED_PARAMETER(ulByteCount);

    PAGED_CODE();

    // Not implemented yet.
} // ReadData

//=============================================================================
_Use_decl_annotations_
void
CSaveData::SaveFrame
(
    _In_ ULONG                  ulFrameNo,
    _In_ ULONG                  ulDataSize
)
{
    PSAVEWORKER_PARAM           pParam = NULL;

    pParam = GetNewWorkItem();
    if (pParam)
    {
        pParam->pSaveData = this;
        pParam->ulFrameNo = ulFrameNo;
        pParam->ulDataSize = ulDataSize;
        pParam->pData = m_pDataBuffer + ulFrameNo * m_ulFrameSize;
        KeResetEvent(&pParam->EventDone);
        IoQueueWorkItem(pParam->WorkItem, SaveFrameWorkerCallback,
                        CriticalWorkQueue, (PVOID)pParam);
    }
} // SaveFrame

//=============================================================================
void
_Use_decl_annotations_
PAGED_CODE_SEG
CSaveData::WaitAllWorkItems
(
    void
)
{
    PAGED_CODE();

    // Save the last partially-filled frame
    if (m_ulBufferOffset > m_ulFrameIndex * m_ulFrameSize)
    {
        ULONG size;
        
        size = m_ulBufferOffset - m_ulFrameIndex * m_ulFrameSize;
        SaveFrame(m_ulFrameIndex, size);
    }
    
    for (int i = 0; i < MAX_WORKER_ITEM_COUNT; i++)
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
} // WaitAllWorkItems

//=============================================================================
_Use_decl_annotations_
void
CSaveData::WriteData
(
    _In_reads_bytes_(ulByteCount)   PBYTE   pBuffer,
    _In_                            ULONG   ulByteCount
)
{
    ASSERT(pBuffer);

    BOOL                        fSaveFrame = FALSE;
    ULONG                       ulSaveFrameIndex = 0;
    KIRQL                       oldIrql;

    // If stream writing is disabled, then exit.
    //
    if (m_fWriteDisabled)
    {
        return;
    }

    if( 0 == ulByteCount )
    {
        return;
    }

    // The logic below assumes that write size is <= than frame size.
    if (ulByteCount > m_ulFrameSize)
    {
        ulByteCount = m_ulFrameSize;
    }
        
    // Check to see if this frame is available.
    KeAcquireSpinLock(&m_FrameInUseSpinLock, &oldIrql);
    if (!m_fFrameUsed[m_ulFrameIndex])
    {
        KeReleaseSpinLock(&m_FrameInUseSpinLock, oldIrql );

        ULONG ulWriteBytes = ulByteCount;

        if( (m_ulBufferSize - m_ulBufferOffset) < ulWriteBytes )
        {
            ulWriteBytes = m_ulBufferSize - m_ulBufferOffset;
        }

        RtlCopyMemory(m_pDataBuffer + m_ulBufferOffset, pBuffer, ulWriteBytes);
        m_ulBufferOffset += ulWriteBytes;

        // Check to see if we need to save this frame
        if (m_ulBufferOffset >= ((m_ulFrameIndex + 1) * m_ulFrameSize))
        {
            fSaveFrame = TRUE;
        }

        // Loop the buffer, if we reached the end.
        if (m_ulBufferOffset == m_ulBufferSize)
        {
            fSaveFrame = TRUE;
            m_ulBufferOffset = 0;
        }

        if (fSaveFrame)
        {
            InterlockedExchange( (LONG *)&(m_fFrameUsed[m_ulFrameIndex]), TRUE );
            ulSaveFrameIndex = m_ulFrameIndex;
            m_ulFrameIndex = (m_ulFrameIndex + 1) % m_ulFrameCount;
        }

        // Write the left over if the next frame is available.
        if (ulWriteBytes != ulByteCount)
        {
            KeAcquireSpinLock(&m_FrameInUseSpinLock, &oldIrql );
            if (!m_fFrameUsed[m_ulFrameIndex])
            {
                KeReleaseSpinLock(&m_FrameInUseSpinLock, oldIrql );
                RtlCopyMemory
                (
                    m_pDataBuffer + m_ulBufferOffset,
                    pBuffer + ulWriteBytes,
                    ulByteCount - ulWriteBytes
                );
                
                m_ulBufferOffset += ulByteCount - ulWriteBytes;
            }
            else
            {
                KeReleaseSpinLock(&m_FrameInUseSpinLock, oldIrql);
            }
        }

        if (fSaveFrame)
        {
            SaveFrame(ulSaveFrameIndex, m_ulFrameSize);
        }
    }
    else
    {
        KeReleaseSpinLock(&m_FrameInUseSpinLock, oldIrql );
    }

} // WriteData


