#!/bin/bash
# Builds the Scam Decoder APK with the plain Android SDK tools (no Gradle).
# Usage: bash android/build.sh https://your-deployed-site.example
set -euo pipefail
BASE="${1:?Pass the deployed site address, e.g. bash android/build.sh https://your-site.example}"
[[ "$BASE" =~ ^https://[A-Za-z0-9.-]+$ ]] || { echo "Address must look like https://host with no path or trailing slash"; exit 1; }

SDK="${ANDROID_HOME:-C:/Android/Sdk}"
BT="$SDK/build-tools/36.1.0"
JAR="$SDK/platforms/android-36/android.jar"
cd "$(dirname "$0")"
rm -rf out && mkdir -p out/res/values out/res/mipmap out/classes out/test out/dex

# 1. On-phone rules must pass their self-check before anything is packaged.
javac -encoding UTF-8 -d out/test src/app/scamdecoder/Rules.java test/RulesCheck.java
java -cp out/test RulesCheck

# 2. Resources: the server address and the icon.
printf '<?xml version="1.0" encoding="utf-8"?>\n<resources><string name="base_url">%s</string></resources>\n' "$BASE" > out/res/values/strings.xml
cp ../public/icons/icon-192.png out/res/mipmap/icon.png
"$BT/aapt2" compile --dir out/res -o out/res.zip
"$BT/aapt2" link -o out/base.apk -I "$JAR" --manifest AndroidManifest.xml --java out/gen out/res.zip

# 3. Code.
javac -encoding UTF-8 --release 11 -cp "$JAR" -d out/classes $(find src out/gen -name '*.java')
"$BT/d8.bat" --lib "$JAR" --min-api 26 --output out/dex $(find out/classes -name '*.class')
(cd out/dex && "$BT/aapt" add ../base.apk classes.dex > /dev/null)

# 4. Align and sign with a local debug key (fine for direct install; not a Play Store key).
"$BT/zipalign" -f 4 out/base.apk out/aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias debug -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Scam Decoder Debug"
"$BT/apksigner.bat" sign --ks debug.keystore --ks-pass pass:android --out scam-decoder.apk out/aligned.apk
"$BT/apksigner.bat" verify scam-decoder.apk
echo "Built android/scam-decoder.apk for $BASE"
