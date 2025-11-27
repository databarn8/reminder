#!/bin/bash

echo "=== ALARM ACTIVITY TOUCH DEBUG SCRIPT ==="
echo ""
echo "1. First, clear any existing logs:"
adb logcat -c

echo ""
echo "2. Start monitoring AlarmActivity logs (run this in a separate terminal):"
echo "adb logcat -s AlarmActivity:D | grep -E '(TOUCHED|STOP SOUND|SCREEN|POINTER)'"

echo ""
echo "3. Trigger a test alarm (run this command):"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.reminder.app/.MainActivity

echo ""
echo "4. Once the alarm is playing, test the touch functionality:"
echo "   - Try touching the screen in different areas"
echo "   - Try pressing the STOP SOUND button"
echo "   - Try pressing the DISMISS ALARM button"

echo ""
echo "5. If touch events aren't detected, try this ADB command to simulate touch:"
echo "   (Get screen coordinates first with: adb shell getevent -l)"
echo "   Then simulate touch with: adb shell input tap 500 1000"

echo ""
echo "6. To test the stop sound function directly via ADB:"
adb shell am broadcast -a com.reminder.app.TEST_SOUND_STOP

echo ""
echo "7. Check for any errors in the logs:"
echo "adb logcat -s AndroidRuntime:E"

echo ""
echo "=== DEBUGGING TIPS ==="
echo "- Look for 'SCREEN TOUCHED' or 'SCREEN PRESSED' messages"
echo "- Check if 'STOP SOUND CALLED' appears when you touch"
echo "- Verify 'Media player STOPPED' messages appear"
echo "- Look for any exceptions or errors in the logs"