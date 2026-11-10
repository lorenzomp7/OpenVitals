#!/usr/bin/env bash
set -euo pipefail
mkdir -p dashboard-emulator-evidence
trap 'adb logcat -d -t 2000 > dashboard-emulator-evidence/logcat.txt || true' EXIT
APK=$(find app/build/outputs/apk/ci -name '*.apk' -type f | head -1)
test -s "$APK"
adb install -r "$APK"
adb shell am start -W -n tech.mmarca.openvitals.ci/tech.mmarca.openvitals.MainActivity
sleep 15
adb shell pidof tech.mmarca.openvitals.ci
adb shell dumpsys activity activities > dashboard-emulator-evidence/activities.txt
adb shell uiautomator dump /sdcard/window.xml
adb pull /sdcard/window.xml dashboard-emulator-evidence/window.xml
mkdir -p dashboard-emulator-evidence
adb shell screencap -p /sdcard/launch.png
adb pull /sdcard/launch.png dashboard-emulator-evidence/launch.png
adb logcat -d -t 2000 > dashboard-emulator-evidence/logcat.txt
