# Development

## Build Requirements

- Android SDK Platform 37.0 with SDK Build-Tools 37.0.0
- JDK 17
- Gradle wrapper files, including `gradle/wrapper/gradle-wrapper.jar`

The wrapper jar is intentionally tracked. `.gitignore` allows this file even though other jars are ignored.

## Local Verification

Run the main checks before pushing architecture or feature changes:

```bash
./gradlew verifyCi
git diff --check
```

`verifyCi` is an aggregate defined in the root `build.gradle.kts`:

- `verifyCiUnitTest` runs `:domain:testCiUnitTest` and `:app:testCiUnitTest`.
- `verifyCiPreflight` runs `verifyTranslations`, `:app:checkCiDuplicateClasses`
  and `:app:compileCiAndroidTestKotlin`.
- `verifyCiLint` runs `:app:lintCi` and `:domain:lintCi`. It is its own aggregate
  because CI runs it only on pushes to `main`, on a job of its own.

Nothing assembles an APK: nothing used it, and dexing it cost a minute and a
half. The release pipeline builds the real one after the gate.

Unit tests run against the `ci` build type. There is no debug unit-test variant,
so the task is `:app:testCiUnitTest`; `:app:testDebugUnitTest` does not exist.
To run a single test class:

```bash
./gradlew :app:testCiUnitTest --tests 'tech.mmarca.openvitals.SomeTest'
```

`verifyAndroidTest` runs `:app:connectedCiAndroidTest`, but only locally and only
when `ANDROID_SERIAL` is set; it is disabled in CI.

### Translation Gate

For translation-only changes, the fast local check is:

```bash
./gradlew verifyTranslations
```

That task shells out to `scripts/verify-translations.py`, which enforces
greater-than-70% coverage per locale, placeholder safety, plural shape, and
`translatable="false"` handling. `verifyCi` includes it, so Weblate pull requests
must keep locale files more than 70% translated and placeholders intact.

Current state: `verifyTranslations` passes and every locale file is at 100%.
A locale under the threshold only prints a note — the coverage floor decides
whether a language is *offered* in the picker, not whether its file may exist,
so a translator's first commit does not break CI. Galician looked like this
before it was completed:

```text
note: app/src/main/res/values-gl/strings.xml is at 3.4% coverage and is not offered in the language picker until it passes 70%.
```

What still fails the build for every locale file, offered or not, is placeholder
and plural-shape safety. A locale may carry *more* CLDR plural categories than
the English base (Czech and Spanish need `few`/`many`); those extra branches are
checked against the base `other` branch so they cannot drop an argument.

A new key goes to every `values-*/strings.xml` together with the base file, in
the plural shape that locale needs; see [translations.md](translations.md).
Weblate then refines the wording.

For Apple Health importer work, there is also a desktop JVM smoke test that can exercise the Kotlin importer against a real local export without building or installing the app:

```bash
./gradlew app:testCiUnitTest \
  --tests tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportSmokeTest \
  -PappleHealthExport=/path/to/export.zip \
  --console=plain
```

`-PappleHealthExport` can point to an Apple Health `export.zip`, `export.xml`, or an unzipped export directory. The test parses XML, parses GPX route files when present, runs supported Health Connect conversion logic, and prints import-shape counts. It is skipped automatically when no export path is provided.

On Windows, use `gradlew.bat`:

```powershell
.\gradlew.bat verifyCi
git diff --check
```

### R8 Keep Check

Unit tests run on the unminified `ci` variant, and no pipeline installs an APK, so nothing runs the R8 build before it ships. Every `minify<Variant>WithR8` task is therefore followed by `verify<Variant>R8Keeps`, which runs [`scripts/verify-r8-keeps.py`](../../scripts/verify-r8-keeps.py) against that build's `mapping.txt`. It fails the build when R8 removed or renamed something that is only reached by name at run time:

- a manifest component of ours, or its no-arg constructor
- a Glance `ActionCallback` subclass, or its no-arg constructor (R8 once stripped one; widget taps were dead from 2.7.0 to 2.7.1)
- a Health Connect record class named in `SyncRecordCodec.kt`, whose simple name is the phone-sync wire format

