"""Enroll fingerprint 1 on a test emulator with PIN 1111 before running Detox."""

import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET


# ANDROID_SERIAL allows local validation alongside other running emulators.
ADB = ["adb"] if os.environ.get("ANDROID_SERIAL") else ["adb", "-e"]
PIN = "1111"


def adb(*args, check=True):
    return subprocess.run(
        [*ADB, *args], capture_output=True, text=True, check=check, timeout=15
    )


def enrolled(user):
    for line in adb("shell", "dumpsys", "fingerprint").stdout.splitlines():
        if line.startswith("{"):
            dump = json.loads(line)
            if any(
                entry.get("id") == user and entry.get("count", 0) > 0
                for entry in dump.get("prints", [])
            ):
                return True
    return False


def tap(node):
    left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def enroll():
    adb("wait-for-device")
    user = int(adb("shell", "am", "get-current-user").stdout.strip())
    if adb("shell", "locksettings", "set-pin", PIN, check=False).returncode:
        adb("shell", "locksettings", "set-pin", "--old", PIN, PIN)

    # Android 36 can boot with a Quickstep ANR covering the enrollment activity.
    adb("shell", "am", "force-stop", "com.android.launcher3")
    adb("shell", "am", "start", "-a", "android.settings.BIOMETRIC_ENROLL")
    deadline = time.monotonic() + 120
    pin_submitted = False
    last_step = None
    while time.monotonic() < deadline:
        if enrolled(user):
            adb("shell", "input", "keyevent", "KEYCODE_HOME")
            print("Fingerprint enrollment verified.", flush=True)
            return

        window_path = "/data/local/tmp/keychain-enrollment.xml"
        result = adb("shell", "uiautomator", "dump", window_path, check=False)
        if "dumped to" not in result.stdout:
            time.sleep(1)
            continue
        dump = adb("exec-out", "cat", window_path).stdout
        start, end = dump.find("<?xml"), dump.rfind("</hierarchy>")
        if start == -1 or end == -1:
            time.sleep(1)
            continue
        hierarchy = ET.fromstring(dump[start : end + len("</hierarchy>")])
        nodes = list(hierarchy.iter("node"))
        close = next(
            (
                node for node in nodes
                if node.get("resource-id") == "android:id/aerr_close"
            ),
            None,
        )
        pin = next(
            (node for node in nodes if node.get("password") == "true"), None
        )
        button = next(
            (
                node
                for node in nodes
                if node.get("enabled") == "true"
                and node.get("text", "").strip().lower()
                in {"i agree", "next", "start", "done", "continue", "more"}
            ),
            None,
        )
        if close is not None:
            step = "Dismissing system ANR dialog"
            tap(close)
            adb("shell", "am", "start", "-a", "android.settings.BIOMETRIC_ENROLL")
        elif pin is not None:
            step = "Confirming test PIN"
            if not pin_submitted:
                adb("shell", "input", "text", PIN)
                adb("shell", "input", "keyevent", "KEYCODE_ENTER")
                pin_submitted = True
        elif button is not None:
            step = "Advancing fingerprint enrollment"
            pin_submitted = False
            tap(button)
        else:
            step = "Sending fingerprint 1 to the enrollment sensor"
            pin_submitted = False
            adb("emu", "finger", "touch", "1")
        if step != last_step:
            print(step, flush=True)
            last_step = step
        time.sleep(1)
    raise RuntimeError("Fingerprint enrollment was not confirmed within 120 seconds")


if __name__ == "__main__":
    try:
        enroll()
    except (RuntimeError, subprocess.SubprocessError, ET.ParseError) as error:
        print(f"Android test setup failed: {error}", flush=True)
        with Path("android-enrollment-failure.png").open("wb") as screenshot:
            subprocess.run(
                [*ADB, "exec-out", "screencap", "-p"], stdout=screenshot, timeout=15
            )
        raise SystemExit(1) from error
