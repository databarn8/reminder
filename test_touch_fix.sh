#!/bin/bash

echo "=== TESTING TOUCH FIX FOR SOUND STOP ==="
echo ""

# Check if device is connected
ADB_PATH="/Users/yunhao/Library/Android/sdk/platform-tools/adb"
$ADB_PATH devices | grep -v "List of devices" | grep -v "^$" > /dev/null
if [ $? -ne 0 ]; then
    echo "ERROR: No device connected. Please connect your Android device."
    echo "Current device status:"
    $ADB_PATH devices
    exit 1
fi

echo "Device connected. Starting touch fix test..."
echo ""

# Step 1: Build and install app
echo "1. Building app..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "BUILD FAILED! Please check errors above."
    exit 1
fi

echo "BUILD SUCCESSFUL!"
echo ""

echo "2. Installing app..."
$ADB_PATH install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo "INSTALLATION FAILED! Please check errors above."
    exit 1
fi

echo "INSTALLATION SUCCESSFUL!"
echo ""

# Step 2: Clear logs and start monitoring
echo "3. Clearing logs and starting monitoring..."
$ADB_PATH logcat -c

echo ""
echo "=== INSTRUCTIONS ==="
echo "1. In a SEPARATE terminal, run this command to monitor logs:"
echo "   adb logcat -s AlarmActivity:D ScreenFlashManager:D | grep -E '(TOUCHED|CLICKED|STOP SOUND|SCREEN)'"
echo ""
echo "2. Trigger a test alarm using this command:"
echo "   adb shell am broadcast -a com.reminder.app.TEST_SOUND_STOP"
echo ""
echo "3. When alarm screen appears:"
echo "   - Try touching anywhere on screen"
echo "   - Try pressing STOP SOUND button"
echo "   - Try pressing DISMISS ALARM button"
echo ""
echo "4. Watch for these messages in logs:"
echo "   - 'TRANSPARENT OVERLAY CLICKED' (when touching background)"
echo "   - 'SCREEN CLICKED (CLICKABLE)' (when touching any element)"
echo "   - 'SCREEN TOUCHED (POINTER INTEROP)' (when touch events detected)"
echo "   - 'STOP SOUND CALLED' (when stop function is triggered)"
echo "   - 'Media player STOPPED' and 'Ringtone STOPPED' (when sound actually stops)"
echo ""

# Step 3: Trigger test alarm
echo "4. Triggering test alarm now..."
$ADB_PATH shell am broadcast -a com.reminder.app.TEST_SOUND_STOP

echo ""
echo "=== TEST ALARM TRIGGERED ==="
echo "Please follow instructions above to test touch functionality."
echo ""
echo "=== EXPECTED BEHAVIOR ==="
echo "✓ Touching anywhere on screen should immediately stop the sound"
echo "✓ Multiple touch methods should work (pointerInteropFilter, click modifier, transparent overlay)"
echo "✓ Sound should stop without requiring multiple taps"
echo "✓ Vibration should also stop when sound stops"
echo ""
echo "=== DEBUGGING TIPS ==="
echo "- If touch events aren't detected, check for 'SCREEN TOUCHED' messages"
echo "- If sound doesn't stop, check for 'STOP SOUND CALLED' messages"
echo "- Verify 'Media player STOPPED' and 'Ringtone STOPPED' messages appear"
echo "- Look for any exceptions or errors in logs"