package com.reminder.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.data.RepeatPattern
import com.reminder.app.data.RepeatType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BasicRepeatPatternSelector(
    repeatPattern: RepeatPattern,
    onRepeatPatternChange: (RepeatPattern) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔄 Repeat Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (repeatPattern.type != RepeatType.NONE) {
                    TextButton(
                        onClick = { showCustomDialog = true }
                    ) {
                        Text("Customize", fontSize = 12.sp)
                    }
                }
            }
            
            // Quick Selection Grid
            if (repeatPattern.type == RepeatType.NONE) {
                QuickRepeatOptions(
                    onOptionSelected = { option ->
                        onRepeatPatternChange(createRepeatPatternFromOption(option))
                    }
                )
            } else {
                CurrentRepeatSettings(
                    repeatPattern = repeatPattern,
                    onRepeatPatternChange = onRepeatPatternChange
                )
            }
        }
    }
    
    // Custom Dialog
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Repeat Settings") },
            text = {
                CustomRepeatDialogContent(
                    repeatPattern = repeatPattern,
                    onRepeatPatternChange = onRepeatPatternChange
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showCustomDialog = false }
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun QuickRepeatOptions(
    onOptionSelected: (RepeatOption) -> Unit
) {
    val quickOptions = listOf(
        RepeatOption(
            title = "No Repeat",
            description = "One-time reminder",
            icon = Icons.Default.Event,
            type = RepeatType.NONE
        ),
        RepeatOption(
            title = "Every 30 Min",
            description = "Every 30 minutes",
            icon = Icons.Default.Schedule,
            type = RepeatType.MINUTELY,
            interval = 30
        ),
        RepeatOption(
            title = "Every Hour",
            description = "Every hour",
            icon = Icons.Default.AccessTime,
            type = RepeatType.HOURLY,
            interval = 1
        ),
        RepeatOption(
            title = "Daily",
            description = "Every day",
            icon = Icons.Default.CalendarToday,
            type = RepeatType.DAILY,
            interval = 1
        ),
        RepeatOption(
            title = "Weekly",
            description = "Every week",
            icon = Icons.Default.DateRange,
            type = RepeatType.WEEKLY,
            interval = 1
        ),
        RepeatOption(
            title = "Monthly",
            description = "Every month",
            icon = Icons.Default.CalendarMonth,
            type = RepeatType.MONTHLY,
            interval = 1
        ),
        RepeatOption(
            title = "Weekdays",
            description = "Mon-Fri",
            icon = Icons.Default.Work,
            type = RepeatType.WEEKLY,
            interval = 1,
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        ),
        RepeatOption(
            title = "Weekends",
            description = "Sat-Sun",
            icon = Icons.Default.Weekend,
            type = RepeatType.WEEKLY,
            interval = 1,
            daysOfWeek = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        ),
        RepeatOption(
            title = "Custom",
            description = "Custom pattern",
            icon = Icons.Default.Settings,
            type = RepeatType.CUSTOM
        )
    )
    
    // Simple grid layout
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in quickOptions.indices step 2) {
            val endIndex = minOf(i + 2, quickOptions.size)
            val rowOptions = quickOptions.subList(i, endIndex)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    RepeatOptionCard(
                        option = option,
                        onClick = { onOptionSelected(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatOptionCard(
    option: RepeatOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CurrentRepeatSettings(
    repeatPattern: RepeatPattern,
    onRepeatPatternChange: (RepeatPattern) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current pattern display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Pattern:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                TextButton(
                    onClick = { onRepeatPatternChange(RepeatPattern()) }
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
            
            Text(
                text = formatRepeatPattern(repeatPattern),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Quick adjustments
            if (repeatPattern.type != RepeatType.NONE) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Quick Adjustments:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Decrease interval
                    OutlinedButton(
                        onClick = {
                            val newInterval = maxOf(1, repeatPattern.interval - 1)
                            onRepeatPatternChange(repeatPattern.copy(interval = newInterval))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Less Often", fontSize = 10.sp)
                    }
                    
                    // Increase interval
                    OutlinedButton(
                        onClick = {
                            onRepeatPatternChange(repeatPattern.copy(interval = repeatPattern.interval + 1))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("More Often", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomRepeatDialogContent(
    repeatPattern: RepeatPattern,
    onRepeatPatternChange: (RepeatPattern) -> Unit
) {
    var localPattern by remember { mutableStateOf(repeatPattern) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Repeat type selector
        Text(
            text = "Repeat Type:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        
        val repeatTypes = listOf(
            RepeatType.MINUTELY to "Minutes",
            RepeatType.HOURLY to "Hours", 
            RepeatType.DAILY to "Days",
            RepeatType.WEEKLY to "Weeks",
            RepeatType.MONTHLY to "Months",
            RepeatType.YEARLY to "Years"
        )
        
        // Simple chip selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeatTypes.chunked(3).forEach { chunk ->
                Column(modifier = Modifier.weight(1f)) {
                    chunk.forEach { (type, label) ->
                        @OptIn(ExperimentalMaterial3Api::class)
                        FilterChip(
                            onClick = {
                                localPattern = localPattern.copy(type = type, interval = 1)
                                onRepeatPatternChange(localPattern)
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            selected = localPattern.type == type
                        )
                    }
                }
            }
        }
        
        // Interval input
        Text(
            text = "Every ${getIntervalUnit(localPattern.type)}:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                        val newInterval = maxOf(1, localPattern.interval - 1)
                        localPattern = localPattern.copy(interval = newInterval)
                        onRepeatPatternChange(localPattern)
                    }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            
            Text(
                text = "${localPattern.interval}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            IconButton(
                onClick = {
                        localPattern = localPattern.copy(interval = localPattern.interval + 1)
                        onRepeatPatternChange(localPattern)
                    }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
        
        // End date
        Text(
            text = "End Date (Optional):",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        
        TextButton(
            onClick = {
                    // Simple end date setter - 30 days from now
                    localPattern = localPattern.copy(endDate = LocalDate.now().plusDays(30))
                    onRepeatPatternChange(localPattern)
                }
        ) {
            Text("End in 30 days", fontSize = 12.sp)
        }
        
        if (localPattern.endDate != null) {
            TextButton(
                onClick = {
                        localPattern = localPattern.copy(endDate = null)
                        onRepeatPatternChange(localPattern)
                }
            ) {
                Text("Remove End Date", fontSize = 12.sp)
            }
        }
    }
}

