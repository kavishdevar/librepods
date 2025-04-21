#ifndef AUTOSTARTMANAGER_HPP
#define AUTOSTARTMANAGER_HPP

#include <QObject>
#include <QSettings>
#include <QStandardPaths>
#include <QFile>
#include <QDir>
#include <QCoreApplication>

class AutoStartManager : public QObject
{
    Q_OBJECT
    Q_PROPERTY(bool autoStartEnabled READ autoStartEnabled WRITE setAutoStartEnabled NOTIFY autoStartEnabledChanged)

public:
    explicit AutoStartManager(QObject *parent = nullptr) : QObject(parent)
    {
        QString autostartDir = QStandardPaths::writableLocation(QStandardPaths::ConfigLocation) + "/autostart";
        QDir().mkpath(autostartDir);
        m_autostartFilePath = autostartDir + "/" + QCoreApplication::applicationName() + ".desktop";
    }

    bool autoStartEnabled() const
    {
        return QFile::exists(m_autostartFilePath);
    }

    void setAutoStartEnabled(bool enabled)
    {
        if (autoStartEnabled() == enabled)
        {
            return;
        }

        if (enabled)
        {
            createAutoStartEntry();
        }
        else
        {
            removeAutoStartEntry();
        }

        emit autoStartEnabledChanged(enabled);
    }

private:
    void createAutoStartEntry()
    {
        QFile desktopFile(m_autostartFilePath);
        if (!desktopFile.open(QIODevice::WriteOnly | QIODevice::Text))
        {
            qWarning() << "Failed to create autostart file:" << desktopFile.errorString();
            return;
        }

        QString appPath = QCoreApplication::applicationFilePath();
        // Handle cases where the path might contain spaces
        if (appPath.contains(' '))
        {
            appPath = "\"" + appPath + "\"";
        }

        QString content = QStringLiteral(
                              "[Desktop Entry]\n"
                              "Type=Application\n"
                              "Name=%1\n"
                              "Exec=%2\n"
                              "Icon=%3\n"
                              "Comment=%4\n"
                              "X-GNOME-Autostart-enabled=true\n"
                              "Terminal=false\n")
                              .arg(
                                  QCoreApplication::applicationName(),
                                  appPath,
                                  QCoreApplication::applicationName().toLower(),
                                  QCoreApplication::applicationName() + " autostart");

        desktopFile.write(content.toUtf8());
        desktopFile.close();
    }

    void removeAutoStartEntry()
    {
        if (QFile::exists(m_autostartFilePath))
        {
            QFile::remove(m_autostartFilePath);
        }
    }

    QString m_autostartFilePath;

signals:
    void autoStartEnabledChanged(bool enabled);
};

#endif // AUTOSTARTMANAGER_HPP