#include "media/playerstatuswatcher.h"
#include <QDBusConnection>
#include <QDBusPendingReply>
#include <QVariantMap>
#include <QDBusReply>

PlayerStatusWatcher::PlayerStatusWatcher(const QString &playerService, QObject *parent)
    : QObject(parent),
      m_playerService(playerService),
      m_iface(new QDBusInterface(playerService, "/org/mpris/MediaPlayer2",
                                 "org.mpris.MediaPlayer2.Player", QDBusConnection::sessionBus(), this)),
      m_serviceWatcher(new QDBusServiceWatcher(playerService, QDBusConnection::sessionBus(),
                                               QDBusServiceWatcher::WatchForOwnerChange, this))
{
    // Register this object on the session bus to receive D-Bus messages
    QDBusConnection::sessionBus().registerObject("/PlayerStatusWatcher", this,
                                               QDBusConnection::ExportAllSlots);

    QDBusConnection::sessionBus().connect(
        playerService, "/org/mpris/MediaPlayer2", "org.freedesktop.DBus.Properties",
        "PropertiesChanged", this, SLOT(onPropertiesChanged(QString,QVariantMap,QStringList))
    );
    connect(m_serviceWatcher, &QDBusServiceWatcher::serviceOwnerChanged,
            this, &PlayerStatusWatcher::onServiceOwnerChanged);
    updateStatus();
}

void PlayerStatusWatcher::onPropertiesChanged(const QString &interface,
                                              const QVariantMap &changed,
                                              const QStringList &)
{
    // Get the service name of the sender
    QString sender = message().service();
    
    // Skip if it's a KDE Connect player
    if (sender.contains("kdeconnect", Qt::CaseInsensitive)) {
        return;
    }

    if (interface == "org.mpris.MediaPlayer2.Player" && changed.contains("PlaybackStatus")) {
        emit playbackStatusChanged(changed.value("PlaybackStatus").toString());
    }
}

void PlayerStatusWatcher::updateStatus() {
    QVariant reply = m_iface->property("PlaybackStatus");
    if (reply.isValid()) {
        emit playbackStatusChanged(reply.toString());
    }
}

void PlayerStatusWatcher::onServiceOwnerChanged(const QString &name, const QString &, const QString &newOwner)
{
    if (name == m_playerService && newOwner.isEmpty()) {
        emit playbackStatusChanged(""); // player disappeared
    } else if (name == m_playerService && !newOwner.isEmpty()) {
        updateStatus(); // player appeared/reappeared
    }
}

QString PlayerStatusWatcher::getCurrentPlaybackStatus(const QString &playerService)
{
    QDBusInterface iface(
        playerService,
        "/org/mpris/MediaPlayer2",
        "org.mpris.MediaPlayer2.Player",
        QDBusConnection::sessionBus());
    QVariant reply = iface.property("PlaybackStatus");
    if (reply.isValid())
    {
        return reply.toString(); // "Playing", "Paused", "Stopped"
    }
    else
    {
        return QString(); // or handle error as needed
    }
}