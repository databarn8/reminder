package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.reminder.app.utils.CloudBackupManager
import com.reminder.app.utils.GoogleSignInHelper
import com.reminder.app.viewmodel.ReminderViewModel
import com.reminder.app.data.Reminder
import com.reminder.app.data.BackupMetadata
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: ReminderViewModel,
    onBack: () -> Unit,
    onSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val cloudBackupManager = remember { CloudBackupManager(context) }
    val googleSignInHelper = remember { GoogleSignInHelper(context) }
    val reminders by viewModel.reminders.collectAsState()
    
    val backupStatus by cloudBackupManager.backupStatus.collectAsState()
    val restoreStatus by cloudBackupManager.restoreStatus.collectAsState()
    val authStatus by cloudBackupManager.authStatus.collectAsState()
    val signInState by googleSignInHelper.signInState.collectAsState()
    var restoredReminders by remember { mutableStateOf<List<Reminder>?>(null) }
    
    var autoBackupEnabled by remember { mutableStateOf(false) }
    var backupInterval by remember { mutableStateOf(24) }
    var showBackupHistory by remember { mutableStateOf(false) }
    var backupHistory by remember { mutableStateOf<List<CloudBackupManager.BackupInfo>>(emptyList()) }
    var backupMetadata by remember { mutableStateOf<BackupMetadata?>(null) }
    
    LaunchedEffect(Unit) {
        backupHistory = cloudBackupManager.getBackupHistory()
        backupMetadata = cloudBackupManager.getBackupMetadata()
        
        // Check if already signed in with Google
        if (googleSignInHelper.isSignedIn()) {
            cloudBackupManager.signInWithGoogle()
        }
    }
    
    LaunchedEffect(signInState) {
        // Update cloud backup manager auth status when sign-in state changes
        when (signInState) {
            GoogleSignInHelper.SignInState.Success -> {
                cloudBackupManager.signInWithGoogle()
            }
            GoogleSignInHelper.SignInState.SignedOut -> {
                cloudBackupManager.signOut()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Status
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Google Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val accountEmail = googleSignInHelper.getAccountEmail()
                                Text(
                                    text = when {
                                        authStatus == CloudBackupManager.AuthStatus.SignedIn && accountEmail != null ->
                                            "Connected as $accountEmail"
                                        authStatus == CloudBackupManager.AuthStatus.SignedIn ->
                                            "Connected to Google Drive"
                                        authStatus == CloudBackupManager.AuthStatus.SignedOut ->
                                            "Not signed in"
                                        authStatus == CloudBackupManager.AuthStatus.Error ->
                                            "Authentication error"
                                        else -> "Local backup only"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when (authStatus) {
                                        CloudBackupManager.AuthStatus.SignedIn -> MaterialTheme.colorScheme.primary
                                        CloudBackupManager.AuthStatus.SignedOut -> MaterialTheme.colorScheme.onSurfaceVariant
                                        CloudBackupManager.AuthStatus.Error -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            
                            if (authStatus == CloudBackupManager.AuthStatus.SignedIn) {
                                OutlinedButton(
                                    onClick = {
                                        googleSignInHelper.signOut()
                                    }
                                ) {
                                    Text("Sign Out")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onSignIn()
                                    },
                                    enabled = signInState != GoogleSignInHelper.SignInState.SigningIn
                                ) {
                                    if (signInState == GoogleSignInHelper.SignInState.SigningIn) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Sign In")
                                }
                            }
                        }
                    }
                }
            }
            
            // Manual Backup
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Manual Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Backup to Google Drive if signed in, otherwise local
                                    GlobalScope.launch {
                                        val reminders = reminders.toList()
                                        val result = if (cloudBackupManager.isSignedIn()) {
                                            cloudBackupManager.backupToGoogleDrive(reminders)
                                        } else {
                                            cloudBackupManager.smartBackupToLocal(reminders)
                                        }
                                        
                                        // Refresh backup metadata
                                        backupMetadata = cloudBackupManager.getBackupMetadata()
                                        // Refresh backup history
                                        backupHistory = cloudBackupManager.getBackupHistory()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = backupStatus != CloudBackupManager.BackupStatus.BackingUp
                            ) {
                                if (backupStatus == CloudBackupManager.BackupStatus.BackingUp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (cloudBackupManager.isSignedIn()) "Cloud Backup" else "Local Backup")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    // Restore from Google Drive if signed in, otherwise local
                                    GlobalScope.launch {
                                        try {
                                            val result = if (cloudBackupManager.isSignedIn()) {
                                                cloudBackupManager.restoreFromGoogleDrive()
                                            } else {
                                                cloudBackupManager.smartRestoreFromLocal()
                                            }
                                            if (result.isSuccess) {
                                                val restoredRemindersList = result.getOrNull()
                                                restoredReminders = restoredRemindersList
                                                android.util.Log.d("BackupSettings", "Restore successful: ${restoredRemindersList?.size} reminders restored")
                                            } else {
                                                android.util.Log.e("BackupSettings", "Restore failed: ${result.exceptionOrNull()?.message}")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("BackupSettings", "Restore error: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = restoreStatus != CloudBackupManager.RestoreStatus.Restoring
                            ) {
                                if (restoreStatus == CloudBackupManager.RestoreStatus.Restoring) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (cloudBackupManager.isSignedIn()) "Cloud Restore" else "Local Restore")
                            }
                        }
                        
                        // Status messages
                        when (backupStatus) {
                            CloudBackupManager.BackupStatus.Success -> {
                                Text(
                                    text = "Backup completed successfully!",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            CloudBackupManager.BackupStatus.Error -> {
                                Text(
                                    text = "Backup failed. Please try again.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            CloudBackupManager.BackupStatus.AuthRequired -> {
                                Text(
                                    text = "Please sign in to Google Drive for cloud backup.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {}
                        }
                        
                        when (restoreStatus) {
                            CloudBackupManager.RestoreStatus.Success -> {
                                Text(
                                    text = "Restore completed successfully!",
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                // Show restored reminders count
                                restoredReminders?.let { reminders: List<Reminder> ->
                                    Text(
                                        text = "${reminders.size} reminders restored",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            CloudBackupManager.RestoreStatus.Error -> {
                                Text(
                                    text = "Restore failed. Please try again.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            CloudBackupManager.RestoreStatus.NoBackup -> {
                                Text(
                                    text = "No backup files found.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            CloudBackupManager.RestoreStatus.AuthRequired -> {
                                Text(
                                    text = "Please sign in to Google Drive for cloud restore.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
            
            // Auto Backup Settings
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Auto Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable automatic backup")
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { enabled ->
                                    autoBackupEnabled = enabled
                                    cloudBackupManager.scheduleAutoBackup(enabled, backupInterval)
                                }
                            )
                        }
                        
                        if (autoBackupEnabled) {
                            Text(
                                text = "Backup interval:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            var expanded by remember { mutableStateOf(false) }
                            val intervals = listOf(6, 12, 24, 48, 72)
                            
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = "${backupInterval} hours",
                                    onValueChange = { },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    intervals.forEach { interval ->
                                        DropdownMenuItem(
                                            text = { Text("${interval} hours") },
                                            onClick = {
                                                backupInterval = interval
                                                cloudBackupManager.scheduleAutoBackup(true, interval)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Text(
                            text = if (cloudBackupManager.isSignedIn()) {
                                "Auto backup saves your reminders to Google Drive every ${backupInterval} hours"
                            } else {
                                "Auto backup saves your reminders locally every ${backupInterval} hours"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Backup Metadata Info
            item {
                backupMetadata?.let { metadata ->
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Smart Backup Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Backup type indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Last Full Backup",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (metadata.lastFullBackup > 0) {
                                        val date = Date(metadata.lastFullBackup)
                                        val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                        Text(
                                            text = formatter.format(date),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        Text(
                                            text = "Never",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                Column {
                                    Text(
                                        text = "Last Delta Backup",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (metadata.lastDeltaBackup > 0) {
                                        val date = Date(metadata.lastDeltaBackup)
                                        val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                        Text(
                                            text = formatter.format(date),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        Text(
                                            text = "Never",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // Storage info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Reminders: ${metadata.totalReminders}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = "Storage Used: ${formatFileSize(metadata.backupSize)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Delta files count
                            if (metadata.deltaFiles.isNotEmpty()) {
                                Text(
                                    text = "Delta Files: ${metadata.deltaFiles.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            // Backup History
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Backup History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            TextButton(
                                onClick = {
                                    // Launch a coroutine to call suspend function
                                    GlobalScope.launch {
                                        backupHistory = cloudBackupManager.getBackupHistory()
                                    }
                                }
                            ) {
                                Text("Refresh")
                            }
                        }
                        
                        if (backupHistory.isEmpty()) {
                            Text(
                                text = "No backup files found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            backupHistory.take(10).forEach { backup ->
                                BackupHistoryItem(backup = backup)
                            }
                        }
                    }
                }
            }
            
            // Backup Content Viewer
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Backup Content Viewer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        var selectedBackup by remember { mutableStateOf<CloudBackupManager.BackupInfo?>(null) }
                        var backupContent by remember { mutableStateOf("") }
                        
                        // Backup file selector
                        var expanded by remember { mutableStateOf(false) }
                        var backupFiles by remember { mutableStateOf<List<CloudBackupManager.BackupInfo>>(emptyList()) }
                        
                        LaunchedEffect(Unit) {
                            backupFiles = cloudBackupManager.getBackupHistory().filter { !it.isCloud }
                        }
                        
                        if (backupFiles.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedBackup?.fileName ?: "Select backup file",
                                    onValueChange = { },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    backupFiles.forEach { backup ->
                                        DropdownMenuItem(
                                            text = { Text(backup.fileName) },
                                            onClick = {
                                                selectedBackup = backup
                                                expanded = false
                                                
                                                // Load backup content
                                                GlobalScope.launch {
                                                    try {
                                                        val backupFile = if (backup.location == "Local") {
                                                            File(context.filesDir, "backups/${backup.fileName}")
                                                        } else {
                                                            File(context.getExternalFilesDir(null), "Downloads/${backup.fileName}")
                                                        }
                                                        
                                                        if (backupFile.exists()) {
                                                            backupContent = backupFile.readText()
                                                        } else {
                                                            backupContent = "File not found: ${backupFile.absolutePath}"
                                                        }
                                                    } catch (e: Exception) {
                                                        backupContent = "Error loading backup: ${e.message}"
                                                        android.util.Log.e("BackupSettings", "Error loading backup content: ${e.message}")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            
                            if (selectedBackup != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Display backup content
                                if (backupContent.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            selectedBackup?.let { backup ->
                                                Text(
                                                    text = "Backup Content: ${backup.fileName}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Parse and display backup content
                                            val backupParseResult = remember(backupContent) {
                                                try {
                                                    val backupObject = org.json.JSONObject(backupContent)
                                                    val remindersArray = backupObject.getJSONArray("reminders")
                                                    Result.success(remindersArray)
                                                } catch (e: Exception) {
                                                    Result.failure(e)
                                                }
                                            }
                                            
                                            backupParseResult.getOrNull()?.let { remindersArray ->
                                                Text(
                                                    text = "Total Reminders: ${remindersArray.length()}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                // Show first few reminders as preview
                                                val previewCount = minOf(3, remindersArray.length())
                                                
                                                for (index in 0 until previewCount) {
                                                    val reminderObject = remindersArray.getJSONObject(index)
                                                    val reminderContent = reminderObject.optString("content", "")
                                                    val reminderCategory = reminderObject.optString("category", "")
                                                    val reminderTime = reminderObject.optLong("reminderTime", 0L)
                                                    
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(8.dp)
                                                        ) {
                                                            Text(
                                                                text = reminderContent.ifEmpty { "No content" },
                                                                style = MaterialTheme.typography.bodySmall,
                                                                maxLines = 2,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            
                                                            if (reminderCategory.isNotEmpty()) {
                                                                Text(
                                                                    text = "Category: $reminderCategory",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                            
                                                            if (reminderTime > 0) {
                                                                val date = Date(reminderTime)
                                                                val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                                                Text(
                                                                    text = "Time: ${formatter.format(date)}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                if (remindersArray.length() > previewCount) {
                                                    Text(
                                                        text = "... and ${remindersArray.length() - previewCount} more reminders",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                    )
                                                }
                                            } ?: backupParseResult.exceptionOrNull()?.let { error ->
                                                Text(
                                                    text = "Error reading backup: ${error.message}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Loading backup content...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No local backup files found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            
            // Export Options
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Export Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Export your reminders to share or migrate to another device:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Export to CSV
                                    GlobalScope.launch {
                                        try {
                                            val reminders = reminders.toList()
                                            val result = com.reminder.app.utils.DataExportImportManager(context).exportToCSV(reminders)
                                            if (result.isSuccess) {
                                                android.util.Log.d("BackupSettings", "CSV exported to: ${result.getOrNull()}")
                                            } else {
                                                android.util.Log.e("BackupSettings", "CSV export failed: ${result.exceptionOrNull()?.message}")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("BackupSettings", "CSV export error: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export CSV")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    // Export to JSON
                                    GlobalScope.launch {
                                        try {
                                            val reminders = reminders.toList()
                                            val result = com.reminder.app.utils.DataExportImportManager(context).exportToJSON(reminders)
                                            if (result.isSuccess) {
                                                android.util.Log.d("BackupSettings", "JSON exported to: ${result.getOrNull()}")
                                            } else {
                                                android.util.Log.e("BackupSettings", "JSON export failed: ${result.exceptionOrNull()?.message}")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("BackupSettings", "JSON export error: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export JSON")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupHistoryItem(backup: CloudBackupManager.BackupInfo) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val sizeText = when {
        backup.size < 1024 -> "${backup.size} B"
        backup.size < 1024 * 1024 -> "${backup.size / 1024} KB"
        else -> "${backup.size / (1024 * 1024)} MB"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = backup.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Backup type badge
                Surface(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = when (backup.type) {
                        "Base Backup" -> MaterialTheme.colorScheme.primaryContainer
                        "Delta Backup" -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = backup.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (backup.type) {
                            "Base Backup" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "Delta Backup" -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = backup.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = dateFormat.format(Date(backup.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (backup.isCloud) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = "Cloud backup",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = "Local backup",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}