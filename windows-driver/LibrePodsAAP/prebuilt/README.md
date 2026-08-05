# Prebuilt LibrePodsAAP driver package

The compiled driver (`LibrePodsAAP.sys` + `.inf` + `.cat`) so you can install
**without building it** — no Visual Studio / C++ / WDK required.

Install (admin PowerShell, Test Mode — see ../../windows-app/README.md):
```powershell
& "..\install.ps1" -PackageDir ".\"
```
`install.ps1` creates a test certificate, signs these files, trusts the cert
and installs the driver. Then run the app (`librepods-tray` or `librepods-ui`).

The app binaries are pure Rust `.exe`s and never need C++ either.
