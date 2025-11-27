#!/bin/bash

echo "=== COMPREHENSIVE TOUCH FUNCTIONALITY TEST ==="
echo ""

# Step 1: Clean and build the app
echo "1. Building the app..."
./gradlew clean assembleDebug

if [ $? -ne 0 ]; then
    echo "BUILD FAILED! Please check the errors above."
    exit 1
fi

echo "BUILD SUCCESSFUL!"
echo ""

# Step 2: Install the app
echo "2. Installing the app..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo "INSTALLATION FAILED! Please check the errors above."
    exit 1
fi

echo "INSTALLATION SUCCESSFUL!"
echo ""

# Step 3: Clear logs and start monitoring
echo "3. Clearing logs and starting monitoring..."
adb logcat -c

echo ""
echo "=== INSTRUCTIONS ==="
echo "1. In a SEPARATE terminal, run this command to monitor logs:"
echo "   adb logcat -s AlarmActivity:D | grep -E '(TRANSPARENT|CLICKED|STOP SOUND|TOUCHED)'"
echo ""
echo "2. Trigger a test alarm using this command:"
echo "   adb shell am broadcast -a com.reminder.app.TEST_SOUND_STOP"
echo ""
echo "3. When the alarm screen appears:"
echo "   - Try touching anywhere on the screen"
echo "   - Try pressing the STOP SOUND button"
echo "   - Try pressing the DISMISS ALARM button"
echo ""
echo "4. Watch the logs for these messages:"
echo "   - 'TRANSPARENT OVERLAY CLICKED' (when touching background)"
echo "   - 'STOP SOUND CALLED' (when stop function is triggered)"
echo "   - 'Media player STOPPED' (when sound actually stops)"
echo ""
echo "5. If touch events aren't detected, try simulating touch:"
echo "   adb shell input tap 500 1000"
echo ""

# Step 4: Trigger test alarm
echo "4. Triggering test alarm now..."
adb shell am broadcast -a com.reminder.app.TEST_SOUND_STOP

echo ""
echo "=== TEST ALARM TRIGGERED ==="
echo "Please follow the instructions above to test the touch functionality."
echo ""