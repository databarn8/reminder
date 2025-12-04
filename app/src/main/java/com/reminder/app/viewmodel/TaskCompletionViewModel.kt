package com.reminder.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.data.Reminder
import com.reminder.app.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskCompletionViewModel(
    private val repository: ReminderRepository,
    application: Application
) : AndroidViewModel(application) {
    
    private val _completedReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val completedReminders: StateFlow<List<Reminder>> = _completedReminders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _selectedReminders = MutableStateFlow<Set<Int>>(emptySet())
    val selectedReminders: StateFlow<Set<Int>> = _selectedReminders.asStateFlow()
    
    init {
        loadCompletedReminders()
    }
    
    fun loadCompletedReminders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val completed = repository.getCompletedRemindersOnce()
                _completedReminders.value = completed
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load completed reminders: ${e.message}"
                _isLoading.value = false
            }
        }
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
        _selectedReminders.value = _completedReminders.value.map { it.id }.toSet()
    }
    
    fun clearSelection() {
        _selectedReminders.value = emptySet()
    }
    
    fun restoreSelected(onReminderRestored: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val selectedIds = _selectedReminders.value.toList()
                selectedIds.forEach { id ->
                    repository.unmarkReminderAsCompleted(id)
                }
                clearSelection()
                loadCompletedReminders()
                onReminderRestored()
                _errorMessage.value = "Restored ${selectedIds.size} reminders"
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore reminders: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun unmarkReminderAsCompleted(id: Int, onReminderRestored: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.unmarkReminderAsCompleted(id)
                // Add a small delay to ensure database update completes before navigation
                kotlinx.coroutines.delay(100)
                loadCompletedReminders()
                onReminderRestored()
                _errorMessage.value = "Reminder restored successfully"
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore reminder: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}