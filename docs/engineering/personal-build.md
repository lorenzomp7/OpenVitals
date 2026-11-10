# Consolidated personal build

`main_chatgpt` starts from the consolidated branch that was rebuilt directly from Claude's `claude/adoring-brown-692amx` at `ce31872527acfd27ca6c4080e9b82c1162831329` and integrates all changes from `main_chatgpt` at `220605999f36def2626bb454f45b3582cda1c47e`. Claude's full history and the fast first-day activity load are retained. The older branches remain unchanged. The screenshot-only `personal-shots` branch is not merged into application history.

## Verification

`verify` builds the CI app and instrumentation APKs once, runs unit tests and preflight, then installs those same APKs on an Android API 35 emulator. It saves launch evidence, test results, light/dark dashboard fixtures, editing and mixed-size fixtures, the six-tile layout and GPS fallback. Online map checks skip explicitly when no on-device Mapbox login is configured. Test failures and timeouts fail the run, while evidence is uploaded even on failure.

The manual `personal-screenshots` workflow reuses `verify`; it never rewrites a public screenshot branch. Release, wiki and Weblate workflows are manual in this personal branch. Store publishing and announcements cannot start on a schedule or tag push.

## Personal signing

The previously exposed `personal.p12` is removed from the current tree. It remains in the inherited Claude history; deleting the file does not erase that history. Its old key must not sign future personal APKs. New personal APKs use the private signing secrets below.

From a local Windows checkout, with GitHub CLI authenticated and JDK 17+ installed, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-personal-signing.ps1
```

The script generates a fresh key outside the repository, stores a password backup protected with Windows DPAPI, and configures three Actions secrets through GitHub CLI. Re-running reuses that new key, preserving update compatibility. Keep the local key backup; the password backup belongs to the Windows account that created it. Losing it can prevent future updates with the same signature.

Then run `personal-apk` manually, or push a commit containing `[apk]` to `main_chatgpt`. The app installs under `tech.mmarca.openvitals.personal`, alongside existing `.ci` and `.debug` builds. The personal build refuses to compile without the persistent private signing key; it never falls back to a temporary debug key. Its version code increases with the personal workflow run number. The workflow verifies the package/certificate, installation, update without uninstall and launch on an emulator before publishing `OpenVitals-personal.apk`. Existing local preferences are not automatically copied between packages. The former CI packages use ephemeral runner debug keys and cannot reliably update one another. An APK contains the certificate but cannot recover a discarded private key. Keep the old installation while setting up the personal build; do not uninstall it to work around signature errors. The CI APK has a separate `.ci` package and remains available without personal signing secrets.

For maps, sign in to Mapbox on the phone as described in [Mapbox configuration](mapbox.md). Neither build receives a Mapbox secret from GitHub.

## Dashboard state and history

The four-column layout uses 1×1, 2×1 and 2×2 tiles in both editing and display. Six medium tiles occupy a three-row page. Widget size and paging preferences live in the preferences repository. The first editor's `dashboard_widget_layout` file is migrated once; existing canonical preferences win.

Older workouts are requested in 30-day windows, revealed ten at a time and loaded further only when requested. There is no fixed twenty-workout cap. Changing day resets the list; stale responses cannot replace a newer day's state. Permission, rate-limit and other errors remain visible instead of being presented as an empty history. Data availability still depends on Health Connect and its history permission. This does not import Zepp history.

## Dashboard periods

The selector at the top left offers Day, Week, Month and Year, remembers the choice, and reuses the shared period navigation and week preference. Future dates are capped at today. Activity, nutrition, hydration and mindfulness are totals over the selected period; sleep is the mean of recorded nights and heart rate is the Health Connect aggregate average and HRV is the mean of recorded days. Goals for cumulative metrics scale to the elapsed period length. Other measurement tiles retain their latest-value semantics; training tiles retain their explicitly weekly window at the period end. The dashboard states these distinctions. Older-workout pagination is available in Day only; other ranges show the workouts inside the chosen period. Range reads use Health Connect aggregates rather than loading one dashboard per day.
