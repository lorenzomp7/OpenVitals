#!/usr/bin/env bash
set -euo pipefail
bash scripts/emulator-smoke.sh
TEST_APK=$(find app/build/outputs/apk/androidTest/ci -name '*.apk' -type f | head -1)
adb install -r "$TEST_APK"
adb shell am instrument -w -r -e class tech.mmarca.openvitals.features.settings.SettingsBackupCardTest,tech.mmarca.openvitals.features.dashboard.DashboardTileOptionsTest,tech.mmarca.openvitals.features.dashboard.PersonalCarouselTest,tech.mmarca.openvitals.features.dashboard.DashboardPeriodLayoutTest,tech.mmarca.openvitals.features.dashboard.AmbientFluidBrandTest,tech.mmarca.openvitals.features.activity.maps.ActivityMapboxFallbackTest,tech.mmarca.openvitals.features.activity.maps.MapboxTokenStoreTest,tech.mmarca.openvitals.features.activity.maps.MapboxAccountCardTest,tech.mmarca.openvitals.features.dashboard.DashboardContentTest,tech.mmarca.openvitals.testing.PersonalScreenshotTest -e openvitals.recordGoldens true tech.mmarca.openvitals.ci.test/androidx.test.runner.AndroidJUnitRunner | tee dashboard-emulator-evidence/instrumentation.txt
adb pull /sdcard/Download/openvitals-goldens dashboard-emulator-evidence/goldens
adb pull /sdcard/Android/data/tech.mmarca.openvitals.ci/files dashboard-emulator-evidence/ui || true
grep -Eq 'OK \([0-9]+ tests?\)' dashboard-emulator-evidence/instrumentation.txt
! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' dashboard-emulator-evidence/instrumentation.txt
