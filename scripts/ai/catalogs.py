#!/usr/bin/env python3
"""Search pinned upstream catalogues; never installs the listed tools."""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
CACHE = ROOT / ".tools/catalogs"


def sources():
    return json.loads((ROOT / "tools/catalogs/sources.json").read_text())


def verified(source, path):
    data = path.read_bytes()
    if hashlib.sha256(data).hexdigest() != source["sha256"]:
        raise ValueError(f"Checksum mismatch for {path.name}; sync again")
    return data.decode("utf-8")


def sync(name, source):
    url = f"https://raw.githubusercontent.com/{source['repository']}/{source['commit']}/{source['path']}"
    request = urllib.request.Request(url, headers={"User-Agent": "OpenVitals-catalog-reader"})
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != source["sha256"]:
        raise ValueError(f"Upstream checksum mismatch for {name}; no cache replaced")
    CACHE.mkdir(parents=True, exist_ok=True)
    temporary = CACHE / f"{name}.tmp"
    temporary.write_bytes(data)
    temporary.replace(CACHE / f"{name}.md")
    print(f"Synced {name}: {source['repository']} @ {source['commit']}")


def main():
    catalogues = sources()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["sync", "search", "verify"])
    parser.add_argument("catalogue", choices=["all", *catalogues])
    parser.add_argument("terms", nargs="*")
    parser.add_argument("--limit", type=int, default=20)
    args = parser.parse_args()
    selected = catalogues if args.catalogue == "all" else {args.catalogue: catalogues[args.catalogue]}
    if args.action == "search" and not args.terms:
        parser.error("search requires one or more literal search terms")
    if args.limit < 1:
        parser.error("--limit must be positive")
    matches = []
    for name, source in selected.items():
        if args.action == "sync":
            sync(name, source)
            continue
        path = CACHE / f"{name}.md"
        if not path.exists():
            parser.error(f"Missing cache for {name}; run sync {name} first")
        content = verified(source, path)
        if args.action == "verify":
            print(f"OK: {name} checksum and UTF-8 content")
            continue
        terms = [term.casefold() for term in args.terms]
        for number, line in enumerate(content.splitlines(), 1):
            if any(term in line.casefold() for term in terms):
                matches.append({"catalogue": name, "line": number, "entry": line.strip(),
                                "source": f"https://github.com/{source['repository']}/blob/{source['commit']}/{source['path']}#L{number}"})
    if args.action == "search":
        print(json.dumps(matches[:args.limit], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
