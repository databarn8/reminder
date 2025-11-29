# Default Reminder Time Configuration Guide

## Overview
This guide explains how to modify the default reminder time when users create a new reminder without specifying a time.

## Current Implementation
The app currently defaults to "today at 10 minutes from now" when creating a reminder without a specified time.

## Files to Modify

### 1. InputScreen.kt
**Location**: `reminder/app/src/main/java/com/reminder/app/ui/screens/InputScreen.kt`
**Line**: 1072

```kotlin
// Current code:
var selectedTime by remember { mutableStateOf(LocalTime.now().plusMinutes(10)) }

// To change to a different default time, modify this line:
// Examples:
var selectedTime by remember { mutableStateOf(LocalTime.now().plusMinutes(30)) }  // 30 minutes from now
var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }              // 9:00 AM today
var selectedTime by remember { mutableStateOf(LocalTime.NOON) }                   // 12:00 PM (original)
```

### 2. SmartVoiceProcessor.kt
**Location**: `reminder/app/src/main/java/com/reminder/app/utils/SmartVoiceProcessor.kt`
**Line**: 250-251

```kotlin
// Current code:
else -> now + (10 * 60 * 1000)  // Default to today at 10 minutes from now

// To change to a different default time, modify this line:
// Examples:
else -> now + (30 * 60 * 1000)  // 30 minutes from now
else -> {                        // 9:00 AM today
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 9)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.timeInMillis
}
```

### 3. InputScreen.kt (calculateReminderTime function)
**Location**: `reminder/app/src/main/java/com/reminder/app/ui/screens/InputScreen.kt`
**Line**: 216

```kotlin
// Current code:
else -> now + (10 * 60 * 1000L) // Default to today at 10 minutes from now

// To change to a different default time, modify this line:
// Examples:
else -> now + (30 * 60 * 1000L)  // 30 minutes from now
else -> {                         // 9:00 AM today
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    calendar.timeInMillis
}
```

## Important Notes

1. **Consistency is Key**: Make sure to update ALL THREE locations to maintain consistency across the app:
   - InputScreen.kt (line 1072) - for the UI default time
   - SmartVoiceProcessor.kt (line 250-251) - for voice input processing
   - InputScreen.kt calculateReminderTime function (line 216) - for text input processing

2. **Time Formats**:
   - `LocalTime.now().plusMinutes(X)` - X minutes from current time
   - `LocalTime.of(hour, minute)` - Specific time (24-hour format)
   - `LocalTime.NOON` - 12:00 PM
   - `LocalTime.MIDNIGHT` - 12:00 AM

3. **Testing**: After making changes, rebuild and test both typed and voice input to ensure they work consistently.

## Common Default Time Options

### Option 1: X minutes from now (recommended)
```kotlin
LocalTime.now().plusMinutes(10)  // 10 minutes from now
now + (10 * 60 * 1000L)         // 10 minutes from now in milliseconds
```

### Option 2: Fixed time of day
```kotlin
LocalTime.of(9, 0)               // 9:00 AM
LocalTime.of(14, 30)             // 2:30 PM
LocalTime.NOON                    // 12:00 PM
```

### Option 3: Next hour
```kotlin
LocalTime.now().plusHours(1).withMinute(0)  // Next hour on the hour
```

## Build and Deploy
After making changes:
1. Build the APK: `cd reminder && ./gradlew assembleDebug`
2. Install on device: `~/Library/Android/sdk/platform-tools/adb install -r reminder/app/build/outputs/apk/debug/app-debug.apk`
3. Test both typed and voice input to verify consistency

## History
- Originally defaulted to 12:00 PM (LocalTime.NOON)
- Changed to 10 minutes from now on November 29, 2024
- This guide created to document the change process