import QtQuick 2.15

Column {
    id: root
    property bool inEar: true
    property string iconSource
    property int batteryLevel: 0
    property bool isCharging: false
    property string indicator: ""
    property real targetOpacity: inEar ? 1 : 0.5

    Timer {
        id: opacityTimer
        interval: 50
        onTriggered: root.opacity = root.targetOpacity
    }

    onInEarChanged: {
        opacityTimer.restart()
    }

    spacing: 5

    Image {
        source: parent.iconSource
        width: parent.indicator === "" ? 92 : 72
        height: 72
        fillMode: Image.PreserveAspectFit
        mipmap: true
        mirror: parent.indicator === "R"
        anchors.horizontalCenter: parent.horizontalCenter
    }

    BatteryIndicator {
        batteryLevel: parent.batteryLevel
        isCharging: parent.isCharging
        indicator: parent.indicator
    }
}