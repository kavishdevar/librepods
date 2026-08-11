# Prebuilt LibrePodsMic driver package

The compiled virtual-microphone driver (`AudioCodec.sys` + `.inf` + `.cat`) so
you can install it **without building it** — no Visual Studio / C++ / WDK
required.

To install, run [`../install.ps1`](../install.ps1) from an **admin** PowerShell —
it test-signs these files and uses `devcon` to (re)create the `ROOT\AudioCodec`
device, so a virtual microphone appears in Sound > Input.
