#!/usr/bin/env bash
# Installa JDK e SDK Android in $ANDROID_HOME (default ~/android-sdk) nelle sessioni cloud.
# Uso: android_sdk.sh [--jdk17] [--setup-script PATH] [PACCHETTO_SDKMANAGER...]
# Richiede che l'ambiente cloud consenta dl.google.com (non incluso nel livello "Trusted").
set -u  # niente pipefail: "yes | sdkmanager" chiude yes con SIGPIPE anche quando riesce
STATUS_FILE=${STATUS_FILE:-/dev/null}
SDK=${ANDROID_HOME:-$HOME/android-sdk}
fail() { echo "ERRORE: $1"; echo "failed: $1" > "$STATUS_FILE"; exit 1; }
as_root() { if [ "$(id -u)" = 0 ]; then "$@"; else sudo -n "$@"; fi; }

JDK17=0; SETUP_SCRIPT=""; PACKAGES=()
while [ $# -gt 0 ]; do
  case "$1" in
    --jdk17) JDK17=1 ;;
    --setup-script) SETUP_SCRIPT=$2; shift ;;
    *) PACKAGES+=("$1") ;;
  esac
  shift
done

if [ "$JDK17" = 1 ] && ! ls -d /usr/lib/jvm/*17* > /dev/null 2>&1; then
  echo "Installo OpenJDK 17 (richiesto dalla toolchain Gradle del progetto)"
  as_root apt-get update -qq && as_root env DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-17-jdk-headless \
    || fail "installazione di OpenJDK 17 non riuscita"
fi

REPO_XML=$(curl -fsS --max-time 30 https://dl.google.com/android/repository/repository2-3.xml) \
  || fail "dl.google.com non raggiungibile: nelle impostazioni dell'ambiente cloud consenti dl.google.com"

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  ZIP=$(echo "$REPO_XML" | grep -o 'commandlinetools-linux-[0-9]*_latest\.zip' | sort -V | tail -1)
  [ -n "$ZIP" ] || ZIP=commandlinetools-linux-13114758_latest.zip
  echo "Scarico $ZIP"
  TMP=$(mktemp -d)
  curl -fsSL --max-time 600 -o "$TMP/tools.zip" "https://dl.google.com/android/repository/$ZIP" \
    || fail "download di $ZIP non riuscito"
  mkdir -p "$SDK/cmdline-tools"
  unzip -q "$TMP/tools.zip" -d "$TMP" || fail "zip degli strumenti Android non valido"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$TMP/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$TMP"
fi

export ANDROID_HOME=$SDK ANDROID_SDK_ROOT=$SDK
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"
yes | sdkmanager --licenses > /dev/null 2>&1 || true
if [ ${#PACKAGES[@]} -gt 0 ]; then
  echo "Installo ${PACKAGES[*]}"
  yes | sdkmanager "${PACKAGES[@]}" > /dev/null || fail "sdkmanager non è riuscito a installare ${PACKAGES[*]}"
fi
if [ -n "$SETUP_SCRIPT" ]; then
  (cd "$CLAUDE_PROJECT_DIR" && sh "$SETUP_SCRIPT") || fail "$SETUP_SCRIPT non riuscito"
fi
printf 'sdk.dir=%s\n' "$SDK" > "$CLAUDE_PROJECT_DIR/local.properties"

# Scarica in anticipo Gradle e i plugin del progetto: il primo build sarà più veloce.
if (cd "$CLAUDE_PROJECT_DIR" && chmod +x gradlew && ./gradlew --no-daemon -q help); then
  echo ready > "$STATUS_FILE"
else
  echo "ready (SDK pronto; il pre-download di Gradle non è riuscito: vedi il log)" > "$STATUS_FILE"
fi
echo "Android pronto in $SDK"
