"""Run the built XCTest bundles on the same simulator type used by Detox."""
import json
import os
from pathlib import Path
import subprocess

def run(*args):
    subprocess.run(args, check=True)

devices = json.loads(subprocess.check_output(
    ["xcrun", "simctl", "list", "devices", "available", "--json"], text=True
))["devices"]
candidates = [device for runtime in sorted(devices, reverse=True)
              for device in devices[runtime] if device["name"] == "iPhone 17 Pro"]
requested = os.environ.get("KEYCHAIN_IOS_DEVICE_ID")
if requested:
    selected = next(device for group in devices.values() for device in group if device["udid"] == requested)
else:
    selected = next((device for device in candidates if device["state"] == "Booted"), candidates[0])
identifier = selected["udid"]
if selected["state"] != "Booted":
    run("xcrun", "simctl", "boot", identifier)
run("xcrun", "simctl", "bootstatus", identifier, "-b")
run("applesimutils", "--byId", identifier, "--biometricEnrollment", "YES")

root = Path(__file__).resolve().parents[2]
os.chdir(root / "KeychainExample" / "ios")
run("xcodebuild", "test-without-building", "-workspace", "KeychainExample.xcworkspace",
    "-scheme", "KeychainExample", "-configuration", "Release",
    "-destination", f"platform=iOS Simulator,id={identifier}",
    "-derivedDataPath", "build", "-parallel-testing-enabled", "NO",
    "-only-testing:ReactTestAppTests/RNKeychainIntegrationTests",
    "-only-testing:ReactTestAppUITests/RNKeychainAuthenticationTests",
    "-resultBundlePath", os.environ.get("KEYCHAIN_XCRESULT", "build/native-tests.xcresult"))
