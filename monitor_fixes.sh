#!/bin/bash

# Monitor the background fix process
LOG_FILE="/home/pinetree/mac/reminder/fix_log_$(date +%Y%m%d)_*.log"
PROJECT_DIR="/home/pinetree/mac/reminder"

echo "=== MONITORING BACKGROUND FIX PROCESS ==="
echo "Project: $PROJECT_DIR"
echo ""

# Check if process is running
if pgrep -f "background_fix_process" > /dev/null; then
    echo "✅ Background process is RUNNING"
    echo "PID: $(pgrep -f background_fix_process)"
else
    echo "❌ Background process is NOT running"
fi

echo ""

# Show latest log entries
echo "=== LATEST LOG ENTRIES ==="
if ls $LOG_FILE 1> /dev/null 2>&1; then
    latest_log=$(ls -t $LOG_FILE | head -1)
    echo "Log file: $latest_log"
    echo "Last 10 entries:"
    tail -10 "$latest_log"
else
    echo "No log files found yet"
fi

echo ""

# Check for completion marker
echo "=== COMPLETION STATUS ==="
if ls /tmp/apk_fixes_completed_* 1> /dev/null 2>&1; then
    latest_marker=$(ls -t /tmp/apk_fixes_completed_* | head -1)
    completion_time=$(stat -c %Y "$latest_marker")
    echo "✅ Fixes completed at: $(date -d @$completion_time)"
else
    echo "⏳ Fixes still in progress..."
fi

echo ""

# Check modified files
echo "=== MODIFIED FILES ==="
cd "$PROJECT_DIR" || exit 1

files_to_check=(
    "app/src/main/java/com/reminder/app/utils/ScreenFlashManager.kt"
    "app/src/main/java/com/reminder/app/utils/NotificationScheduler.kt" 
    "app/src/main/java/com/reminder/app/ui/screens/AlarmActivity.kt"
    "app/src/main/java/com/reminder/app/ui/screens/BackupSettingsScreen.kt"
    "app/src/main/java/com/reminder/app/utils/DataExportImportManager.kt"
)

for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        mtime=$(stat -c %Y "$file")
        age=$(( ($(date +%s) - mtime) / 60 ))
        if [ $age -lt 30 ]; then
            echo "✅ $file (modified ${age} minutes ago)"
        else
            echo "⏸️  $file (not modified recently)"
        fi
    else
        echo "❌ $file (missing)"
    fi
done

echo ""

# Check for build artifacts
echo "=== BUILD STATUS ==="
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    mtime=$(stat -c %Y "app/build/outputs/apk/debug/app-debug.apk")
    build_time=$(date -d @$mtime)
    echo "✅ Debug APK built at: $build_time"
    echo "   Size: $(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)"
else
    echo "❌ No debug APK found"
fi

echo ""
echo "=== MONITORING COMPLETE ==="