# Vendored FFmpeg (LGPL, minimal) — AAC-ELD decoder for the hi-res mic

A **minimal** FFmpeg 7.1 build (LGPL v2.1), configured with only the AAC decoder:

    ./configure --cross-prefix=x86_64-w64-mingw32- --arch=x86_64 --target-os=mingw32 \
      --enable-shared --disable-static --disable-everything --disable-programs \
      --disable-avdevice --disable-avformat --disable-swscale --disable-postproc \
      --disable-avfilter --disable-network --disable-x86asm \
      --enable-decoder=aac --enable-decoder=aac_fixed --enable-decoder=aac_latm

That shrinks avcodec from ~69 MB (BtbN full build) to ~0.7 MB. Committed:
`include/` (libavcodec, libavutil, libswresample), `lib/*.dll.a` (mingw import
libs), and `bin/*.dll` (avcodec-61, avutil-59, swresample-5 — ~1.6 MB total,
shipped next to librepods-tray.exe). See `LICENSE-ffmpeg.txt`.
