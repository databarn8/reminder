#!/bin/bash

# Android Debug Logcat Script
# Usage: ./scripts/debug_logcat.sh

echo "Starting Android debug logcat monitoring..."
echo "Filtering for AlertSettings, Custom, and Profile logs..."
echo "Press Ctrl+C to stop monitoring"

# Clear existing logs
/Users/yunhao/Library/Android/sdk/platform-tools/adb logcat -c

# Monitor logs with filters
/Users/yunhao/Library/Android/sdk/platform-tools/adb logcat | grep -i "AlertSettings\|Custom\|Profile"