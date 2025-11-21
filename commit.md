# Commit: Fix custom profile selection display issue

**Commit ID:** 9e1cf8541a4c8698257cd70a9225ade885d4f5ea

**Author:** yun hao <yunhao@yuns-Mac-mini.local>

**Date:** Thu Nov 20 22:34:18 2025 -0500

**Message:** 
Fix custom profile selection display issue

- Fix InputScreen to properly display selected custom profile name in alert level dropdown
- Fix ReminderListScreen to show custom profile name next to alert level icon
- Separate built-in and custom profile options in dropdowns
- Ensure all custom profiles (like 'hard') appear as individual selectable options

Resolves issue where only first custom profile was selectable in alert level selection screens.

## Files Changed

### InputScreen.kt
- Updated alert level dropdown button text to use `selectedCustomProfileName ?: loadedReminder?.getCustomProfileNameFromField() ?: "Custom"`
- Separated built-in options from custom profile options in dropdown menu

### ReminderListScreen.kt
- Modified alert level icon button to display custom profile name next to icon when selected
- Added Text component to show custom profile name when AlertLevel.CUSTOM is selected
- Separated built-in options from custom profile options in dropdown menu

### AlertSettingsScreenFixed.kt
- Updated custom profile selection logic to handle multiple custom profiles individually
- Added configKey to force recomposition when switching between custom profiles

## Testing

- App builds successfully
- Custom profiles (like 'hard') now appear as individual selectable options in both InputScreen and ReminderListScreen
- Selected custom profile name displays correctly in dropdown button
- Custom profile name shows next to alert level icon in reminder cards
- Changes save correctly to database

## GitHub Push

- Successfully pushed to GitHub repository: databarn8/reminder.git
- Commit ID: 9e1cf8541a4c8698257cd70a9225ade885d4f5ea
- All changes committed and pushed to main branch