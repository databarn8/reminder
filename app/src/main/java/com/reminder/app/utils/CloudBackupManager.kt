package com.reminder.app.utils

import android.content.Context
import androidx.work.*
import com.reminder.app.data.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CloudBackupManager(private val context: Context) {
    
    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()
    
    private val _restoreStatus = MutableStateFlow<RestoreStatus>(RestoreStatus.Idle)
    val restoreStatus: StateFlow<RestoreStatus> = _restoreStatus.asStateFlow()
    
    enum class BackupStatus {
        Idle, BackingUp, Success, Error, NoInternet
    }
    
    enum class RestoreStatus {
        Idle, Restoring, Success, Error, NoBackup, NoInternet
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
    
    // Local Backup (for when cloud is not available)
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
    
    // Get backup history
    suspend fun getBackupHistory(): List<BackupInfo> {
        val backupList = mutableListOf<BackupInfo>()
        
        // Local backups
        try {
            val backupDir = File(context.filesDir, "backups")
            backupDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("reminder_backup_") && file.name.endsWith(".json")) {
                    backupList.add(
                        BackupInfo(
                            fileName = file.name,
                            location = "Local",
                            size = file.length(),
                            timestamp = file.lastModified(),
                            isCloud = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore local backup errors
        }
        
        return backupList.sortedByDescending { it.timestamp }
    }
    
    data class BackupInfo(
        val fileName: String,
        val location: String,
        val size: Long,
        val timestamp: Long,
        val isCloud: Boolean
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

// Worker for automatic backup
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
            
            // Try local backup
            val result = cloudBackupManager.backupToLocal(reminders)
            
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