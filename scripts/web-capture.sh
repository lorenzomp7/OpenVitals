#!/usr/bin/env bash
# Photographs the real app for the web copy (see web-app/README.md). Needs a booted
# emulator with Health Connect (API 34+) and the ci APK and its test APK built.
set -euo pipefail
PKG=tech.mmarca.openvitals.ci
OUT=${1:-web-app/out}
APK=$(find app/build/outputs/apk/ci -name '*.apk' -type f | head -1)
TEST_APK=$(find app/build/outputs/apk/androidTest/ci -name '*.apk' -type f | head -1)
test -s "$APK" && test -s "$TEST_APK"
mkdir -p "$OUT"
trap 'adb logcat -d -t 3000 > "$OUT/logcat.txt" || true' EXIT

adb install -r "$APK"
adb install -r "$TEST_APK"
# Every runtime permission the app asks for, Health Connect's included: no grant dialogs.
adb shell dumpsys package "$PKG" \
  | sed -n '/requested permissions:/,/install permissions:/p' \
  | grep -oE 'android\.permission\.[A-Za-z_.]+' | sort -u \
  | while read -r permission; do adb shell pm grant "$PKG" "$permission" >/dev/null 2>&1 || true; done
adb shell cmd locale set-app-locales "$PKG" --locales it-IT || true

adb shell am instrument -w -r -e class tech.mmarca.openvitals.web.WebDemoSeedTest \
  "$PKG.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUT/seed.txt"
grep -Eq 'OK \([0-9]+ tests?\)' "$OUT/seed.txt"

# A tall screen: every screen fits one photograph, as it would scrolled through.
adb shell wm size 1080x4800
adb shell settings put system screen_off_timeout 1800000
python3 scripts/web_crawl.py "$OUT"
adb shell wm size reset
