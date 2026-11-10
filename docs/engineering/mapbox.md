# Personal workout maps

This fork uses Mapbox Maps Android SDK 11.34.2 (NDK 27, 16 KB pages) in saved workout route cards. Recording and imported offline map packs retain their existing renderers.

## Sign in on the phone

Open Settings → Mapbox → Sign in to Mapbox. The app opens the external browser for password, provider login and MFA. No password, session cookie or personal map token goes through GitHub, CI, the assistant or an application server.

The OAuth flow follows the official [Mapbox CLI implementation](https://github.com/mapbox/mapbox-cli/blob/main/src/auth.rs): dynamic registration with `tokens:read` only, authorization code with PKCE S256, random state and an ephemeral IPv4 loopback callback. The listener stays bound from registration to completion, rejects mismatched/duplicate state and expires after five minutes. Cancelling closes the listener. Process death cancels the login; start again from Settings.

After authorization, OpenVitals requests only `default=true&usage=pk` from the Tokens API and requires an unrestricted default public token with the map-reading scopes. OAuth access credentials remain in memory for this request and are discarded, with no refresh credentials saved. The selected public token is encrypted with AES-GCM using Android Keystore and stored atomically in `noBackupFilesDir`. Remove in Settings deletes the local credential; it does not revoke the token in the Mapbox account or log the browser out. If the token is refreshed in Mapbox, sign in again.

No build reads `MAPBOX_PUBLIC_TOKEN` or `mapboxPublicToken`; remove any obsolete Actions secret. APKs and CI artifacts contain no personal runtime credential. The login screen blocks screenshots. Credentials, callbacks and response bodies must never be logged or included in crash reports.

This protects stored credentials and distribution, not a compromised app process: the public token must enter SDK memory to render maps. Account administrators and modified app code remain outside this security boundary. OAuth registration was verified without an account; successful account login, token retrieval and online maps still require device verification. Provider availability and OAuth policy changes may prevent login; failures leave the local GPS fallback available.

Without a login, saved workouts show the existing imported offline map or local GPS preview. No empty map is created for a workout without valid coordinates.

## Rendering and offline behavior

The map follows the app background brightness, uses Mapbox Light/Dark, an azure line, a green start and red finish, and fits the route once. Default Mapbox gestures remain enabled. The built-in attribution control is retained, including Mapbox's telemetry choice. No location plugin is activated for saved workouts.

Mapbox manages its normal on-device resource cache. This is not a downloaded offline region or a promise that a previously viewed map remains cached. When a map style or tiles fail to load, the locally stored GPS preview remains visible. No additional route data is fetched or written.

## Verification

The consolidated `verify` workflow build an APK, install and launch it, and collect screenshot, UI hierarchy, activity state and logcat. A successful launch is not proof that Mapbox cartography works: this requires a successful on-device Mapbox login, an activity containing GPS samples, network availability and visual inspection. CI deliberately has no personal login and verifies the fallback path, protocol helpers and encrypted token storage; online tests skip explicitly.
