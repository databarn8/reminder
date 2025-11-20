package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.data.AlertConfig
import com.reminder.app.data.AlertLevel
import com.reminder.app.data.AlertLevelConfig
import com.reminder.app.data.AlertLevelOption
import com.reminder.app.data.VibrationPattern
import com.reminder.app.data.VibrationIntensity
import com.reminder.app.data.SoundType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var alertLevelConfig by remember { mutableStateOf(loadAlertLevelConfig(context)) }
    var selectedLevel by remember { mutableStateOf(AlertLevel.LOW) }
    var showCustomProfileDialog by remember { mutableStateOf(false) }
    var customProfileName by remember { mutableStateOf("") }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var editingCustomProfileConfig by remember { mutableStateOf<AlertConfig?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Show save confirmation
            if (showSaveConfirmation) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000) // Show for 2 seconds
                    showSaveConfirmation = false
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
                            text = "✓ Alert settings saved",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Alert Levels",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Configure how alerts behave for different importance levels",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                item {
                    AlertLevelSelector(
                        selectedLevel = selectedLevel,
                        onLevelSelected = { level -> selectedLevel = level }
                    )
                }
                
                item {
                    AlertLevelConfiguration(
                        alertLevel = selectedLevel,
                        alertConfig = getAlertConfigForLevel(alertLevelConfig, selectedLevel),
                        onConfigChanged = { newConfig ->
                            alertLevelConfig = updateAlertLevelConfig(alertLevelConfig, selectedLevel, newConfig)
                            saveAlertLevelConfig(context, alertLevelConfig)
                            showSaveConfirmation = true
                        }
                    )
                }
                
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Custom Profiles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Profile count display
                            val profileCount = alertLevelConfig.customProfiles.size
                            Text(
                                text = "Custom Profiles ($profileCount/3)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (alertLevelConfig.customProfiles.isNotEmpty()) {
                                alertLevelConfig.customProfiles.forEach { (name, config) ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row {
                                                    IconButton(onClick = {
                                                        // Edit profile functionality - open dialog to edit
                                                        customProfileName = name
                                                        editingCustomProfileConfig = config
                                                        showCustomProfileDialog = true
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Settings,
                                                            contentDescription = "Edit Profile"
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        alertLevelConfig = alertLevelConfig.copy(
                                                            customProfiles = alertLevelConfig.customProfiles - name
                                                        )
                                                        saveAlertLevelConfig(context, alertLevelConfig)
                                                        showSaveConfirmation = true
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowBack,
                                                            contentDescription = "Delete Profile"
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Show current settings for this custom profile
                                            Column(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Current Settings:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Vibration: ${if (config.vibration.enabled) "On" else "Off"}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Text(
                                                        text = "Sound: ${if (config.sound.enabled) "On" else "Off"}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                
                                                if (config.vibration.enabled) {
                                                    Text(
                                                        text = "Pattern: ${config.vibration.pattern.name}, Intensity: ${config.vibration.intensity.name}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                
                                                if (config.sound.enabled) {
                                                    Text(
                                                        text = "Type: ${config.sound.type.name}, Volume: ${(config.sound.volume * 100).toInt()}%",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                
                                                if (config.series.enabled) {
                                                    Text(
                                                        text = "Repeat: ${config.series.maxAttempts} times, Escalation: ${if (config.series.escalationEnabled) "On" else "Off"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No custom profiles yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    if (profileCount < 3) {
                                        showCustomProfileDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = profileCount < 3
                            ) {
                                Text(if (profileCount < 3) "Create Custom Profile" else "Maximum 3 profiles reached")
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showCustomProfileDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCustomProfileDialog = false
                editingCustomProfileConfig = null
            },
            title = { 
                Text(
                    if (editingCustomProfileConfig != null) "Edit Custom Profile: $customProfileName" 
                    else "Create Custom Profile"
                ) 
            },
            text = {
                if (editingCustomProfileConfig != null) {
                    editingCustomProfileConfig?.let { currentConfig ->
                        // Show full configuration editor for existing profile
                        CustomProfileConfigEditor(
                            profileName = customProfileName,
                            config = currentConfig,
                            onConfigChanged = { newConfig ->
                                editingCustomProfileConfig = newConfig
                            }
                        )
                    }
                } else {
                    // Simple name input for new profile
                    OutlinedTextField(
                        value = customProfileName,
                        onValueChange = { customProfileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customProfileName.isNotBlank()) {
                            val profileCount = alertLevelConfig.customProfiles.size
                            
                            if (editingCustomProfileConfig != null) {
                                // Update existing profile with its configuration
                                alertLevelConfig = alertLevelConfig.copy(
                                    customProfiles = alertLevelConfig.customProfiles + Pair(customProfileName, editingCustomProfileConfig!!)
                                )
                                saveAlertLevelConfig(context, alertLevelConfig)
                                showSaveConfirmation = true
                            } else if (profileCount < 3) {
                                // Create new profile with default settings
                                val newConfig = AlertConfig.Companion.getMediumLevelDefaults()
                                alertLevelConfig = alertLevelConfig.copy(
                                    customProfiles = alertLevelConfig.customProfiles + Pair(customProfileName, newConfig)
                                )
                                saveAlertLevelConfig(context, alertLevelConfig)
                                showSaveConfirmation = true
                            }
                            
                            customProfileName = ""
                            editingCustomProfileConfig = null
                            showCustomProfileDialog = false
                        }
                    }
                ) {
                    Text(if (editingCustomProfileConfig != null) "Update" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCustomProfileDialog = false
                    editingCustomProfileConfig = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlertLevelSelector(
    selectedLevel: AlertLevel,
    onLevelSelected: (AlertLevel) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Alert Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(Modifier.selectableGroup()) {
                AlertLevel.values().forEach { level ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedLevel == level,
                                onClick = { onLevelSelected(level) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLevel == level,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (level) {
                                    AlertLevel.LOW -> "Low"
                                    AlertLevel.MEDIUM -> "Medium"
                                    AlertLevel.HIGH -> "High"
                                    AlertLevel.URGENT -> "Urgent"
                                    AlertLevel.CUSTOM -> "Custom"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (level) {
                                    AlertLevel.LOW -> "Gentle reminders"
                                    AlertLevel.MEDIUM -> "Standard notifications"
                                    AlertLevel.HIGH -> "Important alerts"
                                    AlertLevel.URGENT -> "Critical notifications"
                                    AlertLevel.CUSTOM -> "Custom profiles"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertLevelConfiguration(
    alertLevel: AlertLevel,
    alertConfig: AlertConfig,
    onConfigChanged: (AlertConfig) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Configuration for ${alertLevel.name} Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Vibration Settings
            Text(
                text = "Vibration",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enabled")
                Switch(
                    checked = alertConfig.vibration.enabled,
                    onCheckedChange = {
                        val newConfig = alertConfig.copy(
                            vibration = alertConfig.vibration.copy(enabled = it)
                        )
                        onConfigChanged(newConfig)
                    }
                )
            }
            
            if (alertConfig.vibration.enabled) {
                Text("Pattern: ${alertConfig.vibration.pattern.name}")
                Text("Intensity: ${alertConfig.vibration.intensity.name}")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sound Settings
            Text(
                text = "Sound",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enabled")
                Switch(
                    checked = alertConfig.sound.enabled,
                    onCheckedChange = {
                        val newConfig = alertConfig.copy(
                            sound = alertConfig.sound.copy(enabled = it)
                        )
                        onConfigChanged(newConfig)
                    }
                )
            }
            
            if (alertConfig.sound.enabled) {
                Text("Type: ${alertConfig.sound.type.name}")
                Text("Volume: ${(alertConfig.sound.volume * 100).toInt()}%")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Series Settings
            Text(
                text = "Repeat & Escalation",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enabled")
                Switch(
                    checked = alertConfig.series.enabled,
                    onCheckedChange = {
                        val newConfig = alertConfig.copy(
                            series = alertConfig.series.copy(enabled = it)
                        )
                        onConfigChanged(newConfig)
                    }
                )
            }
            
            if (alertConfig.series.enabled) {
                Text("Max attempts: ${alertConfig.series.maxAttempts}")
                Text("Interval: ${alertConfig.series.intervalMinutes} minutes")
                Text("Escalation: ${if (alertConfig.series.escalationEnabled) "Enabled" else "Disabled"}")
            }
        }
    }
}

@Composable
fun CustomProfileConfigEditor(
    profileName: String,
    config: AlertConfig,
    onConfigChanged: (AlertConfig) -> Unit
) {
    var localConfig by remember { mutableStateOf(config) }
    
    LaunchedEffect(config) {
        localConfig = config
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Vibration Settings
        Text(
            text = "Vibration Settings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enabled")
            Switch(
                checked = localConfig.vibration.enabled,
                onCheckedChange = {
                    val newConfig = localConfig.copy(
                        vibration = localConfig.vibration.copy(enabled = it)
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                }
            )
        }
        
        if (localConfig.vibration.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Pattern:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VibrationPattern.values().forEach { pattern ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = localConfig.vibration.pattern == pattern,
                                onClick = {
                                    val newConfig = localConfig.copy(
                                        vibration = localConfig.vibration.copy(pattern = pattern)
                                    )
                                    localConfig = newConfig
                                    onConfigChanged(newConfig)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = localConfig.vibration.pattern == pattern,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pattern.name.replace("_", " "))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Intensity:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VibrationIntensity.values().forEach { intensity ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = localConfig.vibration.intensity == intensity,
                                onClick = {
                                    val newConfig = localConfig.copy(
                                        vibration = localConfig.vibration.copy(intensity = intensity)
                                    )
                                    localConfig = newConfig
                                    onConfigChanged(newConfig)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = localConfig.vibration.intensity == intensity,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(intensity.name.replace("_", " "))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sound Settings
        Text(
            text = "Sound Settings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enabled")
            Switch(
                checked = localConfig.sound.enabled,
                onCheckedChange = {
                    val newConfig = localConfig.copy(
                        sound = localConfig.sound.copy(enabled = it)
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                }
            )
        }
        
        if (localConfig.sound.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Sound Type:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoundType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = localConfig.sound.type == type,
                                onClick = {
                                    val newConfig = localConfig.copy(
                                        sound = localConfig.sound.copy(type = type)
                                    )
                                    localConfig = newConfig
                                    onConfigChanged(newConfig)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = localConfig.sound.type == type,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type.name.replace("_", " "))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Volume: ${(localConfig.sound.volume * 100).toInt()}%")
            Slider(
                value = localConfig.sound.volume,
                onValueChange = {
                    val newConfig = localConfig.copy(
                        sound = localConfig.sound.copy(volume = it)
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                },
                valueRange = 0f..1f,
                steps = 9
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Series Settings
        Text(
            text = "Repeat & Escalation Settings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enabled")
            Switch(
                checked = localConfig.series.enabled,
                onCheckedChange = {
                    val newConfig = localConfig.copy(
                        series = localConfig.series.copy(enabled = it)
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                }
            )
        }
        
        if (localConfig.series.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Max Attempts:")
            Slider(
                value = localConfig.series.maxAttempts.toFloat(),
                onValueChange = {
                    val newConfig = localConfig.copy(
                        series = localConfig.series.copy(maxAttempts = it.toInt())
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                },
                valueRange = 1f..10f,
                steps = 8
            )
            Text("${localConfig.series.maxAttempts}")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Interval (minutes):")
            Slider(
                value = localConfig.series.intervalMinutes.toFloat(),
                onValueChange = {
                    val newConfig = localConfig.copy(
                        series = localConfig.series.copy(intervalMinutes = it.toInt())
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                },
                valueRange = 1f..30f,
                steps = 28
            )
            Text("${localConfig.series.intervalMinutes}")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Escalation")
                Switch(
                    checked = localConfig.series.escalationEnabled,
                    onCheckedChange = {
                        val newConfig = localConfig.copy(
                            series = localConfig.series.copy(escalationEnabled = it)
                        )
                        localConfig = newConfig
                        onConfigChanged(newConfig)
                    }
                )
            }
        }
    }
}

// Helper functions for persistence
private fun loadAlertLevelConfig(context: android.content.Context): AlertLevelConfig {
    return try {
        val prefs = context.getSharedPreferences("alert_level_config", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("alert_level_config", null)
        if (json != null) {
            AlertLevelConfig.Companion.fromJson(json)
        } else {
            AlertLevelConfig() // Default
        }
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error loading alert level config: ${e.message}")
        AlertLevelConfig() // Default if parsing fails
    }
}

private fun saveAlertLevelConfig(context: android.content.Context, config: AlertLevelConfig) {
    try {
        val prefs = context.getSharedPreferences("alert_level_config", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("alert_level_config", AlertLevelConfig.Companion.toJson(config))
            .apply()
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error saving alert level config: ${e.message}")
    }
}

private fun getAlertConfigForLevel(levelConfig: AlertLevelConfig, level: AlertLevel): AlertConfig {
    return try {
        when (level) {
            AlertLevel.LOW -> levelConfig.lowLevel
            AlertLevel.MEDIUM -> levelConfig.mediumLevel
            AlertLevel.HIGH -> levelConfig.highLevel
            AlertLevel.URGENT -> levelConfig.urgentLevel
            AlertLevel.CUSTOM -> levelConfig.customProfiles.values.firstOrNull() ?: AlertConfig.Companion.getMediumLevelDefaults()
        }
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error getting alert config for level $level: ${e.message}")
        AlertConfig.Companion.getMediumLevelDefaults() // Fallback to medium defaults
    }
}

private fun updateAlertLevelConfig(
    levelConfig: AlertLevelConfig,
    level: AlertLevel,
    newConfig: AlertConfig
): AlertLevelConfig {
    return try {
        when (level) {
            AlertLevel.LOW -> levelConfig.copy(lowLevel = newConfig)
            AlertLevel.MEDIUM -> levelConfig.copy(mediumLevel = newConfig)
            AlertLevel.HIGH -> levelConfig.copy(highLevel = newConfig)
            AlertLevel.URGENT -> levelConfig.copy(urgentLevel = newConfig)
            AlertLevel.CUSTOM -> levelConfig // Custom profiles handled separately
        }
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error updating alert level config for $level: ${e.message}")
        levelConfig // Return original config if update fails
    }
}