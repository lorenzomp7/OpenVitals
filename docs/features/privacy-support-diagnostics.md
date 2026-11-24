# Privacy, Support, And Diagnostics

> **Status:** Current implemented behavior and support policy.
> **Audience:** Users and contributors.
> **Implementation:** `features/settings`, diagnostics-gated settings, local support docs.
> **Navigation:** `Screen.SettingsDebugDiagnostics` in diagnostics builds, Settings support links.
> **Related:** [Feature map](feature-map.md), [Privacy](../app/privacy.md), [Support](../app/support.md).

OpenVitals is designed as a local-first Health Connect app.

For the standalone privacy reference, see [Privacy](../app/privacy.md). For support-oriented questions, see [FAQ](../app/faq.md).

## Privacy Model

OpenVitals does not require:

- An OpenVitals account.
- OpenVitals cloud sync.
- OpenVitals analytics.
- Ads.

Health data is read from and written to Health Connect on device. OpenVitals stores local preferences such as widget order, goals, reminders, calibration, and display choices.

A small amount of health data lives in the app's own database rather than in Health Connect, because Health Connect has no record type for it: the cycle journal. See [Cycle tracking](cycle-tracking.md).

## Internet Boundary

The app does not need app-level internet access for normal health features. Offline map packs, imports, widgets, and Health Connect reads are handled locally.

The connected-device features keep that boundary rather than widening it. Bluetooth LE sensors are read over Bluetooth, and sync with another phone deliberately uses Bluetooth Classic because any Wi-Fi or TCP socket on Android would require the internet permission the app does not declare. Watches and scales are the business of the separate OpenWatch app, which writes what they record to Health Connect.

## Diagnostics

Diagnostics surfaces help users and maintainers understand local issues without sending health data automatically. They can include app version information, debug build separation, import reports, logs, and crash-report email drafts. Each holds structure, not data: counts, types, reasons and masked error text. See [Text that leaves the app is structure, not data](../engineering/architecture.md#5-text-that-leaves-the-app-is-structure-not-data).

Diagnostics builds add two cards that are useful when data appears to be missing:

- A reminder test, which posts the hydration reminder notification immediately and exactly as a scheduled one would.
- Health Connect sources, which lists the apps that contributed heart-rate and sleep records over the last seven days. It is the quickest way to tell whether a watch app's data is actually landing in Health Connect. Each app also shows the data types its manifest declares it writes, read on the phone from the installed app's requested `android.permission.health.WRITE_*` permissions, and in red the declared heart-rate or sleep types that did not arrive this week: an app that never writes sleep reads apart from one that writes it but sent none. An app that declares writing heart rate or sleep and sent nothing is listed too, with no records. The apps are found through the two Health Connect intents in the manifest's `<queries>` (the permissions rationale and the Android 14 `VIEW_PERMISSION_USAGE` alias); a source synced from another phone is not installed here, so its declarations read as unknown. A declaration is not a grant: whether the app holds the permission is Health Connect's to say.
- What each source writes, a table read only when its button is tapped. Rows are the data types OpenVitals reads (steps, heart rate, HRV, SpO₂, sleep and sleep stages, temperatures, workouts and their routes, body measurements, nutrition and the rest), columns are the source apps; a cell is the date of the app's latest record of that type over the last 30 days, "declared" in red when the app declares writing the type and none arrived, or a dash. It takes one page of records per type (about one Health Connect call each, through the shared pacer and backoff), plus one page filtered to the declaring apps when the first page did not cover the month; a type whose read stopped at the page limit is starred, since an empty cell there proves nothing. Only origins and times are kept, never a value. Types without a read permission are listed apart. With a Zepp source the card adds a fixed note: Zepp sends Health Connect only the types the table shows, stress and PAI have no Health Connect type, and Gadgetbridge can replace Zepp as the source once the watch is paired with Zepp and its authentication key is known.

Apple Health import reports are explicit user downloads with the importer summary, logs, grouped diagnostics, worker logs, and on failure the masked exception stack; the per-record diagnostic log is capped per source (see [Apple Health import](apple-health-import.md#reports-and-diagnostics)) so a large repeated import cannot make the report unbounded, but grouped diagnostic counts stay complete. They hold no record's value or time, no source's name and no file location.

Diagnostics builds export the process logcat from Settings → Debug diagnostics. Every line and tag is kept, so Health Connect and sync issues can be traced, and each line is masked: addresses, identifiers, file locations and dates, and an exception's message whole. The crash and Report an issue drafts carry no log in any build: the saved crash report holds the masked crash, is attached for seven days at most, and is deleted once it has gone into an email, a copy or a file. Copied reports are marked sensitive.

## Support Links

Settings and project metadata can point users toward support and community resources such as Zulip, GitHub, or donation links.

## Health Disclaimer

OpenVitals insights are wellness context. Scores, readiness estimates, cardio load, Body Energy, and sleep explanations are not medical diagnosis or treatment advice.

Medical records are shown as they were received. OpenVitals does not interpret them, and the health disclaimer says so.
