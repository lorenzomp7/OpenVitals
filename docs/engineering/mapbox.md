# Personal workout maps

This fork uses Mapbox Maps Android SDK 11.34.2 (NDK 27, 16 KB pages) in saved workout route cards. Recording and imported offline map packs retain their existing renderers.

## Public token

In this repository on GitHub, open Settings → Secrets and variables → Actions → New repository secret. Name it `MAPBOX_PUBLIC_TOKEN` and paste the **public** token starting with `pk.` from your Mapbox account. The verification workflows inject it into the APK. Rebuild after changing the secret. Secret `sk.` tokens are rejected by Gradle and must never be supplied.

For a local build, set the environment variable `MAPBOX_PUBLIC_TOKEN`, or put `mapboxPublicToken=pk.…` in your user-level `~/.gradle/gradle.properties` (outside the repository). Do not put tokens in tracked files. A public runtime token is embedded in the APK and can be extracted, even when supplied as an Actions secret.

Without a token, saved workouts show the existing imported offline map or the local GPS preview. No empty map is created for a workout without valid coordinates.

## Rendering and offline behavior

The map follows the app background brightness, uses Mapbox Light/Dark, an azure line, a green start and red finish, and fits the route once. Default Mapbox gestures remain enabled. The built-in attribution control is retained, including Mapbox's telemetry choice. No location plugin is activated for saved workouts.

Mapbox manages its normal on-device resource cache. This is not a downloaded offline region or a promise that a previously viewed map remains cached. When a map style or tiles fail to load, the locally stored GPS preview remains visible. No additional route data is fetched or written.

## Verification

`verify` and the dashboard emulator workflow build an APK, install and launch it, and collect screenshot, UI hierarchy, activity state and logcat. A successful launch is not proof that Mapbox cartography works: this requires the public token, an activity containing GPS samples, network availability and visual inspection. APKs built without the token only verify the fallback path.
