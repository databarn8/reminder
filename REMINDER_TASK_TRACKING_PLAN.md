# Reminder Task Tracking Feature Plan

## Overview
This document outlines the plan to implement a task tracking system for reminders, allowing users to track whether a reminder task is completed or still active.

## Current Behavior
Currently, when a user creates a reminder, it remains active indefinitely. There's no way to mark a reminder as "completed" or "done". The user wants to:
1. Have a way to track if a task is finished or not
2. After user clicks "done", the event/reminder should go to archive
3. Be able to retrieve information about archived reminders to see what was completed

## Proposed Implementation

### 1. Data Model Updates
Update the `Reminder` data class to add task tracking fields:
```kotlin
data class Reminder(
    // ... existing fields ...
    val isCompleted: Boolean = false,     // Track if task is completed
    val completedDate: Long? = null,       // When the task was marked as completed
    val completionNotes: String? = null       // Optional notes about completion
)
```

### 2. Database Layer Updates
Update `ReminderDao` to add:
- Query to update reminder completion status
- Query to filter reminders by completion status
- Method to mark reminder as completed

### 3. UI Updates
Add completion tracking to the reminder interface:

#### Option A: Add to ReminderListScreen
- Add a "Mark Complete" button alongside Archive and Delete
- Show visual indicators for completed reminders (strikethrough, different color)
- Add filter options: All, Active, Completed

#### Option B: Add to Reminder Detail View
- When viewing a reminder's details, add a "Mark Complete" checkbox
- Show completion date and notes when completed
- Allow editing completion notes

#### Option C: Quick Actions
- In the reminder list, add a quick complete button (checkmark icon)
- Swipe action to mark as complete
- Long press to show completion options

### 4. Archive Integration
When a reminder is marked as completed:
1. Set `isCompleted` to true
2. Set `completedDate` to current timestamp
3. Optionally ask user for completion notes
4. Move reminder to archive (set `isArchived` to true)
5. Set `archivedDate` to current timestamp
6. Cancel any active notifications for this reminder

### 5. Archive/Restore Screen Updates
Update the Archive/Restore screen to:
- Add a "Completed" tab alongside "Archived" and "Deleted"
- Show completion date and notes for completed reminders
- Allow filtering completed reminders by date range
- Add option to restore completed reminders (if needed)
- Add option to permanently delete very old completed reminders

### 6. Reporting/Statistics (Optional)
Add a screen to show:
- Total reminders created
- Number of active reminders
- Number of completed reminders
- Completion rate
- Average time to complete
- Completion trends over time

## Implementation Steps

1. Update Reminder data class with completion fields
2. Update ReminderDao with completion queries
3. Update ReminderRepository with completion methods
4. Update ReminderViewModel with completion functionality
5. Add completion UI to ReminderListScreen
6. Update Archive/Restore screen with Completed tab
7. Add navigation to new Completed tab
8. Test completion workflow
9. Test archive integration
10. Build and install APK

## User Workflow

1. User creates a reminder as normal
2. Reminder appears in active list
3. When task is done, user marks it as complete:
   - Via "Mark Complete" button
   - Via checkbox in detail view
   - Via swipe action
4. System prompts for optional completion notes
5. Reminder is marked as completed and moved to archive
6. User can view completed reminders in Archive/Restore screen
7. User can see when it was completed and any notes

## Benefits

1. Clear tracking of what's done vs. what's pending
2. Historical record of completed tasks
3. Ability to review completion patterns
4. Cleaner main list (only active tasks)
5. Archived completed tasks for reference
6. Optional completion notes for context

## Considerations

1. Should completed reminders be archived immediately or after a delay?
2. Should there be a separate "Completed" section or just use archive?
3. How to handle recurring reminders when marked complete?
4. Should completion trigger any celebration/feedback?
5. How to handle completion of reminders that are already archived?

## Next Steps

Awaiting user approval to proceed with implementation based on this plan.