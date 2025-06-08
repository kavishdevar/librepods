#pragma once

#include <QObject>
#include <QDBusInterface>
#include <QDBusServiceWatcher>

class PlayerStatusWatcher : public QObject {
    Q_OBJECT
public:
    explicit PlayerStatusWatcher(const QString &playerService, QObject *parent = nullptr);
    static QString getCurrentPlaybackStatus(const QString &playerService);

signals:
    void playbackStatusChanged(const QString &status);

private slots:
    void onPropertiesChanged(const QString &interface, const QVariantMap &changed, const QStringList &);
    void onServiceOwnerChanged(const QString &name, const QString &oldOwner, const QString &newOwner);

private:
    void updateStatus();
    QString m_playerService;
    QDBusInterface *m_iface;
    QDBusServiceWatcher *m_serviceWatcher;
};