#!/usr/bin/env bash
# Hook SessionStart: prepara l'ambiente a ogni avvio di una sessione Claude Code cloud.
# Fuori dal cloud non fa nulla (vedi in_cloud in session_lib.sh).
source "${CLAUDE_PROJECT_DIR:-.}/.claude/hooks/session_lib.sh"
in_cloud || exit 0
cd "$ROOT" || exit 0

# Skill: 7 procedure in .claude/skills/ puntano al submodule tools/agent-skills.
if [ ! -f tools/agent-skills/skills/code-review-and-quality/SKILL.md ]; then
  git submodule update --init tools/agent-skills >> "$LOG" 2>&1 && note "skill agent-skills: scaricate" \
    || note "skill agent-skills: download FALLITO"
else note "skill agent-skills: già pronte"; fi
note "Web (PWA): nessuna dipendenza da installare; test con: npm test --prefix web"
# Android: JDK 17 + SDK (platform e build-tools di scripts/ci-android-setup.sh). Richiede dl.google.com.
export_env 'ANDROID_HOME="$HOME/android-sdk"'
export_env 'ANDROID_SDK_ROOT="$HOME/android-sdk"'
export_env 'PATH="$HOME/android-sdk/cmdline-tools/latest/bin:$HOME/android-sdk/platform-tools:$PATH"'
background android .claude/hooks/android_sdk.sh --jdk17 --setup-script scripts/ci-android-setup.sh platform-tools
finish reload
