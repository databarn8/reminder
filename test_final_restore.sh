#!/bin/bash

# Script to test the final restore functionality with the enhanced UI
echo "Testing final restore functionality with enhanced UI..."

# Navigate to the Backup & Sync screen if not already there
echo "Ensuring we're on the Backup & Sync screen..."
~/Library/Android/sdk/platform-tools/adb shell input tap 540 2000
sleep 2

# Try to find and click the "Browse Device Storage for Backup Files" button
echo "Looking for the 'Browse Device Storage for Backup Files' button..."

# Test a wider range of positions to find the button
for y in 2800 3000 3200 3400 3600 3800 4000 4200 4400 4600 4800 5000 5200 5400 5600
do
    echo "Testing at position (540, $y)"
    ~/Library/Android/sdk/platform-tools/adb shell input tap 540 $y
    sleep 1
done

echo "If the file picker opened, please select a backup file."
echo "With the new enhanced UI, you should see a prominent card with the import button."
echo "Testing complete. Check logcat for activity and the screen for the enhanced UI."