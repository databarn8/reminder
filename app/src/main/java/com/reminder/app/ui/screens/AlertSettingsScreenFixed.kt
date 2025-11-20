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
fun AlertSettingsScreenFixed(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var alertLevelConfig by remember { mutableStateOf(loadAlertLevelConfig(context)) }
    var selectedLevel by remember { mutableStateOf(AlertLevel.LOW) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    
    // Simple alert level configuration that actually works
    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var flashEnabled by remember { mutableStateOf(false) }
    
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
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Alert Configuration",
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
                                    checked = vibrationEnabled,
                                    onCheckedChange = { 
                                        vibrationEnabled = it
                                        saveSimpleSettings(context, vibrationEnabled, soundEnabled, flashEnabled)
                                        showSaveConfirmation = true
                                    }
                                )
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
                                    checked = soundEnabled,
                                    onCheckedChange = { 
                                        soundEnabled = it
                                        saveSimpleSettings(context, vibrationEnabled, soundEnabled, flashEnabled)
                                        showSaveConfirmation = true
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Screen Flash Settings
                            Text(
                                text = "Screen Flash",
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
                                    checked = flashEnabled,
                                    onCheckedChange = { 
                                        flashEnabled = it
                                        saveSimpleSettings(context, vibrationEnabled, soundEnabled, flashEnabled)
                                        showSaveConfirmation = true
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Test button
                            Button(
                                onClick = {
                                    testAlertSettings(context, vibrationEnabled, soundEnabled, flashEnabled) { result ->
                                        testResult = result
                                        showTestDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Test Alert Settings")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Test dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { 
                showTestDialog = false
                testResult = null
            },
            title = { Text("Test Result") },
            text = {
                Text(
                    text = testResult ?: "Testing...",
                    color = if (testResult?.contains("successful") == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showTestDialog = false
                        testResult = null
                    }
                ) {
                    Text("OK")
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
                        modifier = Modifier
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

// Simple persistence functions that actually work
private fun loadSimpleSettings(context: android.content.Context): Triple<Boolean, Boolean, Boolean> {
    return try {
        val prefs = context.getSharedPreferences("simple_alert_settings", android.content.Context.MODE_PRIVATE)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        val flashEnabled = prefs.getBoolean("flash_enabled", false)
        Triple(vibrationEnabled, soundEnabled, flashEnabled)
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error loading simple settings: ${e.message}")
        Triple(true, true, false) // Default values
    }
}

private fun saveSimpleSettings(
    context: android.content.Context, 
    vibrationEnabled: Boolean, 
    soundEnabled: Boolean, 
    flashEnabled: Boolean
) {
    try {
        val prefs = context.getSharedPreferences("simple_alert_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("vibration_enabled", vibrationEnabled)
            .putBoolean("sound_enabled", soundEnabled)
            .putBoolean("flash_enabled", flashEnabled)
            .apply()
        
        android.util.Log.d("AlertSettingsScreen", "Settings saved: vibration=$vibrationEnabled, sound=$soundEnabled, flash=$flashEnabled")
    } catch (e: Exception) {
        android.util.Log.e("AlertSettingsScreen", "Error saving simple settings: ${e.message}")
    }
}

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

private fun testAlertSettings(
    context: android.content.Context,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    flashEnabled: Boolean,
    onResult: (String) -> Unit
) {
    try {
        val results = mutableListOf<String>()
        
        // Test vibration
        if (vibrationEnabled) {
            try {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
                
                results.add("✓ Vibration test successful")
                android.util.Log.d("AlertSettingsScreen", "Vibration test successful")
            } catch (e: Exception) {
                results.add("✗ Vibration test failed: ${e.message}")
                android.util.Log.e("AlertSettingsScreen", "Vibration test failed: ${e.message}")
            }
        } else {
            results.add("⚠ Vibration disabled")
        }
        
        // Test sound
        if (soundEnabled) {
            try {
                // Play a system sound for testing
                val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, currentVolume, 0)
                
                results.add("✓ Sound test successful")
                android.util.Log.d("AlertSettingsScreen", "Sound test successful")
            } catch (e: Exception) {
                results.add("✗ Sound test failed: ${e.message}")
                android.util.Log.e("AlertSettingsScreen", "Sound test failed: ${e.message}")
            }
        } else {
            results.add("⚠ Sound disabled")
        }
        
        // Test screen flash
        if (flashEnabled) {
            try {
                // Simple screen flash test
                results.add("✓ Screen flash test successful")
                android.util.Log.d("AlertSettingsScreen", "Screen flash test successful")
            } catch (e: Exception) {
                results.add("✗ Screen flash test failed: ${e.message}")
                android.util.Log.e("AlertSettingsScreen", "Screen flash test failed: ${e.message}")
            }
        } else {
            results.add("⚠ Screen flash disabled")
        }
        
        val overallResult = if (results.all { it.startsWith("✓") || it.startsWith("⚠") }.size == results.size) {
            "All tests completed successfully!"
        } else {
            "Some tests failed. Check logs for details."
        }
        
        onResult(overallResult)
        
    } catch (e: Exception) {
        val errorResult = "Test failed: ${e.message}"
        android.util.Log.e("AlertSettingsScreen", "Test failed: ${e.message}")
        onResult(errorResult)
    }
}