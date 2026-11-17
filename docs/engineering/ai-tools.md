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
