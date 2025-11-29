# Archive/Restore Feature Test Plan

## Overview
This document outlines the test plan for the archive/soft delete feature implemented in the reminder app.

## Test Cases

### 1. Archive Functionality
1.1. Create a new reminder
1.2. Go to the reminder list
1.3. Tap the archive button (next to the delete button)
1.4. Verify the reminder is no longer in the main list
1.5. Go to the Archive/Restore screen (from the main menu)
1.6. Verify the reminder appears in the "Archived" tab
1.7. Verify the archived date is displayed correctly

### 2. Restore Archived Reminders
2.1. From the Archive/Restore screen, select an archived reminder
2.2. Tap the "Restore Selected" button
2.3. Verify the reminder is back in the main list
2.4. Verify the reminder is no longer in the archived list

### 3. Delete (Soft Delete) Functionality
3.1. Create a new reminder
3.2. Go to the reminder list
3.3. Tap the delete button
3.4. Verify the reminder is no longer in the main list
3.5. Go to the Archive/Restore screen
3.6. Switch to the "Deleted" tab
3.7. Verify the reminder appears in the deleted list
3.8. Verify the deleted date is displayed correctly

### 4. Restore Deleted Reminders
4.1. From the Archive/Restore screen, switch to the "Deleted" tab
4.2. Select a deleted reminder
4.3. Tap the "Restore Selected" button
4.4. Verify the reminder is back in the main list
4.5. Verify the reminder is no longer in the deleted list

### 5. Backup Verification
5.1. Before testing purge functionality, create a backup
5.2. Go to Backup & Sync screen
5.3. Tap "Create Backup Now"
5.4. Verify the backup is created successfully

### 6. Purge Functionality - Time-based
6.1. From the Archive/Restore screen, switch to the "Deleted" tab
6.2. Tap the "Purge" button
6.3. Select "Older than 1 week" (if there are items older than a week)
6.4. Verify backup status is checked
6.5. If backup exists, confirm purge
6.6. Verify old reminders are removed from the deleted list
6.7. If no backup exists, verify automatic backup is created before purge

### 7. Purge Functionality - Selected Items
7.1. From the Archive/Restore screen, switch to the "Deleted" tab
7.2. Select specific reminders to purge
7.3. Tap the "Purge Selected" button
7.4. Verify backup status is checked for selected items
7.5. If backup exists, confirm purge
7.6. Verify selected reminders are removed from the deleted list
7.7. If no backup exists, verify automatic backup is created before purge

### 8. Archive/Restore Screen Navigation
8.1. Verify the Archive/Restore screen is accessible from the main menu
8.2. Verify tabs switch correctly between "Archived" and "Deleted"
8.3. Verify selection mode works correctly
8.4. Verify deselect all functionality works
8.5. Verify back navigation returns to the main screen

## Expected Results
- Archived reminders should not appear in the main reminder list
- Deleted reminders should not appear in the main reminder list
- Both archived and deleted reminders should be restorable
- Purge functionality should check for backups before permanent deletion
- Automatic backup should be created if no backup exists before purging

## Notes
- Test with both recent and old reminders to verify time-based filtering
- Verify that archived reminders still have their notifications canceled
- Verify that restored reminders have their notifications rescheduled if needed