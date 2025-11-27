package com.reminder.app.data

import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Metadata for smart backup system
 * Tracks base backups and delta files for efficient incremental backups
 */
data class BackupMetadata(
    val deviceId: String,
    val lastFullBackup: Long = 0,
    val lastDeltaBackup: Long = 0,
    val baseBackupFile: String = "",
    val deltaFiles: List<String> = emptyList(),
    val totalReminders: Int = 0,
    val backupSize: Long = 0,
    val version: String = "2.0"
) {
    companion object {
        fun fromJson(jsonString: String): BackupMetadata {
            val json = JSONObject(jsonString)
            return BackupMetadata(
                deviceId = json.optString("deviceId", ""),
                lastFullBackup = json.optLong("lastFullBackup", 0),
                lastDeltaBackup = json.optLong("lastDeltaBackup", 0),
                baseBackupFile = json.optString("baseBackupFile", ""),
                deltaFiles = json.optJSONArray("deltaFiles")?.let { array ->
                    (0 until array.length()).map { array.optString(it, "") }
                } ?: emptyList(),
                totalReminders = json.optInt("totalReminders", 0),
                backupSize = json.optLong("backupSize", 0),
                version = json.optString("version", "2.0")
            )
        }
    }
    
    fun toJson(): String {
        return JSONObject().apply {
            put("deviceId", deviceId)
            put("lastFullBackup", lastFullBackup)
            put("lastDeltaBackup", lastDeltaBackup)
            put("baseBackupFile", baseBackupFile)
            put("deltaFiles", org.json.JSONArray(deltaFiles))
            put("totalReminders", totalReminders)
            put("backupSize", backupSize)
            put("version", version)
        }.toString(2)
    }
    
    fun needsFullBackup(): Boolean {
        val now = System.currentTimeMillis()
        val daysSinceLastFull = (now - lastFullBackup) / (1000 * 60 * 60 * 24)
        return daysSinceLastFull >= 7 || baseBackupFile.isEmpty()
    }
    
    fun needsDeltaBackup(): Boolean {
        val now = System.currentTimeMillis()
        val hoursSinceLastDelta = (now - lastDeltaBackup) / (1000 * 60 * 60)
        return hoursSinceLastDelta >= 6
    }
}

/**
 * Represents changes in a delta backup
 */
data class DeltaChanges(
    val added: List<Reminder>,
    val modified: List<Reminder>,
    val deleted: List<Int>
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("version", "2.0")
            put("type", "delta")
            put("timestamp", System.currentTimeMillis())
            put("changes", JSONObject().apply {
                put("added", org.json.JSONArray(added.map { createReminderJson(it) }))
                put("modified", org.json.JSONArray(modified.map { createReminderJson(it) }))
                put("deleted", org.json.JSONArray(deleted))
            })
        }.toString(2)
    }
    
    companion object {
        fun fromJson(jsonString: String): DeltaChanges {
            val json = JSONObject(jsonString)
            val changes = json.getJSONObject("changes")
            
            val added = (0 until changes.getJSONArray("added").length()).map { index ->
                createReminderFromJson(changes.getJSONArray("added").getJSONObject(index))
            }
            
            val modified = (0 until changes.getJSONArray("modified").length()).map { index ->
                createReminderFromJson(changes.getJSONArray("modified").getJSONObject(index))
            }
            
            val deleted = (0 until changes.getJSONArray("deleted").length()).map { index ->
                changes.getJSONArray("deleted").getInt(index)
            }
            
            return DeltaChanges(added, modified, deleted)
        }
    }
}

/**
 * Helper function to convert Reminder to JSON
 */
private fun createReminderJson(reminder: Reminder): JSONObject {
    return JSONObject().apply {
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
}

/**
 * Helper function to create Reminder from JSON
 */
private fun createReminderFromJson(json: JSONObject): Reminder {
    return Reminder(
        id = json.optInt("id", 0),
        content = json.optString("content", ""),
        category = json.optString("category", "Personal"),
        importance = json.optInt("importance", 5),
        reminderTime = json.optLong("reminderTime", System.currentTimeMillis()),
        whenDay = if (json.has("whenDay")) json.optString("whenDay") else null,
        whenTime = if (json.has("whenTime")) json.optString("whenTime") else null,
        repeatType = json.optString("repeatType", "none"),
        repeatInterval = json.optInt("repeatInterval", 1),
        isActive = json.optBoolean("isActive", true),
        voiceInput = if (json.has("voiceInput")) json.optString("voiceInput") else null,
        isProcessed = json.optBoolean("isProcessed", false),
        triggerPoints = if (json.has("triggerPoints")) json.optString("triggerPoints") else null,
        repeatPattern = if (json.has("repeatPattern")) json.optString("repeatPattern") else null,
        alertConfig = if (json.has("alertConfig")) json.optString("alertConfig") else null,
        alertLevel = json.optString("alertLevel", "LOW"),
        createdAt = json.optLong("createdAt", System.currentTimeMillis())
    )
}