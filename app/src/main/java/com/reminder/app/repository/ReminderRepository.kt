package com.reminder.app.repository

import com.reminder.app.data.Reminder
import com.reminder.app.data.ReminderDao
import kotlinx.coroutines.flow.Flow
class ReminderRepository(
    private val reminderDao: ReminderDao
) {
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()
    
    suspend fun getAllRemindersOnce(): List<Reminder> = reminderDao.getAllRemindersSync()

    suspend fun getReminderById(id: Int): Reminder? = reminderDao.getReminderById(id)

    fun getRemindersByCategory(category: String): Flow<List<Reminder>> = 
        reminderDao.getRemindersByCategory(category)

    fun getRemindersByImportance(minImportance: Int): Flow<List<Reminder>> = 
        reminderDao.getRemindersByImportance(minImportance)

    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)

    suspend fun deleteReminderById(id: Int) = reminderDao.deleteReminderById(id)

    suspend fun deleteAllReminders() = reminderDao.deleteAllReminders()
    
    // Archive-related methods
    fun getArchivedReminders(): Flow<List<Reminder>> = reminderDao.getArchivedReminders()
    
    fun getDeletedReminders(): Flow<List<Reminder>> = reminderDao.getDeletedReminders()
    
    suspend fun getArchivedRemindersOnce(): List<Reminder> = reminderDao.getArchivedRemindersSync()
    
    suspend fun getDeletedRemindersOnce(): List<Reminder> = reminderDao.getDeletedRemindersSync()
    
    suspend fun archiveReminder(id: Int) {
        reminderDao.archiveReminder(id, System.currentTimeMillis())
    }
    
    suspend fun unarchiveReminder(id: Int) {
        reminderDao.unarchiveReminder(id)
    }
    
    suspend fun softDeleteReminder(id: Int) {
        reminderDao.softDeleteReminder(id, System.currentTimeMillis())
    }
    
    suspend fun restoreReminder(id: Int) {
        reminderDao.restoreReminder(id)
    }
    
    suspend fun getRemindersOlderThan(weeks: Int): List<Reminder> {
        val cutoffDate = System.currentTimeMillis() - (weeks * 7 * 24 * 60 * 60 * 1000L)
        return reminderDao.getRemindersOlderThan(cutoffDate)
    }
    
    suspend fun purgeSelectedReminders(ids: List<Int>) {
        reminderDao.purgeReminders(ids)
    }
    
    // Task completion related methods
    fun getCompletedReminders(): Flow<List<Reminder>> = reminderDao.getCompletedReminders()
    
    suspend fun getCompletedRemindersOnce(): List<Reminder> = reminderDao.getCompletedRemindersSync()
    
    suspend fun markReminderAsCompleted(id: Int, completionNotes: String? = null) {
        reminderDao.markReminderAsCompleted(id, System.currentTimeMillis(), completionNotes)
    }
    
    suspend fun unmarkReminderAsCompleted(id: Int) {
        reminderDao.unmarkReminderAsCompleted(id)
    }
    
    suspend fun restoreCompletedReminder(id: Int) {
        reminderDao.unmarkReminderAsCompleted(id)
        reminderDao.unarchiveReminder(id)
    }
    
    suspend fun markReminderAsCompletedWithArchive(id: Int, completionNotes: String? = null) {
        reminderDao.markReminderAsCompletedWithArchive(id, System.currentTimeMillis(), completionNotes)
    }
}