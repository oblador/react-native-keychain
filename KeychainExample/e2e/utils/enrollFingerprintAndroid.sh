#!/usr/bin/env bash

DELAY=10
adb shell locksettings set-pin 1111
adb shell am start -a android.settings.SECURITY_SETTINGS
sleep $DELAY
adb shell input tap 274 2300
sleep $DELAY
adb shell input text 1111
adb shell input keyevent 66
sleep $DELAY
adb shell input tap 1100 2700
sleep $DELAY
adb shell input tap 1100 2700
sleep $DELAY
adb -e emu finger touch 1
sleep $DELAY
adb -e emu finger touch 1
sleep $DELAY
adb -e emu finger touch 1
sleep $DELAY
adb shell input keyevent 4
sleep $DELAY
adb shell input keyevent 4
sleep $DELAY
adb shell input keyevent 4
sleep $DELAY
adb shell input keyevent 4
sleep $DELAY
adb shell input keyevent 4
sleep $DELAY
adb shell input keyevent 4