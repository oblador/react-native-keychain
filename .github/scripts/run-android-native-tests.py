"""Run the prebuilt library androidTest APK; adb itself returns 0 on test failures."""
import os
from pathlib import Path
import re
import subprocess
import sys

adb = ["adb"]
if os.environ.get("ANDROID_SERIAL"):
    adb += ["-s", os.environ["ANDROID_SERIAL"]]

subprocess.run(adb + ["install", "-r", sys.argv[1]], check=True, timeout=120)
result = subprocess.run(
    adb + ["shell", "am", "instrument", "-w", "-r",
           "com.oblador.keychain.test/androidx.test.runner.AndroidJUnitRunner"],
    capture_output=True, text=True, timeout=180,
)
output = result.stdout + result.stderr
print(output)
Path("android-native-results.txt").write_text(output)
# Require an actual nonempty passing suite, including a complete instrumentation run.
passed = re.search(r"^OK \([1-9][0-9]* tests?\)$", output, re.MULTILINE)
if result.returncode or not passed or "INSTRUMENTATION_CODE: -1" not in output:
    sys.exit(1)
