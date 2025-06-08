#include "mediacontroller.h"
#include "logger.h"
#include "eardetection.hpp"
#include "playerstatuswatcher.h"

#include <QDebug>
#include <QProcess>
#include <QRegularExpression>
#include <QDBusConnection>
#include <QDBusConnectionInterface>

MediaController::MediaController(QObject *parent) : QObject(parent) {
}

void MediaController::handleEarDetection(EarDetection *earDetection)
{
  if (earDetectionBehavior == Disabled)
  {
    LOG_DEBUG("Ear detection is disabled, ignoring status");
    return;
  }

  bool primaryInEar = earDetection->isPrimaryInEar();
  bool secondaryInEar = earDetection->isSecondaryInEar();

  LOG_DEBUG("Ear detection status: primaryInEar="
            << primaryInEar << ", secondaryInEar=" << secondaryInEar
            << ", isAirPodsActive=" << isActiveOutputDeviceAirPods());

  // First handle playback pausing based on selected behavior
  bool shouldPause = false;
  bool shouldResume = false;

  if (earDetectionBehavior == PauseWhenOneRemoved)
  {
    shouldPause = !primaryInEar || !secondaryInEar;
    shouldResume = primaryInEar && secondaryInEar;
  }
  else if (earDetectionBehavior == PauseWhenBothRemoved)
  {
    shouldPause = !primaryInEar && !secondaryInEar;
    shouldResume = primaryInEar || secondaryInEar;
  }

  if (shouldPause && isActiveOutputDeviceAirPods())
  {
    if (getCurrentMediaState() == Playing)
    {
      pause();
    }
  }

  // Then handle device profile switching
  if (primaryInEar || secondaryInEar)
  {
    LOG_INFO("At least one AirPod is in ear");
    activateA2dpProfile();

    // Resume if conditions are met and we previously paused
    if (shouldResume && wasPausedByApp && isActiveOutputDeviceAirPods())
    {
      play();
    }
  }
  else
  {
    LOG_INFO("Both AirPods are out of ear");
    removeAudioOutputDevice();
  }
}

void MediaController::setEarDetectionBehavior(EarDetectionBehavior behavior)
{
  earDetectionBehavior = behavior;
  LOG_INFO("Set ear detection behavior to: " << behavior);
}

void MediaController::followMediaChanges() {
  playerStatusWatcher = new PlayerStatusWatcher("", this);
  connect(playerStatusWatcher, &PlayerStatusWatcher::playbackStatusChanged,
          this, [this](const QString &status)
          {
            LOG_DEBUG("Playback status changed: " << status);
            MediaState state = mediaStateFromPlayerctlOutput(status);
            emit mediaStateChanged(state);
          });
}

bool MediaController::isActiveOutputDeviceAirPods() {
  QProcess process;
  process.start("pactl", QStringList() << "get-default-sink");
  process.waitForFinished();
  QString output = process.readAllStandardOutput().trimmed();
  LOG_DEBUG("Default sink: " << output);
  return output.contains(connectedDeviceMacAddress);
}

void MediaController::handleConversationalAwareness(const QByteArray &data) {
  LOG_DEBUG("Handling conversational awareness data: " << data.toHex());
  bool lowered = data[9] == 0x01;
  LOG_INFO("Conversational awareness: " << (lowered ? "enabled" : "disabled"));

  if (lowered) {
    if (initialVolume == -1 && isActiveOutputDeviceAirPods()) {
      QProcess process;
      process.start("pactl", QStringList()
                                 << "get-sink-volume" << "@DEFAULT_SINK@");
      process.waitForFinished();
      QString output = process.readAllStandardOutput();
      QRegularExpression re("front-left: \\d+ /\\s*(\\d+)%");
      QRegularExpressionMatch match = re.match(output);
      if (match.hasMatch()) {
        LOG_DEBUG("Matched: " << match.captured(1));
        initialVolume = match.captured(1).toInt();
      } else {
        LOG_ERROR("Failed to parse initial volume from output: " << output);
        return;
      }
    }
    QProcess::execute(
        "pactl", QStringList() << "set-sink-volume" << "@DEFAULT_SINK@"
                               << QString::number(initialVolume * 0.20) + "%");
    LOG_INFO("Volume lowered to 0.20 of initial which is "
             << initialVolume * 0.20 << "%");
  } else {
    if (initialVolume != -1 && isActiveOutputDeviceAirPods()) {
      QProcess::execute("pactl", QStringList()
                                     << "set-sink-volume" << "@DEFAULT_SINK@"
                                     << QString::number(initialVolume) + "%");
      LOG_INFO("Volume restored to " << initialVolume << "%");
      initialVolume = -1;
    }
  }
}

void MediaController::activateA2dpProfile() {
  if (connectedDeviceMacAddress.isEmpty() || m_deviceOutputName.isEmpty()) {
    LOG_WARN("Connected device MAC address or output name is empty, cannot activate A2DP profile");
    return;
  }

  LOG_INFO("Activating A2DP profile for AirPods");
  int result = QProcess::execute(
      "pactl", QStringList()
                   << "set-card-profile"
                   << m_deviceOutputName << "a2dp-sink");
  if (result != 0) {
    LOG_ERROR("Failed to activate A2DP profile");
  }
}

