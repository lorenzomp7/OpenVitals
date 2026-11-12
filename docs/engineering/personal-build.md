# Consolidated personal build

`main_chatgpt` starts from the consolidated branch that was rebuilt directly from Claude's `claude/adoring-brown-692amx` at `ce31872527acfd27ca6c4080e9b82c1162831329` and integrates all changes from `main_chatgpt` at `220605999f36def2626bb454f45b3582cda1c47e`. Claude's full history and the fast first-day activity load are retained. The older branches remain unchanged. The screenshot-only `personal-shots` branch is not merged into application history.

## Verification

`verify` builds the CI app and instrumentation APKs once, runs unit tests and preflight, then installs those same APKs on an Android API 35 emulator. It saves launch evidence, test results, light/dark dashboard fixtures, editing and mixed-size fixtures, the six-tile layout and GPS fallback. Online map checks skip explicitly when no on-device Mapbox login is configured. Test failures and timeouts fail the run, while evidence is uploaded even on failure.

The manual `personal-screenshots` workflow reuses `verify`; it never rewrites a public screenshot branch. Release, wiki and Weblate workflows are manual in this personal branch. Store publishing and announcements cannot start on a schedule or tag push.

## Personal signing

The phone-installable APK uses the same existing personal development key and application ID (`tech.mmarca.openvitals.debug`) as Claude's personal APK. The key in `personal/personal.p12` is a shared development key, already present in Claude's branch, and is not the upstream release signing key. No PC setup or Actions secrets are required.

Run `personal-apk` manually, or push a commit containing `[apk]` to `main_chatgpt`. Its version code increases with the workflow run number. Before publishing `OpenVitals-personal.apk`, the workflow downloads the previously distributed Claude APK from run `42711097393`, compares the signing certificates, installs that earlier APK on an emulator, and updates it with the new APK without uninstalling. An update of that `.debug` app preserves its local settings and permissions.

The `.ci` test package is a different app with a temporary runner signature. It cannot be converted into the `.debug` app by installing an APK. Leave it installed if you need to keep its local settings; the personal APK can coexist with it. Use the settings export/import section to transfer configuration between installed versions that support it. Health Connect records remain in Health Connect.

For maps, sign in to Mapbox on the phone as described in [Mapbox configuration](mapbox.md). Neither build receives a Mapbox secret from GitHub.

## Dashboard state and history

The four-column layout uses 1×1, 2×1 and 2×2 tiles in both editing and display. Six medium tiles occupy a three-row page. Widget size and paging preferences live in the preferences repository. The first editor's `dashboard_widget_layout` file is migrated once; existing canonical preferences win.

Older workouts are requested in 30-day windows, revealed ten at a time and loaded further only when requested. There is no fixed twenty-workout cap. Changing day resets the list; stale responses cannot replace a newer day's state. Permission, rate-limit and other errors remain visible instead of being presented as an empty history. Data availability still depends on Health Connect and its history permission. This does not import Zepp history.

## Dashboard periods

The selector at the top left offers Day, Week, Month and Year, remembers the choice, and reuses the shared period navigation and week preference. Future dates are capped at today. Activity, nutrition, hydration and mindfulness are totals over the selected period; sleep is the mean of recorded nights and heart rate is the Health Connect aggregate average and HRV is the mean of recorded days. Goals for cumulative metrics scale to the elapsed period length. Other measurement tiles retain their latest-value semantics; training tiles retain their explicitly weekly window at the period end. The dashboard states these distinctions. Older-workout pagination is available in Day only; other ranges show the workouts inside the chosen period. Range reads use Health Connect aggregates rather than loading one dashboard per day.

## Workout and weight fixes

Saved route maps have a fullscreen button, a close button and Android Back dismissal,
including the local fallback without Mapbox login. Only one native map is mounted.
Returning from a scale app refreshes body screens and the settled current dashboard;
the initial in-flight dashboard load remains coalesced. Weight still requires a record
written to Health Connect by the scale provider.

Workout steps are aggregated from the session writer rather than the phone priority
source. An absent writer total stays missing. Steps cadence uses spm; cycling uses rpm.
A missing cycling permission no longer prevents reading granted steps cadence.
Speed and cadence charts initially focus a short recorded sample interval while their
axis retains the true elapsed session times. Pinch/pan and double-tap full-session reset
remain available; no samples are invented for unrecorded intervals.

## Profile and header refinements

The dashboard header is the first ordinary item of the dashboard list, with no collapsing or nested-scroll behavior. The launcher symbol uses a continuous blue, red, yellow and green gradient over transparency. Body profile displays age from the complete birth date, keeps that date in settings backups, and stages the name, photo, body metrics, BMR option and caffeine factors until the single Save button is pressed. Camera and delete icons beside the photo choose and remove the profile picture.
