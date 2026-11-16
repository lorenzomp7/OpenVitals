#!/usr/bin/env python3
"""Walk the real app on a connected emulator and record it for the web copy.

Starting from the dashboard, every tappable element of every screen is tapped
once: a tap that lands on a new screen is photographed, with its own tappable
elements, and the link is recorded. Back returns; when Back does not, the app is
restarted and the taps replayed. The result is out/graph.json and one PNG per
screen, which scripts/web_build.py turns into the PWA.

Run by scripts/web-capture.sh, which installs the APK and the demo first.
"""
import hashlib
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = os.environ.get("OPENVITALS_PACKAGE", "tech.mmarca.openvitals.ci")
ACTIVITY = f"{PKG}/tech.mmarca.openvitals.MainActivity"
OUT = sys.argv[1] if len(sys.argv) > 1 else "web-app/out"
MAX_DEPTH = int(os.environ.get("WEB_MAX_DEPTH", "3"))
MAX_SCREENS = int(os.environ.get("WEB_MAX_SCREENS", "160"))
MAX_TAPS_PER_SCREEN = int(os.environ.get("WEB_MAX_TAPS", "40"))
BUDGET_SECONDS = int(os.environ.get("WEB_BUDGET_SECONDS", str(40 * 60)))

# Taps that would change the demo, start a recording or leave the app for good.
SKIP = re.compile(
    r"elimin|cancell|delet|rimuov|remov|reset|azzer|ripristin|restor|backup|esport|export|"
    r"condivid|share|avvia|inizia|start|registra|record|disconnett|disconnect|esci|sign out|"
    r"logout|sincronizza ora|sync now|segnala|report|invia|send|privacy|github|licenz|licens|"
    r"don[ai]|donat|play store|termina|stop|salva|save|blocc|lock|cifra|encrypt",
    re.IGNORECASE,
)
BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")
started = time.monotonic()


def adb(*args, binary=False, check=True):
    result = subprocess.run(["adb", *args], capture_output=True, timeout=60)
    if check and result.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)}: {result.stderr.decode(errors='replace')}")
    return result.stdout if binary else result.stdout.decode(errors="replace")


def screen_size():
    out = adb("shell", "wm", "size")
    sizes = re.findall(r"(\d+)x(\d+)", out)
    w, h = sizes[-1]
    return int(w), int(h)


def dump():
    for _ in range(4):
        out = adb("shell", "uiautomator", "dump", "/sdcard/ov-window.xml", check=False)
        if "dumped" in out:
            xml = adb("exec-out", "cat", "/sdcard/ov-window.xml")
            try:
                return ET.fromstring(xml)
            except ET.ParseError:
                pass
        time.sleep(1)
    raise RuntimeError("uiautomator dump kept failing")


def nodes(root):
    found = []
    for node in root.iter("node"):
        match = BOUNDS.match(node.get("bounds", ""))
        if not match:
            continue
        x1, y1, x2, y2 = map(int, match.groups())
        found.append((node, (x1, y1, x2, y2)))
    return found


def label(node):
    parts = [node.get("text", ""), node.get("content-desc", "")]
    parts += [child.get("text", "") or child.get("content-desc", "") for child in node.iter("node")]
    return " ".join(p.strip() for p in parts if p and p.strip())[:120]


def signature(root):
    """What a screen is, numbers aside: the words on it, in the app's package."""
    words = set()
    for node, _ in nodes(root):
        if node.get("package") != PKG:
            continue
        for key in ("text", "content-desc"):
            value = re.sub(r"[\d.,:%/+\-–]+", "#", node.get(key, "")).strip()
            if value:
                words.add(value)
    return hashlib.sha1("\n".join(sorted(words)).encode()).hexdigest()[:12]


def foreground_package(root):
    """The app's package when any of its nodes is on screen. The first node can be the status
    bar or a system overlay, which made a cold start read as "the app did not open"."""
    packages = [node.get("package") for node in root.iter("node") if node.get("package")]
    if PKG in packages:
        return PKG
    return packages[0] if packages else ""


def settle():
    """Waits until two dumps a moment apart agree: the screen has loaded."""
    time.sleep(1.2)
    last = None
    for _ in range(6):
        root = dump()
        sig = signature(root)
        if sig == last:
            return root
        last = sig
        time.sleep(0.8)
    return root


