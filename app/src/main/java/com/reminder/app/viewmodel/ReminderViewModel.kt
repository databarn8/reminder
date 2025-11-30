package com.reminder.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.data.Reminder
import com.reminder.app.repository.ReminderRepository
import com.reminder.app.utils.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val repository: ReminderRepository,
    private val application: Application
) : AndroidViewModel(application) {
    
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadReminders()
        // Schedule alarms for existing reminders once
        scheduleAllRemindersOnce()
    }

    private fun loadReminders() {
        viewModelScope.launch {
            repository.getAllReminders().collect { reminderList ->
                _reminders.value = reminderList
            }
        }
    }
    
    fun refreshReminders() {
        viewModelScope.launch {
            repository.getAllReminders().collect { reminderList ->
                _reminders.value = reminderList
            }
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.insertReminder(reminder)
                // Schedule alarm for the new reminder
                NotificationScheduler.scheduleReminder(application, reminder)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("ReminderViewModel", "Updating reminder in database: id=${reminder.id}, reminderTime=${reminder.reminderTime}, whenDay=${reminder.whenDay}, whenTime=${reminder.whenTime}")
                repository.updateReminder(reminder)
                // Cancel old alarm and schedule new one for updated reminder
                NotificationScheduler.cancelReminder(application, reminder.id)
                NotificationScheduler.scheduleReminder(application, reminder)
                _errorMessage.value = null
                android.util.Log.d("ReminderViewModel", "Successfully updated reminder: id=${reminder.id}")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update reminder: ${e.message}"
                android.util.Log.e("ReminderViewModel", "Error updating reminder: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteReminder(reminder)
                // Cancel alarm for the deleted reminder
                NotificationScheduler.cancelReminder(application, reminder.id)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getRemindersByCategory(category: String) {
        viewModelScope.launch {
            repository.getRemindersByCategory(category).collect { reminderList ->
                _reminders.value = reminderList
            }
        }
    }

    fun getRemindersByImportance(minImportance: Int) {
        viewModelScope.launch {
            repository.getRemindersByImportance(minImportance).collect { reminderList ->
                _reminders.value = reminderList
            }
        }
    }

    // Archive-related methods
    fun archiveReminder(id: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.archiveReminder(id)
                // Cancel alarm for archived reminder
                NotificationScheduler.cancelReminder(application, id)
                _errorMessage.value = "Reminder archived successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to archive reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unarchiveReminder(id: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.unarchiveReminder(id)
                // Get the reminder to reschedule its alarm
                val reminder = repository.getReminderById(id)
                if (reminder != null && reminder.reminderTime > System.currentTimeMillis()) {
                    NotificationScheduler.scheduleReminder(application, reminder)
                }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to unarchive reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun softDeleteReminder(id: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.softDeleteReminder(id)
                // Cancel alarm for soft-deleted reminder
                NotificationScheduler.cancelReminder(application, id)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreReminder(id: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.restoreReminder(id)
                // Get the reminder to reschedule its alarm
                val reminder = repository.getReminderById(id)
                if (reminder != null && reminder.reminderTime > System.currentTimeMillis()) {
                    NotificationScheduler.scheduleReminder(application, reminder)
                }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getArchivedReminders(): List<Reminder> {
        return repository.getArchivedRemindersOnce()
    }

    suspend fun getDeletedReminders(): List<Reminder> {
        return repository.getDeletedRemindersOnce()
    }

    suspend fun getRemindersOlderThan(weeks: Int): List<Reminder> {
        return repository.getRemindersOlderThan(weeks)
    }

    fun purgeSelectedReminders(ids: List<Int>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.purgeSelectedReminders(ids)
                // Cancel alarms for all purged reminders
                ids.forEach { id ->
                    NotificationScheduler.cancelReminder(application, id)
                }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to purge reminders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Task completion related methods
    fun markReminderAsCompleted(id: Int, completionNotes: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.markReminderAsCompleted(id, completionNotes)
                // Cancel alarm for completed reminder
                NotificationScheduler.cancelReminder(application, id)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to mark reminder as completed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markReminderAsCompletedWithArchive(id: Int, completionNotes: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.markReminderAsCompletedWithArchive(id, completionNotes)
                // Cancel alarm for completed and archived reminder
                NotificationScheduler.cancelReminder(application, id)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to mark reminder as completed and archived: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unmarkReminderAsCompleted(id: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.unmarkReminderAsCompleted(id)
                // Get the reminder to reschedule its alarm if it's still in the future
                val reminder = repository.getReminderById(id)
                if (reminder != null && reminder.reminderTime > System.currentTimeMillis()) {
                    NotificationScheduler.scheduleReminder(application, reminder)
                }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to unmark reminder as completed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getCompletedReminders(): List<Reminder> {
        return repository.getCompletedRemindersOnce()
    }

    suspend fun getReminderById(id: Int): Reminder? {
        val reminder = repository.getReminderById(id)
        android.util.Log.d("ReminderViewModel", "Retrieved reminder by id=$id: reminderTime=${reminder?.reminderTime}, whenDay=${reminder?.whenDay}, whenTime=${reminder?.whenTime}")
        return reminder
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Schedule alarms for all existing reminders once (called on app startup)
     */
    private fun scheduleAllRemindersOnce() {
        viewModelScope.launch {
            try {
                // Use first() instead of collect() to get reminders only once
                val reminderList = repository.getAllRemindersOnce()
                reminderList.forEach { reminder ->
                    // Only schedule future reminders
                    if (reminder.reminderTime > System.currentTimeMillis()) {
                        android.util.Log.d("ReminderViewModel", "Scheduling alarm for existing reminder: ${reminder.content} at ${java.util.Date(reminder.reminderTime)}")
                        NotificationScheduler.scheduleReminder(application, reminder)
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash app
                android.util.Log.e("ReminderViewModel", "Error scheduling existing reminders: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}