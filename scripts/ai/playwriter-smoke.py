#!/usr/bin/env python3
"""Exercise the real PWA through Playwriter with a disposable Chromium profile."""
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import tempfile
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
CLI = ["bash", str(ROOT / "scripts/ai/playwriter.sh")]


def run(args):
    result = subprocess.run(args, cwd=ROOT, text=True, timeout=120, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stdout + "\n" + result.stderr)
    return result.stdout


def main():
    browser = os.environ.get("CHROMIUM_PATH") or shutil.which("chromium") or shutil.which("google-chrome")
    if not browser:
        raise SystemExit("Set CHROMIUM_PATH to a Chromium executable")
    run(["npm", "--prefix", "web", "run", "build"])
    with tempfile.TemporaryDirectory(prefix="openvitals-playwriter-") as profile:
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            port = sock.getsockname()[1]
        server = subprocess.Popen(["python3", "-m", "http.server", str(port), "--bind", "127.0.0.1", "--directory", str(ROOT / "web/dist")], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        chrome = subprocess.Popen([browser, "--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--remote-debugging-port=0", f"--user-data-dir={profile}", "about:blank"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        session = None
        try:
            deadline = time.monotonic() + 30
            active_port = Path(profile) / "DevToolsActivePort"
            while not active_port.exists():
                if chrome.poll() is not None or time.monotonic() > deadline:
                    raise RuntimeError("Chromium did not start")
                time.sleep(0.1)
            debug_port = active_port.read_text().splitlines()[0]
            with urllib.request.urlopen(f"http://127.0.0.1:{debug_port}/json/version", timeout=10) as response:
                endpoint = json.load(response)["webSocketDebuggerUrl"]
            output = run(CLI + ["session", "new", "--direct", endpoint])
            match = re.search(r"Session (\d+) created", output)
            if not match:
                raise RuntimeError(output)
            session = match[1]
            code = '''
state.page = await context.newPage();
state.page.setDefaultTimeout(5000);
state.errors = [];
state.page.on('pageerror', e => state.errors.push(e.message));
await state.page.setViewportSize({width:390,height:844});
await state.page.goto(URL, {waitUntil:'networkidle'});
await state.page.locator('#range').selectOption('week');
if (await state.page.locator('#range').inputValue() !== 'week') throw Error('Period selection failed');
await state.page.goto(URL + '#settings');
await state.page.locator('[data-route="body-profile"]').click();
await state.page.locator('[name="name"]').fill('QA Demo');
await state.page.locator('[name="birth"]').fill('1990-01-01');
await state.page.locator('#profile-form button.filled').click();
await state.page.reload({waitUntil:'networkidle'});
if (await state.page.locator('[name="name"]').inputValue() !== 'QA Demo') throw Error('Profile did not persist');
await state.page.goto(URL + '#backup');
await state.page.locator('[data-action="sample"]').click();
await state.page.locator('#confirm-action').click();
await state.page.goto(URL + '#dashboard');
if (await state.page.locator('.tile').count() < 1) throw Error('No dashboard tiles');
await state.page.goto(URL + '#metric/STEPS');
await state.page.locator('[data-action="range"][data-value="month"]').click();
if (await state.page.locator('[data-action="range"][data-value="month"]').getAttribute('aria-pressed') !== 'true') throw Error('Detail range failed');
if (state.errors.length) throw Error(JSON.stringify(state.errors));
await state.page.close();
console.log('OPENVITALS_PLAYWRITER_OK: mobile dashboard, periods, profile persistence, synthetic demo, detail navigation');
'''.replace('URL', json.dumps(f"http://127.0.0.1:{port}/"))
            result = run(CLI + ["-s", session, "--timeout", "60000", "-e", code])
            if "OPENVITALS_PLAYWRITER_OK" not in result:
                raise RuntimeError(result)
            print(result)
        finally:
            if session:
                subprocess.run(CLI + ["session", "delete", session], cwd=ROOT, timeout=30, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            for process in (chrome, server):
                process.terminate()
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()


if __name__ == "__main__":
    main()
