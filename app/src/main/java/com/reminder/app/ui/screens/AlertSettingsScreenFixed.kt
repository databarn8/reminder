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
import androidx.compose.material.icons.filled.MeetingRoom
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
import com.reminder.app.utils.MeetingModeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsScreenFixed(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var alertLevelConfig by remember { mutableStateOf(loadAlertLevelConfig(context)) }
    var selectedLevel by remember { mutableStateOf(AlertLevel.LOW) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    
    // Meeting mode state
    val meetingModeManager = remember { MeetingModeManager.getInstance(context) }
    var meetingModeEnabled by remember { mutableStateOf(meetingModeManager.isMeetingModeEnabled()) }
    var soundDuration by remember { mutableStateOf(meetingModeManager.getSoundDurationSeconds()) }
    
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
                    MeetingModeSection(
                        meetingModeEnabled = meetingModeEnabled,
                        soundDuration = soundDuration,
                        onMeetingModeToggle = { enabled: Boolean ->
                            meetingModeEnabled = enabled
                            meetingModeManager.setMeetingMode(enabled)
                            showSaveConfirmation = true
                        },
                        onSoundDurationChange = { duration: Int ->
                            soundDuration = duration
                            meetingModeManager.setSoundDurationSeconds(duration)
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
                        alertLevelConfig = alertLevelConfig,
                        onLevelSelected = { level ->
                            selectedLevel = level
                        }
                    )
                }
                
                item {
                    AlertLevelConfiguration(
                        alertLevel = selectedLevel,
                        alertLevelConfig = alertLevelConfig,
                        onConfigChanged = { newConfig ->
                            alertLevelConfig = updateAlertLevelConfig(alertLevelConfig, selectedLevel, newConfig)
                            saveAlertLevelConfig(context, alertLevelConfig)
                            showSaveConfirmation = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertLevelSelector(
    selectedLevel: AlertLevel,
    alertLevelConfig: AlertLevelConfig,
    onLevelSelected: (AlertLevel) -> Unit
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
                // Built-in alert levels - LOW, HIGH, URGENT in one line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(AlertLevel.LOW, AlertLevel.HIGH, AlertLevel.URGENT).forEach { level ->
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
        }
    }
}

@Composable
fun AlertLevelConfiguration(
    alertLevel: AlertLevel,
    alertLevelConfig: AlertLevelConfig,
    onConfigChanged: (AlertConfig) -> Unit
) {
    
    val alertConfig = getAlertConfigForLevel(alertLevelConfig, alertLevel)
    
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Configuration for ${alertLevel.name.lowercase().replaceFirstChar { it.uppercase() }} level",
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

@Composable
fun MeetingModeSection(
    meetingModeEnabled: Boolean,
    soundDuration: Int,
    onMeetingModeToggle: (Boolean) -> Unit,
    onSoundDurationChange: (Int) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = "Meeting Mode",
                        modifier = Modifier.size(20.dp),
                        tint = if (meetingModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Meeting Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = meetingModeEnabled,
                    onCheckedChange = onMeetingModeToggle,
                    modifier = Modifier.scale(0.8f)
                )
            }
            
            if (meetingModeEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "When in meeting mode: only vibration works, no flash or sound. In normal mode: flash and sound work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Sound Duration Control
                Text(
                    text = "Sound Duration: ${soundDuration}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (soundDuration > 1) {
                                onSoundDurationChange(soundDuration - 1)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Text(
                        text = "${soundDuration}s",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Button(
                        onClick = {
                            if (soundDuration < 30) {
                                onSoundDurationChange(soundDuration + 1)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Control how long alert sounds play (1-30 seconds)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getAlertConfigForLevel(levelConfig: AlertLevelConfig, level: AlertLevel): AlertConfig {
    return try {
        when (level) {
            AlertLevel.LOW -> levelConfig.lowLevel
            AlertLevel.HIGH -> levelConfig.highLevel
            AlertLevel.URGENT -> levelConfig.urgentLevel
        }
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error getting alert config for level $level: ${e.message}")
        AlertConfig.Companion.getLowLevelDefaults() // Fallback to low defaults
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
            AlertLevel.HIGH -> levelConfig.copy(highLevel = newConfig)
            AlertLevel.URGENT -> levelConfig.copy(urgentLevel = newConfig)
        }
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error updating alert level config for $level: ${e.message}")
        levelConfig // Return original config if update fails
    }
}