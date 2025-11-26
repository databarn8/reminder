# Data Storage and Cloud Backup Guide

## Current Data Storage

### **Primary Database Location**
```
/data/data/com.reminder.app/databases/reminder_database
```
- **Type**: Room Database with SQLite
- **Database Name**: `reminder_database`
- **Version**: 12 (with destructive migration)
- **Access**: Private to the app only

### **Data Structure**
The `Reminder` entity contains:
- **Basic Info**: id, content, category, importance
- **Timing**: reminderTime, whenDay, whenTime
- **Repeats**: repeatType, repeatInterval, repeatPattern (JSON)
- **Alerts**: triggerPoints (JSON), alertConfig (JSON), alertLevel
- **Metadata**: createdAt, voiceInput, isProcessed, isActive

### **Export/Import Storage**
```
/storage/emulated/0/Android/data/com.reminder.app/files/Downloads/
```
- **Formats**: CSV and JSON
- **Naming**: `reminders_export_YYYYMMDD_HHMMSS.csv/json`
- **Access**: Shared via Android's file sharing system

### **Configuration Storage**
```
/data/data/com.reminder.app/shared_prefs/
```
- Email preferences
- User settings
- Alert configurations

## Cloud Backup Implementation

### **New Cloud Backup Features Added**

#### 1. **Google Drive Integration**
- **Authentication**: Google Sign-In with OAuth 2.0
- **Storage**: App-specific folder (`appDataFolder`)
- **Format**: JSON with metadata
- **Features**: 
  - Automatic backup scheduling
  - Manual backup/restore
  - Backup history tracking
  - Multiple device sync

#### 2. **Local Backup Fallback**
- **Location**: `/data/data/com.reminder.app/files/backups/`
- **Purpose**: Fallback when cloud is unavailable
- **Format**: Same JSON structure as cloud
- **Retention**: All backups retained (user-managed)

#### 3. **Auto Backup System**
- **Worker**: `AutoBackupWorker` using WorkManager
- **Frequency**: Configurable (6, 12, 24, 48, 72 hours)
- **Trigger**: Periodic work with constraints
- **Strategy**: Cloud first, local fallback

#### 4. **Multi-Provider Support**
- **Current**: Google Drive (implemented)
- **Planned**: Dropbox, OneDrive (framework ready)
- **Sync**: Simultaneous backup to multiple providers
- **Status**: Individual provider tracking

## Implementation Details

### **Dependencies Added**
```gradle
// Google Drive API
implementation 'com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0'
implementation 'com.google.api-client:google-api-client-android:2.0.0'
implementation 'com.google.oauth-client:google-oauth-client-jetty:1.34.1'
implementation 'com.google.android.gms:play-services-auth:20.4.1'
```

### **Permissions Required**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
```

### **Key Components Created**

#### 1. **CloudBackupManager.kt**
- Google Drive API integration
- Backup/restore operations
- Auto backup scheduling
- Multi-provider sync framework
- Backup history management

#### 2. **BackupSettingsScreen.kt**
- UI for backup configuration
- Google Sign-In integration
- Auto backup settings
- Backup history display
- Manual backup/restore controls

#### 3. **GoogleSignInHelper.kt**
- Simplified Google authentication
- Account management
- Sign-in state tracking

#### 4. **AutoBackupWorker.kt**
- Background backup execution
- Network-aware scheduling
- Error handling and retry logic

## Backup Data Format

### **JSON Structure**
```json
{
  "version": "1.0",
  "exportedAt": 1703123456789,
  "exportedBy": "Reminder App",
  "reminders": [
    {
      "id": 1,
      "content": "Meeting with team",
      "category": "Work",
      "importance": 8,
      "reminderTime": 1703200000000,
      "whenDay": "Tomorrow",
      "whenTime": "2:00pm",
      "repeatType": "weekly",
      "repeatInterval": 1,
      "isActive": true,
      "voiceInput": "meeting tomorrow at 2pm",
      "isProcessed": true,
      "triggerPoints": "[{\"type\":\"MINUTES_BEFORE\",\"value\":15}]",
      "repeatPattern": "{\"type\":\"WEEKLY\",\"interval\":1,\"daysOfWeek\":[1]}",
      "alertConfig": "{\"sound\":true,\"vibration\":true,\"flash\":true}",
      "alertLevel": "HIGH",
      "customProfileName": null,
      "createdAt": 1703123456789
    }
  ]
}
```

## Usage Instructions

### **For Users**

#### 1. **Setting Up Cloud Backup**
1. Open Reminder App
2. Tap cloud icon (☁️) in top bar
3. Tap "Sign In" and select Google account
4. Enable "Auto backup" if desired
5. Choose backup interval (6-72 hours)

#### 2. **Manual Backup**
1. Go to Backup Settings
2. Tap "Backup to Cloud" or "Backup Locally"
3. Wait for completion confirmation
4. Check backup history to verify

#### 3. **Restore from Backup**
1. Go to Backup Settings
2. Tap "Restore"
3. Choose cloud or local backup
4. Confirm restore action
5. Wait for completion

#### 4. **Export Options**
1. Go to Backup Settings
2. Scroll to "Export Options"
3. Choose CSV or JSON format
4. Share via Android's share menu

### **For Developers**

#### 1. **Adding New Cloud Providers**
```kotlin
// In CloudBackupManager.kt
suspend fun backupToDropbox(reminders: List<Reminder>): Result<String> {
    // Implement Dropbox API integration
}

