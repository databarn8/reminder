#!/bin/bash

# Auto Test Script for File Operations in Reminder App
# This script tests file loading, editing, saving, and voice input functionality

echo "🧪 Starting File Operations Test Script..."
echo "========================================"

# Configuration
APP_PACKAGE="com.reminder.app"
ADB_PATH="$HOME/Library/Android/sdk/platform-tools/adb"
DEVICE_ID=$($ADB_PATH devices | grep -v "List of devices" | head -n 1 | cut -f1)
TEST_DIR="/tmp/reminder_file_test"
TEST_FILE_CONTENT="This is a test file for the reminder app.
It contains multiple lines of text.
Line 1: Basic test content
Line 2: Testing file operations
Line 3: Voice input integration
Line 4: Save and discard functionality
Final line: End of test file"

# Create test directory
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

# Function to check if device is connected
check_device() {
    if [ -z "$DEVICE_ID" ]; then
        echo "❌ No device connected. Please connect your Android device."
        exit 1
    fi
    echo "📱 Using device: $DEVICE_ID"
}

# Function to wait for app to start
wait_for_app() {
    echo "⏳ Waiting for app to start..."
    sleep 3
}

# Function to take screenshot
take_screenshot() {
    local screenshot_name="$1"
    echo "📸 Taking screenshot: $screenshot_name"
    $ADB_PATH -s "$DEVICE_ID" shell screencap -p /sdcard/"$screenshot_name".png
    sleep 1
}

# Function to tap on screen coordinates
tap_screen() {
    local x="$1"
    local y="$2"
    echo "👆 Tapping at coordinates: ($x, $y)"
    $ADB_PATH -s "$DEVICE_ID" shell input tap "$x" "$y"
    sleep 1
}

# Function to input text
input_text() {
    local text="$1"
    echo "⌨️  Inputting text: $text"
    $ADB_PATH -s "$DEVICE_ID" shell input text "$text"
    sleep 1
}

# Function to press back button
press_back() {
    echo "⬅️  Pressing back button"
    $ADB_PATH -s "$DEVICE_ID" shell input keyevent KEYCODE_BACK
    sleep 1
}

# Function to press enter key
press_enter() {
    echo "↩️  Pressing enter key"
    $ADB_PATH -s "$DEVICE_ID" shell input keyevent KEYCODE_ENTER
    sleep 1
}

# Function to clear app data
clear_app_data() {
    echo "🗑️  Clearing app data..."
    $ADB_PATH -s "$DEVICE_ID" shell pm clear "$APP_PACKAGE"
    sleep 2
}

# Function to start app
start_app() {
    echo "🚀 Starting reminder app..."
    $ADB_PATH -s "$DEVICE_ID" shell am start -n "$APP_PACKAGE"/.MainActivity
    wait_for_app
}

# Function to force stop app
stop_app() {
    echo "🛑 Stopping reminder app..."
    $ADB_PATH -s "$DEVICE_ID" shell am force-stop "$APP_PACKAGE"
    sleep 2
}

# Function to create test file
create_test_file() {
    local filename="$1"
    echo "📄 Creating test file: $filename"
    echo "$TEST_FILE_CONTENT" > "$filename"
}

# Function to push file to device
push_file_to_device() {
    local local_file="$1"
    local remote_file="$2"
    echo "📤 Pushing file to device: $local_file -> $remote_file"
    $ADB_PATH -s "$DEVICE_ID" push "$local_file" "$remote_file"
    sleep 1
}

# Function to test basic app functionality
test_basic_functionality() {
    echo ""
    echo "🧪 Testing Basic App Functionality..."
    echo "----------------------------------------"
    
    # Start app
    start_app
    
    # Take initial screenshot
    take_screenshot "01_app_started"
    
    # Test voice input
    echo "🎤 Testing voice input..."
    tap_screen 540 200  # Approximate microphone button position
    sleep 2
    
    # Take screenshot after voice button tap
    take_screenshot "02_voice_button_tapped"
    
    # Test text input
    echo "⌨️ Testing text input..."
    tap_screen 540 400  # Approximate text field position
    input_text "Test reminder from file operations script"
    sleep 2
    
    # Take screenshot after text input
    take_screenshot "03_text_input"
    
    # Press back to return to main screen
    press_back
    sleep 2
    take_screenshot "04_back_to_main"
}

