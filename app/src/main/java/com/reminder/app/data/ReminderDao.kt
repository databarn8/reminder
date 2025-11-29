package com.reminder.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isArchived = 0 AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Query("SELECT * FROM reminders WHERE category = :category AND isArchived = 0 AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getRemindersByCategory(category: String): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE importance >= :minImportance AND isArchived = 0 AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getRemindersByImportance(minImportance: Int): Flow<List<Reminder>>

    @Insert
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
    
    @Query("SELECT * FROM reminders WHERE isArchived = 0 AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllRemindersSync(): List<Reminder>
    
    // Archive-related queries
    @Query("SELECT * FROM reminders WHERE isArchived = 1 AND isDeleted = 0 ORDER BY archivedDate DESC")
    fun getArchivedReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE isDeleted = 1 ORDER BY deletedDate DESC")
    fun getDeletedReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE isArchived = 1 AND isDeleted = 0 ORDER BY archivedDate DESC")
    suspend fun getArchivedRemindersSync(): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE isDeleted = 1 ORDER BY deletedDate DESC")
    suspend fun getDeletedRemindersSync(): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE (isArchived = 1 OR isDeleted = 1) AND (archivedDate < :cutoffDate OR deletedDate < :cutoffDate)")
    suspend fun getRemindersOlderThan(cutoffDate: Long): List<Reminder>
    
    @Query("UPDATE reminders SET isArchived = 1, archivedDate = :timestamp WHERE id = :id")
    suspend fun archiveReminder(id: Int, timestamp: Long)
    
    @Query("UPDATE reminders SET isArchived = 0, archivedDate = NULL WHERE id = :id")
    suspend fun unarchiveReminder(id: Int)
    
    @Query("UPDATE reminders SET isDeleted = 1, deletedDate = :timestamp WHERE id = :id")
    suspend fun softDeleteReminder(id: Int, timestamp: Long)
    
    @Query("UPDATE reminders SET isDeleted = 0, deletedDate = NULL WHERE id = :id")
    suspend fun restoreReminder(id: Int)
    
    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun purgeReminders(ids: List<Int>)
}