/*++

Copyright (c) Microsoft Corporation All Rights Reserved

Module Name:

    KeywordDetector.h

Abstract:

    Sample Keyword Detector.


--*/

#pragma once

typedef struct
{
    SOUNDDETECTOR_PATTERNHEADER Header;
    LONGLONG                    ContosoDetectorConfigurationData;
} CONTOSO_KEYWORDCONFIGURATION;

typedef struct
{
    SOUNDDETECTOR_PATTERNHEADER Header;
    LONGLONG                    ContosoDetectorResultData;
    ULONGLONG                   KeywordStartTimestamp;
    ULONGLONG                   KeywordStopTimestamp;
    GUID                        EventId;
} CONTOSO_KEYWORDDETECTIONRESULT;

DEFINE_GUID(CONTOSO_KEYWORDCONFIGURATION_IDENTIFIER2,
0x207f3d0c, 0x5c79, 0x496f, 0xa9, 0x4c, 0xd3, 0xd2, 0x93, 0x4d, 0xbf, 0xa9);

// {A537F559-2D67-463B-B10E-BEB750A21F31}
DEFINE_GUID(CONTOSO_KEYWORD1,
0xa537f559, 0x2d67, 0x463b, 0xb1, 0xe, 0xbe, 0xb7, 0x50, 0xa2, 0x1f, 0x31);
// {655E417A-80A5-4A77-B3F1-512EAF67ABCF}
DEFINE_GUID(CONTOSO_KEYWORD2,
0x655e417a, 0x80a5, 0x4a77, 0xb3, 0xf1, 0x51, 0x2e, 0xaf, 0x67, 0xab, 0xcf);

#define KEYWORDDETECTOR_POOLTAG 'KWS0'

class CKeywordDetector
{
public:
    PAGED_CODE_SEG
    CKeywordDetector();

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS ResetDetector(_In_ GUID eventId);

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS DownloadDetectorData(_In_ GUID eventId, _In_ LONGLONG Data);

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS GetDetectorData(_In_ GUID eventId, _Out_ LONGLONG *Data);

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    ULONGLONG GetStartTimestamp();

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    ULONGLONG GetStopTimestamp();

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS SetArmed(_In_ GUID eventId, _In_ BOOLEAN Arm);

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    VOID NotifyDetection();

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS GetArmed(_In_ GUID eventId, _Out_ BOOLEAN *Arm);

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    VOID Run();

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    VOID Stop();

    _IRQL_requires_min_(DISPATCH_LEVEL)
    VOID DpcRoutine(_In_ LONGLONG PerformanceCounter, _In_ LONGLONG PerformanceFrequency);

    _IRQL_requires_max_(PASSIVE_LEVEL)
    NTSTATUS GetReadPacket(_In_ ULONG PacketCount, _In_  ULONG PacketSize, _Out_writes_(PacketSize) PVOID *Packets, _Out_ ULONG *PacketNumber, _Out_ ULONGLONG *PerformanceCount, _Out_ BOOLEAN *MoreData);

private:
    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    VOID ResetFifo();

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    NTSTATUS ReadKeywordTimestampRegistry();

    _IRQL_requires_max_(PASSIVE_LEVEL)
    PAGED_CODE_SEG
    VOID StartBufferingStream();

    // The Contoso keyword detector processes 10ms packets of 16KHz 16-bit PCM
    // audio samples
    static const int SamplesPerSecond = 16000;
    static const int SamplesPerPacket = (10 * SamplesPerSecond / 1000);

    typedef struct
    {
        LIST_ENTRY  ListEntry;
        LONGLONG    PacketNumber;
        LONGLONG    QpcWhenSampled;
        UINT16      Samples[SamplesPerPacket];
    } PACKET_ENTRY;

    BOOLEAN         m_streamRunning;

    BOOLEAN         m_SoundDetectorArmed1;
    BOOLEAN         m_SoundDetectorArmed2;
    LONGLONG        m_SoundDetectorData1;
    LONGLONG        m_SoundDetectorData2;

    LONGLONG        m_qpcStartCapture;
    LONGLONG        m_qpcFrequency;
    LONGLONG        m_nLastQueuedPacket;

    ULONGLONG       m_ullKeywordStartTimestamp;
    ULONGLONG       m_ullKeywordStopTimestamp;

    KSPIN_LOCK      PacketPoolSpinLock;
    LIST_ENTRY      PacketPoolHead;
    PACKET_ENTRY    PacketPool[1 * SamplesPerSecond / SamplesPerPacket];    // Enough storage for 1 second of audio data

    KSPIN_LOCK      PacketFifoSpinLock;
    LIST_ENTRY      PacketFifoHead;

};

