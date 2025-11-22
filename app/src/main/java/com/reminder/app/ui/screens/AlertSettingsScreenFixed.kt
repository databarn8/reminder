package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.scale
import com.reminder.app.data.AlertConfig
import com.reminder.app.data.AlertLevel
import com.reminder.app.data.AlertLevelConfig
import com.reminder.app.data.AlertLevelOption
import com.reminder.app.data.VibrationPattern
import com.reminder.app.data.VibrationIntensity
import com.reminder.app.data.SoundType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsScreenFixed(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var alertLevelConfig by remember { mutableStateOf(loadAlertLevelConfig(context)) }
    var selectedLevel by remember { mutableStateOf(AlertLevel.LOW) }
    var selectedCustomProfile by remember { mutableStateOf<String?>(null) }
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
                            text = "✓ Alert settings saved successfully",
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
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Alert Levels",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                item {
                    AlertLevelSelector(
                        selectedLevel = selectedLevel,
                        selectedCustomProfile = selectedCustomProfile,
                        alertLevelConfig = alertLevelConfig,
                        onLevelSelected = { level ->
                            selectedLevel = level
                            selectedCustomProfile = null
                        },
                        onCustomProfileSelected = { profileName ->
                            selectedLevel = AlertLevel.CUSTOM
                            selectedCustomProfile = profileName
                        }
                    )
                }
                
                item {
                    AlertLevelConfiguration(
                        alertLevel = selectedLevel,
                        selectedCustomProfile = selectedCustomProfile,
                        alertLevelConfig = alertLevelConfig,
                        onConfigChanged = { newConfig ->
                            alertLevelConfig = updateAlertLevelConfig(alertLevelConfig, selectedLevel, selectedCustomProfile, newConfig)
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
                                .padding(8.dp)
                        ) {
                            // Profile count display
                            val profileCount = alertLevelConfig.customProfiles.size
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Profiles ($profileCount/3)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        if (profileCount < 3) {
                                            showCustomProfileDialog = true
                                        }
                                    },
                                    enabled = profileCount < 3,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Create", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            
                            if (alertLevelConfig.customProfiles.isNotEmpty()) {
                                // Display profiles in a horizontal scrollable row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    alertLevelConfig.customProfiles.forEach { (name, config) ->
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    customProfileName = name
                                                    editingCustomProfileConfig = config
                                                    showCustomProfileDialog = true
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                // Compact status indicators
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "V:${if (config.vibration.enabled) "✓" else "✗"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (config.vibration.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "S:${if (config.sound.enabled) "✓" else "✗"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (config.sound.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "R:${if (config.series.enabled) config.series.maxAttempts else "0"}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            customProfileName = name
                                                            editingCustomProfileConfig = config
                                                            showCustomProfileDialog = true
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Settings,
                                                            contentDescription = "Edit Profile",
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            alertLevelConfig = alertLevelConfig.copy(
                                                                customProfiles = alertLevelConfig.customProfiles - name
                                                            )
                                                            saveAlertLevelConfig(context, alertLevelConfig)
                                                            showSaveConfirmation = true
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Profile",
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No custom profiles yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
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
                    // Show full configuration editor for existing profile
                    editingCustomProfileConfig?.let { config ->
                        CustomProfileConfigEditor(
                            profileName = customProfileName,
                            config = config,
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
    selectedCustomProfile: String?,
    alertLevelConfig: AlertLevelConfig,
    onLevelSelected: (AlertLevel) -> Unit,
    onCustomProfileSelected: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Select Alert Level",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Column(Modifier.selectableGroup()) {
                // Built-in alert levels - use horizontal layout for better space
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(AlertLevel.LOW, AlertLevel.MEDIUM).forEach { level ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .selectable(
                                        selected = selectedLevel == level,
                                        onClick = { onLevelSelected(level) },
                                        role = Role.RadioButton
                                    )
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RadioButton(
                                    selected = selectedLevel == level,
                                    onClick = null,
                                    modifier = Modifier.scale(0.8f)
                                )
                                Text(
                                    text = when (level) {
                                        AlertLevel.LOW -> "Low"
                                        AlertLevel.MEDIUM -> "Medium"
                                        else -> level.name
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(AlertLevel.HIGH, AlertLevel.URGENT).forEach { level ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .selectable(
                                        selected = selectedLevel == level,
                                        onClick = { onLevelSelected(level) },
                                        role = Role.RadioButton
                                    )
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RadioButton(
                                    selected = selectedLevel == level,
                                    onClick = null,
                                    modifier = Modifier.scale(0.8f)
                                )
                                Text(
                                    text = when (level) {
                                        AlertLevel.HIGH -> "High"
                                        AlertLevel.URGENT -> "Urgent"
                                        else -> level.name
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                
                // Custom profiles section - use horizontal layout
                if (alertLevelConfig.customProfiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Custom Profiles:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    
                    // Display custom profiles in a horizontal row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        alertLevelConfig.customProfiles.forEach { (profileName, _) ->
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = selectedLevel == AlertLevel.CUSTOM && selectedCustomProfile == profileName,
                                        onClick = { onCustomProfileSelected(profileName) },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedLevel == AlertLevel.CUSTOM && selectedCustomProfile == profileName,
                                    onClick = null,
                                    modifier = Modifier.scale(0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = profileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                            }
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
    selectedCustomProfile: String?,
    alertLevelConfig: AlertLevelConfig,
    onConfigChanged: (AlertConfig) -> Unit
) {
    
    val alertConfig = when {
        alertLevel == AlertLevel.CUSTOM && selectedCustomProfile != null -> {
            alertLevelConfig.customProfiles[selectedCustomProfile] ?: AlertConfig.getMediumLevelDefaults()
        }
        else -> {
            getAlertConfigForLevel(alertLevelConfig, alertLevel, selectedCustomProfile)
        }
    }
    
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = when {
                    alertLevel == AlertLevel.CUSTOM && selectedCustomProfile != null -> {
                        "Configuration for $selectedCustomProfile"
                    }
                    else -> {
                        "Configuration for ${alertLevel.name.lowercase().replaceFirstChar { it.uppercase() }} level"
                    }
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            // Vibration Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            text = "Vibration",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = alertConfig.vibration.enabled,
                            onCheckedChange = {
                                val newConfig = alertConfig.copy(
                                    vibration = alertConfig.vibration.copy(enabled = it)
                                )
                                onConfigChanged(newConfig)
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    
                    if (alertConfig.vibration.enabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Pattern Selection
                        Text(
                            text = "Pattern",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VibrationPattern.values().filter { it != VibrationPattern.CUSTOM && it != VibrationPattern.PULSE }.forEach { pattern ->
                                Row(
                                    modifier = Modifier
                                        .selectable(
                                            selected = alertConfig.vibration.pattern == pattern,
                                            onClick = {
                                                val newConfig = alertConfig.copy(
                                                    vibration = alertConfig.vibration.copy(pattern = pattern)
                                                )
                                                onConfigChanged(newConfig)
                                            }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = alertConfig.vibration.pattern == pattern,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = pattern.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Intensity Selection
                        Text(
                            text = "Intensity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VibrationIntensity.values().forEach { intensity ->
                                Row(
                                    modifier = Modifier
                                        .selectable(
                                            selected = alertConfig.vibration.intensity == intensity,
                                            onClick = {
                                                val newConfig = alertConfig.copy(
                                                    vibration = alertConfig.vibration.copy(intensity = intensity)
                                                )
                                                onConfigChanged(newConfig)
                                            }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = alertConfig.vibration.intensity == intensity,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = intensity.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sound Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            text = "Sound",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = alertConfig.sound.enabled,
                            onCheckedChange = {
                                val newConfig = alertConfig.copy(
                                    sound = alertConfig.sound.copy(enabled = it)
                                )
                                onConfigChanged(newConfig)
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    
                    if (alertConfig.sound.enabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Sound Type Selection
                        Text(
                            text = "Type",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SoundType.values().filter { it != SoundType.CUSTOM && it != SoundType.URGENT }.forEach { type ->
                                Row(
                                    modifier = Modifier
                                        .selectable(
                                            selected = alertConfig.sound.type == type,
                                            onClick = {
                                                val newConfig = alertConfig.copy(
                                                    sound = alertConfig.sound.copy(type = type)
                                                )
                                                onConfigChanged(newConfig)
                                            }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = alertConfig.sound.type == type,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (type.name) {
                                            "CHIME" -> "Chime"
                                            "GENTLE" -> "Gentle"
                                            "CUSTOM" -> "Custom"
                                            "ALARM" -> "Alarm"
                                            else -> type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Volume Slider
                        Text(
                            text = "Volume: ${(alertConfig.sound.volume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = alertConfig.sound.volume,
                            onValueChange = {
                                val newConfig = alertConfig.copy(
                                    sound = alertConfig.sound.copy(volume = it)
                                )
                                onConfigChanged(newConfig)
                            },
                            valueRange = 0f..1f,
                            steps = 9
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Series Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            text = "Repeat & Escalation",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = alertConfig.series.enabled,
                            onCheckedChange = {
                                val newConfig = alertConfig.copy(
                                    series = alertConfig.series.copy(enabled = it)
                                )
                                onConfigChanged(newConfig)
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    
                    if (alertConfig.series.enabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Max Attempts Slider
                        Text(
                            text = "Max Attempts: ${alertConfig.series.maxAttempts}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = alertConfig.series.maxAttempts.toFloat(),
                            onValueChange = {
                                val newConfig = alertConfig.copy(
                                    series = alertConfig.series.copy(maxAttempts = it.toInt())
                                )
                                onConfigChanged(newConfig)
                            },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Interval Slider
                        Text(
                            text = "Interval: ${alertConfig.series.intervalMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = alertConfig.series.intervalMinutes.toFloat(),
                            onValueChange = {
                                val newConfig = alertConfig.copy(
                                    series = alertConfig.series.copy(intervalMinutes = it.toInt())
                                )
                                onConfigChanged(newConfig)
                            },
                            valueRange = 1f..30f,
                            steps = 28
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Escalation Switch - put on same line as label
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Escalate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = alertConfig.series.escalationEnabled,
                                onCheckedChange = {
                                    val newConfig = alertConfig.copy(
                                        series = alertConfig.series.copy(escalationEnabled = it)
                                    )
                                    onConfigChanged(newConfig)
                                },
                                modifier = Modifier.scale(0.6f)
                            )
                        }
                    }
                }
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
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (localConfig.vibration.enabled) "On" else "Off",
                style = MaterialTheme.typography.bodySmall
            )
            Switch(
                checked = localConfig.vibration.enabled,
                onCheckedChange = {
                    val newConfig = localConfig.copy(
                        vibration = localConfig.vibration.copy(enabled = it)
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                },
                modifier = Modifier.scale(0.8f)
            )
        }
        
        if (localConfig.vibration.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Pattern:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VibrationPattern.values().filter { it != VibrationPattern.CUSTOM && it != VibrationPattern.PULSE }.forEach { pattern ->
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pattern.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Intensity:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = intensity.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sound Settings
        Text(
            text = "Sound Settings",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (localConfig.sound.enabled) "On" else "Off",
                style = MaterialTheme.typography.bodySmall
            )
            Switch(
                checked = localConfig.sound.enabled,
                onCheckedChange = {
                    val newConfig = localConfig.copy(
                        sound = localConfig.sound.copy(enabled = it)
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                },
                modifier = Modifier.scale(0.8f)
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
                SoundType.values().filter { it != SoundType.CUSTOM && it != SoundType.URGENT }.forEach { type ->
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (type.name) {
                                "CHIME" -> "Chime"
                                "GENTLE" -> "Gentle"
                                "CUSTOM" -> "Custom"
                                "ALARM" -> "Alarm"
                                else -> type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp
                        )
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
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (localConfig.series.enabled) "On" else "Off",
                style = MaterialTheme.typography.bodySmall
            )
            Switch(
                checked = localConfig.series.enabled,
                onCheckedChange = {
                    val newConfig = localConfig.copy(
                        series = localConfig.series.copy(enabled = it)
                    )
                    localConfig = newConfig
                    onConfigChanged(newConfig)
                },
                modifier = Modifier.scale(0.8f)
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
            Text(
                text = "${localConfig.series.maxAttempts}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
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
            Text(
                text = "${localConfig.series.intervalMinutes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (localConfig.series.escalationEnabled) "On" else "Off",
                    style = MaterialTheme.typography.bodySmall
                )
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

private fun getAlertConfigForLevel(levelConfig: AlertLevelConfig, level: AlertLevel, selectedCustomProfile: String? = null): AlertConfig {
    return try {
        when (level) {
            AlertLevel.LOW -> levelConfig.lowLevel
            AlertLevel.MEDIUM -> levelConfig.mediumLevel
            AlertLevel.HIGH -> levelConfig.highLevel
            AlertLevel.URGENT -> levelConfig.urgentLevel
            AlertLevel.CUSTOM -> {
                if (selectedCustomProfile != null) {
                    levelConfig.customProfiles[selectedCustomProfile] ?: AlertConfig.Companion.getMediumLevelDefaults()
                } else {
                    levelConfig.customProfiles.values.firstOrNull() ?: AlertConfig.Companion.getMediumLevelDefaults()
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error getting alert config for level $level: ${e.message}")
        AlertConfig.Companion.getMediumLevelDefaults() // Fallback to medium defaults
    }
}

private fun updateAlertLevelConfig(
    levelConfig: AlertLevelConfig,
    level: AlertLevel,
    selectedCustomProfile: String?,
    newConfig: AlertConfig
): AlertLevelConfig {
    return try {
        when (level) {
            AlertLevel.LOW -> levelConfig.copy(lowLevel = newConfig)
            AlertLevel.MEDIUM -> levelConfig.copy(mediumLevel = newConfig)
            AlertLevel.HIGH -> levelConfig.copy(highLevel = newConfig)
            AlertLevel.URGENT -> levelConfig.copy(urgentLevel = newConfig)
            AlertLevel.CUSTOM -> {
                if (selectedCustomProfile != null) {
                    levelConfig.copy(
                        customProfiles = levelConfig.customProfiles + (selectedCustomProfile to newConfig)
                    )
                } else {
                    levelConfig
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error updating alert level config for $level: ${e.message}")
        levelConfig // Return original config if update fails
    }
}