package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.reminder.app.data.Reminder
import com.reminder.app.data.AlertLevel
import com.reminder.app.utils.EnhancedEmailService
import com.reminder.app.utils.ScreenFlashManager
import com.reminder.app.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.view.WindowManager
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

// Fresh Button Implementation - Simple flash with vibration and system beep
fun performSafeFreshFlash(context: Context) {
    try {
        android.util.Log.d("FreshButton", "Starting simple flash implementation")
        
        // Use ScreenFlashManager with simplified approach
        ScreenFlashManager.triggerFlash(
            context = context,
            flashColor = androidx.compose.ui.graphics.Color.Yellow,
            flashDurationMs = 300, // Short flash duration
            flashCount = 1, // Single flash for simplicity
            intervalMs = 200 // Short interval
        )
        
        android.util.Log.d("FreshButton", "Simple flash completed successfully")
        
    } catch (e: Exception) {
        android.util.Log.e("FreshButton", "Simple flash failed: ${e.message}")
        // Fallback to vibration only if flash fails
        try {
            ScreenFlashManager.triggerVibration(context)
        } catch (e2: Exception) {
            android.util.Log.e("FreshButton", "Fallback vibration also failed: ${e2.message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderViewModel,
    onAddReminder: () -> Unit,
    onReminderClick: (Reminder) -> Unit,
    onEditClick: (Reminder) -> Unit,
    onCalendarClick: () -> Unit = {},
    onEmailClick: (Reminder) -> Unit = {},
    onEmailSettingsClick: () -> Unit = {},
    onAlertSettingsClick: () -> Unit = {},
    onBackupSettingsClick: () -> Unit = {},
    onArchiveRestoreClick: () -> Unit = {},
    onTaskCompletionClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showGlobalSaveConfirmation by remember { mutableStateOf(false) }
    
    // Filter reminders based on search query
    val filteredReminders = remember(searchQuery, reminders) {
        if (searchQuery.isBlank()) {
            reminders
        } else {
            reminders.filter { reminder ->
                reminder.content.contains(searchQuery, ignoreCase = true) ||
                reminder.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Reminders")
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "${filteredReminders.size} found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Gear icon for general settings - first position
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onAlertSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alert Settings"
                        )
                    }
                    // Task Completion button
                    IconButton(onClick = onTaskCompletionClick) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Task Completion",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Archive/Restore button
                    IconButton(onClick = onArchiveRestoreClick) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive/Restore",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Email settings removed - already accessible via main settings
                    IconButton(onClick = onCalendarClick) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar"
                        )
                    }
                    IconButton(onClick = onBackupSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Backup Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminder,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Reminder",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Global save confirmation (snackbar-style)
            if (showGlobalSaveConfirmation) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000) // Show for 2 seconds
                    showGlobalSaveConfirmation = false
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            text = "✓ Alert level saved successfully",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search reminders...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom =16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { /* Handle search action if needed */ }
                ),
                singleLine = true
            )

            // Error message
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Reminders list
            if (filteredReminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No reminders yet" else "No reminders found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "Tap + button to add your first reminder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredReminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onClick = { onReminderClick(reminder) },
                            onEditClick = { onEditClick(reminder) },
                            onDeleteClick = { viewModel.deleteReminder(reminder) },
                            onArchiveClick = { viewModel.archiveReminder(reminder.id) },
                            onEmailClick = { onEmailClick(reminder) },
                            onTaskCompleteClick = { viewModel.markReminderAsCompleted(reminder.id) },
                            onAlertLevelChange = { reminder, level ->
                                val updatedReminder = reminder.copy(
                                    alertLevel = level.name
                                )
                                viewModel.updateReminder(updatedReminder)
                                showGlobalSaveConfirmation = true
                            }
                        )
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onArchiveClick: () -> Unit = {},
    onEmailClick: () -> Unit,
    onTaskCompleteClick: () -> Unit = {},
    onAlertLevelChange: (Reminder, AlertLevel) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    var showSavedConfirmation by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row with action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Row {
                    IconButton(onClick = onEmailClick) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Alert level selector
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alert Level",
                                    tint = when (reminder.getAlertLevelEnum()) {
                                        AlertLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                        AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                                        AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // Only show built-in alert levels
                            listOf(
                                AlertLevel.LOW to "Low",
                                AlertLevel.HIGH to "High",
                                AlertLevel.URGENT to "Urgent"
                            ).forEach { (level, displayName) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = displayName,
                                            color = when (level) {
                                                AlertLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                                AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                                                AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                    },
                                    onClick = {
                                        onAlertLevelChange(reminder, level)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    IconButton(onClick = onTaskCompleteClick) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Mark as Complete",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onArchiveClick) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message content with dynamic font size
            Text(
                text = reminder.content,
                style = if (reminder.content.length > 50) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bottom row with alert level and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alert level display
                Text(
                    text = "Alert: ${when (reminder.getAlertLevelEnum()) {
                        AlertLevel.LOW -> "Low"
                        AlertLevel.HIGH -> "High"
                        AlertLevel.URGENT -> "Urgent"
                    }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (reminder.getAlertLevelEnum()) {
                        AlertLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                        AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                        AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (reminder.reminderTime == 0L) {
                Text(
                    text = "⚠️ Day/Time not specified",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = dateFormat.format(Date(reminder.reminderTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
        }
    }
}
