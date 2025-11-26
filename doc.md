# Android Reminder App - Technical Documentation

## Overview

A comprehensive Android reminder application built with Kotlin and Jetpack Compose, featuring voice input, local storage, notifications, and advanced reminder management capabilities.

## Application Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Database with SQLite
- **Async Operations**: Kotlin Coroutines and Flow
- **Navigation**: Jetpack Navigation Compose
- **Build System**: Gradle with KSP (Kotlin Symbol Processing)

### Minimum Requirements
- **Android SDK**: API 26+ (Android 8.0)
- **Target SDK**: API 33 (Android 13)
- **Java Version**: 11
- **Kotlin Version**: 1.7.20

## File Structure

```
app/src/main/java/com/reminder/app/
├── MainActivity.kt                    # Main entry point and navigation setup
├── data/                            # Data layer
│   ├── AlertConfig.kt              # Alert configuration data structures
│   ├── EmailPreferences.kt         # Email preferences management
│   ├── Reminder.kt                 # Main reminder entity with repeat patterns
│   ├── ReminderDao.kt              # Database access object
│   └── ReminderDatabase.kt         # Room database configuration
├── repository/                      # Repository pattern
│   └── ReminderRepository.kt        # Data repository abstraction
├── ui/                             # UI layer
│   ├── calendar/
│   │   └── CalendarUtils.kt        # Calendar utility functions
│   ├── components/                  # Reusable UI components
│   │   ├── AlertSettingsComponent.kt
│   │   ├── BasicRepeatPatternSelector.kt
│   │   ├── EmailClientSelector.kt
│   │   ├── RepeatDataStructures.kt
│   │   ├── SimpleRepeatPatternSelector.kt
│   │   ├── SimplifiedAlertTimingSelector.kt
│   │   └── SimplifiedRepeatPatternSelector.kt
│   ├── screens/                     # Main UI screens
│   │   ├── AlarmActivity.kt         # Alarm trigger activity
│   │   ├── AlertSettingsScreenFixed.kt
│   │   ├── CalendarScreen.kt        # Calendar view screen
│   │   ├── ConfirmationScreen.kt    # Voice input confirmation
│   │   ├── EmailSettingsScreen.kt   # Email configuration
│   │   ├── InputScreen.kt           # Reminder creation/editing
│   │   └── ReminderListScreen.kt    # Main reminder list
│   └── theme/                       # Material Design theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── utils/                           # Utility classes
│   ├── AlternativeSpeechManager.kt
│   ├── BootReceiver.kt             # Boot completion receiver
│   ├── DataExportImportManager.kt  # CSV/JSON import/export
│   ├── DateTimeParser.kt           # Date/time parsing utilities
│   ├── EmailService.kt.broken      # (Broken implementation)
│   ├── EnhancedEmailService.kt     # Enhanced email functionality
│   ├── ImprovedEmailService.kt      # Improved email service
│   ├── NotificationScheduler.kt     # Alarm and notification management
│   ├── PromptEnhancer.kt           # Voice prompt enhancement
│   ├── PromptEnhancerTest.kt       # Test utilities
│   ├── ScreenFlashManager.kt       # Screen flash notifications
│   ├── SimpleEmailService.kt        # Simple email implementation
│   ├── SmartVoiceProcessor.kt      # Advanced voice processing
│   ├── SpeechManager.kt            # Speech recognition
│   └── VoiceDataParser.kt         # Voice input parsing
└── viewmodel/                      # ViewModels
    ├── ReminderViewModel.kt        # Main reminder business logic
    └── ReminderViewModelFactory.kt  # ViewModel factory
```

## Core Components

### Data Models

#### Reminder Entity (`Reminder.kt`)
The core data structure representing a reminder with the following properties:
- **id**: Primary key (auto-generated)
- **content**: Reminder text content
- **category**: Work, Family, Personal
- **importance**: Priority level (1-10)
- **reminderTime**: Timestamp for reminder
- **whenDay/whenTime**: Natural language date/time
- **repeatType**: none, daily, weekly, monthly, yearly
- **repeatInterval**: Interval for repeating reminders
- **isActive**: Active/inactive status
- **voiceInput**: Raw voice input data
- **triggerPoints**: JSON string of alert trigger points
- **repeatPattern**: JSON string for complex repeat patterns
- **alertConfig**: JSON string for alert configuration
- **alertLevel**: LOW, MEDIUM, HIGH, URGENT, or custom