def tappables(root, width, height):
    seen, taps = set(), []
    for node, (x1, y1, x2, y2) in nodes(root):
        if node.get("package") != PKG or node.get("clickable") != "true":
            continue
        if node.get("checkable") == "true" or "EditText" in node.get("class", ""):
            continue
        if x2 - x1 < 8 or y2 - y1 < 8 or x1 >= width or y1 >= height:
            continue
        text = label(node)
        if SKIP.search(text):
            continue
        key = (text, x1 // 20, y1 // 20)
        if key in seen:
            continue
        seen.add(key)
        taps.append({"label": text, "bounds": [x1, y1, x2, y2]})
    return taps[:MAX_TAPS_PER_SCREEN]


def layout(root, height):
    """Where the content ends and the bottom bar starts, to trim the tall capture."""
    boxes = [b for node, b in nodes(root) if node.get("package") == PKG]
    bar = [b[1] for b in boxes if b[1] >= height - 320 and b[3] - b[1] < 320]
    bar_top = min(bar) if bar else None
    limit = bar_top if bar_top is not None else height
    content = [b[3] for b in boxes if b[3] <= limit and (b[3] - b[1]) < 0.9 * height]
    bottom = min(limit, (max(content) if content else limit) + 48)
    return bottom, bar_top


def tap(bounds):
    x1, y1, x2, y2 = bounds
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def back():
    adb("shell", "input", "keyevent", "4")


def restart():
    # A cold start on a freshly resized screen can take longer than one wait; try a few times.
    root = None
    for attempt in range(4):
        adb("shell", "am", "force-stop", PKG)
        adb("shell", "input", "keyevent", "224")  # wake
        adb("shell", "wm", "dismiss-keyguard", check=False)
        adb("shell", "am", "start", "-W", "-n", ACTIVITY)
        time.sleep(3 + attempt * 3)
        root = settle()
        if foreground_package(root) == PKG:
            return root
        print(f"the app is not in front yet (attempt {attempt + 1})", flush=True)
    return root


def replay(path):
    root = restart()
    for bounds in path:
        tap(bounds)
        root = settle()
    return root


def main():
    os.makedirs(os.path.join(OUT, "screens"), exist_ok=True)
    width, height = screen_size()
    screens, order = {}, []

    def record(root, path, depth, parent=None):
        sig = signature(root)
        if sig in screens:
            return sig
        name = f"s{len(order):03d}"
        with open(os.path.join(OUT, "screens", f"{name}.png"), "wb") as png:
            png.write(adb("exec-out", "screencap", "-p", binary=True))
        bottom, bar_top = layout(root, height)
        screens[sig] = {
            "id": name, "path": path, "depth": depth, "links": [], "parent": parent,
            "taps": tappables(root, width, height), "contentBottom": bottom, "barTop": bar_top,
        }
        order.append(sig)
        print(f"[{len(order)}] {name} depth {depth}", flush=True)
        return sig

    root = restart()
    if foreground_package(root) != PKG:
        raise RuntimeError("the app did not open")
    start = record(root, [], 0)

    index = 0
    while index < len(order):
        sig = order[index]
        index += 1
        screen = screens[sig]
        if screen["depth"] >= MAX_DEPTH:
            continue
        root = replay(screen["path"])
        if signature(root) != sig:
            print(f"cannot return to {screen['id']}, skipped", flush=True)
            continue
        for target in screen["taps"]:
            if time.monotonic() - started > BUDGET_SECONDS or len(order) >= MAX_SCREENS:
                break
            tap(target["bounds"])
            after = settle()
            if foreground_package(after) != PKG:
                back()
                after = settle()
            elif signature(after) != sig:
                after_sig = signature(after)
                # 86: the app's own back arrow lands on the screen this one was opened from:
                # in the web copy it goes back, as the arrow does, instead of opening it anew.
                if after_sig == screen.get("parent"):
                    screen["links"].append({"bounds": target["bounds"], "to": "back", "label": target["label"]})
                else:
                    landed = record(after, screen["path"] + [target["bounds"]], screen["depth"] + 1, parent=sig)
                    screen["links"].append({"bounds": target["bounds"], "to": screens[landed]["id"], "label": target["label"]})
                    back()
                    after = settle()
            if signature(after) != sig:
                after = replay(screen["path"])
                if signature(after) != sig:
                    print(f"lost {screen['id']} after a tap, moving on", flush=True)
                    break
        if time.monotonic() - started > BUDGET_SECONDS:
            print("time budget spent", flush=True)
            break

    graph = {
        "start": screens[start]["id"],
        "width": width,
        "height": height,
        "screens": {
            s["id"]: {
                "image": f"screens/{s['id']}.png",
                "links": s["links"],
                "contentBottom": s["contentBottom"],
                "barTop": s["barTop"],
            }
            for s in screens.values()
        },
    }
    with open(os.path.join(OUT, "graph.json"), "w", encoding="utf-8") as out:
        json.dump(graph, out, ensure_ascii=False, indent=1)
    print(f"recorded {len(order)} screens", flush=True)


if __name__ == "__main__":
    main()
