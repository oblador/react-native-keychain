#!/usr/bin/env bash

DELAY=3
adb shell locksettings set-pin 1111
adb shell am start -a android.settings.SECURITY_SETTINGS
sleep $DELAY
adb shell input tap 274 1150
sleep $DELAY
adb shell input text 1111
adb shell input keyevent 66
sleep $DELAY
adb shell input tap 900 2200
sleep $DELAY
adb shell input tap 900 2200
sleep $DELAY
adb -e emu finger touch 1
sleep $DELAY
adb -e emu finger touch 1
sleep $DELAY
adb -e emu finger touch 1
sleep $DELAY
adb shell input keyevent KEYCODE_HOME