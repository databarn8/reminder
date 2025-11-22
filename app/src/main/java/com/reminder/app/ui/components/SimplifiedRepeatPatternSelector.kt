package com.reminder.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
        
        // Quick selection grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPatterns) { pattern ->
                val isSelected = repeatPattern.type == pattern.type && repeatPattern.interval == pattern.interval
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRepeatPatternChange(pattern) }
                        .padding(8.dp),
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
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(endOptions) { (label, count) ->
                    val isSelected = repeatPattern.endDate != null && 
                        java.time.LocalDate.now().plusDays(count.toLong()).isEqual(repeatPattern.endDate)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                val newEndDate = java.time.LocalDate.now().plusDays(count.toLong())
                                onRepeatPatternChange(repeatPattern.copy(endDate = newEndDate))
                            }
                            .padding(8.dp),
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
            }
        }
    }
}