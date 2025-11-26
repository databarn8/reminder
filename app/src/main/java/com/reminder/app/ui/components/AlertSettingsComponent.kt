package com.reminder.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.data.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AlertSettingsSection(
    reminder: Reminder,
    alertConfig: AlertConfig = AlertConfig(),
    repeatPattern: RepeatPattern = RepeatPattern(),
    onAlertConfigChange: (AlertConfig) -> Unit,
    onRepeatPatternChange: (RepeatPattern) -> Unit,
    onTriggerPointsChange: (List<TriggerPoint>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp) // Reduced padding
        ) {
            // Header with expand/collapse functionality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ Alert Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                TextButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = if (isExpanded) "Hide" else "Configure",
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Animated expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 300, easing = EaseOutQuart)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 300, easing = EaseInQuart)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced spacing
                ) {
                    // Alert Type Selection
                    AlertTypeSelector(
                        selectedType = alertConfig.alertType,
                        onTypeSelected = { onAlertConfigChange(alertConfig.copy(alertType = it)) }
                    )
                    
                    // Repeat Configuration - Use basic selector
                    BasicRepeatPatternSelector(
                        repeatPattern = repeatPattern,
                        onRepeatPatternChange = onRepeatPatternChange
                    )
                    
                    // Alert Timing Configuration
                    AlertTimingConfigurationSection(
                        triggerPoints = reminder.getTriggerPointsList(),
                        onTriggerPointsChange = onTriggerPointsChange
                    )
                    
                    // Vibration Configuration
                    VibrationConfigurationSection(
                        vibrationConfig = alertConfig.vibration,
                        onVibrationConfigChange = { onAlertConfigChange(alertConfig.copy(vibration = it)) }
                    )
                    
                    // Sound Configuration
                    SoundConfigurationSection(
                        soundConfig = alertConfig.sound,
                        onSoundConfigChange = { onAlertConfigChange(alertConfig.copy(sound = it)) }
                    )
                    
                }
            }
        }
    }
}