# Function to test file menu
test_file_menu() {
    echo ""
    echo "📁 Testing File Menu..."
    echo "----------------------------------------"
    
    # Start app
    start_app
    
    # Take initial screenshot
    take_screenshot "05_file_menu_start"
    
    # Tap menu button
    echo "📂 Tapping file menu button..."
    tap_screen 100 100  # Approximate menu button position
    sleep 2
    
    # Take screenshot after menu tap
    take_screenshot "06_menu_tapped"
    
    # Test new file option
    echo "📄 Testing new file option..."
    tap_screen 300 300  # Approximate new file position
    sleep 2
    
    # Take screenshot after new file tap
    take_screenshot "07_new_file_tapped"
    
    # Check if in file mode
    echo "🔍 Checking if in file mode..."
    tap_screen 540 100  # Approximate mode toggle position
    sleep 2
    
    # Take screenshot after mode toggle
    take_screenshot "08_mode_toggled"
    
    # Return to main screen
    press_back
    sleep 2
    take_screenshot "09_back_from_file_menu"
}

# Function to test file loading
test_file_loading() {
    echo ""
    echo "📂 Testing File Loading..."
    echo "----------------------------------------"
    
    # Create test file
    local test_file="test_reminder_file.txt"
    create_test_file "$test_file"
    
    # Push file to device
    push_file_to_device "$test_file" "/sdcard/Download/$test_file"
    
    # Start app
    start_app
    
    # Take initial screenshot
    take_screenshot "10_file_loading_start"
    
    # Open file menu
    echo "📂 Opening file menu..."
    tap_screen 100 100
    sleep 2
    
    # Tap open file option
    echo "📂 Tapping open file option..."
    tap_screen 300 200  # Approximate open file position
    sleep 2
    
    # Take screenshot after open file tap
    take_screenshot "11_open_file_tapped"
    
    # Wait for file picker to appear
    echo "⏳ Waiting for file picker..."
    sleep 3
    
    # Navigate to Downloads (may need to swipe or tap)
    echo "📂 Navigating to Downloads..."
    tap_screen 540 500  # Approximate Downloads position
    sleep 2
    
    # Tap on test file
    echo "📄 Tapping on test file..."
    tap_screen 540 400  # Approximate file position
    sleep 2
    
    # Take screenshot after file selection
    take_screenshot "12_file_selected"
    
    # Confirm file selection
    echo "✅ Confirming file selection..."
    tap_screen 800 500  # Approximate OK button position
    sleep 2
    
    # Take screenshot after file confirmation
    take_screenshot "13_file_confirmed"
    
    # Wait for file to load
    echo "⏳ Waiting for file to load..."
    sleep 3
    
    # Take screenshot after file load
    take_screenshot "14_file_loaded"
}

# Function to test file editing
test_file_editing() {
    echo ""
    echo "✏️ Testing File Editing..."
    echo "----------------------------------------"
    
    # Start app (assuming file is already loaded)
    start_app
    
    # Take initial screenshot
    take_screenshot "15_file_editing_start"
    
    # Tap on text field to edit
    echo "📝 Tapping on text field..."
    tap_screen 540 400
    sleep 2
    
    # Clear existing content
    echo "🗑️ Clearing existing content..."
    $ADB_PATH -s "$DEVICE_ID" shell input keyevent KEYCODE_CTRL_A
    sleep 1
    $ADB_PATH -s "$DEVICE_ID" shell input keyevent KEYCODE_DEL
    sleep 1
    
    # Input new test content
    echo "⌨️ Inputting new test content..."
    input_text "Edited content from test script"
    sleep 2
    
    # Take screenshot after content edit
    take_screenshot "16_content_edited"
    
    # Test save functionality
    echo "💾 Testing save functionality..."
    tap_screen 100 100  # Open menu
    sleep 2
    
    # Tap save option
    tap_screen 300 400  # Approximate save position
    sleep 2
    
    # Take screenshot after save tap
    take_screenshot "17_save_tapped"
    
    # Wait for save to complete
    echo "⏳ Waiting for save to complete..."
    sleep 3
    
    # Take screenshot after save
    take_screenshot "18_file_saved"
}

# Function to test voice input with file content
test_voice_file_integration() {
    echo ""
    echo "🎤 Testing Voice Input with File Content..."
    echo "----------------------------------------"
    
    # Start app
    start_app
    
    # Take initial screenshot
    take_screenshot "19_voice_file_start"
    
    # Switch to file mode if not already
    echo "🔄 Ensuring file mode is active..."
    tap_screen 540 100  # Mode toggle position
    sleep 2
    
    # Tap voice input button
    echo "🎤 Tapping voice input button..."
    tap_screen 540 200  # Voice button position
    sleep 2
    
    # Take screenshot after voice tap
    take_screenshot "20_voice_tapped"
    
    # Wait for voice recording to start
    echo "⏳ Waiting for voice recording..."
    sleep 3
    
    # Take screenshot during voice recording
    take_screenshot "21_voice_recording"
    
    # Stop voice recording
    echo "⏹️ Stopping voice recording..."
    tap_screen 540 200  # Tap same button to stop
    sleep 2
    
    # Take screenshot after voice stop
    take_screenshot "22_voice_stopped"
    
    # Wait for voice processing
    echo "⏳ Waiting for voice processing..."
    sleep 3
    
    # Take screenshot after voice processing
    take_screenshot "23_voice_processed"
}

