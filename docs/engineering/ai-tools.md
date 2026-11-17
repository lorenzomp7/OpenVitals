# AI development tools

## agent-skills

Upstream: https://github.com/addyosmani/agent-skills (MIT). The complete
repository, including its license, commands, scripts and shared references,
is pinned as the `tools/agent-skills` Git submodule. It is development tooling;
it adds no Android or web runtime dependencies.

### Setup

```bash
git submodule update --init tools/agent-skills
python3 scripts/verify-agent-skills.py
```

Clone with `git clone --recurse-submodules` to initialize it immediately.
On Windows, enable Git symlinks and Developer Mode before cloning so the
tracked `.agents` links are materialized as directories.

Codex CLI discovers the 25 upstream workflows in `.agents/skills`. Invoke
a workflow by name, for example `$spec-driven-development` or
`$code-review-and-quality`. Restart the CLI after checkout if needed.

Claude Code can load the complete native plugin from the project root:

```bash
claude --plugin-dir ./tools/agent-skills
```

Use `/agent-skills:spec`, `/agent-skills:plan`, `/agent-skills:build`,
`/agent-skills:test`, `/agent-skills:review` or `/agent-skills:ship`.
The existing OpenVitals `.claude/skills` remain in place.
This does not install a plugin into ChatGPT or enable an agent service.

Read `AGENTS.md` before using a workflow. Use Kotlin/Compose and existing
Gradle checks for native features; use `web/README.md` for PWA checks.
Use synthetic records for examples involving health data. A workflow is
not evidence that Android device behavior or Health Connect was tested.

### Updating

Choose and review an upstream commit explicitly, then:

```bash
git -C tools/agent-skills fetch origin
git -C tools/agent-skills checkout <reviewed-commit-sha>
python3 scripts/verify-agent-skills.py
git add tools/agent-skills
git commit -m "chore: update pinned agent-skills"
```

Do not use automatic floating updates. The Git submodule entry records the
exact version; `git submodule status tools/agent-skills` shows it.

## container-use

Source: https://github.com/dagger/container-use (Apache-2.0).
`scripts/ai/container-use.sh install` builds the exact upstream commit
`2e43e625e95216b719ec9338f4034fd3a0be2734` into ignored `.tools/bin`.
Requires Go (toolchain auto-download enabled), Git and a running Docker daemon.

```bash
bash scripts/ai/container-use.sh install
bash scripts/ai/container-use.sh doctor
```

Claude Code loads the project `.mcp.json` after project trust/approval.
For Codex CLI register the same wrapper from the project root:

```bash
codex mcp add container-use -- bash "$PWD/scripts/ai/container-use.sh" stdio
```

The tracked `.container-use/environment.json` provisions Node 22, JDK 17,
Python 3 and Android platform 37.0 / build-tools 37.0.0. Android SDK license
acceptance happens inside the development container during provisioning.
It does not provision an emulator or release signing keys.
Ask the connected agent to create an environment, then run `./gradlew verifyCi`
and `npm --prefix web test` there. Review with `container-use diff <id>`
(using the wrapper) before applying any environment changes.
Initialize `tools/agent-skills` separately if a workflow needs it; container
worktree snapshots do not guarantee initialized submodule content.

This integration does not launch parallel agents automatically. Docker is
required to test environment creation; configuration validation alone does
not demonstrate an Android build.

## Gemini CLI

Source: https://github.com/google-gemini/gemini-cli (Apache-2.0).
The published `@google/gemini-cli` package is pinned to `0.68.0` in the
separate `tools/ai-node/package.json` and npm lockfile.

```bash
npm ci --prefix tools/ai-node
bash scripts/ai/gemini.sh --version
bash scripts/ai/gemini.sh
```

Complete Google's interactive authentication on your own machine, or provide
`GEMINI_API_KEY` in the process environment. Credentials are not stored here.
No model calls or paid requests run during installation or `--version`.
The project `.gemini/settings.json` loads `AGENTS.md` and disables telemetry.
Use `/review-openvitals` for the project review command. Workflow Markdown in
`tools/agent-skills` can be read explicitly when relevant; installing Gemini
CLI does not automatically install the upstream pack into Gemini's skill list.
The normal interactive approval policy is preserved. Account access, quota
and successful model responses require authentication and are separate from
CLI installation checks.

