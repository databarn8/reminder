package com.reminder.app.utils

import android.content.Context
import android.provider.Settings
import androidx.work.*
import com.reminder.app.data.BackupMetadata
import com.reminder.app.data.DeltaChanges
import com.reminder.app.data.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.client.http.FileContent
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
// import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.DriveScopes

class CloudBackupManager(private val context: Context) {
    
    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()
    
    private val _restoreStatus = MutableStateFlow<RestoreStatus>(RestoreStatus.Idle)
    val restoreStatus: StateFlow<RestoreStatus> = _restoreStatus.asStateFlow()
    
    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Idle)
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()
    
    private val deviceId = getDeviceId()
    private val metadataFile = File(context.filesDir, "backup_metadata.json")
    private val googleSignInHelper = GoogleSignInHelper(context)
    private var driveService: Drive? = null
    private val appFolderName = "ReminderApp Backups"
    
    enum class AuthStatus {
        Idle, SignedIn, SignedOut, Error
    }
    
    enum class BackupStatus {
        Idle, BackingUp, Success, Error, NoInternet, AuthRequired
    }
    
    enum class RestoreStatus {
        Idle, Restoring, Success, Error, NoBackup, NoInternet, AuthRequired
    }
    
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
    }
    
    // Google Drive Authentication
    suspend fun signInWithGoogle(accountName: String? = null): Result<Boolean> {
        return try {
            _authStatus.value = AuthStatus.Idle
            android.util.Log.d("CloudBackupManager", "signInWithGoogle called")
            
            // Check if already signed in
            val signedIn = googleSignInHelper.isSignedIn()
            android.util.Log.d("CloudBackupManager", "googleSignInHelper.isSignedIn(): $signedIn")
            
            if (signedIn) {
                val account = googleSignInHelper.getSignedInAccount()
                android.util.Log.d("CloudBackupManager", "Signed in account: ${account?.email}")
                
                _authStatus.value = AuthStatus.SignedIn
                // Initialize Drive service
                initializeDriveService()
                Result.success(true)
            } else {
                android.util.Log.d("CloudBackupManager", "Not signed in")
                _authStatus.value = AuthStatus.SignedOut
                Result.failure(Exception("Not signed in. Please sign in through the app's main authentication flow."))
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudBackupManager", "Exception in signInWithGoogle: ${e.message}")
            _authStatus.value = AuthStatus.Error
            Result.failure(e)
        }
    }
    
    fun signOut() {
        googleSignInHelper.signOut()
        driveService = null
        _authStatus.value = AuthStatus.SignedOut
    }
    
    fun isSignedIn(): Boolean {
        return googleSignInHelper.isSignedIn() && _authStatus.value == AuthStatus.SignedIn
    }
    
    private fun initializeDriveService() {
        try {
            val account = googleSignInHelper.getSignedInAccount()
            if (account != null) {
                // Note: In a real implementation, you would need to properly initialize
                // the Drive service with the Google Account credentials
                // This is a simplified version that would need proper OAuth2 setup
                // Create credentials using the signed-in account
                val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
                    context, listOf(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = account.account
                
                // Build Drive service
                val transport = com.google.api.client.http.javanet.NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()
                
                driveService = Drive.Builder(transport, jsonFactory, credential)
                    .setApplicationName("Reminder App")
                    .build()
                
                android.util.Log.d("CloudBackupManager", "Drive service initialized successfully for account: ${account.email}")
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudBackupManager", "Error initializing Drive service: ${e.message}")
            _authStatus.value = AuthStatus.Error
        }
    }
    
    // Auto Backup Configuration
    fun scheduleAutoBackup(enabled: Boolean, intervalHours: Int = 24) {
        if (enabled) {
            val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "auto_backup",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("auto_backup")
        }
    }
    
    // Google Drive Backup Methods
    suspend fun backupToGoogleDrive(reminders: List<Reminder>): Result<String> {
        return try {
            android.util.Log.d("CloudBackupManager", "backupToGoogleDrive called with ${reminders.size} reminders")
            
            if (!isSignedIn()) {
                android.util.Log.d("CloudBackupManager", "Not signed in for Google Drive backup")
                _backupStatus.value = BackupStatus.AuthRequired
                return Result.failure(Exception("Not signed in to Google Drive"))
            }
            
            android.util.Log.d("CloudBackupManager", "Drive service is null: ${driveService == null}")
            
            _backupStatus.value = BackupStatus.BackingUp
            
            // Create local backup first
            val localResult = smartBackupToLocal(reminders)
            if (localResult.isFailure) {
                android.util.Log.e("CloudBackupManager", "Local backup failed: ${localResult.exceptionOrNull()?.message}")
                _backupStatus.value = BackupStatus.Error
                return Result.failure(localResult.exceptionOrNull() ?: Exception("Local backup failed"))
            }
            
            // Upload to Google Drive
            val backupFile = File(localResult.getOrNull() ?: "")
            android.util.Log.d("CloudBackupManager", "Created local backup file: ${backupFile.absolutePath}")
            
            val driveFileId = uploadToGoogleDrive(backupFile)
            android.util.Log.d("CloudBackupManager", "Upload result, driveFileId: $driveFileId")
            
            if (driveFileId != null) {
                _backupStatus.value = BackupStatus.Success
                Result.success(driveFileId)
            } else {
                _backupStatus.value = BackupStatus.Error
                Result.failure(Exception("Failed to upload to Google Drive"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudBackupManager", "Exception in backupToGoogleDrive: ${e.message}")
            e.printStackTrace()
            _backupStatus.value = BackupStatus.Error
            Result.failure(e)
        }
    }
    
    suspend fun restoreFromGoogleDrive(): Result<List<Reminder>> {
        return try {
            if (!isSignedIn()) {
                _restoreStatus.value = RestoreStatus.AuthRequired
                return Result.failure(Exception("Not signed in to Google Drive"))
            }
            
            _restoreStatus.value = RestoreStatus.Restoring
            
            // Get latest backup from Google Drive
            val backupFiles = getGoogleDriveBackupHistory()
            if (backupFiles.isEmpty()) {
                _restoreStatus.value = RestoreStatus.NoBackup
                return Result.failure(Exception("No backup files found in Google Drive"))
            }
            
            // Download the latest backup
            val latestBackup = backupFiles.first()
            val downloadedFile = downloadFromGoogleDrive(latestBackup.driveFileId ?: "")
            
            if (downloadedFile != null && downloadedFile.exists()) {
                val backupData = downloadedFile.readText()
                val reminders = parseBackupData(backupData)
                _restoreStatus.value = RestoreStatus.Success
                Result.success(reminders)
            } else {
                _restoreStatus.value = RestoreStatus.Error
                Result.failure(Exception("Failed to download backup from Google Drive"))
            }
        } catch (e: Exception) {
            _restoreStatus.value = RestoreStatus.Error
            Result.failure(e)
        }
    }
    
    suspend fun getGoogleDriveBackupHistory(): List<BackupInfo> {
        return try {
            if (!isSignedIn() || driveService == null) {
                return emptyList()
            }
            
            // Get app folder
            val appFolder = getOrCreateAppFolder()
            if (appFolder == null) {
                return emptyList()
            }
            
            // List all files in app folder
            val query = "parents in '${appFolder.id}'"
            val result = driveService!!.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name,size,modifiedTime,createdTime)")
                .setOrderBy("modifiedTime desc")
                .execute()
            
            val backupList = mutableListOf<BackupInfo>()
            
            result.files.forEach { file ->
                val type = when {
                    file.name.startsWith("reminder_base_") -> "Base Backup"
                    file.name.startsWith("reminder_delta_") -> "Delta Backup"
                    else -> "Full Backup"
                }
                
                backupList.add(
                    BackupInfo(
                        fileName = file.name,
                        location = "Google Drive",
                        size = (file.size as? Long) ?: 0L,
                        timestamp = (file.modifiedTime?.value as? String)?.toLongOrNull() ?: 0L,
                        isCloud = true,
                        type = type,
                        driveFileId = file.id
                    )
                )
            }
            
            android.util.Log.d("CloudBackupManager", "Found ${backupList.size} backup files in Google Drive")
            backupList
        } catch (e: Exception) {
            android.util.Log.e("CloudBackupManager", "Error getting Google Drive backup history: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun uploadToGoogleDrive(file: File): String? {
        return try {
            android.util.Log.d("CloudBackupManager", "uploadToGoogleDrive called for file: ${file.absolutePath}")
            
            if (driveService == null) {
                android.util.Log.e("CloudBackupManager", "Drive service not initialized")
                return null
            }
            
            // Get or create app folder
            val appFolder = getOrCreateAppFolder()
            if (appFolder == null) {
                android.util.Log.e("CloudBackupManager", "Failed to get or create app folder")
                return null
            }
            
            android.util.Log.d("CloudBackupManager", "App folder ID: ${appFolder.id}")
            
            // Check if file with same name already exists
            val existingFile = findFileInFolder(file.name, appFolder.id)
            
            // Create metadata for the file
            val metadata = com.google.api.services.drive.model.File().apply {
                name = file.name
                parents = listOf(appFolder.id)
                modifiedTime = com.google.api.client.util.DateTime(file.lastModified())
            }
            
            // Prepare file content
            val mediaContent = FileContent("application/json", file)
            
            // Upload or update file
            val uploadedFile = if (existingFile != null) {
                android.util.Log.d("CloudBackupManager", "Updating existing file: ${file.name}")
                driveService!!.files().update(existingFile.id, metadata, mediaContent).execute()
            } else {
                android.util.Log.d("CloudBackupManager", "Uploading new file: ${file.name}")
                driveService!!.files().create(metadata, mediaContent).execute()
            }
            
            android.util.Log.d("CloudBackupManager", "File uploaded successfully: ${uploadedFile.name} (ID: ${uploadedFile.id})")
            uploadedFile.id
        } catch (e: GoogleJsonResponseException) {
            android.util.Log.e("CloudBackupManager", "Google Drive API error: ${e.details.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            android.util.Log.e("CloudBackupManager", "Error uploading to Google Drive: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    private suspend fun downloadFromGoogleDrive(fileId: String): File? {
        return try {
            if (driveService == null) {
                android.util.Log.e("CloudBackupManager", "Drive service not initialized")
                return null
            }
            
            // Get file metadata
            val fileMetadata = driveService!!.files().get(fileId).setFields("name,size").execute()
            
            // Create temporary file for download
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val downloadedFile = File(backupDir, "downloaded_${fileMetadata.name}")
            
            // Download file content
            val outputStream = FileOutputStream(downloadedFile)
            driveService!!.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            
            android.util.Log.d("CloudBackupManager", "File downloaded successfully: ${fileMetadata.name}")
            downloadedFile
        } catch (e: GoogleJsonResponseException) {
            android.util.Log.e("CloudBackupManager", "Google Drive API error: ${e.details.message}")
            null
        } catch (e: Exception) {
            android.util.Log.e("CloudBackupManager", "Error downloading from Google Drive: ${e.message}")
            null
        }
    }
    
    private suspend fun getOrCreateAppFolder(): DriveFile? {
        return try {
            // Search for existing app folder
            val query = "name='$appFolderName' and mimeType='application/vnd.google-apps.folder'"
            val result = driveService!!.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name)")
                .execute()
            
            if (result.files.isNotEmpty()) {
                android.util.Log.d("CloudBackupManager", "Found existing app folder: ${result.files[0].name}")
                return result.files[0]
            }
            
            // Create new folder
            val folderMetadata = com.google.api.services.drive.model.File().apply {
                name = appFolderName
                mimeType = "application/vnd.google-apps.folder"
            }
            
            val folder = driveService!!.files().create(folderMetadata)
                .setFields("id,name")
                .execute()
                
            android.util.Log.d("CloudBackupManager", "Created new app folder: ${folder.name}")
            folder
        } catch (e: Exception) {
            android.util.Log.e("CloudBackupManager", "Error getting/creating app folder: ${e.message}")
            null
        }
    }
    
    private suspend fun findFileInFolder(fileName: String, folderId: String): DriveFile? {
        return try {
            val query = "name='$fileName' and parents in '$folderId'"
            val result = driveService!!.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name)")
                .execute()
                
            if (result.files.isNotEmpty()) {
                result.files[0]
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudBackupManager", "Error finding file: ${e.message}")
            null
        }
    }
    
    // Smart Local Backup with delta logic
    suspend fun smartBackupToLocal(reminders: List<Reminder>): Result<String> {
        return try {
            _backupStatus.value = BackupStatus.BackingUp
            
            val metadata = loadBackupMetadata()
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val result = if (metadata.needsFullBackup()) {
                // Create full backup
                createFullBackup(reminders, backupDir, metadata)
            } else {
                // Create delta backup
                createDeltaBackup(reminders, backupDir, metadata)
            }
            
            _backupStatus.value = BackupStatus.Success
            result
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error
            Result.failure(e)
        }
    }
    
    // Legacy Local Backup (for when cloud is not available)
    suspend fun backupToLocal(reminders: List<Reminder>): Result<String> {
        return try {
            _backupStatus.value = BackupStatus.BackingUp
            val backupData = createBackupData(reminders)
            val fileName = "reminder_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val backupFile = File(backupDir, fileName)
            FileOutputStream(backupFile).use { fos ->
                fos.write(backupData.toByteArray())
            }
            
            _backupStatus.value = BackupStatus.Success
            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error
            Result.failure(e)
        }
    }
    
    private suspend fun createFullBackup(
        reminders: List<Reminder>,
        backupDir: File,
        metadata: BackupMetadata
    ): Result<String> {
        val fileName = "reminder_base_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.json"
        val backupFile = File(backupDir, fileName)
        
        val backupData = createBackupData(reminders)
        FileOutputStream(backupFile).use { fos ->
            fos.write(backupData.toByteArray())
        }
        
        // Update metadata
        val newMetadata = metadata.copy(
            lastFullBackup = System.currentTimeMillis(),
            baseBackupFile = fileName,
            deltaFiles = emptyList(),
            totalReminders = reminders.size,
            backupSize = backupFile.length()
        )
        saveBackupMetadata(newMetadata)
        
        return Result.success(backupFile.absolutePath)
    }
    
    private suspend fun createDeltaBackup(
        reminders: List<Reminder>,
        backupDir: File,
        metadata: BackupMetadata
    ): Result<String> {
        // Get previous reminders from base backup
        val previousReminders = loadRemindersFromBackup(File(backupDir, metadata.baseBackupFile))
        
        // Calculate changes
        val changes = calculateDeltaChanges(previousReminders, reminders)
        
        // Create delta file
        val fileName = "reminder_delta_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
        val deltaFile = File(backupDir, fileName)
        
        val deltaData = changes.toJson()
        FileOutputStream(deltaFile).use { fos ->
            fos.write(deltaData.toByteArray())
        }
        
        // Update metadata
        val newDeltaFiles = metadata.deltaFiles + fileName
        val newMetadata = metadata.copy(
            lastDeltaBackup = System.currentTimeMillis(),
            deltaFiles = newDeltaFiles,
            totalReminders = reminders.size,
            backupSize = metadata.backupSize + deltaFile.length()
        )
        saveBackupMetadata(newMetadata)
        
        return Result.success(deltaFile.absolutePath)
    }
    
    private fun calculateDeltaChanges(
        previousReminders: List<Reminder>,
        currentReminders: List<Reminder>
    ): DeltaChanges {
        val previousMap = previousReminders.associateBy { it.id }
        val currentMap = currentReminders.associateBy { it.id }
        
        val added = currentReminders.filter { !previousMap.containsKey(it.id) }
        val modified = currentReminders.filter { current ->
            previousMap.containsKey(current.id) && previousMap[current.id] != current
        }
        val deleted = previousReminders.filter { !currentMap.containsKey(it.id) }.map { it.id }
        
        return DeltaChanges(added, modified, deleted)
    }
    
    private suspend fun loadRemindersFromBackup(backupFile: File): List<Reminder> {
        return if (backupFile.exists()) {
            val backupData = backupFile.readText()
            parseBackupData(backupData)
        } else {
            emptyList()
        }
    }
    
    private fun loadBackupMetadata(): BackupMetadata {
        return if (metadataFile.exists()) {
            try {
                BackupMetadata.fromJson(metadataFile.readText())
            } catch (e: Exception) {
                BackupMetadata(deviceId = deviceId)
            }
        } else {
            BackupMetadata(deviceId = deviceId)
        }
    }
    
    private fun saveBackupMetadata(metadata: BackupMetadata) {
        metadataFile.writeText(metadata.toJson())
    }
    
    // Smart restore from base + delta files
    suspend fun smartRestoreFromLocal(): Result<List<Reminder>> {
        return try {
            _restoreStatus.value = RestoreStatus.Restoring
            val metadata = loadBackupMetadata()
            
            if (metadata.baseBackupFile.isEmpty()) {
                _restoreStatus.value = RestoreStatus.NoBackup
                return Result.failure(Exception("No base backup found"))
            }
            
            val backupDir = File(context.filesDir, "backups")
            val baseBackupFile = File(backupDir, metadata.baseBackupFile)
            
            if (!baseBackupFile.exists()) {
                _restoreStatus.value = RestoreStatus.NoBackup
                return Result.failure(Exception("Base backup file not found"))
            }
            
            // Load base reminders
            var reminders = loadRemindersFromBackup(baseBackupFile)
            
            // Apply delta changes in sequence
            metadata.deltaFiles.forEach { deltaFileName ->
                val deltaFile = File(backupDir, deltaFileName)
                if (deltaFile.exists()) {
                    val deltaData = deltaFile.readText()
                    val changes = com.reminder.app.data.DeltaChanges.fromJson(deltaData)
                    reminders = applyDeltaChanges(reminders, changes)
                }
            }
            
            _restoreStatus.value = RestoreStatus.Success
            Result.success(reminders)
        } catch (e: Exception) {
            _restoreStatus.value = RestoreStatus.Error
            Result.failure(e)
        }
    }
    
    private fun applyDeltaChanges(
        currentReminders: List<Reminder>,
        changes: DeltaChanges
    ): List<Reminder> {
        val reminderMap = currentReminders.associateBy { it.id }.toMutableMap()
        
        // Remove deleted reminders
        changes.deleted.forEach { id -> reminderMap.remove(id) }
        
        // Add new reminders
        changes.added.forEach { reminder -> reminderMap[reminder.id] = reminder }
        
        // Update modified reminders
        changes.modified.forEach { reminder -> reminderMap[reminder.id] = reminder }
        
        return reminderMap.values.toList()
    }
    
    // Restore from local backup
    suspend fun restoreFromLocal(): Result<List<Reminder>> {
        return try {
            _restoreStatus.value = RestoreStatus.Restoring
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                _restoreStatus.value = RestoreStatus.NoBackup
                return Result.failure(Exception("No backup directory found"))
            }
            
            val backupFiles = backupDir.listFiles { file -> 
                file.name.startsWith("reminder_backup_") && file.name.endsWith(".json") 
            }?.sortedByDescending { it.lastModified() }
            
            if (backupFiles.isNullOrEmpty()) {
                _restoreStatus.value = RestoreStatus.NoBackup
                return Result.failure(Exception("No local backup files found"))
            }
            
            val latestBackup = backupFiles[0]
            val backupData = latestBackup.readText()
            val reminders = parseBackupData(backupData)
            
            _restoreStatus.value = RestoreStatus.Success
            Result.success(reminders)
        } catch (e: Exception) {
            _restoreStatus.value = RestoreStatus.Error
            Result.failure(e)
        }
    }
    
    // Get backup history (includes both local and cloud backups)
    suspend fun getBackupHistory(): List<BackupInfo> {
        val backupList = mutableListOf<BackupInfo>()
        
        // Local backups
        try {
            val backupDir = File(context.filesDir, "backups")
            backupDir.listFiles()?.forEach { file ->
                if ((file.name.startsWith("reminder_backup_") ||
                     file.name.startsWith("reminder_base_") ||
                     file.name.startsWith("reminder_delta_")) &&
                     file.name.endsWith(".json")) {
                    
                    val type = when {
                        file.name.startsWith("reminder_base_") -> "Base Backup"
                        file.name.startsWith("reminder_delta_") -> "Delta Backup"
                        else -> "Full Backup"
                    }
                    
                    backupList.add(
                        BackupInfo(
                            fileName = file.name,
                            location = "Local",
                            size = file.length(),
                            timestamp = file.lastModified(),
                            isCloud = false,
                            type = type
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore local backup errors
        }
        
        // Google Drive backups
        try {
            val cloudBackups = getGoogleDriveBackupHistory()
            backupList.addAll(cloudBackups)
        } catch (e: Exception) {
            // Ignore cloud backup errors
        }
        
        return backupList.sortedByDescending { it.timestamp }
    }
    
    // Get backup metadata for UI display
    fun getBackupMetadata(): BackupMetadata {
        return loadBackupMetadata()
    }
    
    data class BackupInfo(
        val fileName: String,
        val location: String,
        val size: Long,
        val timestamp: Long,
        val isCloud: Boolean,
        val type: String = "Full Backup",
        val driveFileId: String? = null
    )
    
    // Private helper methods
    private fun createBackupData(reminders: List<Reminder>): String {
        val jsonArray = JSONArray()
        reminders.forEach { reminder ->
            val jsonObject = org.json.JSONObject().apply {
                put("id", reminder.id)
                put("content", reminder.content)
                put("category", reminder.category)
                put("importance", reminder.importance)
                put("reminderTime", reminder.reminderTime)
                put("whenDay", reminder.whenDay)
                put("whenTime", reminder.whenTime)
                put("repeatType", reminder.repeatType)
                put("repeatInterval", reminder.repeatInterval)
                put("isActive", reminder.isActive)
                put("voiceInput", reminder.voiceInput)
                put("isProcessed", reminder.isProcessed)
                put("triggerPoints", reminder.triggerPoints)
                put("repeatPattern", reminder.repeatPattern)
                put("alertConfig", reminder.alertConfig)
                put("alertLevel", reminder.alertLevel)
                put("createdAt", reminder.createdAt)
            }
            jsonArray.put(jsonObject)
        }
        
        // Add metadata
        val backupObject = org.json.JSONObject().apply {
            put("version", "1.0")
            put("exportedAt", System.currentTimeMillis())
            put("exportedBy", "Reminder App")
            put("reminders", jsonArray)
        }
        
        return backupObject.toString(2)
    }
    
    private fun parseBackupData(backupData: String): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        val backupObject = org.json.JSONObject(backupData)
        val jsonArray = backupObject.getJSONArray("reminders")
        
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val reminder = Reminder(
                id = jsonObject.optInt("id", 0),
                content = jsonObject.optString("content", ""),
                category = jsonObject.optString("category", "Personal"),
                importance = jsonObject.optInt("importance", 5),
                reminderTime = jsonObject.optLong("reminderTime", System.currentTimeMillis()),
                whenDay = if (jsonObject.has("whenDay")) jsonObject.optString("whenDay") else null,
                whenTime = if (jsonObject.has("whenTime")) jsonObject.optString("whenTime") else null,
                repeatType = jsonObject.optString("repeatType", "none"),
                repeatInterval = jsonObject.optInt("repeatInterval", 1),
                isActive = jsonObject.optBoolean("isActive", true),
                voiceInput = if (jsonObject.has("voiceInput")) jsonObject.optString("voiceInput") else null,
                isProcessed = jsonObject.optBoolean("isProcessed", false),
                triggerPoints = if (jsonObject.has("triggerPoints")) jsonObject.optString("triggerPoints") else null,
                repeatPattern = if (jsonObject.has("repeatPattern")) jsonObject.optString("repeatPattern") else null,
                alertConfig = if (jsonObject.has("alertConfig")) jsonObject.optString("alertConfig") else null,
                alertLevel = jsonObject.optString("alertLevel", "LOW"),
                createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
            )
            reminders.add(reminder)
        }
        
        return reminders
    }
}

// Worker for automatic smart backup (supports both local and cloud)
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val cloudBackupManager = CloudBackupManager(applicationContext)
            
            // Get reminders from database
            val database = com.reminder.app.data.ReminderDatabase.getDatabase(applicationContext)
            val reminders = database.reminderDao().getAllRemindersSync()
            
            // Try cloud backup first if signed in, otherwise local backup
            val result = if (cloudBackupManager.isSignedIn()) {
                cloudBackupManager.backupToGoogleDrive(reminders)
            } else {
                cloudBackupManager.smartBackupToLocal(reminders)
            }
            
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}