# Function to test file discard
test_file_discard() {
    echo ""
    echo "🗑️ Testing File Discard..."
    echo "----------------------------------------"
    
    # Start app (assuming file is loaded with modifications)
    start_app
    
    # Take initial screenshot
    take_screenshot "24_discard_start"
    
    # Make some changes first
    echo "📝 Making changes to file..."
    tap_screen 540 400  # Text field position
    sleep 1
    input_text "Modified content - should be discarded"
    sleep 2
    
    # Take screenshot after modifications
    take_screenshot "25_modifications_made"
    
    # Open menu
    echo "📂 Opening file menu..."
    tap_screen 100 100
    sleep 2
    
    # Tap discard option
    echo "🗑️ Tapping discard option..."
    tap_screen 300 500  # Approximate discard position
    sleep 2
    
    # Take screenshot after discard tap
    take_screenshot "26_discard_tapped"
    
    # Wait for discard to complete
    echo "⏳ Waiting for discard to complete..."
    sleep 3
    
    # Take screenshot after discard
    take_screenshot "27_discard_completed"
}

# Function to test file close
test_file_close() {
    echo ""
    echo "❌ Testing File Close..."
    echo "----------------------------------------"
    
    # Start app (assuming file is loaded)
    start_app
    
    # Take initial screenshot
    take_screenshot "28_close_start"
    
    # Open menu
    echo "📂 Opening file menu..."
    tap_screen 100 100
    sleep 2
    
    # Tap close file option
    echo "❌ Tapping close file option..."
    tap_screen 300 600  # Approximate close position
    sleep 2
    
    # Take screenshot after close tap
    take_screenshot "29_close_tapped"
    
    # Wait for close to complete
    echo "⏳ Waiting for close to complete..."
    sleep 3
    
    # Take screenshot after close
    take_screenshot "30_close_completed"
}

# Function to generate test report
generate_report() {
    echo ""
    echo "📊 Generating Test Report..."
    echo "========================================"
    
    local report_file="file_operations_test_report.txt"
    {
        echo "File Operations Test Report"
        echo "Date: $(date)"
        echo "Device: $DEVICE_ID"
        echo "App Package: $APP_PACKAGE"
        echo ""
        echo "Tests Performed:"
        echo "1. Basic App Functionality"
        echo "2. File Menu Access"
        echo "3. File Loading"
        echo "4. File Editing"
        echo "5. Voice Input with File Content"
        echo "6. File Discard"
        echo "7. File Close"
        echo ""
        echo "Screenshots taken:"
        ls -la *.png 2>/dev/null | grep -v "total" || echo "No screenshots found"
        echo ""
        echo "Test files created:"
        ls -la *.txt 2>/dev/null | grep -v "total" || echo "No test files found"
    } > "$report_file"
    
    echo "📄 Report saved to: $report_file"
    
    # Pull screenshots and report to local machine
    echo "📥 Pulling test artifacts..."
    mkdir -p ./test_results
    $ADB_PATH -s "$DEVICE_ID" pull /sdcard/*.png ./test_results/ 2>/dev/null
    cp "$report_file" ./test_results/
    
    echo "✅ Test complete! Check ./test_results/ for screenshots and report."
}

# Main execution
main() {
    echo "🧪 Reminder App File Operations Test Script"
    echo "========================================"
    
    # Check device connection
    check_device
    
    # Clear app data for clean test
    echo "🔄 Clearing app data for clean test..."
    clear_app_data
    
    # Run all tests
    test_basic_functionality
    test_file_menu
    test_file_loading
    test_file_editing
    test_voice_file_integration
    test_file_discard
    test_file_close
    
    # Generate report
    generate_report
    
    echo ""
    echo "🎉 All tests completed!"
    echo "📁 Check ./test_results/ directory for screenshots and detailed report"
}

# Cleanup function
cleanup() {
    echo ""
    echo "🧹 Cleaning up..."
    cd /
    rm -rf "$TEST_DIR"
    echo "✅ Cleanup completed"
}

# Trap to ensure cleanup on exit
trap cleanup EXIT

# Run main function
main "$@"