## Playwriter

Source: https://github.com/remorses/playwriter. The npm package is pinned to
`0.12.0` with its dependency lock in `tools/ai-node`.

```bash
npm ci --prefix tools/ai-node
bash scripts/ai/playwriter.sh --help
bash scripts/ai/serve-pwa.sh 8765
```

For your existing Chrome install the upstream Playwriter extension, open
`http://127.0.0.1:8765`, and enable the extension on that tab. Claude Code
loads the `playwriter` entry in `.mcp.json`; Codex CLI can register it with:

```bash
codex mcp add playwriter -- bash "$PWD/scripts/ai/playwriter.sh"
```

The CLI wrapper also supports sessions and `-e` execution. This controls
browser pages; it does not test native Compose or Health Connect.
Only use synthetic PWA records in QA sessions.

```bash
python3 scripts/ai/playwriter-smoke.py
```

The smoke test builds the real PWA and exercises mobile dashboard rendering,
period selection, profile save/reload, synthetic demo records and detail
navigation through Playwriter's direct CDP mode. It launches an isolated
Chromium profile, then cleans up its session, browser and HTTP server.
Set `CHROMIUM_PATH` if Chromium is not on PATH. The extension path requires
manual browser enablement; direct CDP smoke does not verify that path.
In restricted hosts where Chromium cannot create sockets, run this test on
your development machine or through the `AI tools` GitHub Actions workflow.

## vibetest-use

Source: https://github.com/browser-use/vibetest-use (declared MIT upstream).
`tools/vibetest/pyproject.toml` pins commit
`fe0b90ac9d6a129ce06f5b53074241a9f205015b` and browser-use `0.13.14`.
`requirements.lock` pins the resolved transitive dependencies. Browser-use 1.x
removed `BrowserSession`, so floating upstream requirements cannot be used.
Our MCP adapter uses the current `MCPServer` name from the pinned MCP 2.1.1,
maps upstream `aclose()` cleanup to `stop()`, and replaces the retired model
names at all upstream call sites with `VIBETEST_MODEL`. The adapter also marks
reports incomplete when requested agents failed instead of reporting a pass.

```bash
bash scripts/ai/vibetest.sh install
bash scripts/ai/serve-pwa.sh 8765
```

Installation requires uv, Git, Python 3.12 and Chromium's system libraries.
On Linux, if needed, run `tools/vibetest/.venv/bin/python -m playwright install --with-deps chromium`.
Set `GOOGLE_API_KEY` and `VIBETEST_MODEL` in the environment of your MCP client
before launching it. Choose a Google model currently available to your account.
No credentials or model quota are included. AI QA sends page content and
screenshots to that model and may incur charges: use synthetic records only.
Keys are inherited from the process, never written into `.mcp.json`.

Claude Code reads the `vibetest` project MCP entry. For Codex CLI:

```bash
codex mcp add vibetest -- bash "$PWD/scripts/ai/vibetest.sh"
```

Ask the connected agent to call `start` with
`url=http://127.0.0.1:8765`, `num_agents=3`, `headless=true`, then call `results`
with the returned test ID. Keep that MCP server alive: reports live in memory.
Test dashboard periods, metric details, profile save/reload, backup validation
and mobile layout. These are PWA checks, not native Android tests.

A credential-free protocol test is included:

```bash
tools/vibetest/.venv/bin/python scripts/ai/vibetest-smoke.py
```

It exercises real MCP initialization, tool discovery/calls, missing-key
handling, model override and incomplete-run reporting. It does not run model
QA or claim browser/provider connectivity. CI runs this same smoke test.
To update dependencies, run `uv lock --project tools/vibetest`, then
`uv export --project tools/vibetest --locked --no-hashes --no-emit-project --format requirements-txt --output-file tools/vibetest/requirements.lock`,
review the changes and repeat the protocol test. The large intermediate
`uv.lock` and local virtualenv are ignored; commit the exported lock.

## Verification scope

`AI tools` CI installs the pinned Node tools, builds/tests the PWA, runs the
Playwriter direct-CDP smoke, compiles Container Use and checks its configuration
against a running Docker host, and tests the Vibetest MCP adapter without API
requests. It does not create an Android container or compile an APK. Successful
provider calls and the Chrome extension connection need their own authenticated
runtime checks. None of these tools is automatically activated in ChatGPT chat.

