# Alert System Requirements and Implementation Details

## Overview
This document outlines the complete requirements for the alert system functionality in the reminder app, focusing on three main areas: save functionality, custom profiles, and dropdown behavior.

## Requirement 1: Proper Save Functionality

### Current Issue
- AlertSettingsScreen (accessed via alarm icon in top bar) allows users to configure Low/Medium/High/Urgent alert behaviors
- Toggle switches for vibration, sound, and repeat settings may not be persisting changes properly
- No clear indication that settings are being saved

### Required Functionality
**Option A: Auto-save Implementation**
- When user toggles any switch (vibration, sound, repeat), changes must save immediately
- Show confirmation message "✓ Alert settings saved" for 2 seconds
- Changes must persist across app restarts

**Option B: Explicit Save Button**
- Add "Save" button at bottom of AlertSettingsScreen
- Only save changes when user clicks Save
- Enable "Discard" option to revert changes
- Show confirmation only when Save is clicked

### Implementation Details
- All switch toggles must call `saveAlertLevelConfig()` function
- Configuration changes must update `AlertLevelConfig` in SharedPreferences
- Changes must be reflected immediately in reminder behavior
- Add validation to ensure settings are properly saved before closing screen

## Requirement 2: Individual Custom Profiles in Dropdowns

### Current Issue
- When users create custom profiles (e.g., "ShortAlert"), they don't appear as individual options
- Only generic "Custom" option appears in alert level dropdowns
- Users cannot select specific custom profiles for different reminders

### Required Functionality
**Custom Profile Creation**
- Users can create up to 3 custom profiles with unique names
- Each profile has its own vibration, sound, and repeat settings
- Profiles are created in AlertSettingsScreen with "Create Custom Profile" dialog

**Dropdown Behavior**
- All alert level dropdowns throughout app must show:
  - Built-in levels: Low, Medium, High, Urgent
  - All created custom profiles by their actual names (e.g., "ShortAlert", "GentleWake")
  - No generic "Custom" option
- Maximum 3 custom profiles allowed
- If 3 profiles exist, all 3 must appear in dropdown

### Implementation Details
- Modify `AlertLevel.values()` to include custom profiles dynamically
- Update all dropdown components to use enhanced alert level list
- Add profile management (create, edit, delete) in AlertSettingsScreen
- Store custom profiles in `AlertLevelConfig.customProfiles` map
- Ensure custom profiles are loaded and available throughout app

## Requirement 3: Maximum 3 Custom Profiles

### Current Issue
- No limit on custom profile creation
- Profile management may be unclear or non-functional

### Required Functionality
**Profile Limits**
- Maximum 3 custom profiles can be created
- When limit reached, show message "Maximum 3 custom profiles reached"
- Disable "Create Custom Profile" button when limit is reached
- Allow deletion of existing profiles to make room for new ones

**Profile Management**
- List all created profiles with edit and delete options
- Confirmation dialog before deleting profiles
- Prevent deletion of profiles currently in use by reminders
- Show profile count: "2 of 3 custom profiles used"

### Implementation Details
- Add validation in custom profile creation dialog
- Check `alertLevelConfig.customProfiles.size` before allowing creation
- Add profile count display in AlertSettingsScreen
- Implement profile deletion with confirmation
- Add check to prevent deletion if profile is in use

## Technical Implementation Notes

### Data Structure
```kotlin
// AlertLevelConfig should contain:
data class AlertLevelConfig(
    val lowLevel: AlertConfig,
    val mediumLevel: AlertConfig,
    val highLevel: AlertConfig,
    val urgentLevel: AlertConfig,
    val customProfiles: Map<String, AlertConfig> // Max 3 entries
)
```

### Dropdown Implementation
```kotlin
// Enhanced alert level selector should show:
fun getAvailableAlertLevels(customProfiles: Map<String, AlertConfig>): List<AlertLevelOption> {
    val builtInLevels = listOf(
        AlertLevelOption("Low", AlertLevel.LOW),
        AlertLevelOption("Medium", AlertLevel.MEDIUM),
        AlertLevelOption("High", AlertLevel.HIGH),
        AlertLevelOption("Urgent", AlertLevel.URGENT)
    )
    
    val customLevels = customProfiles.map { (name, config) ->
        AlertLevelOption(name, AlertLevel.CUSTOM, config)
    }
    
    return builtInLevels + customLevels
}
```

### Save Implementation
```kotlin
// Auto-save pattern:
onConfigChanged = { newConfig ->
    alertLevelConfig = updateAlertLevelConfig(alertLevelConfig, selectedLevel, newConfig)
    saveAlertLevelConfig(context, alertLevelConfig)
    showSaveConfirmation = true
}
```

## User Experience Flow

### Creating Custom Profile
1. User goes to AlertSettingsScreen (alarm icon in top bar)
2. Clicks "Create Custom Profile" button
3. Enters profile name (e.g., "ShortAlert")
4. Profile is created with default medium settings
5. "ShortAlert" now appears in all alert level dropdowns throughout app

### Using Custom Profile
1. User creates/edits a reminder
2. Clicks alert level dropdown
3. Sees: Low, Medium, High, Urgent, "ShortAlert", "GentleWake"
4. Selects "ShortAlert" for that specific reminder
5. Reminder uses the exact settings defined in "ShortAlert" profile

### Managing Profiles
1. User goes to AlertSettingsScreen
2. Sees "Custom Profiles" section with list of created profiles
3. Can edit or delete existing profiles
4. Sees count: "2 of 3 custom profiles used"
5. Can create new profile if under limit

## Success Criteria

### Requirement 1 Success
- All toggle switches save immediately
- Confirmation message appears on save
- Settings persist after app restart
- No data loss or reversion to defaults

### Requirement 2 Success
- Custom profiles appear by name in all dropdowns
- No generic "Custom" option
- Each profile is individually selectable
- Profiles work consistently across entire app

### Requirement 3 Success
- Maximum 3 custom profiles enforced
- Clear profile management interface
- Profile count displayed
- Graceful handling of limit reached

## Testing Checklist

### Save Functionality Testing
- [ ] Toggle vibration switch and verify it saves
- [ ] Toggle sound switch and verify it saves
- [ ] Toggle repeat switch and verify it saves
- [ ] Restart app and verify settings persist
- [ ] Confirmation message appears on save

### Custom Profile Testing
- [ ] Create custom profile and verify it appears in dropdowns
- [ ] Create 3 profiles and verify all appear
- [ ] Attempt to create 4th profile and verify it's blocked
- [ ] Delete profile and verify it's removed from dropdowns
- [ ] Edit profile and verify changes are reflected

### Integration Testing
- [ ] Custom profiles work in ReminderListScreen dropdowns
- [ ] Custom profiles work in InputScreen dropdowns
- [ ] Custom profiles work in AlarmActivity
- [ ] Profile changes apply to existing reminders using that profile

## Implementation Priority

1. **High Priority**: Fix save functionality for basic alert level configurations
2. **High Priority**: Implement custom profile creation and management
3. **Medium Priority**: Update all dropdowns to show custom profiles by name
4. **Medium Priority**: Add 3-profile limit and validation
5. **Low Priority**: Add profile editing functionality
6. **Low Priority**: Add profile usage tracking and warnings

## Notes for Developers

- This system must work consistently across all screens that use alert levels
- Custom profiles should be treated as first-class citizens, not afterthoughts
- Consider migration strategy for existing data if changing data structures
- Ensure backward compatibility with existing reminders
- Add proper error handling for profile creation and management
- Consider adding import/export functionality for custom profiles in future versions