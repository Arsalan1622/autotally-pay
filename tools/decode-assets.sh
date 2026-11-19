#!/bin/sh
# Decode the base64-bundled binary assets (font, launcher icons, Gradle
# wrapper jar) into their build locations.
#
# Why: the GitHub connector can only push UTF-8 text files, so binaries
# live in tools/assets-b64/ as .b64 text. Run this once after cloning,
# and CI runs it before every build. The decoded files are gitignored;
# the .b64 files are the source of truth.
set -e
cd "$(dirname "$0")/.."

mkdir -p app/src/main/res/font gradle/wrapper
for dpi in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  mkdir -p "app/src/main/res/mipmap-$dpi"
done

base64 -d tools/assets-b64/NotoNastaliqUrdu.ttf.b64 > app/src/main/res/font/noto_nastaliq_urdu.ttf
base64 -d tools/assets-b64/gradle-wrapper.jar.b64 > gradle/wrapper/gradle-wrapper.jar
for dpi in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  for n in ic_launcher ic_launcher_round; do
    base64 -d "tools/assets-b64/${dpi}-${n}.png.b64" > "app/src/main/res/mipmap-$dpi/$n.png"
  done
done
chmod +x gradlew
echo "Assets decoded."
