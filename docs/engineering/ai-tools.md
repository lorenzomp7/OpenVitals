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
