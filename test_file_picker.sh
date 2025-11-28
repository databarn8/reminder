#!/bin/bash

# Script to test the file picker functionality by simulating touches at different positions
echo "Testing file picker button at various positions..."

# Center of screen (x=540)
CENTER_X=540

# Test positions from y=4500 to y=7300 in increments of 100
for y in 4500 4600 4700 4800 4900 5000 5100 5200 5300 5400 5500 5600 5700 5800 5900 6000 6100 6200 6300 6400 6500 6600 6700 6800 6900 7000 7100 7200 7300
do
    echo "Testing at position ($CENTER_X, $y)"
    ~/Library/Android/sdk/platform-tools/adb shell input tap $CENTER_X $y
    sleep 1
done

echo "Testing complete. Check logcat for any activity."