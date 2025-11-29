#!/bin/bash

# Test script to detect duplicate alarms and random firing issues
# This script monitors logcat for alarm-related events and reports potential issues

echo "=== Duplicate Alarm Detection Test ==="
echo "Starting monitoring for duplicate alarms and random firing..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Clear logcat buffer first
~/Library/Android/sdk/platform-tools/adb logcat -c

# Monitor for alarm scheduling and triggering
~/Library/Android/sdk/platform-tools/adb logcat -s "NotificationScheduler:D" "BootReceiver:D" "ReminderViewModel:D" | while read line; do
    # Extract timestamp
    timestamp=$(echo "$line" | awk '{print $1 " " $2}')
    
    # Check for alarm scheduling
    if echo "$line" | grep -q "Scheduling alarm"; then
        echo "[$timestamp] ALARM_SCHEDULED: $line"
    fi
    
    # Check for alarm triggering
    if echo "$line" | grep -q "Alarm received"; then
        echo "[$timestamp] ALARM_TRIGGERED: $line"
    fi
    
    # Check for BootReceiver rescheduling
    if echo "$line" | grep -q "Rescheduling alarm"; then
        echo "[$timestamp] BOOT_RESCHEDULE: $line"
    fi
    
    # Check for ViewModel scheduling
    if echo "$line" | grep -q "Scheduling alarm for existing reminder"; then
        echo "[$timestamp] VIEWMODEL_SCHEDULE: $line"
    fi
    
    # Check for alarm cancellation
    if echo "$line" | grep -q "Cancelled all alarms"; then
        echo "[$timestamp] ALARM_CANCELLED: $line"
    fi
done