#!/usr/bin/env bash
set -euo pipefail
apk=personal-apk/OpenVitals-personal.apk
package=tech.mmarca.openvitals.personal
# Both installation paths must work; never uninstall an existing app to hide a signing failure.
adb install "$apk" | tee personal-apk/install.txt
adb install -r "$apk" | tee personal-apk/update.txt
grep -q '^Success' personal-apk/install.txt
grep -q '^Success' personal-apk/update.txt
adb shell am start -W -n "$package/tech.mmarca.openvitals.MainActivity" | tee personal-apk/launch.txt
! grep -q 'Error:' personal-apk/launch.txt
adb shell pidof "$package" > personal-apk/process.txt
