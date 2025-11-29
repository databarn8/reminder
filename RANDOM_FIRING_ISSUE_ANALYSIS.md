# Random Firing Issue Analysis and Fixes

## Problem Description
Some reminders were firing randomly without following the setup page's date and time. This was a critical issue as the whole reminder system relies on correct firing times.

## Root Causes Identified

### 1. Multiple ViewModel Initializations
- **Issue**: `ReminderViewModel` was calling `scheduleAllReminders()` in its `init` block
- **Problem**: Every time the ViewModel was created, it would schedule alarms for ALL existing reminders
- **Impact**: Created duplicate alarms every time the app was opened or the ViewModel was recreated

### 2. BootReceiver Triggering on App Updates
- **Issue**: `BootReceiver` was listening to `MY_PACKAGE_REPLACED` and `MY_PACKAGE_RESTARTED` events
- **Problem**: Every time you install a new APK, it reschedules ALL alarms without canceling existing ones
- **Impact**: Created duplicate alarms after each app update

### 3. Flow.collect() Multiple Executions
- **Issue**: Both `ReminderViewModel.scheduleAllReminders()` and `BootReceiver.onReceive()` used `collect()` on a Flow
- **Problem**: `collect()` can trigger multiple times as the database updates
- **Impact**: Multiple scheduling of the same alarms

## Fixes Implemented

### 1. Changed to One-Time Retrieval
- **File**: `ReminderViewModel.kt`
- **Change**: 
  - Renamed `scheduleAllReminders()` to `scheduleAllRemindersOnce()`
  - Changed from `collect()` to `getAllRemindersOnce()` which returns a List
  - Added detailed logging to track what's being scheduled
- **Impact**: Alarms are only scheduled once when ViewModel is created

### 2. Added getAllRemindersOnce() Method
- **File**: `ReminderRepository.kt`
- **Change**: Added `suspend fun getAllRemindersOnce(): List<Reminder>` 
- **Implementation**: Uses existing `getAllRemindersSync()` from DAO
- **Impact**: Provides one-time retrieval instead of reactive Flow

### 3. Fixed BootReceiver
- **File**: `BootReceiver.kt`
- **Change**:
  - Changed from `collect()` to `getAllRemindersOnce()`
  - Added detailed logging showing how many alarms are being rescheduled
  - Added check to skip past reminders
- **Impact**: Prevents multiple executions and skips past reminders

### 4. Enhanced Logging
- Added detailed logging to track:
  - When alarms are scheduled
  - Which reminders are being processed
  - Whether reminders are future or past
  - Request codes being used for alarms

## Testing Scripts Created

### 1. test_duplicate_alarms.sh
- Monitors logcat for alarm-related events
- Tracks scheduling, triggering, cancellation, and boot rescheduling
- Helps identify if duplicate alarms are being created

### 2. How to Use
```bash
# Make script executable
chmod +x reminder/test_duplicate_alarms.sh

# Run to monitor for duplicate alarms
./reminder/test_duplicate_alarms.sh
```

## Potential Issues to Monitor

### 1. App Restart Scenarios
- When the app is killed by the system
- When the device is restarted
- When the app is updated

### 2. Time Zone Changes
- Daylight saving time transitions
- Manual time zone changes
- Travel across time zones

### 3. System Time Changes
- Manual time adjustments
- Network time sync updates

## Recommendations for Future

### 1. Alarm Verification
- Add a verification step after scheduling to confirm the alarm was set
- Check if the alarm time matches the expected time
- Log any discrepancies

### 2. Alarm Cleanup
- Implement periodic cleanup of expired alarms
- Remove alarms for deleted reminders more aggressively
- Add a "reset all alarms" function for debugging

### 3. Duplicate Detection
- Maintain a map of scheduled alarms with their request codes
- Check for duplicates before scheduling new alarms
- Log warnings when duplicates are detected

### 4. Test Coverage
- Add unit tests for alarm scheduling logic
- Test edge cases (time changes, app updates, device restarts)
- Automated testing for duplicate detection

## Files Modified
1. `ReminderViewModel.kt` - Changed to one-time alarm scheduling
2. `ReminderRepository.kt` - Added getAllRemindersOnce() method
3. `BootReceiver.kt` - Fixed to use one-time retrieval and skip past reminders
4. `test_duplicate_alarms.sh` - Created monitoring script
5. `RANDOM_FIRING_ISSUE_ANALYSIS.md` - This documentation file

## Verification Steps
1. Create several reminders with different times
2. Install app update to trigger BootReceiver
3. Monitor logs using test_duplicate_alarms.sh
4. Verify no duplicate alarms are scheduled
5. Confirm alarms fire only at their scheduled times

## Status
- ✅ Fixed duplicate alarm scheduling in ViewModel
- ✅ Fixed duplicate alarm scheduling in BootReceiver
- ✅ Added comprehensive logging
- ✅ Created test scripts for monitoring
- ⏳ Need to test with various scenarios
- ⏳ Need to monitor for any remaining issues