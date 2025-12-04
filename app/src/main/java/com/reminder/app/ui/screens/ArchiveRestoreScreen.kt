package com.reminder.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.data.Reminder
import com.reminder.app.ui.theme.ReminderAppTheme
import com.reminder.app.viewmodel.ArchiveRestoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveRestoreScreen(
    viewModel: ArchiveRestoreViewModel = viewModel(),
    onReminderRestored: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val archivedReminders by viewModel.archivedReminders.collectAsState()
    val deletedReminders by viewModel.deletedReminders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedReminders by viewModel.selectedReminders.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Archived", "Deleted")
    
    var showPurgeDialog by remember { mutableStateOf(false) }
    var purgeOption by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.refreshData(onReminderRestored)
    }
    
    // Show error message if present
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Handle error display (could show a snackbar or dialog)
            viewModel.clearError()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar with back arrow, home icon and tabs
        TopAppBar(
            title = {
                // Empty title - tabs will show the current section
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home icon - consistent with other screens
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Tab navigation
                    tabs.forEachIndexed { index, title ->
                        FilterChip(
                            onClick = { selectedTab = index },
                            label = { Text(title) },
                            selected = selectedTab == index,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        )
        
        // Action buttons
        if (selectedReminders.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == 0) { // Archived tab
                    Button(
                        onClick = { viewModel.unarchiveSelected() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unarchive Selected")
                    }
                } else { // Deleted tab
                    Button(
                        onClick = { viewModel.restoreSelected() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore Selected")
                    }
                }
                
                Button(
                    onClick = { viewModel.clearSelection() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Clear Selection")
                }
            }
        }
        
        // Purge options - Compact design with smaller buttons and larger font
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Purge Options",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Even more compact buttons with even larger text
                    Button(
                        onClick = {
                            purgeOption = "week"
                            showPurgeDialog = true
                        },
                        modifier = Modifier.weight(1f).height(28.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "> 1 Week",
                            fontSize = 12.sp
                        )
                    }
                    
                    Button(
                        onClick = {
                            purgeOption = "month"
                            showPurgeDialog = true
                        },
                        modifier = Modifier.weight(1f).height(28.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "> 1 Month",
                            fontSize = 12.sp
                        )
                    }
                    
                    if (selectedReminders.isNotEmpty()) {
                        Button(
                            onClick = {
                                purgeOption = "selected"
                                showPurgeDialog = true
                            },
                            modifier = Modifier.weight(1.5f).height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Purge (${selectedReminders.size})",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Content based on selected tab
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            when (selectedTab) {
                0 -> ArchivedRemindersList(
                    archivedReminders = archivedReminders,
                    selectedReminders = selectedReminders,
                    backupStatus = backupStatus,
                    onReminderClick = { id -> viewModel.toggleSelection(id) },
                    onSelectAll = { viewModel.selectAll() },
                    onUnarchive = { id -> viewModel.unarchiveSingle(id) }
                )
                1 -> DeletedRemindersList(
                    deletedReminders = deletedReminders,
                    selectedReminders = selectedReminders,
                    backupStatus = backupStatus,
                    onReminderClick = { id -> viewModel.toggleSelection(id) },
                    onSelectAll = { viewModel.selectAll() }
                )
            }
        }
    }
    
    // Purge confirmation dialog
    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            title = { Text("Confirm Purge") },
            text = {
                Text(
                    when (purgeOption) {
                        "week" -> "This will permanently delete all archived and deleted reminders older than one week. This action cannot be undone."
                        "month" -> "This will permanently delete all archived and deleted reminders older than one month. This action cannot be undone."
                        "selected" -> "This will permanently delete ${selectedReminders.size} selected reminders. This action cannot be undone."
                        else -> "This action cannot be undone."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (purgeOption) {
                            "week" -> viewModel.purgeOldReminders(1)
                            "month" -> viewModel.purgeOldReminders(4)
                            "selected" -> viewModel.purgeSelected()
                        }
                        showPurgeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Purge")
                }
            },
            dismissButton = {
                Button(onClick = { showPurgeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ArchivedRemindersList(
    archivedReminders: List<Reminder>,
    selectedReminders: Set<Int>,
    backupStatus: Map<Int, Boolean>,
    onReminderClick: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onUnarchive: (Int) -> Unit = {}
) {
    Column {
        if (archivedReminders.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${archivedReminders.size} Archived Items",
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSelectAll) {
                    Text("Select All")
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Group reminders by age for better organization
                val now = System.currentTimeMillis()
                val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)
                val oneMonthAgo = now - (30 * 24 * 60 * 60 * 1000L)
                
                val recentArchived = archivedReminders.filter { it.archivedDate != null && it.archivedDate!! >= oneWeekAgo }
                val weekOldArchived = archivedReminders.filter { it.archivedDate != null && it.archivedDate!! < oneWeekAgo && it.archivedDate!! >= oneMonthAgo }
                val monthOldArchived = archivedReminders.filter { it.archivedDate != null && it.archivedDate!! < oneMonthAgo }
                
                // Show recent archived reminders
                if (recentArchived.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent (less than 1 week)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(recentArchived) { reminder ->
                        ArchivedReminderItem(
                            reminder = reminder,
                            isSelected = selectedReminders.contains(reminder.id),
                            hasBackup = backupStatus[reminder.id] ?: false,
                            onClick = { onReminderClick(reminder.id) },
                            onUnarchive = onUnarchive
                        )
                    }
                }
                
                // Show week old archived reminders
                if (weekOldArchived.isNotEmpty()) {
                    item {
                        Text(
                            text = "1 week to 1 month old",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(weekOldArchived) { reminder ->
                        ArchivedReminderItem(
                            reminder = reminder,
                            isSelected = selectedReminders.contains(reminder.id),
                            hasBackup = backupStatus[reminder.id] ?: false,
                            onClick = { onReminderClick(reminder.id) },
                            onUnarchive = onUnarchive
                        )
                    }
                }
                
                // Show month old archived reminders
                if (monthOldArchived.isNotEmpty()) {
                    item {
                        Text(
                            text = "Older than 1 month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(monthOldArchived) { reminder ->
                        ArchivedReminderItem(
                            reminder = reminder,
                            isSelected = selectedReminders.contains(reminder.id),
                            hasBackup = backupStatus[reminder.id] ?: false,
                            onClick = { onReminderClick(reminder.id) },
                            onUnarchive = onUnarchive
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No archived reminders")
            }
        }
    }
}

@Composable
fun DeletedRemindersList(
    deletedReminders: List<Reminder>,
    selectedReminders: Set<Int>,
    backupStatus: Map<Int, Boolean>,
    onReminderClick: (Int) -> Unit,
    onSelectAll: () -> Unit
) {
    Column {
        if (deletedReminders.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${deletedReminders.size} Deleted Items",
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSelectAll) {
                    Text("Select All")
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(deletedReminders) { reminder ->
                    DeletedReminderItem(
                        reminder = reminder,
                        isSelected = selectedReminders.contains(reminder.id),
                        hasBackup = backupStatus[reminder.id] ?: false,
                        onClick = { onReminderClick(reminder.id) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No deleted reminders")
            }
        }
    }
}

@Composable
fun ArchivedReminderItem(
    reminder: Reminder,
    isSelected: Boolean,
    hasBackup: Boolean,
    onClick: () -> Unit,
    onUnarchive: (Int) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
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
                onCheckedChange = { onClick() }
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
                    text = "Archived: ${dateFormat.format(Date(reminder.archivedDate ?: 0))}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                
                Text(
                    text = "Created: ${dateFormat.format(Date(reminder.createdAt))}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            // Unarchive button
            IconButton(
                onClick = { onUnarchive(reminder.id) }
            ) {
                Icon(
                    imageVector = Icons.Default.Unarchive,
                    contentDescription = "Unarchive",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (hasBackup) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Backup exists",
                    tint = Color.Green,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "No backup",
                    tint = Color.Red,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DeletedReminderItem(
    reminder: Reminder,
    isSelected: Boolean,
    hasBackup: Boolean,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
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
                .padding(16.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() }
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = reminder.content,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "Deleted: ${dateFormat.format(Date(reminder.deletedDate ?: 0))}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                
                Text(
                    text = "Created: ${dateFormat.format(Date(reminder.createdAt))}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            if (hasBackup) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Backup exists",
                    tint = Color.Green,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "No backup",
                    tint = Color.Red,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}