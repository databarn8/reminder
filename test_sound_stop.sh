#!/bin/bash

# Comprehensive test script for alarm sound stop functionality
# This script tests various scenarios to ensure sounds stop properly

echo "=== ALARM SOUND STOP TEST SCRIPT ==="
echo "Starting comprehensive sound stop tests..."
echo ""

# Function to wait for user input
wait_for_user() {
    echo ""
    read -p "Press Enter to continue to next test..."
    echo ""
}

# Function to run ADB command and check result
run_adb_command() {
    local command=$1
    local description=$2
    
    echo "=== $description ==="
    echo "Running: $command"
    adb shell $command
    echo ""
    sleep 2
}

# Function to test sound stop
test_sound_stop() {
    local test_name=$1
    local action=$2
    
    echo "=========================================="
    echo "TEST: $test_name"
    echo "=========================================="
    
    # Clear logcat first
    echo "Clearing logcat..."
    adb logcat -c
    
    # Trigger alarm
    echo "Triggering test alarm..."
    run_adb_command "am broadcast -a com.reminder.app.TEST_SOUND_STOP" "Trigger test alarm"
    
    # Wait for alarm to start
    echo "Waiting 5 seconds for alarm to start..."
    sleep 5
    
    # Perform action
    echo "Performing action: $action"
    run_adb_command "$action" "Perform test action"
    
    # Wait and check logs
    echo "Waiting 3 seconds and checking logs..."
    sleep 3
    
    # Check for sound stop in logs
    echo "Checking for sound stop confirmation in logs..."
    adb logcat -d | grep -E "(STOP SOUND|SOUND STOP|Media player STOPPED|Ringtone STOPPED)" | tail -10
    
    wait_for_user
}

# Check if device is connected
echo "Checking device connection..."
adb devices | grep -v "List of devices" | grep -v "^$" > /dev/null
if [ $? -ne 0 ]; then
    echo "ERROR: No device connected. Please connect your Android device."
    exit 1
fi

echo "Device connected. Starting tests..."
wait_for_user

# Test 1: Stop Sound Button
test_sound_stop "Stop Sound Button" "input tap 540 1200"

# Test 2: Dismiss Alarm
test_sound_stop "Dismiss Alarm" "input tap 540 1000"

# Test 3: Touch Screen to Stop Sound
test_sound_stop "Touch Screen to Stop Sound" "input tap 300 800"

# Test 4: Back Button
test_sound_stop "Back Button" "input keyevent KEYCODE_BACK"

# Test 5: Multiple rapid taps
test_sound_stop "Multiple Rapid Taps" "input tap 540 1200 && sleep 0.5 && input tap 540 1200 && sleep 0.5 && input tap 540 1200"

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
    echo "Checking logs for $level level alarm..."
    adb logcat -d | grep -E "(STOP SOUND|SOUND STOP)" | tail -5
    
    wait_for_user
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
adb logcat -d | grep -E "(STOP SOUND|SOUND STOP|Media player|Ringtone)" | tail -10

wait_for_user

# Test 8: Test with meeting mode on/off
echo "=========================================="
echo "TEST: Meeting Mode Impact"
echo "=========================================="

# Test with meeting mode off
echo "Testing with meeting mode OFF..."
adb shell "am broadcast -a com.reminder.app.TEST_SOUND_STOP"
sleep 5
adb shell "input tap 540 1200"
sleep 3
echo "Meeting mode OFF test completed"

wait_for_user

# Final comprehensive log analysis
echo "=========================================="
echo "FINAL COMPREHENSIVE LOG ANALYSIS"
echo "=========================================="

echo "Analyzing all sound-related logs from tests..."
adb logcat -d | grep -E "(AlarmActivity|ScreenFlashManager|Media player|Ringtone|STOP SOUND|SOUND STOP)" | tail -50

echo ""
echo "=== TEST SCRIPT COMPLETED ==="
echo "Please review the output above to verify sound stop functionality"
echo "Key things to look for:"
echo "1. 'STOP SOUND CALLED' messages appear when actions are performed"
echo "2. 'Media player STOPPED' and 'Ringtone STOPPED' messages appear"
echo "3. No error messages related to sound stopping"
echo "4. Sound actually stops on the device"