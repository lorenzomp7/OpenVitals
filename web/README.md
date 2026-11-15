# OpenVitals PWA

Installable browser version of OpenVitals with manual demo records. The PWA replaces the earlier standalone dashboard concept. It uses the Android dashboard's structure: app header, period/date navigation, centre ring and two side arcs, Start workout / Log / Edit actions, configurable metric tiles and activity history.

Live PWA: https://openvitals-demo-lorenzomp7.meshedarbor16.chatgpt.site/

## Current web functionality

- Forty widget definitions imported from the Android source: names in Italian/English, accent colours, icon identifiers and default order. `scripts/sync-android.js` regenerates `android-catalog.js` during every build, including CI. This links the catalogue to Android changes; it does not translate Kotlin screen implementations automatically.
- Day/week/month/year navigation and per-metric detail pages, with bar or line charts, statistics, history and manual daily entries. Missing values stay missing. Body composition includes lean mass and body water mass.
- Editable dashboard order, visibility, size (1×1, 2×1, 2×2), detail count and six-tile page layout. Pointer drag and accessible move buttons. Finish editing with Save/Discard.
- Activity list/detail, manual workouts, and a local workout timer. No GPS or sensor recording in the PWA.
- Body profile, circular photo with view/change/delete menu, name, birth date with calculated age, sex and height.
- Theme (light/dark/AMOLED/system/warm), metric language, goals, achievements based on recorded demo data, version/about screen.
- Local JSON backup/restore of records, workouts, profile, photo, goals and layout. Legacy version 1 record exports remain importable. Corrupt backups are rejected before replacing local state.
- Standalone manifest, Home screen icons and a versioned service worker. After first complete online load, the shell and local data remain usable offline. iOS installation guidance is inside Settings → Install OpenVitals.

## Boundaries

This is a web port, not an APK emulator, and is not yet full feature or pixel parity with every Android destination. Native-only device/Health Connect/HealthKit/OpenWatch connections are not active (the requested demo uses manual data). Android screenshot OCR, clinical document management, GPX/FIT import, detailed recovery algorithms, workout plans and reports are not implemented in this PWA. The UI does not claim fabricated sensor readings or generated recovery scores.

Data stays in localStorage on the browser's origin, independently per browser/device. Exports contain the user's local data. Use a backup before clearing browser storage. The timer does not depend on interval scheduling for elapsed time, but its unsaved active state is lost if the page is closed/reloaded.

## Build and checks

Node.js 22+, no npm dependencies:

```bash
npm run build
npm test
python3 -m http.server 8000 --directory dist
```

Run these from `web/`. Build copies only PWA assets into `dist/`, regenerates the Android catalogue and generates the service worker with a content-hashed cache version. Tests cover periods, missing values, numeric validation, backups, Android catalogue completeness and offline navigation fallback with all precached files present.

## Hosting and branch workflow

Work is on `main_chatgpt_04`. `.github/workflows/web.yml` builds and tests every push to this branch and `main`, and pull requests to `main`. GitHub Pages deployment runs only from `main`, after web checks, once Settings → Pages → Source is set to GitHub Actions. Android builds remain in `verify.yml`; web files are not Gradle modules and never enter the APK.

The live chatgpt.site publication is a separate hosting target updated by the publishing workflow in this conversation. The GitHub Pages action does not update that URL. Its host project identity is recorded in `hosting-target.json` so future work can update the same publication. Do not create a second Site for this PWA.

Source is distributed under the repository's [AGPL-3.0 licence](../LICENSE).
