cmake_minimum_required(VERSION 3.16)

project(linux VERSION 0.1 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(Qt6 6.4 REQUIRED COMPONENTS Quick Widgets Bluetooth DBus)
find_package(OpenSSL REQUIRED)

qt_standard_project_setup(REQUIRES 6.4)

qt_add_executable(librepods
    main.cpp
    logger.h
    media/mediacontroller.cpp
    media/mediacontroller.h
    airpods_packets.h
    trayiconmanager.cpp
    trayiconmanager.h
    enums.h
    battery.hpp
    BluetoothMonitor.cpp
    BluetoothMonitor.h
    autostartmanager.hpp
    BasicControlCommand.hpp
    deviceinfo.hpp
    ble/bleutils.cpp
    ble/bleutils.h
    ble/blemanager.cpp
    ble/blemanager.h
    thirdparty/QR-Code-generator/qrcodegen.cpp
    thirdparty/QR-Code-generator/qrcodegen.hpp
    QRCodeImageProvider.hpp
    eardetection.hpp
    media/playerstatuswatcher.cpp
    media/playerstatuswatcher.h
    systemsleepmonitor.hpp
)

qt_add_qml_module(librepods
    URI linux
    VERSION 1.0
    QML_FILES
        Main.qml
        BatteryIndicator.qml
        SegmentedControl.qml
        PodColumn.qml
        Icon.qml
        KeysQRDialog.qml
)

# Add the resource file
qt_add_resources(librepods "resources"
    PREFIX "/icons"
    FILES
        assets/airpods.png
        assets/pod.png
        assets/pod_case.png
        assets/pod3.png
        assets/pod3_case.png
        assets/pod4_case.png
        assets/podpro.png
        assets/podpro_case.png
        assets/podmax.png
        assets/fonts/SF-Symbols-6.ttf
)

target_link_libraries(librepods
    PRIVATE Qt6::Quick Qt6::Widgets Qt6::Bluetooth Qt6::DBus OpenSSL::SSL OpenSSL::Crypto
)

include(GNUInstallDirs)
install(TARGETS librepods
    BUNDLE DESTINATION .
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
)
