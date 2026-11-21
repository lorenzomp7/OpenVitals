#!/usr/bin/env python3
"""Check the pinned developer pack and native discovery paths without Android SDK."""
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
pack = root / "tools/agent-skills"
assert (pack / "LICENSE").is_file(), "Initialize tools/agent-skills first"
for name in ("skills", "references"):
    assert (root / ".agents" / name).resolve() == pack / name, name
    assert (pack / name).is_dir(), name
for manifest in (".claude-plugin/plugin.json", ".codex-plugin/plugin.json"):
    data = json.loads((pack / manifest).read_text())
    assert data["name"] == "agent-skills", manifest
skills = sorted((pack / "skills").glob("*/SKILL.md"))
assert skills, "No discoverable skills"
for skill in skills:
    text = skill.read_text()
    assert text.startswith("---\n"), skill
    frontmatter = text.split("---", 2)[1]
    assert "name:" in frontmatter and "description:" in frontmatter, skill
assert (root / ".agents/references/definition-of-done.md").is_file()
entry = subprocess.check_output(
    ["git", "ls-files", "--stage", "tools/agent-skills"], cwd=root, text=True
).split()
assert entry[0] == "160000", "Pack must be a pinned submodule"
actual = subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=pack, text=True
).strip()
assert entry[1] == actual, "Submodule checkout differs from staged pin"
print(f"OK: {len(skills)} skills; Claude/Codex manifests; references; pin {actual}")
