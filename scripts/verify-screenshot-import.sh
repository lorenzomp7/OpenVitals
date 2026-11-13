#!/usr/bin/env bash
set -euo pipefail
mkdir -p screenshot-import-evidence
adb install -r verified-ci/ci/app-ci.apk
adb install -r verified-ci/androidTest/ci/app-ci-androidTest.apk
adb shell am instrument -w -r -e class tech.mmarca.openvitals.features.imports.screenshot.ScreenshotDeviceTest tech.mmarca.openvitals.ci.test/androidx.test.runner.AndroidJUnitRunner | tee screenshot-import-evidence/instrumentation.txt
adb pull /sdcard/Android/data/tech.mmarca.openvitals.ci/files screenshot-import-evidence/ui
! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' screenshot-import-evidence/instrumentation.txt
grep -Eq 'OK \([0-9]+ tests?\)' screenshot-import-evidence/instrumentation.txt
