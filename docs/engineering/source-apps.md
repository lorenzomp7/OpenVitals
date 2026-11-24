# Source apps: what they write to Health Connect

Plan row "Integrazioni GitHub", OpenVitals 1 (REA). Checked 2026-11-24 from **public
sources, not the APKs**. On the phone the authoritative list is the one OpenVitals reads
itself: Diagnostics → "What each source writes" shows each installed app's requested
`android.permission.health.WRITE_*` permissions (`HealthConnectSourceDeclarations`), so the
APK step of the plan (pull the APK, read its manifest with REA) is only needed for an app that
is not installed.

| App | Package | What it writes, according to public sources | Notes |
|---|---|---|---|
| Zepp (Amazfit, GTR 3) | `com.huami.watch.hmwatchmanager` | since January 2025 about 26 data types, chosen one by one under Profile → 3rd-party account linking → Health Connect: heart rate, calories, sleep, blood pressure, weight and others ([Gadgets & Wearables](https://gadgetsandwearables.com/2025/01/24/zepp-health-connect/), [Notebookcheck](https://www.notebookcheck.net/Amazfit-smartwatches-get-new-Health-Connect-data-sync-feature.951489.0.html)) | write only, it reads nothing back; a type left unticked is declared in the manifest but never arrives, which is exactly the red "declared" cell |
| Zepp Life (formerly Mi Fit) | `com.xiaomi.hm.health` | no direct Health Connect link documented: it syncs to Google Fit, and Google Fit to Health Connect ([Android Authority](https://androidauthority.com/mi-band-samsung-health-3246266), [Welltory](https://help.welltory.com/en/articles/3364869-how-to-connect-mi-band)) | not Xiaomi's Mi Fitness app (`com.xiaomi.wearable`); with Google Fit's app connections ending on 30 June 2025 ([Welltory](https://help.welltory.com/en/articles/3364869-how-to-connect-mi-band)), records may show Google Fit as their origin, or stop |
| Gadgetbridge | `nodomain.freeyourgadget.gadgetbridge` | steps, distance, active calories, heart rate, SpO₂, blood glucose, when the gadget records them; export enabled under Settings → External Integrations → Health Connect, per device ([Gadgetbridge docs](https://gadgetbridge.org/basics/integrations/health-connect/)) | an open pull request adds heart rate and sleep stages from dedicated providers ([Codeberg #6249](https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/6249)); sleep is therefore not guaranteed today |

## What this means for the table

- A Zepp row with "declared" cells is usually a permission the user left off in Zepp, not a
  sync failure: the Zepp note in Diagnostics can point to that screen.
- Zepp Life data that arrives through Google Fit carries Google Fit's package as origin, so it
  will not appear in a Zepp Life column.
- With Gadgetbridge as the source, missing sleep is expected until the pull request above lands.

## Release check

The second half of the plan row (signature and exported components of each OpenVitals
release) is done in `publish-personal-release`: `apksigner` compares the signing certificate with the
previously published APK and `scripts/check-release-manifest.py` the permissions and exported
components; see
[personal-build.md](personal-build.md#release-manifest-check).
