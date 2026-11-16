# OpenVitals on the web

The web copy is not a second app. It is the real APK of a commit, photographed screen by
screen, with every tap that leads somewhere turned into a link. It cannot drift from the app,
and nobody maintains it by hand.

On every push to `main`, `.github/workflows/web-app.yml`:

1. builds the `ci` APK and its test APK;
2. starts an Android 15 emulator, grants the app its permissions and runs
   `WebDemoSeedTest`, which writes thirty days of demo records into Health Connect and takes
   the app past onboarding, in Italian;
3. runs `scripts/web_crawl.py`, which opens the dashboard on a tall screen (so a whole page
   fits one photograph) and taps every tappable element of every screen, three levels deep.
   A tap that leads to a new screen is photographed; taps that would change the demo, start a
   recording or leave the app are skipped;
4. runs `scripts/web_build.py`, which crops each photograph into its content and its bottom
   bar and writes the PWA from `web-app/site/`;
5. publishes it to GitHub Pages. Pushes to `main_claude` build it as an artifact only.

The demo is in `WebDemoSeedTest`: change it there and the next build shows the new data.

What it cannot do: type, save, scroll a chart's scrubber or start a workout. It shows what
the app shows; it does not compute.

GitHub Pages must be on, once: Settings → Pages → Source → GitHub Actions.