It needs no device, so it runs in the release pipeline as part of the build. To run it by hand: `./gradlew :app:minifyReleaseWithR8`. When it fails, add a keep rule to `app/proguard-rules.pro` and say why. A new kind of by-name lookup needs a new check in the script.

## Hilt And KSP

The local app uses Hilt in the `:app` module:

- `@HiltAndroidApp` on `OpenVitalsApp`
- `@AndroidEntryPoint` on `MainActivity`
- `@HiltViewModel` for screen ViewModels
- `hiltViewModel()` in navigation destinations
- KSP for Hilt code generation

AGP 9 built-in Kotlin currently requires `android.disallowKotlinSourceSets=false` so KSP generated sources are accepted. Keep this in `gradle.properties` unless the Android Gradle Plugin/KSP behavior changes.

## Docs links

`scripts/sync-wiki.py --check` is the verify gate's `docs-links` job: it fails on a relative link in the repository's Markdown to a file that does not exist. Run it before pushing a docs change:

```bash
python3 scripts/sync-wiki.py --repo lorenzomp7/OpenVitals --check
```

Two Markdown files with the same first heading are also refused: the script names both.

## CI

CI is GitHub Actions on the stock `ubuntu-26.04` runner, which ships JDK 17
and the Android SDK. It is pinned rather than `ubuntu-latest`, which moves from
Ubuntu 24.04 to 26.04 between 2026-10-19 and 2026-11-19, so no job switches
image mid-way. The image's default Java is 25; `scripts/ci-gradle.sh` takes its
JDK 17 from `JAVA_HOME_17_X64`. `scripts/ci-android-setup.sh` installs the platform and
build-tools the app compiles against when the runner image lacks them (a
prebaked container image did this until 2026-10; pulling it cost as much as the
install). The Gradle jobs cache Gradle with `gradle/actions/setup-gradle` (v5:
v6 moves its caching into a proprietary component under Gradle's terms of use) and
go through `scripts/ci-android-gradle.sh`, which sizes the JVMs for the
runner's 16 GB. `verify.yml` is the gate, on every pull request
and push to `main`: the `app-test` job always, `lint` on pushes to `main` only:

```bash
# tests job: every core gets a test fork, since nothing else runs here
./gradlew --no-daemon -Popenvitals.testForks=4 verifyCiUnitTest verifyCiPreflight
git diff --check
# lint job
./gradlew --no-daemon verifyCiLint
git diff --check
```

Lint alone is half of the gate's task time, so in a single job it stretched
the tests, which shared the runner's four cores with it. A pull request's gate
is the tests job alone; a lint finding is caught on `main` and by the release run.

The Gradle build cache is on (`org.gradle.caching` in `gradle.properties`), and
`setup-gradle` keeps it between runs along with the dependency cache. A code
change invalidates `:app`'s compile and test tasks either way, so the win is on
`:domain` while only the app changed, on reruns, and on the release build, which
restores what the `main` run of the same commit produced.

`publish-personal-release.yml` publishes the phone APK from `main_claude`: it
runs `verify` beside a signed build and an update install on an emulator, and
publishes a GitHub release only when all of them pass. After publishing it
deletes the older releases and their tags, keeping the newest three
(`scripts/prune-github-releases.sh`). See
[Consolidated personal build](personal-build.md). `personal-screenshots.yml`
reruns `verify` by hand, and `cleanup.yml` prunes releases and old workflow
runs by hand.

### The bundled OpenLink sources

The app builds the OpenLink client from the source tree at `openlink/`
(`includeBuild("openlink")` in `settings.gradle.kts`). This is bundled source,
so cloning and building do not require a separate upstream checkout.
The original AGPL license is retained at `openlink/LICENSE`.

## Release Checklist

1. In `.github/workflows/publish-personal-release.yml`, bump the version name
   (`OPENVITALS_VERSION_NAME` and the release title) and rewrite the release
   notes: what changed, in words the phone user reads. The notes describe the
   change, not the request it came from.
2. Run the local checks:

```bash
./gradlew verifyCi
git diff --check
```

   CI has no device for the instrumented tests outside the release run. To run
   them on a phone or emulator:

```bash
ANDROID_SERIAL=<serial> ./gradlew verifyAndroidTest
```

3. Push to `main_claude`. The change to the workflow file starts the release
   run; when it has passed, the new APK is the latest release and only the
   newest three releases remain.