#### Trigger System
- **TriggerType**: AT_DUE_TIME, MINUTES_BEFORE, HOURS_BEFORE, DAYS_BEFORE, WEEKS_BEFORE, CUSTOM_OFFSET
- **TriggerPoint**: Configurable alert triggers with flash, sound, and vibration options

### UI Screens

#### 1. ReminderListScreen (`ReminderListScreen.kt`)
**Purpose**: Main screen displaying all reminders
**Features**:
- List view of all active reminders
- Floating action button for adding new reminders
- Navigation to calendar, email settings, and alert settings
- Swipe-to-delete functionality
- Email sharing for individual reminders
- Search and filter capabilities

#### 2. InputScreen (`InputScreen.kt`)
**Purpose**: Create and edit reminders
**Features**:
- Text input with voice recognition
- Category selection (Work, Family, Personal)
- Importance level slider (1-10)
- Date/time picker with natural language input
- Repeat pattern configuration
- Alert settings integration
- Calendar integration for date selection

#### 3. CalendarScreen (`CalendarScreen.kt`)
**Purpose**: Calendar view for reminder management
**Features**:
- Monthly calendar view
- Reminder indicators on calendar dates
- Navigate to specific dates
- Add reminders for selected dates
- Overview of reminders by date

#### 4. AlertSettingsScreenFixed (`AlertSettingsScreenFixed.kt`)
**Purpose**: Configure alert preferences
**Features**:
- Alert level selection (LOW, MEDIUM, HIGH, URGENT)
- Custom alert profile creation
- Trigger point configuration
- Flash, sound, and vibration settings
- Default alert preferences

#### 5. EmailSettingsScreen (`EmailSettingsScreen.kt`)
**Purpose**: Email notification configuration
**Features**:
- Email client selection
- Email address configuration
- Email template customization
- Test email functionality
- Email preferences management

#### 6. AlarmActivity (`AlarmActivity.kt`)
**Purpose**: Handle alarm triggers
**Features**:
- Full-screen alarm display
- Snooze functionality
- Dismiss options
- Screen wake and lock screen bypass
- Audio and visual alerts

### Utility Services

#### NotificationScheduler (`NotificationScheduler.kt`)
**Purpose**: Manage alarms and notifications
**Features**:
- Schedule exact alarms using AlarmManager
- Handle boot completion and alarm rescheduling
- Notification channel management
- Background alarm processing
- System permission handling

#### SpeechManager (`SpeechManager.kt`)
**Purpose**: Voice input and output
**Features**:
- Speech recognition using Android's built-in recognizer
- Text-to-speech for confirmation
- Permission handling for audio recording
- Voice command processing
- Multi-language support

#### EnhancedEmailService (`EnhancedEmailService.kt`)
**Purpose**: Email notification system
**Features**:
- Email client integration
- Attachment support for reminder data
- Email preference management
- Template-based email generation
- Client selection and fallback handling

#### SmartVoiceProcessor (`SmartVoiceProcessor.kt`)
**Purpose**: Advanced voice input processing
**Features**:
- Natural language parsing for dates/times
- Context-aware reminder creation
- Voice command recognition
- Smart text extraction
- Error handling and fallbacks

#### ScreenFlashManager (`ScreenFlashManager.kt`)
**Purpose**: Visual alert system
**Features**:
- Full-screen flash notifications
- Configurable flash patterns
- Integration with alert levels
- Battery-conscious implementation
- Customizable colors and durations

#### DataExportImportManager (`DataExportImportManager.kt`)
**Purpose**: Data backup and restore
**Features**:
- CSV export/import
- JSON export/import
- Backup to external storage
- Data validation and error handling
- Batch operations support

## Navigation Structure

The app uses Jetpack Navigation Compose with the following routes:

```
reminder_list (start)
├── input_screen?reminderId={id}&selectedDate={date}
├── calendar
├── alert_settings
├── email_settings
└── confirmation_screen/{text}
```