void MediaController::removeAudioOutputDevice() {
  if (connectedDeviceMacAddress.isEmpty() || m_deviceOutputName.isEmpty()) {
    LOG_WARN("Connected device MAC address or output name is empty, cannot remove audio output device");
    return;
  }
  
  LOG_INFO("Removing AirPods as audio output device");
  int result = QProcess::execute(
      "pactl", QStringList()
                   << "set-card-profile"
                   << m_deviceOutputName << "off");
  if (result != 0) {
    LOG_ERROR("Failed to remove AirPods as audio output device");
  }
}

void MediaController::setConnectedDeviceMacAddress(const QString &macAddress) {
  connectedDeviceMacAddress = macAddress;
  m_deviceOutputName = getAudioDeviceName();
  LOG_INFO("Device output name set to: " << m_deviceOutputName);
}

MediaController::MediaState MediaController::mediaStateFromPlayerctlOutput(
    const QString &output) const {
  if (output == "Playing") {
    return MediaState::Playing;
  } else if (output == "Paused") {
    return MediaState::Paused;
  } else {
    return MediaState::Stopped;
  }
}

MediaController::MediaState MediaController::getCurrentMediaState() const
{
  return mediaStateFromPlayerctlOutput(PlayerStatusWatcher::getCurrentPlaybackStatus(""));
}

bool MediaController::sendMediaPlayerCommand(const QString &method)
{
  // Connect to the session bus
  QDBusConnection bus = QDBusConnection::sessionBus();

  // Find available MPRIS-compatible media players
  QStringList services = bus.interface()->registeredServiceNames().value();
  QStringList mprisServices;
  for (const QString &service : services)
  {
    if (service.startsWith("org.mpris.MediaPlayer2."))
    {
      mprisServices << service;
    }
  }

  if (mprisServices.isEmpty())
  {
    LOG_ERROR("No MPRIS-compatible media players found on DBus");
    return false;
  }

  bool success = false;
  // Try each MPRIS service until one succeeds
  for (const QString &service : mprisServices)
  {
    QDBusInterface playerInterface(
        service,
        "/org/mpris/MediaPlayer2",
        "org.mpris.MediaPlayer2.Player",
        bus);

    if (!playerInterface.isValid())
    {
      LOG_ERROR("Invalid DBus interface for service: " << service);
      continue;
    }

    // Send the Play or Pause command
    if (method == "Play" || method == "Pause")
    {
      QDBusReply<void> reply = playerInterface.call(method);
      if (reply.isValid())
      {
        LOG_INFO("Successfully sent " << method << " to " << service);
        success = true;
        break; // Exit after the first successful command
      }
      else
      {
        LOG_ERROR("Failed to send " << method << " to " << service
                                    << ": " << reply.error().message());
      }
    }
    else
    {
      LOG_ERROR("Unsupported method: " << method);
      return false;
    }
  }

  if (!success)
  {
    LOG_ERROR("No media player responded successfully to " << method);
  }
  return success;
}

void MediaController::play()
{
  if (sendMediaPlayerCommand("Play"))
  {
    LOG_INFO("Resumed playback via DBus");
    wasPausedByApp = false;
  }
  else
  {
    LOG_ERROR("Failed to resume playback via DBus");
  }
}

void MediaController::pause()
{
  if (sendMediaPlayerCommand("Pause"))
  {
    LOG_INFO("Paused playback via DBus");
    wasPausedByApp = true;
  }
  else
  {
    LOG_ERROR("Failed to pause playback via DBus");
  }
}

MediaController::~MediaController() {
}

QString MediaController::getAudioDeviceName()
{
  if (connectedDeviceMacAddress.isEmpty()) { return QString(); }

  // Set up QProcess to run pactl directly
  QProcess process;
  process.start("pactl", QStringList() << "list" << "cards" << "short");
  if (!process.waitForFinished(3000)) // Timeout after 3 seconds
  {
    LOG_ERROR("pactl command failed or timed out: " << process.errorString());
    return QString();
  }

  // Check for execution errors
  if (process.exitCode() != 0)
  {
    LOG_ERROR("pactl exited with error code: " << process.exitCode());
    return QString();
  }

  // Read and parse the command output
  QString output = process.readAllStandardOutput();
  QStringList lines = output.split("\n", Qt::SkipEmptyParts);

  // Iterate through each line to find a matching Bluetooth sink
  for (const QString &line : lines)
  {
    QStringList fields = line.split("\t", Qt::SkipEmptyParts);
    if (fields.size() < 2) { continue; }

    QString sinkName = fields[1].trimmed();
    if (sinkName.startsWith("bluez") && sinkName.contains(connectedDeviceMacAddress))
    {
      return sinkName;
    }
  }

  // No matching sink found
  LOG_ERROR("No matching Bluetooth sink found for MAC address: " << connectedDeviceMacAddress);
  return QString();
}