#!/usr/bin/env bash
# Fetch the minimal FFmpeg 7.1 shared libraries the daemon links against for
# AAC-ELD decode (avcodec / avutil / swresample) into vendor/ffmpeg — so the
# ~28k lines of FFmpeg headers are NOT vendored into git. Runs both on the
# Windows CI runner (Git Bash) and locally (WSL / Linux cross-build).
#
# We copy BOTH import-lib formats: .lib (MSVC — what CI's default target uses)
# and .dll.a (MinGW — the x86_64-pc-windows-gnu cross-build), so build.rs links
# under either toolchain. Pinned by URL + SHA256; if BtbN rebuilds the n7.1
# "latest" asset the checksum will mismatch and the build fails loudly — bump
# URL_SHA256 below to the new value then.
set -euo pipefail

URL="https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-n7.1-latest-win64-lgpl-shared-7.1.zip"
URL_SHA256="a5243d934aec40825e3a80c8beaa4f713fadd0c943b2034add8e170905021c2b"

here="$(cd "$(dirname "$0")" && pwd)"
dest="$here/vendor/ffmpeg"

# Already fetched (or vendored) — nothing to do.
if [ -f "$dest/lib/libavcodec.dll.a" ] || [ -f "$dest/lib/avcodec.lib" ]; then
  echo "ffmpeg already present in $dest"
  exit 0
fi

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
echo "fetching FFmpeg 7.1 shared libs…"
curl -fsSL -o "$tmp/ff.zip" "$URL"
echo "${URL_SHA256}  $tmp/ff.zip" | sha256sum -c -
unzip -q "$tmp/ff.zip" -d "$tmp/x"
root="$(find "$tmp/x" -maxdepth 1 -type d -name 'ffmpeg*' | head -1)"

mkdir -p "$dest/include" "$dest/lib" "$dest/bin"
cp -r "$root"/include/libavcodec "$root"/include/libavutil "$root"/include/libswresample "$dest/include/"
# import libs — both MSVC (.lib) and MinGW (.dll.a)
cp "$root"/lib/avcodec.lib "$root"/lib/avutil.lib "$root"/lib/swresample.lib "$dest/lib/"
cp "$root"/lib/libavcodec.dll.a "$root"/lib/libavutil.dll.a "$root"/lib/libswresample.dll.a "$dest/lib/"
# runtime DLLs
cp "$root"/bin/avcodec-61.dll "$root"/bin/avutil-59.dll "$root"/bin/swresample-5.dll "$dest/bin/"

echo "ffmpeg fetched into $dest"
