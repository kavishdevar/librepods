/*++

Copyright (c) Microsoft Corporation.  All rights reserved.

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    SaveData.h

Abstract:

    Declaration of data saving class for ACX driver samples. This class supplies services
to save data to disk.


--*/

#pragma once

//-----------------------------------------------------------------------------
//  Forward declaration
//-----------------------------------------------------------------------------
class CSaveData;
typedef CSaveData *PCSaveData;


//-----------------------------------------------------------------------------
//  Structs
//-----------------------------------------------------------------------------

// Parameter to workitem.
#include <pshpack1.h>
typedef struct _SAVEWORKER_PARAM {
    PIO_WORKITEM     WorkItem;
    ULONG            ulFrameNo;
    ULONG            ulDataSize;
    PBYTE            pData;
    PCSaveData       pSaveData;
    KEVENT           EventDone;
} SAVEWORKER_PARAM;
typedef SAVEWORKER_PARAM *PSAVEWORKER_PARAM;
#include <poppack.h>

// wave file header.
#include <pshpack1.h>
typedef struct _OUTPUT_FILE_HEADER
{
    DWORD           dwRiff;
    DWORD           dwFileSize;
    DWORD           dwWave;
    DWORD           dwFormat;
    DWORD           dwFormatLength;
} OUTPUT_FILE_HEADER;
typedef OUTPUT_FILE_HEADER *POUTPUT_FILE_HEADER;

typedef struct _OUTPUT_DATA_HEADER
{
    DWORD           dwData;
    DWORD           dwDataLength;
} OUTPUT_DATA_HEADER;
typedef OUTPUT_DATA_HEADER *POUTPUT_DATA_HEADER;

#include <poppack.h>

//-----------------------------------------------------------------------------
//  Classes
//-----------------------------------------------------------------------------

///////////////////////////////////////////////////////////////////////////////
// CSaveData
//   Saves the wave data to disk.
//
__drv_maxIRQL(PASSIVE_LEVEL)
PAGED_CODE_SEG
IO_WORKITEM_ROUTINE SaveFrameWorkerCallback;

class CSaveData
{
protected:
    UNICODE_STRING              m_FileName;         // DataFile name.
    HANDLE                      m_FileHandle;       // DataFile handle.
    PBYTE                       m_pDataBuffer;      // Data buffer.
    ULONG                       m_ulBufferSize;     // Total buffer size.

    ULONG                       m_ulFrameIndex;     // Current Frame.
    ULONG                       m_ulFrameCount;     // Frame count.
    ULONG                       m_ulFrameSize;
    ULONG                       m_ulBufferOffset;   // index in buffer.
    PBOOL                       m_fFrameUsed;       // Frame usage table.
    KSPIN_LOCK                  m_FrameInUseSpinLock; // Spinlock for synch.
    KMUTEX                      m_FileSync;         // Synchronizes file access

    OBJECT_ATTRIBUTES           m_objectAttributes; // Used for opening file.

    OUTPUT_FILE_HEADER          m_FileHeader;
    PWAVEFORMATEX               m_waveFormat;
    OUTPUT_DATA_HEADER          m_DataHeader;
    PLARGE_INTEGER              m_pFilePtr;

    static PDEVICE_OBJECT       m_pDeviceObject;
    static ULONG                m_ulStreamId;
    static ULONG                m_ulOffloadStreamId;
    static PSAVEWORKER_PARAM    m_pWorkItems;

    BOOL                        m_fWriteDisabled;

    BOOL                        m_bInitialized;

public:
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    CSaveData();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    ~CSaveData();

    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    void
    Cleanup(
        void
        );

    static
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS             
    InitializeWorkItems(
        _In_  PDEVICE_OBJECT    DeviceObject
        );

    static
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    void
    DestroyWorkItems(
        void
        );
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    void
    Disable(
        _In_ BOOL               fDisable
        );
    
    static
    __drv_maxIRQL(DISPATCH_LEVEL)
    PSAVEWORKER_PARAM
    GetNewWorkItem(
        void
        );
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    Initialize(
        _In_ BOOL               _bOffloaded
        );
	
    static
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    SetDeviceObject(
	    _In_  PDEVICE_OBJECT    DeviceObject
	    );
	
    static
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    PDEVICE_OBJECT
    GetDeviceObject(
	    void
	    );
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    void
    ReadData(
        _Inout_updates_bytes_all_(ulByteCount)  PBYTE   pBuffer,
        _In_                                    ULONG   ulByteCount
        );
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    SetDataFormat(
        _In_  PKSDATAFORMAT     pDataFormat
        );
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    SetMaxWriteSize(
        _In_  ULONG             ulMaxWriteSize
        );  
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    void
    WaitAllWorkItems(
        void
        );

    __drv_maxIRQL(DISPATCH_LEVEL)
    void
    WriteData(
        _In_reads_bytes_(ulByteCount)   PBYTE   pBuffer,
        _In_                            ULONG   ulByteCount
        );

private:
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    FileClose(
        void
        );
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    FileOpen(
        _In_  BOOL              fOverWrite
        );
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    FileWrite(
        _In_reads_bytes_(ulDataSize)    PBYTE   pData,
        _In_                            ULONG   ulDataSize
        );
    
    __drv_maxIRQL(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS
    FileWriteHeader(
        void
        );

    __drv_maxIRQL(DISPATCH_LEVEL)
    void
    SaveFrame(
        _In_ ULONG              ulFrameNo,
        _In_ ULONG              ulDataSize
        );

    friend
    IO_WORKITEM_ROUTINE         SaveFrameWorkerCallback;
};
typedef CSaveData *PCSaveData;

