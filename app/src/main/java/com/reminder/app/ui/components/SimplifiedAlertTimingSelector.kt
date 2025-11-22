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
import com.reminder.app.data.TriggerPoint
import com.reminder.app.data.TriggerType

@Composable
fun SimplifiedAlertTimingSelector(
    selectedTriggerPoint: TriggerPoint?,
    onTriggerPointChange: (TriggerPoint) -> Unit
) {
    // Quick timing options with smart defaults
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "⏰ Alert Timing",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Quick timing options grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(quickOptions.size) { index ->
                val (label, triggerPoint) = quickOptions[index]
                val isSelected = selectedTriggerPoint == triggerPoint
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTriggerPointChange(triggerPoint) }
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}