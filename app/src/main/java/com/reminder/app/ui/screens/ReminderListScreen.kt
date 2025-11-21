package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
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
import com.reminder.app.data.AlertLevelOption
import com.reminder.app.utils.EnhancedEmailService
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
        com.reminder.app.utils.ScreenFlashManager.triggerFlash(
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
            com.reminder.app.utils.ScreenFlashManager.triggerVibration(context)
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
    onAlertSettingsClick: () -> Unit = {}
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
                    IconButton(onClick = onAlertSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alert Settings"
                        )
                    }
                    IconButton(onClick = onEmailSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Settings"
                        )
                    }
                    IconButton(onClick = onCalendarClick) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar"
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
                    .padding(bottom = 16.dp),
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
                            onEmailClick = { onEmailClick(reminder) },
                            onAlertLevelChange = { reminder, level ->
                                val (alertLevelName, customProfileName) = when (level) {
                                    AlertLevel.CUSTOM -> {
                                        // Get the selected custom profile name from the option
                                        val alertLevelConfig = loadAlertLevelConfig(context)
                                        val customProfiles = alertLevelConfig.customProfiles
                                        if (customProfiles.isNotEmpty()) {
                                            // For now, use the first custom profile or keep existing
                                            val profileName = reminder.getCustomProfileNameFromField() ?: customProfiles.keys.first()
                                            Pair(profileName, profileName)
                                        } else {
                                            Pair("Custom", null)
                                        }
                                    }
                                    else -> Pair(level.name, null)
                                }
                                val updatedReminder = reminder.copy(
                                    alertLevel = alertLevelName,
                                    customProfileName = customProfileName
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
    onEmailClick: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
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
                                        AlertLevel.MEDIUM -> MaterialTheme.colorScheme.primary
                                        AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                                        AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                                        AlertLevel.CUSTOM -> MaterialTheme.colorScheme.tertiary
                                    }
                                )
                                // Show custom profile name if selected
                                if (reminder.getAlertLevelEnum() == AlertLevel.CUSTOM) {
                                    val customProfileName = reminder.getCustomProfileNameFromField()
                                    if (!customProfileName.isNullOrBlank()) {
                                        Text(
                                            text = customProfileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // Get custom profiles from alert level config
                            val context = LocalContext.current
                            val alertLevelConfig = loadAlertLevelConfig(context)
                            
                            // First add built-in options
                            AlertLevelOption.getBuiltInOptions().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            color = when (option.level) {
                                                AlertLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                                AlertLevel.MEDIUM -> MaterialTheme.colorScheme.primary
                                                AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                                                AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                                                AlertLevel.CUSTOM -> MaterialTheme.colorScheme.tertiary
                                            }
                                        )
                                    },
                                    onClick = {
                                        onAlertLevelChange(reminder, option.level)
                                        expanded = false
                                    }
                                )
                            }
                            
                            // Then add custom profile options
                            AlertLevelOption.getCustomOptions(alertLevelConfig.customProfiles).forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    },
                                    onClick = {
                                        // For custom profiles, we need to handle the profile name properly
                                        // Update the reminder with the specific custom profile name
                                        val (alertLevelName, customProfileName) = Pair(option.customProfileName ?: option.displayName, option.customProfileName ?: option.displayName)
                                        val updatedReminder = reminder.copy(
                                            alertLevel = alertLevelName,
                                            customProfileName = customProfileName
                                        )
                                        // We need to access the viewModel from the parent component
                                        // For now, just call the onAlertLevelChange callback with CUSTOM
                                        onAlertLevelChange(reminder, AlertLevel.CUSTOM)
                                        expanded = false
                                    }
                                )
                            }
                        }
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
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                // Alert level display
                Text(
                    text = "Alert: ${when (reminder.getAlertLevelEnum()) {
                        AlertLevel.LOW -> "Low"
                        AlertLevel.MEDIUM -> "Medium"
                        AlertLevel.HIGH -> "High"
                        AlertLevel.URGENT -> "Urgent"
                        AlertLevel.CUSTOM -> reminder.getCustomProfileNameFromField() ?: "Custom"
                    }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (reminder.getAlertLevelEnum()) {
                        AlertLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                        AlertLevel.MEDIUM -> MaterialTheme.colorScheme.primary
                        AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                        AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                        AlertLevel.CUSTOM -> MaterialTheme.colorScheme.tertiary
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

// Helper function to load alert level config for custom profiles
private fun loadAlertLevelConfig(context: android.content.Context): com.reminder.app.data.AlertLevelConfig {
    return try {
        val prefs = context.getSharedPreferences("alert_level_config", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("alert_level_config", null)
        if (json != null) {
            com.reminder.app.data.AlertLevelConfig.Companion.fromJson(json)
        } else {
            com.reminder.app.data.AlertLevelConfig() // Default
        }
    } catch (e: Exception) {
        android.util.Log.e("ReminderListScreen", "Error loading alert level config: ${e.message}")
        com.reminder.app.data.AlertLevelConfig() // Default if parsing fails
    }
}