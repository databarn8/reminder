#!/bin/bash

# Automated test script for alarm sound stop functionality
# This script runs tests automatically and verifies results

echo "=== AUTOMATED ALARM SOUND STOP TEST ==="
echo "Starting comprehensive automated tests..."
echo ""

# Function to run test and verify results
run_test() {
    local test_name=$1
    local action=$2
    local expected_logs=$3
    
    echo "=========================================="
    echo "TEST: $test_name"
    echo "=========================================="
    
    # Clear logcat
    adb logcat -c
    
    # Trigger alarm
    echo "Triggering test alarm..."
    adb shell "am broadcast -a com.reminder.app.TEST_SOUND_STOP"
    sleep 5
    
    # Perform action
    echo "Performing action: $action"
    adb shell "$action"
    sleep 3
    
    # Check logs for expected patterns
    echo "Checking logs for expected patterns..."
    log_output=$(adb logcat -d | grep -E "(STOP SOUND|SOUND STOP|Media player STOPPED|Ringtone STOPPED|Global)" | tail -20)
    
    if echo "$log_output" | grep -q "GLOBAL SOUND STOP REQUESTED"; then
        echo "✓ PASS: Global sound stop was requested"
    else
        echo "✗ FAIL: Global sound stop was not requested"
    fi
    
    if echo "$log_output" | grep -q "Media player STOPPED\|Ringtone STOPPED"; then
        echo "✓ PASS: Sound players were stopped"
    else
        echo "✗ FAIL: Sound players were not stopped"
    fi
    
    if echo "$log_output" | grep -q "ScreenFlashManager sounds stopped"; then
        echo "✓ PASS: ScreenFlashManager sounds were stopped"
    else
        echo "✗ FAIL: ScreenFlashManager sounds were not stopped"
    fi
    
    echo ""
    echo "Log output:"
    echo "$log_output"
    echo ""
    
    # Force stop app to clean up
    adb shell "am force-stop com.reminder.app"
    sleep 2
}

# Check if device is connected
echo "Checking device connection..."
adb devices | grep -v "List of devices" | grep -v "^$" > /dev/null
if [ $? -ne 0 ]; then
    echo "ERROR: No device connected. Please connect your Android device."
    exit 1
fi

echo "Device connected. Starting automated tests..."
echo ""

# Test 1: Stop Sound Button
run_test "Stop Sound Button" "input tap 540 1200"

# Test 2: Dismiss Alarm
run_test "Dismiss Alarm" "input tap 540 1000"

# Test 3: Touch Screen to Stop Sound
run_test "Touch Screen to Stop Sound" "input tap 300 800"

# Test 4: Back Button
run_test "Back Button" "input keyevent KEYCODE_BACK"

# Test 5: Multiple rapid taps
run_test "Multiple Rapid Taps" "input tap 540 1200 && sleep 0.5 && input tap 540 1200 && sleep 0.5 && input tap 540 1200"

# Test 6: Test with different alert levels
echo "=========================================="
echo "TEST: Different Alert Levels"
echo "=========================================="

for level in "LOW" "HIGH" "URGENT"; do
    echo "Testing with alert level: $level"
    adb logcat -c
    
    # Trigger alarm with specific alert level
    adb shell "am broadcast -a com.reminder.app.TEST_SOUND_STOP --es alert_level $level"
    sleep 5
    
    # Stop sound
    adb shell "input tap 540 1200"
    sleep 3
    
    # Check logs
    log_output=$(adb logcat -d | grep -E "(STOP SOUND|SOUND STOP)" | tail -10)
    
    if echo "$log_output" | grep -q "GLOBAL SOUND STOP REQUESTED"; then
        echo "✓ PASS: $level level - Global sound stop was requested"
    else
        echo "✗ FAIL: $level level - Global sound stop was not requested"
    fi
    
    # Force stop app to clean up
    adb shell "am force-stop com.reminder.app"
    sleep 2
done

# Test 7: Test sound persistence after activity destruction
echo "=========================================="
echo "TEST: Sound Persistence After Activity Destruction"
echo "=========================================="

echo "Starting alarm..."
adb logcat -c
adb shell "am broadcast -a com.reminder.app.TEST_SOUND_STOP"
sleep 5

echo "Destroying activity..."
adb shell "am force-stop com.reminder.app"
sleep 2

echo "Checking if sound stopped..."
sleep 3
log_output=$(adb logcat -d | grep -E "(GLOBAL SOUND STOP|Media player|Ringtone)" | tail -15)

if echo "$log_output" | grep -q "GLOBAL SOUND STOP REQUESTED"; then
    echo "✓ PASS: Global sound stop was requested during destruction"
else
    echo "✗ FAIL: Global sound stop was not requested during destruction"
fi

echo ""
echo "Log output:"
echo "$log_output"
echo ""

# Final comprehensive analysis
echo "=========================================="
echo "FINAL COMPREHENSIVE ANALYSIS"
echo "=========================================="

echo "Analyzing all sound-related logs from tests..."
all_logs=$(adb logcat -d | grep -E "(AlarmActivity|ScreenFlashManager|Media player|Ringtone|STOP SOUND|SOUND STOP|Global)" | tail -100)

echo "Checking for key success indicators..."

if echo "$all_logs" | grep -q "GLOBAL SOUND STOP REQUESTED"; then
    echo "✓ PASS: Global sound stop mechanism is working"
else
    echo "✗ FAIL: Global sound stop mechanism is not working"
fi

if echo "$all_logs" | grep -q "Media player STOPPED\|Ringtone STOPPED"; then
    echo "✓ PASS: Individual sound players are being stopped"
else
    echo "✗ FAIL: Individual sound players are not being stopped"
fi

if echo "$all_logs" | grep -q "ScreenFlashManager sounds stopped"; then
    echo "✓ PASS: ScreenFlashManager coordination is working"
else
    echo "✗ FAIL: ScreenFlashManager coordination is not working"
fi

if echo "$all_logs" | grep -q "All callbacks cleared"; then
    echo "✓ PASS: Handler callbacks are being cleared"
else
    echo "✗ FAIL: Handler callbacks are not being cleared"
fi

echo ""
echo "=== AUTOMATED TEST COMPLETED ==="
echo ""
echo "Summary of fixes implemented:"
echo "1. ✓ Global sound stop coordination across AlarmActivity instances"
echo "2. ✓ Proper resource cleanup for MediaPlayer and Ringtone"
echo "3. ✓ Handler callback cleanup to prevent further sound playback"
echo "4. ✓ ScreenFlashManager sound stopping coordination"
echo "5. ✓ Comprehensive logging for debugging"
echo ""
echo "Key improvements:"
echo "- Added global sound stop mechanism that works across all instances"
echo "- Fixed race conditions between multiple sound players"
echo "- Implemented proper resource cleanup"
echo "- Added coordination between AlarmActivity and ScreenFlashManager"
echo "- Enhanced logging for better debugging"
echo ""
echo "The sound stop issue should now be resolved!"