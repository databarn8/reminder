# Archive and Soft Delete Feature Implementation Plan

## Overview
This document outlines the implementation plan for adding an archive/soft delete feature to the reminder app, along with a restore page and purge functionality with backup verification.

## User Requirements
1. Add an archive/soft delete button in addition to the existing permanent delete button
2. Provide a way to restore archived reminders
3. Add purge functionality with time-based options (one week, one month) or selective deletion
4. Ensure backup exists before permanent deletion, automatically create backup if needed

## Implementation Plan

### 1. Data Model Changes

#### 1.1 Update Reminder Data Class
- Add `isArchived: Boolean = false` field to the `Reminder` data class
- Add `archivedDate: Long? = null` field to track when the reminder was archived
- Add `isDeleted: Boolean = false` field to track soft-deleted items
- Add `deletedDate: Long? = null` field to track when the reminder was deleted

#### 1.2 Database Schema Updates
- Update `ReminderDao` to include new fields in the database table
- Add new query methods:
  - `getArchivedReminders(): Flow<List<Reminder>>`
  - `getDeletedReminders(): Flow<List<Reminder>>`
  - `getRemindersOlderThan(date: Long): List<Reminder>`
  - `purgeReminders(reminderIds: List<Long>)`

### 2. Repository Layer Updates

#### 2.1 ReminderRepository
- Add archive functionality:
  - `suspend fun archiveReminder(id: Long)`
  - `suspend fun unarchiveReminder(id: Long)`
- Add soft delete functionality:
  - `suspend fun softDeleteReminder(id: Long)`
  - `suspend fun restoreReminder(id: Long)`
- Add purge functionality:
  - `suspend fun purgeOlderThan(weeks: Int): Int`
  - `suspend fun purgeSelected(reminderIds: List<Long>): Int`
- Add backup verification:
  - `suspend fun verifyBackupExists(reminderId: Long): Boolean`
  - `suspend fun createBackupBeforeDelete(reminderId: Long)`

### 3. UI Components

#### 3.1 Reminder List Screen Updates
- Add archive button next to delete button in reminder items
- Add confirmation dialog for archive action
- Update swipe actions to include archive option

#### 3.2 New Archive/Restore Screen
- Create new screen: `ArchiveRestoreScreen.kt`
- Display tabs for Archived and Deleted reminders
- Show reminder details with restore/unarchive options
- Include bulk selection and restore functionality
- Add search and filter options

#### 3.3 Purge Functionality UI
- Add purge section to Archive/Restore screen
- Include options:
  - Purge items older than one week
  - Purge items older than one month
  - Select specific items to purge
- Add backup verification step before purge
- Show backup status and create backup if needed

### 4. Backup Integration

#### 4.1 Backup Verification
- Check if backup exists for reminders being purged
- Use existing `CloudBackupManager` to verify backup status
- Automatically create backup if none exists

#### 4.2 Email Backup Integration
- Offer to email backup before purge if no backup exists
- Use existing `SimpleEmailService` for email functionality

### 5. ViewModel Updates

#### 5.1 ReminderViewModel
- Add archive/restore methods:
  - `archiveReminder(id: Long)`
  - `unarchiveReminder(id: Long)`
  - `softDeleteReminder(id: Long)`
  - `restoreReminder(id: Long)`
- Add purge methods:
  - `purgeOldReminders(weeks: Int)`
  - `purgeSelectedReminders(ids: List<Long>)`
- Add new state variables for archived/deleted reminders

#### 5.2 New ArchiveRestoreViewModel
- Create dedicated ViewModel for archive/restore functionality
- Handle state management for archived/deleted items
- Manage purge operations with backup verification

### 6. Navigation Updates

#### 6.1 Add New Screen to Navigation
- Add Archive/Restore screen to navigation drawer
- Add appropriate route handling

### 7. Implementation Steps

1. **Data Model Updates**
   - Update Reminder data class with new fields
   - Update database schema and DAO methods

2. **Repository Layer**
   - Implement archive/restore methods in ReminderRepository
   - Add purge functionality with backup verification

3. **UI Implementation**
   - Update reminder list items with archive button
   - Create Archive/Restore screen with tabs
   - Implement purge functionality UI

4. **ViewModel Updates**
   - Update ReminderViewModel with new methods
   - Create ArchiveRestoreViewModel

5. **Navigation Integration**
   - Add new screen to navigation

6. **Testing**
   - Test archive/restore functionality
   - Test purge with backup verification
   - Test automatic backup creation

### 8. Key Considerations

#### 8.1 Performance
- Ensure efficient queries for archived/deleted items
- Implement pagination for large lists of archived items

#### 8.2 User Experience
- Clear distinction between archive and delete actions
- Intuitive restore process
- Clear warnings before permanent deletion

#### 8.3 Data Safety
- Ensure backup verification works correctly
- Prevent accidental permanent deletion
- Maintain data integrity during archive/restore operations

### 9. Files to be Modified/Created

#### New Files:
- `app/src/main/java/com/reminder/app/ui/screens/ArchiveRestoreScreen.kt`
- `app/src/main/java/com/reminder/app/viewmodel/ArchiveRestoreViewModel.kt`

#### Modified Files:
- `app/src/main/java/com/reminder/app/data/Reminder.kt`
- `app/src/main/java/com/reminder/app/data/ReminderDao.kt`
- `app/src/main/java/com/reminder/app/repository/ReminderRepository.kt`
- `app/src/main/java/com/reminder/app/viewmodel/ReminderViewModel.kt`
- `app/src/main/java/com/reminder/app/ui/screens/ReminderListScreen.kt`
- `app/src/main/java/com/reminder/app/MainActivity.kt` (for navigation)

This implementation plan provides a comprehensive solution for archive/soft delete functionality while ensuring data safety through backup verification.