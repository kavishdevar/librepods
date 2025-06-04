#ifndef BLEMANAGER_H
#define BLEMANAGER_H

#include <QObject>
#include <QBluetoothDeviceDiscoveryAgent>
#include <QMap>
#include <QString>
#include <QDateTime>
#include "enums.h"

class QTimer;

class BleInfo
{
public:
    QString name;
    QString address;
    int leftPodBattery = -1; // -1 indicates not available
    int rightPodBattery = -1;
    int caseBattery = -1;
    bool leftCharging = false;
    bool rightCharging = false;
    bool caseCharging = false;
    AirpodsTrayApp::Enums::AirPodsModel modelName = AirpodsTrayApp::Enums::AirPodsModel::Unknown; // Default model
    quint8 lidOpenCounter = 0;
    QString color = "Unknown"; // Default color
    quint8 status = 0;
    QByteArray rawData;
    QByteArray encryptedPayload; // 16 bytes of encrypted payload

    // Additional status flags from Kotlin version
    bool isLeftPodInEar = false;
    bool isRightPodInEar = false;
    bool isPrimaryInEar = false;
    bool isSecondaryInEar = false;
    bool isLeftPodMicrophone = false;
    bool isRightPodMicrophone = false;
    bool isThisPodInTheCase = false;
    bool isOnePodInCase = false;
    bool areBothPodsInCase = false;
    bool primaryLeft = true; // True if left pod is primary, false if right pod is primary

    // Lid state enumeration
    enum class LidState
    {
        OPEN = 0x0,
        CLOSED = 0x1,
        UNKNOWN,
    };
    LidState lidState = LidState::UNKNOWN;

    // Connection state enumeration
    enum class ConnectionState : uint8_t
    {
        DISCONNECTED = 0x00,
        IDLE = 0x04,
        MUSIC = 0x05,
        CALL = 0x06,
        RINGING = 0x07,
        HANGING_UP = 0x09,
        UNKNOWN = 0xFF // Using 0xFF for representing null in the original
    };
    ConnectionState connectionState = ConnectionState::UNKNOWN;

    QDateTime lastSeen; // Timestamp of last detection
};

class BleManager : public QObject
{
    Q_OBJECT
public:
    explicit BleManager(QObject *parent = nullptr);
    ~BleManager();

    void startScan();
    void stopScan();
    const QMap<QString, BleInfo> &getDevices() const;

private slots:
    void onDeviceDiscovered(const QBluetoothDeviceInfo &info);
    void onScanFinished();
    void onErrorOccurred(QBluetoothDeviceDiscoveryAgent::Error error);
    void pruneOldDevices();

signals:
    void deviceFound(const BleInfo &device);

private:
    QBluetoothDeviceDiscoveryAgent *discoveryAgent;
    QMap<QString, BleInfo> devices;

    QTimer *pruneTimer;                         // Timer for periodic pruning
    static const int PRUNE_INTERVAL_MS = 5000;  // Check every 5 seconds
    static const int DEVICE_TIMEOUT_MS = 10000; // Remove after 10 seconds
};

#endif // BLEMANAGER_H