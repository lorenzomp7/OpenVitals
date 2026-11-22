# Consolidated personal build

The phone-installable APK is built from `main_claude` by `publish-personal-release`, the only workflow that publishes it. It runs when a push to `main_claude` (or `main`) changes its own workflow file, which holds the version name and the release notes, or when started manually (Actions → publish-personal-release → Run workflow). It runs the full `verify`, a signed build, a signature check against the previously published APK, an update install on an Android 15 emulator, and only then publishes a GitHub release tagged `personal-claude-<run id>` and marked latest, so `releases/latest/download/OpenVitals.apk` always points to the newest build. It then deletes the older releases and their tags, keeping the newest three. The manual `cleanup` workflow does the same on demand and also deletes finished workflow runs older than a week.

The version code goes above every code stated in published release notes and above the published APK, so Android never refuses an update as a downgrade.

## Verification

`verify` builds the CI app and instrumentation APKs once, runs unit tests and preflight, then installs those same APKs on an Android API 35 emulator. It saves launch evidence, test results, light/dark dashboard fixtures, editing and mixed-size fixtures, the six-tile layout and GPS fallback. Online map checks skip explicitly when no on-device Mapbox login is configured. Test failures and timeouts fail the run, while evidence is uploaded even on failure.

The manual `personal-screenshots` workflow reuses `verify`; it never rewrites a public screenshot branch. There is no store publishing, wiki or translation workflow: the GitHub release is the only place the APK is published.

## Personal signing

The phone-installable APK uses the same existing personal development key and application ID (`tech.mmarca.openvitals.debug`) as Claude's personal APK. The key in `personal/personal.p12` is a shared development key, already present in Claude's branch, and is not the upstream release signing key. No PC setup or Actions secrets are required.

Its version code is above every code already published. Before publishing `OpenVitals.apk`, the workflow downloads the previously published APK, compares the signing certificates, installs that earlier APK on an emulator, and updates it with the new APK without uninstalling. An update of that `.debug` app preserves its local settings and permissions.

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
axis retains the true elapsed session times; no samples are invented for unrecorded intervals.
Charts do not pinch: every chart has a full-screen button instead, and the full-screen
view turns the chart a quarter turn to read it along the long side.

## Profile and header refinements

The dashboard header is the first ordinary item of the dashboard list, with no collapsing or nested-scroll behavior. The launcher symbol uses a continuous blue, red, yellow and green gradient over a solid white launcher background. Body profile displays age from the complete birth date, keeps that date in settings backups, and stages the name, photo, body metrics, BMR option and caffeine factors until the single Save button is pressed. The centered profile picture opens a menu with View, Change and Delete; View shows the full uncropped picture.
