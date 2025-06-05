import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import QtQuick.Window 2.15

Window {
    id: root
    title: "Magic Cloud Keys QR Code"
    flags: Qt.Dialog
    modality: Qt.WindowModal

    // Use system palette for dynamic theming
    SystemPalette { id: systemPalette }
    color: systemPalette.window // Background adapts to theme

    width: Math.min(Screen.width * 0.8, 300)
    height: Math.min(Screen.height * 0.7, 350)

    property string irk: ""
    property string encKey: ""

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 20
        spacing: 20

        // QR Code Container
        Rectangle {
            id: qrContainer
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.minimumHeight: width
            radius: 4
            color: systemPalette.base
            border.color: systemPalette.mid

            Image {
                id: qrCodeImage
                anchors.centerIn: parent
                width: Math.min(parent.width * 0.9, parent.height * 0.9)
                height: width
                fillMode: Image.PreserveAspectFit
                source: "image://qrcode/" + root.encKey + ";" + root.irk

                BusyIndicator {
                    anchors.centerIn: parent
                    running: qrCodeImage.status === Image.Loading
                }

                Label {
                    anchors.centerIn: parent
                    visible: qrCodeImage.status === Image.Error
                    text: "Failed to generate QR code"
                    color: systemPalette.text // Dynamic text color
                }
            }
        }

        // Instruction text
        Label {
            Layout.fillWidth: true
            text: "Scan this QR code to transfer\nthe Magic Cloud Keys to another device"
            horizontalAlignment: Text.AlignHCenter
            wrapMode: Text.WordWrap
            color: systemPalette.text // Adapts to dark/light mode
            font.pixelSize: 14
        }
    }
}