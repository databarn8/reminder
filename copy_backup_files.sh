#!/bin/bash

# Script to copy reminder app backup files from Android device to local computer
# Usage: ./copy_backup_files.sh

echo "Reminder App Backup File Copy Script"
echo "===================================="

# Set variables
ADB_PATH="$HOME/Library/Android/sdk/platform-tools/adb"
BACKUP_DIR="/data/data/com.reminder.app/files/backups"
LOCAL_DIR="./reminder_backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Create local backup directory if it doesn't exist
mkdir -p "$LOCAL_DIR"

echo "Creating backup directory: $LOCAL_DIR/$TIMESTAMP"
mkdir -p "$LOCAL_DIR/$TIMESTAMP"

# Check if device is connected
if ! $ADB_PATH devices | grep -q "device$"; then
    echo "Error: No Android device connected. Please connect your device and enable USB debugging."
    exit 1
fi

echo "Device connected. Copying backup files..."

# List backup files on device
echo "Available backup files on device:"
$ADB_PATH shell run-as com.reminder.app ls -la "$BACKUP_DIR"

# Copy all backup files
echo ""
echo "Copying backup files..."

# Copy main backup files
$ADB_PATH shell run-as com.reminder.app cat "$BACKUP_DIR/reminder_base_20251127.json" > "$LOCAL_DIR/$TIMESTAMP/reminder_base_20251127.json"
$ADB_PATH shell run-as com.reminder.app cat "$BACKUP_DIR/reminder_delta_20251127_1759.json" > "$LOCAL_DIR/$TIMESTAMP/reminder_delta_20251127_1759.json"

# Copy metadata file
$ADB_PATH shell run-as com.reminder.app cat "/data/data/com.reminder.app/files/backup_metadata.json" > "$LOCAL_DIR/$TIMESTAMP/backup_metadata.json"

# Copy database file as well (optional)
echo "Copying main database file..."
$ADB_PATH shell run-as com.reminder.app cat "/data/data/com.reminder.app/databases/reminder_database" > "$LOCAL_DIR/$TIMESTAMP/reminder_database"

echo ""
echo "Backup files copied successfully!"
echo "Location: $LOCAL_DIR/$TIMESTAMP"
echo ""

# Show copied files
echo "Copied files:"
ls -la "$LOCAL_DIR/$TIMESTAMP"

echo ""
echo "Backup contents preview:"
echo "===================="
if [ -f "$LOCAL_DIR/$TIMESTAMP/reminder_base_20251127.json" ]; then
    echo "Main backup file contains $(grep -c '"id"' "$LOCAL_DIR/$TIMESTAMP/reminder_base_20251127.json") reminders"
fi

echo ""
echo "All done! Your backup files are now available in the reminder_backups folder."
echo "You can view the JSON files with any text editor or JSON viewer."