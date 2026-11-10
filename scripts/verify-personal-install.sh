#!/usr/bin/env bash
set -euo pipefail
apk=personal-apk/OpenVitals-personal.apk
package=tech.mmarca.openvitals.debug
previous=$(find previous-apk -name '*.apk' -type f | head -1)
test -n "$previous"
signer=$(find "$ANDROID_HOME/build-tools" -name apksigner -type f | sort -V | tail -1)
# Compare the actual earlier distributed APK with the new build, not only a reinstall.
"$signer" verify --print-certs "$previous" > personal-apk/previous-certificate.txt
for cert in previous-certificate signing-certificate; do
  grep 'Signer #1 certificate SHA-256 digest:' "personal-apk/$cert.txt" > "personal-apk/$cert.sha256"
done
cmp personal-apk/previous-certificate.sha256 personal-apk/signing-certificate.sha256
adb install "$previous" | tee personal-apk/previous-install.txt
adb install -r "$apk" | tee personal-apk/update.txt
grep -q '^Success' personal-apk/previous-install.txt
grep -q '^Success' personal-apk/update.txt
adb shell am start -W -n "$package/tech.mmarca.openvitals.MainActivity" | tee personal-apk/launch.txt
! grep -q 'Error:' personal-apk/launch.txt
adb shell pidof "$package" > personal-apk/process.txt
