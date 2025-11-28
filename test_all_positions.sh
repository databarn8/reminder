#!/bin/bash

# Script to test all positions on the screen to find the file picker button
echo "Testing all positions on screen to find the file picker button..."

# Test positions from y=800 to y=2000 in increments of 100 (top area of screen)
for y in 800 900 1000 1100 1200 1300 1400 1500 1600 1700 1800 1900 2000
do
    echo "Testing at position (540, $y)"
    ~/Library/Android/sdk/platform-tools/adb shell input tap 540 $y
    sleep 0.5
done

# Test positions from y=2000 to y=4000 in increments of 100 (middle area of screen)
for y in 2000 2100 2200 2300 2400 2500 2600 2700 2800 2900 3000 3100 3200 3300 3400 3500 3600 3700 3800 3900 4000
do
    echo "Testing at position (540, $y)"
    ~/Library/Android/sdk/platform-tools/adb shell input tap 540 $y
    sleep 0.5
done

echo "Testing complete. Check logcat for any activity."