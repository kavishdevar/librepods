# Prebuilt LibrePodsMic driver package

The compiled virtual-microphone driver (`AudioCodec.sys` + `.inf` + `.cat`) so
you can install it **without building it** — no Visual Studio / C++ / WDK
required.

The one-shot [`windows/dist/install.ps1`](../../../windows/dist/install.ps1)
bundles these (in `windows/dist/driver-mic/`) alongside `tools/devcon.exe`,
signs them with the same test certificate as the AAP driver, and creates the
`ROOT\AudioCodec` device — so a virtual microphone appears in Sound > Input.

To install just this driver standalone, see [`../install.ps1`](../install.ps1).
