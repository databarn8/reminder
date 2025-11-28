#!/bin/bash

# Script to test the file picker and restore functionality
echo "Testing file picker and restore functionality..."

# First, let's navigate to the Backup & Sync screen
echo "Navigating to Backup & Sync screen..."
~/Library/Android/sdk/platform-tools/adb shell input tap 540 2000
sleep 2

# Now let's try to find and click the "Browse Device Storage for Backup Files" button
# This button should be in the Email Backup section
echo "Looking for the 'Browse Device Storage for Backup Files' button..."

# Try a few positions where the button might be located
for y in 3000 3200 3400 3600 3800 4000 4200 4400 4600 4800
do
    echo "Testing at position (540, $y)"
    ~/Library/Android/sdk/platform-tools/adb shell input tap 540 $y
    sleep 1
done

echo "If the file picker opened, please select a backup file and check if the import button appears."
echo "Testing complete. Check logcat for activity."