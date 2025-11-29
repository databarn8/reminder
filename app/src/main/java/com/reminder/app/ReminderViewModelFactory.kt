package com.reminder.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.reminder.app.repository.ReminderRepository
import com.reminder.app.viewmodel.ReminderViewModel
import com.reminder.app.viewmodel.ArchiveRestoreViewModel
import com.reminder.app.viewmodel.TaskCompletionViewModel

class ReminderViewModelFactory(
    private val repository: ReminderRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(repository, application) as T
        }
        if (modelClass.isAssignableFrom(ArchiveRestoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArchiveRestoreViewModel(repository, application) as T
        }
        if (modelClass.isAssignableFrom(TaskCompletionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskCompletionViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}