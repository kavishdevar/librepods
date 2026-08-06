/*++

    THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
    KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
    PURPOSE.

Module Name:

    KeywordDetector.cpp

Abstract:

    Sample keyword detector management.

Environment:

    Kernel mode

--*/

#include "private.h"
#include "public.h"
#include <ks.h>
#include <mmsystem.h>
#include <ksmedia.h>
#include "streamengine.h"
#include "KeywordDetector.h"

PAGED_CODE_SEG
CKeywordDetector::CKeywordDetector()
    :
    m_streamRunning(FALSE),
    m_qpcStartCapture(0),
    m_nLastQueuedPacket(-1),
    m_SoundDetectorArmed1(FALSE),
    m_SoundDetectorArmed2(FALSE),
    m_SoundDetectorData1(0),
    m_SoundDetectorData2(0),
    m_ullKeywordStartTimestamp(0),
    m_ullKeywordStopTimestamp(0)
{
    PAGED_CODE();

    // Initialize our pool of packets and the list structures
    KeInitializeSpinLock(&PacketPoolSpinLock);
    KeInitializeSpinLock(&PacketFifoSpinLock);
    ResetFifo();
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
NTSTATUS CKeywordDetector::ReadKeywordTimestampRegistry()
{
    PAGED_CODE();

    NTSTATUS                    ntStatus;
    PDRIVER_OBJECT              DriverObject;
    HANDLE                      DriverKey;

    RTL_QUERY_REGISTRY_TABLE    paramTable[] = {
        // QueryRoutine     Flags                                           Name                                EntryContext                    DefaultType                                                     DefaultData                         DefaultLength
        { NULL,   RTL_QUERY_REGISTRY_DIRECT | RTL_QUERY_REGISTRY_TYPECHECK, L"KeywordDetectorStartTimestamp",   &m_ullKeywordStartTimestamp,    (REG_QWORD << RTL_QUERY_REGISTRY_TYPECHECK_SHIFT) | REG_QWORD,  &m_ullKeywordStartTimestamp,        sizeof(ULONGLONG) },
        { NULL,   RTL_QUERY_REGISTRY_DIRECT | RTL_QUERY_REGISTRY_TYPECHECK, L"KeywordDetectorStopTimestamp",    &m_ullKeywordStopTimestamp,     (REG_QWORD << RTL_QUERY_REGISTRY_TYPECHECK_SHIFT) | REG_QWORD,  &m_ullKeywordStopTimestamp,         sizeof(ULONGLONG) },
        { NULL,   0,                                                        NULL,                               NULL,                           0,                                                              NULL,                               0 }
    };

    DriverObject = WdfDriverWdmGetDriverObject(WdfGetDriver());
    DriverKey = NULL;
    ntStatus = IoOpenDriverRegistryKey(DriverObject, 
                                 DriverRegKeyParameters,
                                 KEY_READ,
                                 0,
                                 &DriverKey);

    if (!NT_SUCCESS(ntStatus))
    {
        return ntStatus;
    }

    ntStatus = RtlQueryRegistryValues(RTL_REGISTRY_HANDLE,
                                  (PCWSTR) DriverKey,
                                  &paramTable[0],
                                  NULL,
                                  NULL);
    if (DriverKey)
    {
        ZwClose(DriverKey);
    }

    return ntStatus;
}


PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
NTSTATUS CKeywordDetector::ResetDetector(_In_ GUID eventId)
{
    PAGED_CODE();

    if (eventId == CONTOSO_KEYWORD1)
    {
        m_SoundDetectorData1 = 0;
        m_SoundDetectorArmed1 = FALSE;
    }
    else if(eventId == CONTOSO_KEYWORD2)
    {
        m_SoundDetectorData2 = 0;
        m_SoundDetectorArmed2 = FALSE;
    }
    else if(eventId == GUID_NULL)
    {
        // When DownloadDetectorData is called to set the pattern for multiple keywords
        // at once, all keyword detectors must be reset. Also used during keyword detector
        // initialization and cleanup to restore it back to initial state and power down.
        m_SoundDetectorData1 = 0;
        m_SoundDetectorArmed1 = FALSE;
        m_SoundDetectorData2 = 0;
        m_SoundDetectorArmed2 = FALSE;
    }
    else
    {
        return STATUS_INVALID_PARAMETER;
    }

    return STATUS_SUCCESS;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
NTSTATUS CKeywordDetector::DownloadDetectorData(_In_ GUID eventId, _In_ LONGLONG Data)
{
    PAGED_CODE();

    // reset the detector for this event Id
    ResetDetector(eventId);

    // In this example, the driver supports detection data 
    // set with a single call for both detectors, or each
    // detector set individually.
    if (eventId == CONTOSO_KEYWORD1)
    {
        m_SoundDetectorData1 = Data;
    }
    else if(eventId == CONTOSO_KEYWORD2)
    {
        m_SoundDetectorData2 = Data;
    }
    else if(eventId == GUID_NULL)
    {
        // in this simplified example "Data" is set on both detectors,
        // however in a real system "Data" could be a data structure which
        // contains different values for each detector.
        m_SoundDetectorData1 = m_SoundDetectorData2 = Data;
    }
    else
    {
        return STATUS_INVALID_PARAMETER;
    }

    return STATUS_SUCCESS;
}

// The following function is only applicable to single keyword detection systems,
// and assumes keyword detector #1.
PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
NTSTATUS CKeywordDetector::GetDetectorData(_In_ GUID eventId, _Out_ LONGLONG *Data)
{
    PAGED_CODE();

    if (eventId == CONTOSO_KEYWORD1)
    {
        *Data = m_SoundDetectorData1;
    }
    else if(eventId == CONTOSO_KEYWORD2)
    {
        *Data = m_SoundDetectorData2;
    }
    else
    {
        return STATUS_INVALID_PARAMETER;
    }


    return STATUS_SUCCESS;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
ULONGLONG CKeywordDetector::GetStartTimestamp()
{
    PAGED_CODE();

    return m_ullKeywordStartTimestamp;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
ULONGLONG CKeywordDetector::GetStopTimestamp()
{
    PAGED_CODE();

    return m_ullKeywordStopTimestamp;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
VOID CKeywordDetector::ResetFifo()
{
    PAGED_CODE();

    m_qpcStartCapture = 0;
    m_nLastQueuedPacket = (-1);
    InitializeListHead(&PacketPoolHead);
    InitializeListHead(&PacketFifoHead);

    for (int i = 0; i < ARRAYSIZE(PacketPool); i++)
    {
        InsertTailList(&PacketPoolHead, &PacketPool[i].ListEntry);
    }
    return;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
NTSTATUS CKeywordDetector::SetArmed(_In_ GUID eventId, _In_ BOOLEAN Arm)
{
    PAGED_CODE();

    BOOL previousArming = FALSE;
    NTSTATUS ntStatus = STATUS_SUCCESS;

    // the previous state is "armed" if either detector is armed.
    // this reflects the fact that both detectors are sharing the
    // same stream.
    previousArming = m_SoundDetectorArmed1 || m_SoundDetectorArmed2;

    if (eventId == CONTOSO_KEYWORD1)
    {
        m_SoundDetectorArmed1 = Arm;
    }
    else if(eventId == CONTOSO_KEYWORD2)
    {
        m_SoundDetectorArmed2 = Arm;
    }
    else
    {
        return STATUS_INVALID_PARAMETER;
    }

    if (Arm && !previousArming && m_qpcStartCapture == 0)
    {
        StartBufferingStream();
    }
    else if (!Arm && previousArming && !m_streamRunning)
    {
        // if it's not actively streaming and everything has been disarmed,
        // then stop buffering.
        ResetFifo();
    }

    return ntStatus;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
NTSTATUS CKeywordDetector::GetArmed(_In_ GUID eventId, _Out_ BOOLEAN *Arm)
{
    PAGED_CODE();
    NTSTATUS ntStatus = STATUS_SUCCESS;

    if (eventId == CONTOSO_KEYWORD1)
    {
        *Arm = m_SoundDetectorArmed1;
    }
    else if(eventId == CONTOSO_KEYWORD2)
    {
        *Arm = m_SoundDetectorArmed2;
    }
    else
    {
        return STATUS_INVALID_PARAMETER;
    }

    return ntStatus;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
VOID CKeywordDetector::Run()
{
    PAGED_CODE();

    if (m_qpcStartCapture == 0)
    {
        StartBufferingStream();
    }

    m_streamRunning = TRUE;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
VOID CKeywordDetector::Stop()
{
    PAGED_CODE();

    ResetFifo();
    m_streamRunning = FALSE;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
VOID CKeywordDetector::StartBufferingStream()
{
    LARGE_INTEGER qpc;
    LARGE_INTEGER qpcFrequency;

    PAGED_CODE();

    qpc = KeQueryPerformanceCounter(&qpcFrequency);
    m_qpcStartCapture = qpc.QuadPart;
    m_qpcFrequency = qpcFrequency.QuadPart;

    return;
}

PAGED_CODE_SEG
_IRQL_requires_max_(PASSIVE_LEVEL)
VOID CKeywordDetector::NotifyDetection()
{
    PAGED_CODE();

    // A detection will only happen if armed and the
    // stream is already running. If there isn't a client
    // running, then set the stream start time to align
    // with this detection.
    if (!m_streamRunning)
    {
        StartBufferingStream();

        // The following code is for testing purposes only.
        // m_qpcFrequency is defined to be the number of ticks in 1 second.
        // Use the stream start time (the current time retrieved in StartBufferStream) to 
        // mark when the keyword ended, and the start time minus 1 second worth of ticks
        // to mark when the keyword started. Also, adjust the stream start time to align
        // to this new keyword start time, so that the simulated stream contains the full keyword.

        m_ullKeywordStopTimestamp = m_qpcStartCapture; // stop time is the current time
        m_qpcStartCapture = m_qpcStartCapture - m_qpcFrequency; // buffer start time is 1 second ago
        m_ullKeywordStartTimestamp = m_qpcStartCapture; // buffer start time = keyword start time

    }
    else
    {
        // The following code is for testing purposes only.
        // If the stream is running, we cannot modify qpcStartCapture to be in
        // the past, so instead make the keyword start & stop times fit within the
        // time period that the keyword has been running. If it has been running
        // for more than 1 second, then set the keyword start time to be 1 second back
        // into the stream, as though we just figured out there was a keyword there.
        // If it has been running less than one second, then the keyword size ends
        // up being however long the stream has been running. 

        LARGE_INTEGER qpc;
        qpc = KeQueryPerformanceCounter(NULL);

        m_ullKeywordStopTimestamp = qpc.QuadPart; // stop time is the current time

        if (m_qpcStartCapture < (qpc.QuadPart - m_qpcFrequency))
        {
            m_ullKeywordStartTimestamp = (qpc.QuadPart - m_qpcFrequency); 
        }
        else
        {
            m_ullKeywordStartTimestamp = m_qpcStartCapture;
        }

    }

    return;
}

_IRQL_requires_min_(DISPATCH_LEVEL)
VOID CKeywordDetector::DpcRoutine(_In_ LONGLONG PerformanceCounter, _In_ LONGLONG PerformanceFrequency)
{
    LONGLONG currentPacket;
    LONGLONG packetsToQueue;

    // TODO: the timer only runs when the stream is open, but really for KWS it should be building up a collection of burst data
    // in the queue from 1.5 sec before the trigger happens. Is there some way to simulate that behavior here? Without doing that,
    // there isn't really a burst that happens, just a trickle because while the timestamps will be right, the queue won't contain
    // anything until the timer fires at the normal rate.

    if (m_qpcStartCapture <= 0)
    {
        return;
    }

    currentPacket = (PerformanceCounter - m_qpcStartCapture) * (SamplesPerSecond / SamplesPerPacket) / PerformanceFrequency;
    packetsToQueue = currentPacket - m_nLastQueuedPacket;

    while (packetsToQueue > 0)
    {
        LIST_ENTRY*     packetListEntry;
        PACKET_ENTRY*   packetEntry;

        do
        {
            packetListEntry = ExInterlockedRemoveHeadList(&PacketPoolHead, &PacketPoolSpinLock);
            if (packetListEntry != NULL) break;

            // Pool is empty, no room to buffer more, an overrun is occurring. Drop and reuse the
            // oldest packet from head of fifo.

            // Since the pool is empty, the fifo should be full. However, although unlikely, the
            // driver might empty the fifo before this routine removes a packet. In that case, the
            // pool should have packets available again. Therefore this is a retry loop.
            packetListEntry = ExInterlockedRemoveHeadList(&PacketFifoHead, &PacketFifoSpinLock);
            if (packetListEntry != NULL) break;
        } while (TRUE);

        packetEntry = CONTAINING_RECORD(packetListEntry, PACKET_ENTRY, ListEntry);

        packetEntry->PacketNumber = ++m_nLastQueuedPacket;
        packetEntry->QpcWhenSampled = m_qpcStartCapture + (packetEntry->PacketNumber * PerformanceFrequency * SamplesPerPacket / SamplesPerSecond);

        // TODO: this should really put something real in the buffer. Use the sine tone generator maybe?
        RtlZeroMemory(&packetEntry->Samples[0], sizeof(packetEntry->Samples));

        ExInterlockedInsertTailList(&PacketFifoHead, packetListEntry, &PacketFifoSpinLock);

        packetsToQueue -= 1;
    }
}

_IRQL_requires_max_(PASSIVE_LEVEL)
NTSTATUS CKeywordDetector::GetReadPacket
(
    _In_ ULONG PacketCount,
    _In_ ULONG PacketSize,
    _Out_writes_(PacketSize) PVOID *Packets,
    _Out_ ULONG *PacketNumber,
    _Out_ ULONG64 *PerformanceCounterValue,
    _Out_ BOOLEAN *MoreData
)
{
    NTSTATUS ntStatus;
    BYTE *packetData;
    PACKET_ENTRY *packetEntry;
    LIST_ENTRY *packetListEntry = NULL;

    packetListEntry = ExInterlockedRemoveHeadList(&PacketFifoHead, &PacketFifoSpinLock);
    if (packetListEntry == NULL)
    {
        ntStatus = STATUS_DEVICE_NOT_READY;
        goto Exit;
    }
    packetEntry = CONTAINING_RECORD(packetListEntry, PACKET_ENTRY, ListEntry);

    ntStatus = RtlLongLongToULong(packetEntry->PacketNumber, PacketNumber);
    if (!NT_SUCCESS(ntStatus))
    {
        goto Exit;
    }

    packetData = (PBYTE) Packets[(*PacketNumber) % PacketCount];

    *PerformanceCounterValue = packetEntry->QpcWhenSampled;
    *MoreData = !IsListEmpty(&PacketFifoHead);

    // TODO: the packet size here needs to line up to the packet size allocated.
    // Also, handle the first packet offset
    RtlCopyMemory(packetData, packetEntry->Samples, min(sizeof(packetEntry->Samples), PacketSize));

Exit:
    if (packetListEntry != NULL)
    {
        ExInterlockedInsertTailList(&PacketPoolHead, packetListEntry, &PacketPoolSpinLock);
    }

    return ntStatus;
}
