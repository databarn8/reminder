package com.reminder.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.data.Reminder
import com.reminder.app.viewmodel.TaskCompletionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCompletionScreen(
    viewModel: TaskCompletionViewModel,
    onBack: () -> Unit = {},
    onReminderRestored: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    val completedReminders by viewModel.completedReminders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedReminders by viewModel.selectedReminders.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    
    LaunchedEffect(Unit) {
        viewModel.loadCompletedReminders()
    }
    
    // Show error message if present
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearError()
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar with home icon and stats
        TopAppBar(
            title = {
                // Show task count in title area
                Text(
                    text = "${completedReminders.size} tasks",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            navigationIcon = {
                Row {
                    // Home icon - first position
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = {
                // No actions needed
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Selection controls
        if (completedReminders.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.selectAll() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select All")
                }
                
                Button(
                    onClick = { viewModel.clearSelection() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("Clear Selection")
                }
                
                if (selectedReminders.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.restoreSelected(onReminderRestored) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Restore Selected (${selectedReminders.size})")
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1
            )
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (completedReminders.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "No completed tasks yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Mark reminders as complete to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // List of completed reminders
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(completedReminders) { reminder ->
                    CompletedTaskCard(
                        reminder = reminder,
                        isSelected = selectedReminders.contains(reminder.id),
                        dateFormat = dateFormat,
                        onSelect = { viewModel.toggleSelection(reminder.id) },
                        onRestore = { viewModel.unmarkReminderAsCompleted(reminder.id, onReminderRestored) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTaskCard(
    reminder: Reminder,
    isSelected: Boolean,
    dateFormat: SimpleDateFormat,
    onSelect: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelect() }
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = reminder.content,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "Completed: ${dateFormat.format(Date(reminder.completedDate ?: System.currentTimeMillis()))}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                
                Text(
                    text = "Created: ${dateFormat.format(Date(reminder.createdAt))}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            // Restore button
            IconButton(
                onClick = onRestore
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Int
) {
    Divider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        thickness = thickness.dp
    )
}