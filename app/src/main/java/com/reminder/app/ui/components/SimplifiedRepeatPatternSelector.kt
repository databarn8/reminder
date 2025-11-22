package com.reminder.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.data.RepeatPattern
import com.reminder.app.data.RepeatType

@Composable
fun SimplifiedRepeatPatternSelector(
    repeatPattern: RepeatPattern,
    onRepeatPatternChange: (RepeatPattern) -> Unit
) {
    // Common repeat patterns with smart defaults
    val quickPatterns = listOf(
        RepeatPattern(type = RepeatType.NONE, interval = 0),
        RepeatPattern(type = RepeatType.MINUTELY, interval = 5),
        RepeatPattern(type = RepeatType.MINUTELY, interval = 15),
        RepeatPattern(type = RepeatType.MINUTELY, interval = 30),
        RepeatPattern(type = RepeatType.HOURLY, interval = 1),
        RepeatPattern(type = RepeatType.DAILY, interval = 1),
        RepeatPattern(type = RepeatType.WEEKLY, interval = 1),
        RepeatPattern(type = RepeatType.MONTHLY, interval = 1)
    )
    
    val patternLabels = mapOf(
        RepeatType.NONE to "Never",
        RepeatType.MINUTELY to "${quickPatterns[1].interval} min",
        RepeatType.HOURLY to "${quickPatterns[2].interval} min",
        RepeatType.DAILY to "Daily",
        RepeatType.WEEKLY to "Weekly",
        RepeatType.MONTHLY to "Monthly"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "🔄 Repeat",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Quick selection grid (using Column with Rows instead of LazyVerticalGrid)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Split into rows of 3 items each
            quickPatterns.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { pattern ->
                        val isSelected = repeatPattern.type == pattern.type && repeatPattern.interval == pattern.interval
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onRepeatPatternChange(pattern) }
                                .padding(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = patternLabels[pattern.type] ?: "Custom",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    // Fill remaining space if row has less than 3 items
                    if (rowItems.size < 3) {
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        // End condition options
        if (repeatPattern.type != RepeatType.NONE) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "End after:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            val endOptions = listOf(
                "3 times" to 3,
                "5 times" to 5,
                "10 times" to 10,
                "20 times" to 20
            )
            
            // End condition options (using Column with Rows instead of LazyVerticalGrid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Split into rows of 2 items each
                endOptions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (label, count) ->
                            val isSelected = repeatPattern.endDate != null &&
                                java.time.LocalDate.now().plusDays(count.toLong()).isEqual(repeatPattern.endDate)
                            
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val newEndDate = java.time.LocalDate.now().plusDays(count.toLong())
                                        onRepeatPatternChange(repeatPattern.copy(endDate = newEndDate))
                                    }
                                    .padding(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        // Fill remaining space if row has less than 2 items
                        if (rowItems.size < 2) {
                            repeat(2 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}