## Database Schema

### Room Database (`ReminderDatabase.kt`)
- **Version**: 12
- **Entity**: Reminder
- **Migration Strategy**: Destructive migration (fallbackToDestructiveMigration)

### DAO Operations (`ReminderDao.kt`)
- CRUD operations for reminders
- Flow-based reactive queries
- Search and filtering
- Batch operations

## Permissions Required

### Essential Permissions
- `RECORD_AUDIO`: Voice input functionality
- `POST_NOTIFICATIONS`: Display notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM`: Precise alarm scheduling
- `USE_EXACT_ALARM`: Enhanced alarm capabilities
- `VIBRATE`: Haptic feedback
- `WAKE_LOCK`: Device wake for alarms

### Storage Permissions
- `READ_EXTERNAL_STORAGE`: Import backup files
- `WRITE_EXTERNAL_STORAGE`: Export backup files

### System Permissions
- `RECEIVE_BOOT_COMPLETED`: Reschedule alarms after reboot
- `QUICKBOOT_POWERON`: Handle quick boot scenarios

## Integration Points

### Google Assistant Integration
The app registers intent filters for:
- `CREATE_NOTE`: Standard note creation
- `com.google.android.gms.actions.CREATE_NOTE`: Google Assistant actions
- `com.reminder.app.CREATE_REMINDER`: Custom reminder creation
- `SEND/SENDTO`: Text sharing from other apps

### Email Client Integration
- Automatic email client detection
- User preference storage
- Fallback to system email client
- Attachment support via FileProvider

### Calendar Integration
- Native calendar date selection
- Reminder synchronization
- Date-based reminder organization

## Build Configuration

### Gradle Dependencies
- **Core**: AndroidX, Compose BOM, Material 3
- **Database**: Room with KSP
- **Architecture**: ViewModel, Navigation
- **Async**: Coroutines, Flow
- **Serialization**: Kotlinx Serialization
- **Network**: Retrofit, OkHttp (for potential online services)
- **Testing**: JUnit, Espresso, Compose Testing

### Build Variants
- **debug**: Development build with debugging enabled
- **release**: Production build with minification disabled

## Security Considerations

### Data Protection
- Local storage only (no cloud sync)
- FileProvider for secure file sharing
- Permission-based access control
- No sensitive data logging

### Privacy
- No analytics or telemetry
- No network calls for core functionality
- Local voice processing only
- User data never leaves device

## Performance Optimizations

### Database
- Room database with proper indexing
- Flow-based reactive updates
- Efficient query patterns
- Connection pooling

### UI
- Lazy loading for large lists
- Compose recomposition optimization
- Memory-efficient image handling
- Background thread processing

### Background Processing
- WorkManager for non-critical tasks
- AlarmManager for precise timing
- Coroutines for async operations
- Proper lifecycle management

## Testing Strategy

### Unit Tests
- ViewModel testing
- Repository testing
- Utility function testing
- Data model validation

### Integration Tests
- Database operations
- Email service integration
- Notification scheduling
- Voice recognition flow

### UI Tests
- Compose UI testing
- Navigation flow testing
- User interaction testing
- Accessibility testing

## Future Enhancements

### Potential Features
- Cloud synchronization
- Multi-device support
- Advanced natural language processing
- Custom notification sounds
- Widget support
- Wear OS integration

### Technical Improvements
- Dependency injection with Hilt
- Modular architecture
- Advanced offline support
- Performance monitoring
- Crash reporting

## Troubleshooting

### Common Issues
1. **Permission Denied**: Ensure all required permissions are granted
2. **Alarm Not Triggering**: Check exact alarm permission and battery optimization
3. **Voice Recognition Not Working**: Verify microphone permission and speech services
4. **Email Not Sending**: Check email client configuration and preferences

### Debug Tools
- Debug logcat script (`scripts/debug_logcat.sh`)
- Database clearing utility
- Notification testing functionality
- Email service testing

## Conclusion

This Android Reminder App demonstrates modern Android development practices with a focus on user experience, reliability, and privacy. The architecture supports scalability while maintaining simplicity, and the feature set provides comprehensive reminder management capabilities.