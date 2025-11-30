package com.reminder.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.data.Reminder
import com.reminder.app.repository.ReminderRepository
import com.reminder.app.utils.CloudBackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArchiveRestoreViewModel(
    private val repository: ReminderRepository,
    private val application: Application
) : AndroidViewModel(application) {
    
    private val _archivedReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val archivedReminders: StateFlow<List<Reminder>> = _archivedReminders.asStateFlow()
    
    private val _allRemindersForPurge = MutableStateFlow<Pair<List<Reminder>, List<Reminder>>>(Pair(emptyList(), emptyList()))
    val allRemindersForPurge: StateFlow<Pair<List<Reminder>, List<Reminder>>> = _allRemindersForPurge.asStateFlow()
    
    private val _deletedReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val deletedReminders: StateFlow<List<Reminder>> = _deletedReminders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _selectedReminders = MutableStateFlow<Set<Int>>(emptySet())
    val selectedReminders: StateFlow<Set<Int>> = _selectedReminders.asStateFlow()
    
    private val _backupStatus = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val backupStatus: StateFlow<Map<Int, Boolean>> = _backupStatus.asStateFlow()
    
    init {
        loadArchivedReminders()
        loadDeletedReminders()
    }
    
    private fun loadArchivedReminders() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val archived = repository.getArchivedRemindersOnce()
                _archivedReminders.value = archived
                checkBackupStatus(archived)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load archived reminders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadDeletedReminders() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val deleted = repository.getDeletedRemindersOnce()
                _deletedReminders.value = deleted
                checkBackupStatus(deleted)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load deleted reminders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun checkBackupStatus(reminders: List<Reminder>) {
        viewModelScope.launch {
            try {
                val backupManager = CloudBackupManager(application)
                val statusMap = mutableMapOf<Int, Boolean>()
                
                reminders.forEach { reminder ->
                    statusMap[reminder.id] = backupManager.doesBackupExist(reminder.id)
                }
                
                _backupStatus.value = statusMap
            } catch (e: Exception) {
                // If we can't check backup status, assume no backup exists
                val statusMap = reminders.associate { it.id to false }
                _backupStatus.value = statusMap
            }
        }
    }
    
    fun refreshData() {
        loadArchivedReminders()
        loadDeletedReminders()
    }
    
    fun refreshData(onReminderRestored: () -> Unit = {}) {
        loadArchivedReminders()
        loadDeletedReminders()
        onReminderRestored()
    }
    
    fun toggleSelection(reminderId: Int) {
        val currentSelection = _selectedReminders.value.toMutableSet()
        if (currentSelection.contains(reminderId)) {
            currentSelection.remove(reminderId)
        } else {
            currentSelection.add(reminderId)
        }
        _selectedReminders.value = currentSelection
    }
    
    fun selectAll() {
        val allIds = (_archivedReminders.value + _deletedReminders.value).map { it.id }
        _selectedReminders.value = allIds.toSet()
    }
    
    fun clearSelection() {
        _selectedReminders.value = emptySet()
    }
    
    fun restoreSelected() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _selectedReminders.value.forEach { id ->
                    repository.restoreReminder(id)
                }
                clearSelection()
                loadArchivedReminders()
                loadDeletedReminders()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore reminders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun unarchiveSelected() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _selectedReminders.value.forEach { id ->
                    repository.unarchiveReminder(id)
                }
                clearSelection()
                loadArchivedReminders()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to unarchive reminders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun unarchiveSingle(reminderId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.unarchiveReminder(reminderId)
                loadArchivedReminders()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to unarchive reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun purgeOldReminders(weeks: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val oldReminders = repository.getRemindersOlderThan(weeks)
                val reminderIds = oldReminders.map { it.id }
                
                // Check if all selected reminders have backups
                val backupManager = CloudBackupManager(application)
                val remindersWithoutBackup = mutableListOf<Int>()
                
                oldReminders.forEach { reminder ->
                    if (!backupManager.doesBackupExist(reminder.id)) {
                        remindersWithoutBackup.add(reminder.id)
                    }
                }
                
                // Create backups for reminders without existing backups
                if (remindersWithoutBackup.isNotEmpty()) {
                    remindersWithoutBackup.forEach { id ->
                        val reminder = repository.getReminderById(id)
                        if (reminder != null) {
                            backupManager.createBackup(reminder)
                        }
                    }
                }
                
                // Purge the reminders
                repository.purgeSelectedReminders(reminderIds)
                
                loadArchivedReminders()
                loadDeletedReminders()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to purge old reminders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun purgeSelected() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val backupManager = CloudBackupManager(application)
                val remindersWithoutBackup = mutableListOf<Int>()
                
                // Check if all selected reminders have backups
                _selectedReminders.value.forEach { id ->
                    if (!_backupStatus.value[id]!!) {
                        remindersWithoutBackup.add(id)
                    }
                }
                
                // Create backups for reminders without existing backups
                if (remindersWithoutBackup.isNotEmpty()) {
                    remindersWithoutBackup.forEach { id ->
                        val reminder = repository.getReminderById(id)
                        if (reminder != null) {
                            backupManager.createBackup(reminder)
                        }
                    }
                }
                
                // Purge the selected reminders
                repository.purgeSelectedReminders(_selectedReminders.value.toList())
                
                clearSelection()
                loadArchivedReminders()
                loadDeletedReminders()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to purge selected reminders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}