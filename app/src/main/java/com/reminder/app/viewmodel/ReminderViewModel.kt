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