## awesome-mcp-servers catalogue

`tools/catalogs/sources.json` pins `punkpeye/awesome-mcp-servers` to a commit
and the SHA-256 of its README. The project search utility downloads that exact
snapshot into ignored `.tools/catalogs` and verifies it before every search.

```bash
python3 scripts/ai/catalogs.py sync mcp
python3 scripts/ai/catalogs.py search mcp android health map --limit 15
python3 scripts/ai/catalogs.py verify mcp
```

Search returns the original entry and a permanent upstream line link. This is
a discovery catalogue: entries do not become installed MCP servers. Select
and review an individual server before adding a runtime integration to
`.mcp.json`. The catalogue is not a Health Connect bridge or a health-data
provider. No upstream README text is redistributed in this repository.

## awesome-ai-plugins catalogue

The second pinned source is `hashgraph-online/awesome-ai-plugins`. It uses the
same checksum-verified local search, so it can be consulted with no plugin
installation and without sending project or health data to a provider.

```bash
python3 scripts/ai/catalogs.py sync plugins
python3 scripts/ai/catalogs.py search plugins testing code --limit 10
python3 scripts/ai/catalogs.py search all android testing --limit 15
python3 scripts/ai/catalogs.py verify all
```

Run `sync all` once before searching both sources. Source commits and README
hashes live in `tools/catalogs/sources.json`. Updating the manifest is an
explicit reviewed change; a cached snapshot is not a live availability check
of every listed service. Only the catalogue search is integrated here.

## Darts: local time-series experiments

Source: https://github.com/unit8co/darts (Apache-2.0). Darts `0.48.1` is
installed only in `tools/darts/.venv`, with a separate dependency lock.
No Python, server, model API or forecasting dependency enters the APK/PWA.
The integration runs a local `NaiveDrift` linear extrapolation and compares
its held-out MAE with a last-observed-value baseline. It requires no account,
API key, model download, Torch or external transfer of the input records.

```bash
bash scripts/ai/darts.sh install
bash scripts/ai/darts.sh /path/to/openvitals-backup.json --metric weight --horizon 7
bash scripts/ai/darts.sh /path/to/measurements.csv --metric restingHeart --horizon 7
```

Inputs are PWA JSON backups with `records` or a CSV with `date,value` columns
(or `date,<metric>`). Supported metric keys are `weight`, `restingHeart`,
`hrv`, `steps`, `sleep`, `bodyFat`, `leanMass`, and `bodyWater`, matching PWA
record names. The CSV `value` column must use the selected metric's unit.
Only the selected metric is read for the forecast; no Health Connect access
or automatic export is introduced.

The CLI prints a JSON report to stdout: observation count, last date, future
dates/values, package version, metric/unit, holdout length, both MAEs and
whether drift beat the baseline. At least 14 consecutive daily observations
are required; horizon is 1–30 days. Missing values are skipped and any resulting
date gap is rejected. Duplicates and non-finite/boolean values are rejected.
No missing-day imputation or silent replacement with zero occurs.
This is an exploratory forecast, with no confidence interval, anomaly detector,
clinical interpretation or automatic change to user records.

```bash
tools/darts/.venv/bin/python tools/darts/smoke.py
```

The smoke test runs real Darts forecasts on synthetic trends, checks held-out
accuracy against the baseline, exercises CSV and PWA imports and the CLI JSON,
and checks rejection of incomplete/duplicate/invalid series and invalid horizons.
CI runs the same checks without real health records.

Update the explicit Darts version in `tools/darts/pyproject.toml`, then run:

```bash
uv lock --project tools/darts
uv export --project tools/darts --locked --no-hashes --no-emit-project --format requirements-txt --output-file tools/darts/requirements.lock
```

Review and commit the exported requirements after installing and testing them.
The intermediate `uv.lock` and virtualenv stay ignored.

## Remaining proposals from the original list

LibSignal concerns traffic simulations; Syncfusion flutter-examples concerns
Flutter widgets, while this project uses Kotlin/Compose and a separate JS PWA.
Neither is integrated. MiMo-Code was an optional curiosity, outside the selected
OpenVitals development tools; it is not installed. The two awesome repositories
are searchable catalogues, not active providers. Darts is a local experiment,
not an implemented forecast screen in the application.
