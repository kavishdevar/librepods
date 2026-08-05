# LibrePodsAAP — Windows AAP L2CAP driver

Open-source KMDF Bluetooth **profile driver** that lets Windows talk to AirPods
over Apple's Accessory Protocol (AAP). AAP runs on a classic-Bluetooth **L2CAP
channel at PSM `0x1001`**, which Windows user-mode Winsock cannot open (only
RFCOMM is exposed; raw L2CAP `connect()` fails with `WSAENETDOWN`). This driver
opens that channel in kernel mode and bridges it to user space via
`DeviceIoControl`, so the LibrePods app can read battery, toggle ANC, etc.

## How it binds (the key trick)

AirPods advertise an SDP service with UUID `{74ec2172-0bad-4d01-8f77-997b2be0722a}`
(the same one LibrePods uses on Linux). Windows enumerates a devnode
`BTHENUM\{74ec2172-...}_VID&0001004c_PID&2027` for it. Our INF matches that
hardware ID, so Windows loads this driver as the **function driver for the AAP
service PDO** — with the Bluetooth stack as its parent I/O target. From there we
`BthAllocateBrb` + submit `BRB_L2CA_OPEN_CHANNEL` to open PSM `0x1001` outbound.
No filter driver is needed (unlike PS3/BthPS3), because AirPods advertise the
service. Architecture reference: MS `bthecho` sample + `nefarius/BthPS3`.

## Files

| File | Role |
|------|------|
| `LibrePodsAAP.h` | IOCTL contract, device context, prototypes |
| `Driver.c` | `DriverEntry`, device creation, IOCTL queue |
| `Device.c` | PnP: query `BTH_PROFILE_DRIVER_INTERFACE`, get I/O target |
| `L2cap.c` | connect / disconnect / send / receive via BRBs |
| `Ioctl.c` | user-mode bridge (DeviceIoControl → L2CAP) |
| `LibrePodsAAP.inf` | binds to the AAP service, installs KMDF service |
| `LibrePodsAAP.vcxproj` | KMDF x64 project |

## IOCTL contract (for the user-mode transport)

Device interface GUID `{C0FFEE00-1337-4A5B-9E6F-A1B2C3D4E5F6}`, `FILE_DEVICE 0x8000`:
`CONNECT 0x800 {u64 addr; u16 psm}`, `DISCONNECT 0x801`, `SEND 0x802` (raw bytes),
`RECEIVE 0x803` (raw bytes out), `GET_STATUS 0x804`.

## Build

Needs VS2022/2026 with the C++ workload + Windows SDK/WDK (matching build numbers,
e.g. 28000). From a Developer prompt:

```
msbuild LibrePodsAAP.vcxproj /p:Configuration=Release /p:Platform=x64
```

Then generate the catalog from a folder holding `LibrePodsAAP.sys` + `.inf`:

```
inf2cat /driver:<pkg-dir> /os:10_X64
```

## Install (test-signed — requires Test Mode)

The driver is not attestation-signed, so Windows must be in test mode. **Advanced
users only; back up your BitLocker recovery key and make a restore point first.**

```powershell
# 1. test cert + sign
$c = New-SelfSignedCertificate -Type CodeSigningCert -Subject "CN=LibrePods Test" -CertStoreLocation Cert:\LocalMachine\My
signtool sign /fd SHA256 /sha1 $c.Thumbprint LibrePodsAAP.sys
signtool sign /fd SHA256 /sha1 $c.Thumbprint librepodsaap.cat
# 2. trust the cert (Trusted Root + Trusted Publishers, LocalMachine)
# 3. enable test signing, disable Secure Boot in firmware, reboot
bcdedit /set testsigning on
# 4. install (binds to the AirPods AAP devnode)
pnputil /add-driver LibrePodsAAP.inf /install
```

Uninstall: `pnputil /delete-driver LibrePodsAAP.inf /uninstall`, then
`bcdedit /set testsigning off` and re-enable Secure Boot.

## Status

Compiles, links, and passes the inf2cat signability test. On-hardware
integration testing (open channel + AAP battery/ANC round-trip) is the next step.