@Composable
fun AlertTypeSelector(
    selectedType: AlertType,
    onTypeSelected: (AlertType) -> Unit
) {
    val alertTypes = listOf(
        AlertType.NOTIFICATION_ONLY to "Notification Only",
        AlertType.NOTIFICATION_VIBRATION to "Notification + Vibration",
        AlertType.NOTIFICATION_SOUND to "Notification + Sound",
        AlertType.FULL_ALERT to "Full Alert (All)"
    )
    
    Column {
        Text(
            text = "🔔 Alert Type",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Alert type chips in a grid
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            alertTypes.forEach { (type, description) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = getAlertTypeDescription(type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RepeatConfigurationSection(
    repeatPattern: RepeatPattern,
    onRepeatPatternChange: (RepeatPattern) -> Unit
) {
    Column {
        Text(
            text = "🔄 Repeat",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Repeat type selector
        val repeatTypes = listOf(
            RepeatType.NONE to "None",
            RepeatType.MINUTELY to "Minutely",
            RepeatType.HOURLY to "Hourly",
            RepeatType.DAILY to "Daily",
            RepeatType.WEEKLY to "Weekly",
            RepeatType.MONTHLY to "Monthly",
            RepeatType.YEARLY to "Yearly"
        )
        
        // Repeat type dropdown
        var expanded by remember { mutableStateOf(false) }
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = repeatTypes.find { it.first == repeatPattern.type }?.second ?: "None",
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    @OptIn(ExperimentalMaterial3Api::class)
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select repeat type")
                    }
                }
            )
            
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                repeatTypes.forEach { (type, description) ->
                    DropdownMenuItem(
                        text = { Text(description) },
                        onClick = {
                            onRepeatPatternChange(repeatPattern.copy(type = type))
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // Interval configuration (only show if not NONE)
    if (repeatPattern.type != RepeatType.NONE) {
        Spacer(modifier = Modifier.height(6.dp)) // Reduced spacing
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Every",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                @OptIn(ExperimentalMaterial3Api::class)
                OutlinedTextField(
                    value = repeatPattern.interval.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let {
                            onRepeatPatternChange(repeatPattern.copy(interval = it.coerceAtLeast(1)))
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                Text(
                    text = getIntervalUnit(repeatPattern.type),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // End date configuration
            Spacer(modifier = Modifier.height(6.dp)) // Reduced spacing
            
            var showEndDatePicker by remember { mutableStateOf(false) }
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndDatePicker = true },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), // Reduced padding
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📅 End Date (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = repeatPattern.endDate?.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)) 
                                ?: "No end date",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Select end date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // End date picker dialog (simplified for now)
            if (showEndDatePicker) {
                AlertDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    title = { Text("Set End Date") },
                    text = {
                        Column {
                            Text("Choose when to end repeating:")
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Quick options
                            val quickOptions = listOf(
                                "Never" to null,
                                "After 3 times" to LocalDate.now().plusDays(3 * when (repeatPattern.type) {
                                    RepeatType.MINUTELY -> 1
                                    RepeatType.HOURLY -> 1
                                    RepeatType.DAILY -> 1
                                    RepeatType.WEEKLY -> 7
                                    RepeatType.MONTHLY -> 30
                                    RepeatType.YEARLY -> 365
                                    else -> 1
                                }.toLong()),
                                "After 5 times" to LocalDate.now().plusDays(5 * when (repeatPattern.type) {
                                    RepeatType.MINUTELY -> 1
                                    RepeatType.HOURLY -> 1
                                    RepeatType.DAILY -> 1
                                    RepeatType.WEEKLY -> 7
                                    RepeatType.MONTHLY -> 30
                                    RepeatType.YEARLY -> 365
                                    else -> 1
                                }.toLong()),
                                "After 10 times" to LocalDate.now().plusDays(10 * when (repeatPattern.type) {
                                    RepeatType.MINUTELY -> 1
                                    RepeatType.HOURLY -> 1
                                    RepeatType.DAILY -> 1
                                    RepeatType.WEEKLY -> 7
                                    RepeatType.MONTHLY -> 30
                                    RepeatType.YEARLY -> 365
                                    else -> 1
                                }.toLong()),
                                "In 1 week" to LocalDate.now().plusWeeks(1),
                                "In 2 weeks" to LocalDate.now().plusWeeks(2),
                                "In 1 month" to LocalDate.now().plusMonths(1),
                                "In 3 months" to LocalDate.now().plusMonths(3),
                                "In 6 months" to LocalDate.now().plusMonths(6),
                                "In 1 year" to LocalDate.now().plusYears(1)
                            )
                            
                            quickOptions.forEach { (label, date) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onRepeatPatternChange(repeatPattern.copy(endDate = date))
                                            showEndDatePicker = false
                                        }
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = repeatPattern.endDate == date,
                                        onClick = {
                                            onRepeatPatternChange(repeatPattern.copy(endDate = date))
                                            showEndDatePicker = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showEndDatePicker = false
                            }
                        ) {
                            Text("Done")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun VibrationConfigurationSection(
    vibrationConfig: VibrationConfig,
    onVibrationConfigChange: (VibrationConfig) -> Unit
) {
    Column {
        Text(
            text = "📳 Vibration",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Enable vibration toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = vibrationConfig.enabled,
                onCheckedChange = { onVibrationConfigChange(vibrationConfig.copy(enabled = it)) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enable vibration",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        if (vibrationConfig.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Vibration pattern selector
            Text(
                text = "Pattern",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
        val patterns = listOf(
            VibrationPattern.SINGLE to "1x",
            VibrationPattern.DOUBLE to "2x",
            VibrationPattern.TRIPLE to "3x",
            VibrationPattern.LONG to "Long"
        )
            
            // Pattern selector - use RadioButtons for cleaner layout
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                patterns.forEach { (pattern, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = vibrationConfig.pattern == pattern,
                            onClick = { onVibrationConfigChange(vibrationConfig.copy(pattern = pattern)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Intensity slider
            Text(
                text = "Intensity: ${vibrationConfig.intensity.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            val intensityValues = listOf(VibrationIntensity.LIGHT, VibrationIntensity.MEDIUM, VibrationIntensity.STRONG)
            Slider(
                value = intensityValues.indexOf(vibrationConfig.intensity).toFloat(),
                onValueChange = { 
                    onVibrationConfigChange(vibrationConfig.copy(intensity = intensityValues[it.toInt()]))
                },
                valueRange = 0f..2f,
                steps = 2,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
        }
    }
}

@Composable
fun SoundConfigurationSection(
    soundConfig: SoundConfig,
    onSoundConfigChange: (SoundConfig) -> Unit
) {
    Column {
        Text(
            text = "🔊 Sound",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Enable sound toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = soundConfig.enabled,
                onCheckedChange = { onSoundConfigChange(soundConfig.copy(enabled = it)) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enable sound",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        if (soundConfig.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sound type selector
            Text(
                text = "Sound Type",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            val soundTypes = listOf(
                SoundType.CHIME to "Chime",
                SoundType.ALARM to "Alarm",
                SoundType.GENTLE to "Gentle"
            )
            
            // Sound type selector - use RadioButtons for cleaner layout
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                soundTypes.forEach { (type, description) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = soundConfig.type == type,
                            onClick = { onSoundConfigChange(soundConfig.copy(type = type)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Volume slider
            Text(
                text = "Volume: ${(soundConfig.volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Slider(
                value = soundConfig.volume,
                onValueChange = { onSoundConfigChange(soundConfig.copy(volume = it)) },
                valueRange = 0f..1f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )
            
        }
    }
}


// Helper functions
private fun getAlertTypeDescription(type: AlertType): String {
    return when (type) {
        AlertType.NOTIFICATION_ONLY -> "Basic notification only"
        AlertType.NOTIFICATION_VIBRATION -> "Notification with vibration feedback"
        AlertType.NOTIFICATION_SOUND -> "Notification with custom sound"
        AlertType.FULL_ALERT -> "All alert features enabled"
    }
}

@Composable
fun AlertTimingConfigurationSection(
    triggerPoints: List<TriggerPoint>,
    onTriggerPointsChange: (List<TriggerPoint>) -> Unit
) {
    Column {
        Text(
            text = "⏰ Alert Timing",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Quick timing options
        val quickOptions = listOf(
            "On time" to TriggerPoint(TriggerType.AT_DUE_TIME),
            "5 min before" to TriggerPoint(TriggerType.MINUTES_BEFORE, 5),
            "15 min before" to TriggerPoint(TriggerType.MINUTES_BEFORE, 15),
            "30 min before" to TriggerPoint(TriggerType.MINUTES_BEFORE, 30),
            "1 hour before" to TriggerPoint(TriggerType.HOURS_BEFORE, 1),
            "2 hours before" to TriggerPoint(TriggerType.HOURS_BEFORE, 2),
            "1 day before" to TriggerPoint(TriggerType.DAYS_BEFORE, 1),
            "1 week before" to TriggerPoint(TriggerType.WEEKS_BEFORE, 1)
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickOptions.chunked(2).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunk.forEach { (label, triggerPoint) ->
                        OutlinedButton(
                            onClick = {
                                // Update trigger points with the new selection
                                onTriggerPointsChange(listOf(triggerPoint))
                            },
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            // Custom minutes before input
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Custom minutes before:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentMinutesBefore = if (triggerPoints.isNotEmpty() &&
                    triggerPoints.first().type == TriggerType.MINUTES_BEFORE) {
                    triggerPoints.first().value.toString()
                } else {
                    "0"
                }
                
                @OptIn(ExperimentalMaterial3Api::class)
                OutlinedTextField(
                    value = currentMinutesBefore,
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { minutes ->
                            val newTriggerPoint = TriggerPoint(TriggerType.MINUTES_BEFORE, minutes)
                            onTriggerPointsChange(listOf(newTriggerPoint))
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    label = { Text("Minutes", fontSize = 10.sp) }
                )
                
                Text(
                    text = "minutes before due time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
