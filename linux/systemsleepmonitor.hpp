#ifndef SYSTEMSLEEPMONITOR_HPP
#define SYSTEMSLEEPMONITOR_HPP

#include <QObject>
#include <QDBusConnection>
#include <QDBusInterface>
#include <QDBusMessage>
#include <QDebug>

class SystemSleepMonitor : public QObject {
    Q_OBJECT

public:
    explicit SystemSleepMonitor(QObject *parent = nullptr) : QObject(parent) {
        // Connect to the system D-Bus
        QDBusConnection systemBus = QDBusConnection::systemBus();
        if (!systemBus.isConnected()) {
            qWarning() << "Cannot connect to system D-Bus";
            return;
        }

        // Subscribe to PrepareForSleep signal from logind
        systemBus.connect(
            "org.freedesktop.login1",
            "/org/freedesktop/login1",
            "org.freedesktop.login1.Manager",
            "PrepareForSleep",
            this,
            SLOT(handlePrepareForSleep(bool))
        );
    }

    ~SystemSleepMonitor() override = default;

signals:
    void systemGoingToSleep();
    void systemWakingUp();

private slots:
    void handlePrepareForSleep(bool sleeping) {
        if (sleeping) {
            emit systemGoingToSleep();
        } else {
            emit systemWakingUp();
        }
    }
};

#endif // SYSTEMSLEEPMONITOR_HPP