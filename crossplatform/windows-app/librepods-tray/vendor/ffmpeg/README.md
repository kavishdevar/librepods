# Vendored FFmpeg (LGPL) — AAC-ELD decoder for the hi-res mic

From BtbN's **`ffmpeg-master-latest-win64-lgpl-shared`**
(https://github.com/BtbN/FFmpeg-Builds/releases/latest) — LGPLv3, no GPL/nonfree,
`--disable-libfdk-aac`. See `LICENSE-ffmpeg.txt`.

Committed (needed to *build*): `include/` (libavcodec, libavutil, libswresample)
and `lib/*.dll.a` (mingw import libs). The `bin/*.dll` runtime libraries
(`avcodec-63.dll`, `avutil-61.dll`, `swresample-7.dll`, ~75 MB) are **not** in git
— download the same BtbN build and copy them into `bin/`, and ship them next to
`librepods-tray.exe`.
