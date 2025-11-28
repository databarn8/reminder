#!/bin/bash

# Script to backup, reconstruct all historical reminder data, and create zip for email
# Usage: ./reconstruct_backup_data.sh

echo "Reminder Data Backup, Reconstruction & Email Script"
echo "=============================================="

# Set variables
ADB_PATH="$HOME/Library/Android/sdk/platform-tools/adb"
BACKUP_DIR="/data/data/com.reminder.app/files/backups"
LOCAL_DIR="./reminder_backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_TIMESTAMP_DIR="$LOCAL_DIR/backup_$TIMESTAMP"
OUTPUT_FILE="$BACKUP_TIMESTAMP_DIR/reconstructed_reminders_$TIMESTAMP.json"
ZIP_FILE="$LOCAL_DIR/reminder_backup_$TIMESTAMP.zip"

# Interactive mode flag
INTERACTIVE_MODE=false

# Create local backup directory if it doesn't exist
mkdir -p "$LOCAL_DIR"
mkdir -p "$BACKUP_TIMESTAMP_DIR"

# Parse command line arguments
INTERACTIVE_MODE=false
CLEAN_OLD_BACKUPS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -i|--interactive)
            INTERACTIVE_MODE=true
            ;;
        -c|--clean-old)
            CLEAN_OLD_BACKUPS=true
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -i, --interactive    Interactive mode with user prompts"
            echo "  -c, --clean-old    Clean old backups (older than 30 days)"
            echo "  -h, --help         Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use -h for help"
            exit 1
            ;;
    esac
    shift
done

# Check if device is connected
if ! $ADB_PATH devices | grep -q "device$"; then
    echo "Error: No Android device connected. Please connect your device and enable USB debugging."
    exit 1
fi

echo "Device connected. Starting backup and reconstruction process..."

# Step 1: Create a fresh backup first
echo "Step 1: Creating fresh backup from device..."
$ADB_PATH shell run-as com.reminder.app ls -la "$BACKUP_DIR"

# Create temporary directory for processing
TEMP_DIR="./temp_backup_processing"
mkdir -p "$TEMP_DIR"

# Get list of all backup files
echo "Step 2: Fetching all backup files from device..."
BACKUP_FILES=$($ADB_PATH shell run-as com.reminder.app ls "$BACKUP_DIR" | grep -E '\.json$')

# Copy all backup files to temp directory
for file in $BACKUP_FILES; do
    echo "Copying: $file"
    $ADB_PATH shell run-as com.reminder.app cat "$BACKUP_DIR/$file" > "$TEMP_DIR/$file"
done

# Also copy metadata and database
echo "Copying metadata and database files..."
$ADB_PATH shell run-as com.reminder.app cat "/data/data/com.reminder.app/files/backup_metadata.json" > "$TEMP_DIR/backup_metadata.json"
$ADB_PATH shell run-as com.reminder.app cat "/data/data/com.reminder.app/databases/reminder_database" > "$TEMP_DIR/reminder_database"

echo ""
echo "Step 3: Processing backup files..."

# Start building the reconstructed JSON
echo "{
  \"reconstructedAt\": $(date +%s)000,
  \"reconstructedBy\": \"Reminder Backup Reconstruction Script\",
  \"sourceFiles\": [" > "$OUTPUT_FILE"

# Add source files list
first=true
for file in $BACKUP_FILES; do
    if [ "$first" = true ]; then
        echo -n "\"$file\"" >> "$OUTPUT_FILE"
        first=false
    else
        echo -n ", \"$file\"" >> "$OUTPUT_FILE"
    fi
done

echo "],
  \"allReminders\": [" >> "$OUTPUT_FILE"

# Process each backup file and extract reminders
processed_ids=""
reminder_count=0

for file in $BACKUP_FILES; do
    if [[ "$file" == *".json" ]]; then
        echo "Processing: $file"
        
        # Extract reminders from this file, handling potential JSON structure issues
        if [[ -f "$TEMP_DIR/$file" ]]; then
            # Use Python to safely parse and extract reminders
            python3 -c "
import json
import sys

try:
    with open('$TEMP_DIR/$file', 'r') as f:
        data = json.load(f)
    
    # Handle different backup file structures
    reminders = []
    if 'reminders' in data:
        reminders = data['reminders']
    elif isinstance(data, list):
        reminders = data
    else:
        # Try to find any array in the data
        for key, value in data.items():
            if isinstance(value, list):
                reminders = value
                break
    
    # Track processed IDs to avoid duplicates
    processed_ids = set('$processed_ids'.split(','))
    
    for reminder in reminders:
        if 'id' in reminder and str(reminder['id']) not in processed_ids:
            # Add source file information
            reminder['sourceFile'] = '$file'
            print(json.dumps(reminder, ensure_ascii=False))
            processed_ids.add(str(reminder['id']))
    
except Exception as e:
    print(f'Error processing {file}: {str(e)}', file=sys.stderr)
