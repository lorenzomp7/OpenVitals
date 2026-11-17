#!/usr/bin/env bash
# Runs inside the isolated container, never on the host.
set -euo pipefail
sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
# Avoid SIGPIPE from yes under pipefail, while preserving sdkmanager failures.
"$sdkmanager" --licenses <<< "$(printf 'y\n%.0s' {1..100})" >/dev/null
"$sdkmanager" 'platforms;android-37.0' 'build-tools;37.0.0' platform-tools
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
java -version
node --version
