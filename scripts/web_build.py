#!/usr/bin/env python3
"""Turns the crawl of the real app (scripts/web_crawl.py) into the installable web copy.

    python3 scripts/web_build.py web-app/out web-app/dist <commit>

Each screen becomes a picture of its content and, when it has one, a picture of its
bottom bar, which stays pinned as in the app. Every recorded tap becomes a link.
"""
import json
import os
import shutil
import sys

from PIL import Image

SOURCE, DIST = sys.argv[1], sys.argv[2]
COMMIT = (sys.argv[3] if len(sys.argv) > 3 else "local")[:12]
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEMPLATE = os.path.join(ROOT, "web-app", "site")


def main():
    shutil.rmtree(DIST, ignore_errors=True)
    os.makedirs(os.path.join(DIST, "screens"))
    graph = json.load(open(os.path.join(SOURCE, "graph.json"), encoding="utf-8"))
    width = graph["width"]
    screens = {}
    for name, screen in graph["screens"].items():
        picture = Image.open(os.path.join(SOURCE, screen["image"])).convert("RGB")
        height = picture.height
        bar_top = screen["barTop"]
        bottom = min(screen["contentBottom"], bar_top or height)
        picture.crop((0, 0, width, bottom)).save(os.path.join(DIST, "screens", f"{name}.webp"), quality=82, method=5)
        entry = {"h": bottom, "bar": None, "links": [], "barLinks": []}
        if bar_top is not None:
            picture.crop((0, bar_top, width, height)).save(
                os.path.join(DIST, "screens", f"{name}-bar.webp"), quality=82, method=5,
            )
            entry["bar"] = height - bar_top
        for link in screen["links"]:
            x1, y1, x2, y2 = link["bounds"]
            if bar_top is not None and y1 >= bar_top:
                entry["barLinks"].append({"b": [x1, y1 - bar_top, x2, y2 - bar_top], "to": link["to"], "l": link["label"]})
            elif y1 < bottom:
                entry["links"].append({"b": [x1, y1, x2, min(y2, bottom)], "to": link["to"], "l": link["label"]})
        screens[name] = entry
    site = {"start": graph["start"], "width": width, "commit": COMMIT, "screens": screens}
    with open(os.path.join(DIST, "app.json"), "w", encoding="utf-8") as out:
        json.dump(site, out, ensure_ascii=False, separators=(",", ":"))

    for file in os.listdir(TEMPLATE):
        shutil.copy(os.path.join(TEMPLATE, file), DIST)
    images = sorted(f"screens/{f}" for f in os.listdir(os.path.join(DIST, "screens")))
    worker = open(os.path.join(DIST, "sw.js"), encoding="utf-8").read()
    worker = worker.replace("__VERSION__", COMMIT).replace("__IMAGES__", json.dumps(images))
    open(os.path.join(DIST, "sw.js"), "w", encoding="utf-8").write(worker)
    icon = Image.open(os.path.join(ROOT, "docs", "images", "readme-logo.png")).convert("RGBA")
    for size in (192, 512):
        icon.resize((size, size), Image.LANCZOS).save(os.path.join(DIST, f"icon-{size}.png"))
    print(f"built {len(screens)} screens into {DIST}")


if __name__ == "__main__":
    main()
