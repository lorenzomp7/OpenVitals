#!/usr/bin/env python3
"""Check what a release APK declares against the inventory kept in the repository.

The inventory lists every permission the APK requests and every component it exports
(activities, activity aliases, services, receivers, providers), one per line. A release
whose merged manifest asks for a permission or exports a component the inventory does
not list fails, and so does one that drops a listed entry: the inventory is the record of
what the app may declare, and changing it is a reviewed commit, not a side effect of a
dependency update. The signing certificate is checked separately, by apksigner, in
publish-personal-release.

  scripts/check-release-manifest.py --apk app.apk --inventory personal/release-manifest.txt
  scripts/check-release-manifest.py --apk app.apk --previous-apk old.apk --inventory ...
  scripts/check-release-manifest.py --apk app.apk --inventory ... --write

--previous-apk also prints what changed since the published APK (for the run log).
--write rewrites the inventory from the APK; review the diff before committing it.
--manifest-xml / --previous-manifest-xml read a decoded manifest instead of an APK.

The manifest is decoded by apkanalyzer from the Android SDK command-line tools
($ANDROID_HOME/cmdline-tools/latest/bin), the same decoded manifest REA reads with
inspect_android_package.
"""

from __future__ import annotations

import argparse
import os
import pathlib
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
PERMISSION_TAGS = ("uses-permission", "uses-permission-sdk-23")
COMPONENT_TAGS = ("activity", "activity-alias", "service", "receiver", "provider")
HEADER = """\
# What the personal release APK declares: every requested permission and every exported
# component of the merged manifest. scripts/check-release-manifest.py fails a release
# that differs from this list. Regenerate it with --write and review the diff: a new
# line here is a new permission or a new entry point into the app.
"""


def apkanalyzer() -> str:
    found = shutil.which("apkanalyzer")
    if found:
        return found
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        candidate = pathlib.Path(sdk, "cmdline-tools", "latest", "bin", "apkanalyzer")
        if candidate.is_file():
            return str(candidate)
    sys.exit("apkanalyzer not found: install the Android SDK command-line tools or set ANDROID_HOME.")


def decoded_manifest(apk: pathlib.Path) -> str:
    result = subprocess.run(
        [apkanalyzer(), "manifest", "print", str(apk)],
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout


def inventory(manifest_xml: str) -> list[str]:
    root = ET.fromstring(manifest_xml)
    package = root.get("package", "")
    entries: set[str] = set()
    for tag in PERMISSION_TAGS:
        for element in root.iter(tag):
            name = element.get(f"{ANDROID}name")
            if name:
                entries.add(f"permission {name}")
    application = root.find("application")
    if application is not None:
        for tag in COMPONENT_TAGS:
            for element in application.iter(tag):
                if element.get(f"{ANDROID}exported") != "true":
                    continue
                name = element.get(f"{ANDROID}name", "")
                if name.startswith("."):
                    name = package + name
                entries.add(f"exported {tag} {name}")
    return sorted(entries)


def read_inventory(path: pathlib.Path) -> list[str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    return sorted({line.strip() for line in lines if line.strip() and not line.startswith("#")})


def describe(added: list[str], removed: list[str]) -> str:
    return "\n".join([f"+ {line}" for line in added] + [f"- {line}" for line in removed])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--apk", type=pathlib.Path)
    source.add_argument("--manifest-xml", type=pathlib.Path)
    previous = parser.add_mutually_exclusive_group()
    previous.add_argument("--previous-apk", type=pathlib.Path)
    previous.add_argument("--previous-manifest-xml", type=pathlib.Path)
    parser.add_argument("--inventory", type=pathlib.Path, required=True)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()

    manifest = args.manifest_xml.read_text(encoding="utf-8") if args.manifest_xml else decoded_manifest(args.apk)
    current = inventory(manifest)
    permissions = sum(1 for line in current if line.startswith("permission "))
    print(f"{permissions} permissions, {len(current) - permissions} exported components")

    if args.previous_apk or args.previous_manifest_xml:
        old_manifest = (
            args.previous_manifest_xml.read_text(encoding="utf-8")
            if args.previous_manifest_xml
            else decoded_manifest(args.previous_apk)
        )
        old = inventory(old_manifest)
        changes = describe(sorted(set(current) - set(old)), sorted(set(old) - set(current)))
        print("Since the published APK:\n" + (changes or "  no change"))

    if args.write:
        args.inventory.write_text(HEADER + "\n".join(current) + "\n", encoding="utf-8")
        print(f"Wrote {args.inventory}")
        return 0

    expected = read_inventory(args.inventory)
    added = sorted(set(current) - set(expected))
    removed = sorted(set(expected) - set(current))
    if added or removed:
        print(
            f"The APK does not match {args.inventory} (+ only in the APK, - only in the inventory):\n"
            + describe(added, removed)
            + "\nIf the change is intended, regenerate the inventory with --write and commit it with the reason.",
            file=sys.stderr,
        )
        return 1
    print(f"Matches {args.inventory}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