suspend fun backupToOneDrive(reminders: List<Reminder>): Result<String> {
    // Implement OneDrive API integration
}
```

#### 2. **Custom Backup Formats**
```kotlin
private fun createCustomBackupData(reminders: List<Reminder>): String {
    // Implement custom serialization
}
```

#### 3. **Backup Validation**
```kotlin
private fun validateBackupData(data: String): Boolean {
    // Implement backup integrity checks
}
```

## Security Considerations

### **Data Protection**
- **Encryption**: All cloud backups use HTTPS/TLS
- **Authentication**: OAuth 2.0 with limited scopes
- **Storage**: App-specific Google Drive folder
- **Privacy**: No data shared with third parties

### **Access Control**
- **Google Drive**: `DRIVE_FILE` scope (app-specific folder only)
- **Local Storage**: Android's private app storage
- **Network**: HTTPS only for all API calls

### **Data Minimization**
- **Scope**: Only reminder data is backed up
- **Metadata**: Minimal metadata for backup management
- **Retention**: User-controlled backup retention

## Troubleshooting

### **Common Issues**

#### 1. **Google Sign-In Fails**
- Check internet connection
- Verify Google Play Services
- Clear app cache and retry
- Ensure Google account is active

#### 2. **Backup Upload Fails**
- Check network connectivity
- Verify Google Drive storage space
- Check Google Drive API quota
- Try manual backup

#### 3. **Auto Backup Not Working**
- Verify auto backup is enabled
- Check WorkManager constraints
- Review battery optimization settings
- Check background data restrictions

#### 4. **Restore Fails**
- Verify backup file integrity
- Check network connection for cloud restore
- Ensure app has storage permissions
- Try local backup restore

### **Debug Information**
```kotlin
// Check backup status
val status = cloudBackupManager.backupStatus.value
when (status) {
    BackupStatus.BackingUp -> // Backup in progress
    BackupStatus.Success -> // Backup completed
    BackupStatus.Error -> // Backup failed
    BackupStatus.NoInternet -> // Network issue
}
```

## Future Enhancements

### **Planned Features**
1. **Multi-Provider Sync**: Dropbox, OneDrive integration
2. **Incremental Backups**: Only backup changed reminders
3. **Compression**: Reduce backup file size
4. **Encryption**: End-to-end encryption option
5. **Conflict Resolution**: Handle sync conflicts
6. **Backup Sharing**: Share backups with other users

### **Advanced Options**
1. **Scheduled Exports**: Automatic CSV/JSON exports
2. **Backup Validation**: Check backup integrity
3. **Selective Backup**: Backup specific categories
4. **Backup Analytics**: Usage statistics and insights

## Migration Guide

### **From Local to Cloud**
1. Install updated app with cloud backup
2. Sign in to Google account
3. Go to Backup Settings
4. Tap "Backup to Cloud"
5. Verify in backup history

### **From Cloud to Local**
1. Go to Backup Settings
2. Tap "Restore"
3. Select cloud backup
4. Confirm restore
5. Verify reminders appear

### **Device Transfer**
1. **Old Device**: Create cloud backup
2. **New Device**: Install app, sign in to same Google account
3. **New Device**: Restore from cloud backup
4. **Verify**: All reminders transferred successfully

## Conclusion

The Reminder App now provides comprehensive cloud backup functionality with:
- **Automatic cloud synchronization** via Google Drive
- **Local backup fallback** for offline scenarios
- **Manual backup/restore** options
- **Export capabilities** for data portability
- **Multi-provider framework** for future expansion

This ensures users' reminder data is safely backed up and accessible across devices while maintaining privacy and security standards.