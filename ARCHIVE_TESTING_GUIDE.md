# Archive/Restore Feature Testing Guide

This guide provides step-by-step instructions for testing the archive/soft delete feature on your phone.

## Prerequisites
1. Ensure the latest APK with archive feature is installed on your phone
2. Have at least 3-5 existing reminders to test with (or create new ones)

## Testing Steps

### 1. Archive Functionality Test
1.1. Open the reminder app
1.2. Go to the main reminder list
1.3. Find a reminder you want to archive
1.4. Tap the archive button (archive icon) next to the delete button
1.5. Verify the reminder is no longer in the main list
1.6. Tap the archive icon in the top bar (next to settings)
1.7. Verify you're on the Archive/Restore screen
1.8. Ensure the "Archived" tab is selected
1.9. Verify your archived reminder appears in the list with the correct archived date
1.10. Check if there's a backup status icon (green cloud if backed up, red if not)

### 2. Restore Archived Reminder Test
2.1. From the Archive/Restore screen, with "Archived" tab selected
2.2. Tap on the archived reminder to select it (checkbox should be checked)
2.3. Tap the "Unarchive Selected" button
2.4. Go back to the main reminder list
2.5. Verify the reminder is back in the main list
2.6. Return to Archive/Restore screen
2.7. Verify the reminder is no longer in the Archived tab

### 3. Soft Delete Functionality Test
3.1. From the main reminder list
3.2. Find a reminder you want to delete
3.3. Tap the delete button (trash icon)
3.4. Verify the reminder is no longer in the main list
3.5. Go to Archive/Restore screen
3.6. Switch to the "Deleted" tab
3.7. Verify your deleted reminder appears in the list with the correct deleted date
3.8. Check if there's a backup status icon

### 4. Restore Deleted Reminder Test
4.1. From the Archive/Restore screen, with "Deleted" tab selected
4.2. Tap on the deleted reminder to select it
4.3. Tap the "Restore Selected" button
4.4. Go back to the main reminder list
4.5. Verify the reminder is back in the main list
4.6. Return to Archive/Restore screen
4.7. Verify the reminder is no longer in the Deleted tab

### 5. Purge Functionality Test - Time-based
5.1. From the Archive/Restore screen, go to the "Deleted" tab
5.2. If you have items older than 1 week, tap "Purge > 1 Week"
5.3. A confirmation dialog should appear
5.4. Check if it mentions creating backups if needed
5.5. Confirm the purge
5.6. Verify old deleted items are removed from the list
5.7. If no items were older than 1 week, try "Purge > 1 Month"

### 6. Purge Functionality Test - Selected Items
6.1. From the Archive/Restore screen, go to either "Archived" or "Deleted" tab
6.2. Select multiple items by tapping on them (checkboxes should be checked)
6.3. Tap the "Purge Selected" button (appears when items are selected)
6.4. A confirmation dialog should appear
6.5. Confirm the purge
6.6. Verify selected items are removed from the list

### 7. Backup Verification Test
7.1. Before purging, check the backup status icons next to items
7.2. Try purging items without backups (red cloud icon)
7.3. Verify that the system creates backups before purging
7.4. Check your backup settings to confirm new backups were created

### 8. Navigation Test
8.1. Test all navigation paths:
   - Main list → Archive/Restore screen
   - Archive/Restore screen → Main list (back navigation)
   - Between Archived and Deleted tabs

### 9. Edge Cases Test
9.1. Test with empty archived list
9.2. Test with empty deleted list
9.3. Test selecting all items using "Select All" button
9.4. Test clearing selection with "Clear Selection" button

## Expected Results
- Archived reminders should not appear in main list
- Deleted reminders should not appear in main list
- Both archived and deleted reminders should be restorable
- Purge should create backups if they don't exist
- Time-based purge should only remove items older than specified time
- Selected purge should only remove selected items
- All navigation should work smoothly

## Troubleshooting
- If archive button doesn't work: Check if the app has proper permissions
- If Archive/Restore screen doesn't open: Check if navigation is properly set up
- If purge doesn't work: Check if backup system is functioning
- If reminders don't restore: Check if database operations are working

## Feedback
Please report any issues found during testing with:
1. Step number where issue occurred
2. Expected behavior
3. Actual behavior
4. Any error messages shown
5. Phone model and Android version