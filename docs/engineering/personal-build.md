# Consolidated personal build

`main_chatgpt` combines the verified Mapbox work with the personal dashboard layout. The older branches remain unchanged. The screenshot-only `personal-shots` branch is not merged into application history.

## Verification

`verify` builds the CI app and instrumentation APKs once, runs unit tests and preflight, then installs those same APKs on an Android API 35 emulator. It saves launch evidence, test results, light/dark dashboard fixtures, editing and mixed-size fixtures, the six-tile layout and GPS fallback. Online map checks skip explicitly when no on-device Mapbox login is configured. Test failures and timeouts fail the run, while evidence is uploaded even on failure.

The manual `personal-screenshots` workflow reuses `verify`; it never rewrites a public screenshot branch. Release, wiki and Weblate workflows are manual in this personal branch. Store publishing and announcements cannot start on a schedule or tag push.

## Personal signing

The previously exposed `personal.p12` is not imported into this branch or its ancestry. Its old key must not sign future personal APKs. This branch never contains a private signing key or password.

From a local Windows checkout, with GitHub CLI authenticated and JDK 17+ installed, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-personal-signing.ps1
```

The script generates a fresh key outside the repository, stores a password backup protected with Windows DPAPI, and configures three Actions secrets through GitHub CLI. Re-running reuses that new key, preserving update compatibility. Keep the local key backup; the password backup belongs to the Windows account that created it. Losing it can prevent future updates with the same signature.

Then run `personal-apk` manually, or push a commit containing `[apk]` to `main_chatgpt`. The app installs under `tech.mmarca.openvitals.debug`. The new signature cannot update an installed APK signed with the exposed old key; installing it may require removing the old personal build. Preserve/export personal app data before doing so. The CI APK has a separate `.ci` package and remains available without personal signing secrets.

For maps, sign in to Mapbox on the phone as described in [Mapbox configuration](mapbox.md). Neither build receives a Mapbox secret from GitHub.

## Dashboard state and history

The four-column layout uses 1×1, 2×1 and 2×2 tiles in both editing and display. Six medium tiles occupy a three-row page. Widget size and paging preferences live in the preferences repository. The first editor's `dashboard_widget_layout` file is migrated once; existing canonical preferences win.

Older workouts are requested in 30-day windows, revealed ten at a time and loaded further only when requested. There is no fixed twenty-workout cap. Changing day resets the list; stale responses cannot replace a newer day's state. Permission, rate-limit and other errors remain visible instead of being presented as an empty history. Data availability still depends on Health Connect and its history permission. This does not import Zepp history.