" >> "$OUTPUT_FILE.tmp"
            
            # Update processed IDs
            new_ids=$(python3 -c "
import json
try:
    with open('$TEMP_DIR/$file', 'r') as f:
        data = json.load(f)
    
    reminders = []
    if 'reminders' in data:
        reminders = data['reminders']
    elif isinstance(data, list):
        reminders = data
    
    ids = [str(r.get('id', '')) for r in reminders if 'id' in r]
    print(','.join(ids))
except:
    print('')
")
            
            if [ -n "$new_ids" ]; then
                if [ -n "$processed_ids" ]; then
                    processed_ids="$processed_ids,$new_ids"
                else
                    processed_ids="$new_ids"
                fi
            fi
            
            # Count reminders
            file_count=$(python3 -c "
import json
try:
    with open('$TEMP_DIR/$file', 'r') as f:
        data = json.load(f)
    
    reminders = []
    if 'reminders' in data:
        reminders = data['reminders']
    elif isinstance(data, list):
        reminders = data
    
    print(len(reminders))
except:
    print(0)
")
            reminder_count=$((reminder_count + file_count))
        fi
    fi
done

# Clean up the temporary file and finalize JSON
if [ -f "$OUTPUT_FILE.tmp" ]; then
    # Remove trailing comma and add proper JSON structure
    sed '$ s/,$//' "$OUTPUT_FILE.tmp" > "$OUTPUT_FILE.final"
    
    # Combine with the main output file
    cat "$OUTPUT_FILE.final" >> "$OUTPUT_FILE"
    rm "$OUTPUT_FILE.tmp" "$OUTPUT_FILE.final"
fi

echo "],
  \"totalReminders\": $reminder_count,
  \"reconstructionDate\": \"$(date)\",
  \"metadata\": " >> "$OUTPUT_FILE"

# Add metadata
if [ -f "$TEMP_DIR/backup_metadata.json" ]; then
    cat "$TEMP_DIR/backup_metadata.json" >> "$OUTPUT_FILE"
else
    echo "{}" >> "$OUTPUT_FILE"
fi

echo "}" >> "$OUTPUT_FILE"

# Step 4: Create ZIP file with all backup data
echo ""
echo "Step 4: Creating ZIP archive with all backup data..."
cd "$BACKUP_TIMESTAMP_DIR"
zip -r "../../$(basename "$ZIP_FILE")" .
cd - > /dev/null

# Step 5: Create README file for the zip
echo "Creating README file..."
cat > "$BACKUP_TIMESTAMP_DIR/README.txt" << EOF
Reminder App Backup Archive
========================

Generated: $(date)
Device: Android Phone
App: Reminder App

Contents:
- reconstructed_reminders_$TIMESTAMP.json: All reminders merged from backup files
- backup_metadata.json: Backup metadata
- reminder_database: Raw database file
- Original backup files: All individual backup files from device

Total reminders: $reminder_count

To restore:
1. Use the Reminder App's import function
2. Select the reconstructed_reminders_$TIMESTAMP.json file
3. Follow the on-screen instructions

This backup was created using the backup reconstruction script.
EOF

# Re-add README to zip
cd "$BACKUP_TIMESTAMP_DIR"
zip -u "../../$(basename "$ZIP_FILE")" README.txt
cd - > /dev/null

# Clean up temporary directory
rm -rf "$TEMP_DIR"

echo ""
echo "Backup and reconstruction completed!"
echo "================================="
echo "Backup directory: $BACKUP_TIMESTAMP_DIR"
echo "ZIP file: $ZIP_FILE"
echo "Total reminders found: $reminder_count"
echo ""

# Show preview of reconstructed data
echo "Preview of reconstructed data:"
echo "============================="
python3 -c "
import json
try:
    with open('$OUTPUT_FILE', 'r') as f:
        data = json.load(f)
    
    print(f'Version: {data.get(\"version\", \"N/A\")}')
    print(f'Total reminders: {data.get(\"totalReminders\", 0)}')
    print(f'Source files: {len(data.get(\"sourceFiles\", []))}')
    
    if 'allReminders' in data and data['allReminders']:
        print('\\nFirst few reminders:')
        for i, reminder in enumerate(data['allReminders'][:3]):
            print(f'{i+1}. {reminder.get(\"content\", \"No content\")} (ID: {reminder.get(\"id\", \"N/A\")})')
        
        if len(data['allReminders']) > 3:
            print(f'... and {len(data[\"allReminders\"]) - 3} more')
except Exception as e:
    print(f'Error reading output file: {e}')
"

echo ""
echo "All done! Your backup ZIP file is ready for email distribution."
echo ""
echo "Next steps:"
echo "1. Run the email client: python3 reminder_email_client.py"
echo "2. Select the ZIP file: $ZIP_FILE"
echo "3. Send to your email address"
echo ""
echo ""
echo "Email Options:"
echo "- Email: Sent to ${RECIPIENT_EMAIL:-"your email address"}"
echo "- Storage: Saved locally at $BACKUP_TIMESTAMP_DIR"

echo ""
echo "Realistic methods to get this ZIP file back:"
echo "- Email attachment (most reliable)"
echo "- Cloud storage (Google Drive, Dropbox, etc.)"
echo "- USB transfer to another computer"
echo "- Messaging apps (WhatsApp, Telegram - for smaller files)"
echo "- Direct device transfer (Android's Nearby Share)"