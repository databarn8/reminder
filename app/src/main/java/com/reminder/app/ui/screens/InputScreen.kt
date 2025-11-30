package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import com.reminder.app.utils.SpeechManager
import com.reminder.app.utils.SmartVoiceProcessor
import com.reminder.app.utils.FileManager
import com.reminder.app.viewmodel.ReminderViewModel
import com.reminder.app.MainActivity
// import com.reminder.app.ui.components.AlertSettingsComponent // Not used, using AlertSettingsScreenFixed instead
import com.reminder.app.data.AlertConfig
import com.reminder.app.data.RepeatPattern
import com.reminder.app.data.RepeatType
import com.reminder.app.data.AlertLevel
import com.reminder.app.data.AlertType
import com.reminder.app.data.Reminder
import com.reminder.app.data.TriggerPoint
import com.reminder.app.data.TriggerType
import com.reminder.app.ui.components.SimplifiedAlertTimingSelector
import com.reminder.app.ui.components.SimplifiedRepeatPatternSelector
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// Enhanced extraction functions for common reminder patterns

fun extractCategory(text: String): String {
    return when {
        text.contains("work", ignoreCase = true) || text.contains("meeting", ignoreCase = true) || 
        text.contains("office", ignoreCase = true) || text.contains("client", ignoreCase = true) -> "Work"
        text.contains("family", ignoreCase = true) || text.contains("mom", ignoreCase = true) || 
        text.contains("dad", ignoreCase = true) || text.contains("kids", ignoreCase = true) -> "Family"
        text.contains("buy", ignoreCase = true) || text.contains("store", ignoreCase = true) || 
        text.contains("shop", ignoreCase = true) || text.contains("grocery", ignoreCase = true) -> "Shopping"
        text.contains("doctor", ignoreCase = true) || text.contains("appointment", ignoreCase = true) || 
        text.contains("health", ignoreCase = true) -> "Health"
        text.contains("call", ignoreCase = true) || text.contains("email", ignoreCase = true) || 
        text.contains("text", ignoreCase = true) -> "Communication"
        else -> "Personal"
    }
}

fun extractTime(text: String): String {
    // Only return time-related strings if they're explicitly mentioned as separate words
    // Use word boundaries to avoid false positives
    val words = text.lowercase().split(Regex("\\s+"))
    
    return when {
        words.contains("today") && !words.contains("tomorrow") -> {
            when {
                words.contains("morning") -> "Today Morning"
                words.contains("afternoon") -> "Today Afternoon"
                words.contains("evening") -> "Today Evening"
                words.contains("night") -> "Tonight"
                else -> "Today"
            }
        }
        words.contains("tomorrow") -> {
            when {
                words.contains("morning") -> "Tomorrow Morning"
                words.contains("afternoon") -> "Tomorrow Afternoon"
                words.contains("evening") -> "Tomorrow Evening"
                words.contains("night") -> "Tomorrow Night"
                else -> "Tomorrow"
            }
        }
        words.contains("next") && words.contains("week") -> "Next Week"
        words.contains("monday") -> "Monday"
        words.contains("tuesday") -> "Tuesday"
        words.contains("wednesday") -> "Wednesday"
        words.contains("thursday") -> "Thursday"
        words.contains("friday") -> "Friday"
        words.contains("saturday") -> "Saturday"
        words.contains("sunday") -> "Sunday"
        words.contains("morning") -> "Morning"
        words.contains("afternoon") -> "Afternoon"
        words.contains("evening") -> "Evening"
        words.contains("night") -> "Night"
        else -> ""
    }
}

fun extractDay(text: String): String {
    // Only return day-related strings if they're explicitly mentioned as separate words
    // Use word boundaries to avoid false positives
    val words = text.lowercase().split(Regex("\\s+"))
    
    return when {
        words.contains("today") && !words.contains("tomorrow") -> "Today"
        words.contains("tomorrow") -> "Tomorrow"
        words.contains("monday") -> "Monday"
        words.contains("tuesday") -> "Tuesday"
        words.contains("wednesday") -> "Wednesday"
        words.contains("thursday") -> "Thursday"
        words.contains("friday") -> "Friday"
        words.contains("saturday") -> "Saturday"
        words.contains("sunday") -> "Sunday"
        words.contains("next") && words.contains("week") -> "Next Week"
        else -> ""
    }
}

fun extractTimeOnly(text: String): String {
    val timePatterns = listOf(
        Regex("(\\d{1,2})\\s*[:\\.]?\\s*\\d{0,2}\\s*(a\\.?m\\.?|p\\.?m\\.?)", RegexOption.IGNORE_CASE),
        Regex("(\\d{1,2})\\s*(am|pm)", RegexOption.IGNORE_CASE),
        Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm)", RegexOption.IGNORE_CASE),
        Regex("(\\d{1,2})\\s*o'clock", RegexOption.IGNORE_CASE)
    )
    
    for (pattern in timePatterns) {
        val match = pattern.find(text)
        if (match != null) {
            val timeText = match.value.lowercase()
                .replace(".", "")
                .replace(" ", "")
                .trim()
            Log.d("TimeExtraction", "Found time: '$timeText' in text: '$text'")
            return timeText
        }
    }
    
    Log.d("TimeExtraction", "No time found in text: '$text'")
    return ""
}

fun extractPriority(text: String): String {
    return when {
        text.contains("urgent", ignoreCase = true) || text.contains("asap", ignoreCase = true) || 
        text.contains("important", ignoreCase = true) -> "High"
        text.contains("sometime", ignoreCase = true) || text.contains("when possible", ignoreCase = true) -> "Low"
        else -> "Medium"
    }
}

fun calculateReminderTime(text: String): Long {
    val now = System.currentTimeMillis()
    val oneDay = 24 * 60 * 60 * 1000L
    val oneHour = 60 * 60 * 1000L
    val calendar = java.util.Calendar.getInstance()
    
    // Add debug logging to understand what's happening
    android.util.Log.d("calculateReminderTime", "Input text: '$text'")
    android.util.Log.d("calculateReminderTime", "Current time: $now")
    
    // Extract specific time like "3pm", "3:00", etc.
    val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
    val timeMatch = timePattern.find(text)
    
    var targetHour = -1
    var targetMinute = 0
    
    if (timeMatch != null) {
        val hour = timeMatch.groupValues[1].toInt()
        val minute = timeMatch.groupValues[2].takeIf { it.isNotBlank() }?.toInt() ?: 0
        val ampm = timeMatch.groupValues[3].lowercase()
        
        targetHour = when {
            ampm == "am" -> if (hour == 12) 0 else hour
            ampm == "pm" -> if (hour == 12) 12 else hour + 12
            hour <= 12 -> hour // Default to AM for single digit hours
            else -> hour
        }
        targetMinute = minute
    }
    
    // Calculate base time (day offset)
    val baseTime = when {
        text.contains("today", ignoreCase = true) -> now
        text.contains("tomorrow", ignoreCase = true) -> now + oneDay
        text.contains("next week", ignoreCase = true) -> now + 7 * oneDay
        text.contains("monday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(1) * oneDay
        text.contains("tuesday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(2) * oneDay
        text.contains("wednesday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(3) * oneDay
        text.contains("thursday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(4) * oneDay
        text.contains("friday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(5) * oneDay
        text.contains("saturday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(6) * oneDay
        text.contains("sunday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(7) * oneDay
        else -> now + (10 * 60 * 1000L) // Default to today at 10 minutes from now
    }
    
    android.util.Log.d("calculateReminderTime", "Base time: $baseTime")
    android.util.Log.d("calculateReminderTime", "Target hour: $targetHour, Target minute: $targetMinute")
    
    // If we have a specific time, set it
    return if (targetHour != -1) {
        val targetCalendar = java.util.Calendar.getInstance()
        targetCalendar.timeInMillis = baseTime
        targetCalendar.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
        targetCalendar.set(java.util.Calendar.MINUTE, targetMinute)
        targetCalendar.set(java.util.Calendar.SECOND, 0)
        targetCalendar.set(java.util.Calendar.MILLISECOND, 0)
        
        android.util.Log.d("calculateReminderTime", "Specific time detected, target time: ${targetCalendar.timeInMillis}")
        
        // If time is in the past for today, move to tomorrow
        if (text.contains("today", ignoreCase = true) && targetCalendar.timeInMillis <= now) {
            targetCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            android.util.Log.d("calculateReminderTime", "Time in past for today, moved to tomorrow: ${targetCalendar.timeInMillis}")
        }
        
        targetCalendar.timeInMillis
    } else {
        // If no specific time, just use the base time (which already includes 10 minutes)
        // Don't add any additional time offsets
        android.util.Log.d("calculateReminderTime", "No specific time, using base time: $baseTime")
        baseTime
    }
}

fun getDaysUntilDayOfWeek(targetDay: Int): Int {
    val calendar = java.util.Calendar.getInstance()
    val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    val daysUntil = if (targetDay >= currentDay) targetDay - currentDay else 7 - (currentDay - targetDay)
    return if (daysUntil == 0) 7 else daysUntil // If today, schedule for next week
}

// Helper function to get the next occurrence of a specific weekday
fun getNextWeekday(today: LocalDate, targetDay: java.time.DayOfWeek): LocalDate {
    var current = today
    while (current.dayOfWeek != targetDay) {
        current = current.plusDays(1)
    }
    return current
}

// Helper function to get interval unit for repeat type
fun getIntervalUnit(type: RepeatType): String {
    return when (type) {
        RepeatType.MINUTELY -> "minute(s)"
        RepeatType.HOURLY -> "hour(s)"
        RepeatType.DAILY -> "day(s)"
        RepeatType.WEEKLY -> "week(s)"
        RepeatType.MONTHLY -> "month(s)"
        RepeatType.YEARLY -> "year(s)"
        else -> ""
    }
}

// Helper function to update day references in content text
fun updateContentDay(content: String, newDay: String): String {
    if (content.isBlank()) return content
    
    Log.d("InputScreen", "updateContentDay called with content='$content', newDay='$newDay'")
    
    // Regex patterns to match various day references
    val dayPatterns = listOf(
        Regex("\\btoday\\b", RegexOption.IGNORE_CASE),
        Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE),
        Regex("\\bmonday\\b", RegexOption.IGNORE_CASE),
        Regex("\\btuesday\\b", RegexOption.IGNORE_CASE),
        Regex("\\bwednesday\\b", RegexOption.IGNORE_CASE),
        Regex("\\bthursday\\b", RegexOption.IGNORE_CASE),
        Regex("\\bfriday\\b", RegexOption.IGNORE_CASE),
        Regex("\\bsaturday\\b", RegexOption.IGNORE_CASE),
        Regex("\\bsunday\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnext week\\b", RegexOption.IGNORE_CASE)
    )
    
    var updatedContent = content
    
    // Replace all day references with the new day
    dayPatterns.forEach { pattern ->
        val oldContent = updatedContent
        updatedContent = pattern.replace(updatedContent, newDay)
        Log.d("InputScreen", "Pattern ${pattern.pattern} replaced: old='$oldContent' -> new='$updatedContent'")
    }
    
    Log.d("InputScreen", "Final updated content: '$updatedContent'")
    return updatedContent
}


// Compact Slider Date Picker Dialog Component with Smart Defaults
@Composable
fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    var tempYear by remember { mutableStateOf(selectedDate.year.toFloat()) }
    var tempMonth by remember { mutableStateOf((selectedDate.monthValue - 1).toFloat()) } // 0-based
    var tempDay by remember { mutableStateOf(selectedDate.dayOfMonth.toFloat()) }
    
    // Smart date suggestions based on current date and context
    val smartDateSuggestions = remember(today) {
        val suggestions = mutableListOf<Pair<String, LocalDate>>()
        
        // Add today and tomorrow
        suggestions.add("Today" to today)
        suggestions.add("Tomorrow" to today.plusDays(1))
        
        // Add upcoming weekdays
        val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        val currentDayOfWeek = today.dayOfWeek.value % 7 // Convert to 0-6 (Sunday=0)
        
        weekdays.forEach { dayName ->
            val targetDay = java.time.DayOfWeek.valueOf(dayName.uppercase())
            val daysUntil = if (targetDay.value > currentDayOfWeek) {
                targetDay.value - currentDayOfWeek
            } else {
                7 - (currentDayOfWeek - targetDay.value)
            }
            if (daysUntil in 1..6) {
                suggestions.add(dayName to today.plusDays(daysUntil.toLong()))
            }
        }
        
        // Add weekend days
        val saturday = today.plusDays(((6 - currentDayOfWeek + 7) % 7).toLong())
        val sunday = today.plusDays(((0 - currentDayOfWeek + 7) % 7).toLong())
        if (saturday.isAfter(today)) suggestions.add("Saturday" to saturday)
        if (sunday.isAfter(today)) suggestions.add("Sunday" to sunday)
        
        // Add next week and end of month
        suggestions.add("Next Week" to today.plusDays(7))
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        if (endOfMonth.isAfter(today)) {
            suggestions.add("End of Month" to endOfMonth)
        }
        
        suggestions.take(9) // Limit to 9 suggestions for 3x3 grid
    }
    
    // Helper functions for date formatting
    fun formatMonth(month: Float): String {
        return java.time.Month.of((month.toInt() + 1)).name.take(3)
    }
    
    fun formatDate(y: Float, m: Float, d: Float): String {
        val year = y.toInt()
        val month = java.time.Month.of((m.toInt() + 1)).name.take(3)
        val day = d.toInt()
        return "$month $day, $year"
    }
    
    // Get max days for current month/year
    fun getMaxDays(): Int {
        return try {
            val year = tempYear.toInt()
            val month = tempMonth.toInt() + 1
            LocalDate.of(year, month, 1).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with current date preview
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "📅 Select Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Compact date display
                    Text(
                        text = formatDate(tempYear, tempMonth, tempDay),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Smart Date Suggestions (Compact 3x3 Grid)
                Text(
                    text = "🎯 Smart Suggestions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(smartDateSuggestions) { (label, date) ->
                        val isToday = label == "Today"
                        val isSelected = date.dayOfMonth == tempDay.toInt() &&
                                       date.monthValue == tempMonth.toInt() + 1 &&
                                       date.year == tempYear.toInt()
                        
                        OutlinedButton(
                            onClick = {
                                tempYear = date.year.toFloat()
                                tempMonth = (date.monthValue - 1).toFloat()
                                tempDay = date.dayOfMonth.toFloat()
                            },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                },
                                contentColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            Text(
                                text = label.replace(" ", "\n"),
                                fontSize = 8.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = 8.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Compact Date Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month control
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val newMonth = if (tempMonth > 0) tempMonth - 1 else 11f
                                    tempMonth = newMonth
                                    val maxDays = getMaxDays().toFloat()
                                    if (tempDay > maxDays) {
                                        tempDay = maxDays
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("−", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Text(
                                text = formatMonth(tempMonth),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(40.dp)
                            )
                            
                            IconButton(
                                onClick = {
                                    val newMonth = if (tempMonth < 11) tempMonth + 1 else 0f
                                    tempMonth = newMonth
                                    val maxDays = getMaxDays().toFloat()
                                    if (tempDay > maxDays) {
                                        tempDay = maxDays
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    // Day control
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val maxDays = getMaxDays().toFloat()
                            IconButton(
                                onClick = {
                                    val newDay = if (tempDay > 1) tempDay - 1 else maxDays
                                    tempDay = newDay
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("−", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Text(
                                text = "${tempDay.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(40.dp)
                            )
                            
                            IconButton(
                                onClick = {
                                    val newDay = if (tempDay < maxDays) tempDay + 1 else 1f
                                    tempDay = newDay
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    // Year control
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Year",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val newYear = (tempYear - 1).coerceAtLeast(today.year - 5f)
                                    tempYear = newYear
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("−", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Text(
                                text = "${tempYear.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(40.dp)
                            )
                            
                            IconButton(
                                onClick = {
                                    val newYear = (tempYear + 1).coerceAtMost(today.year + 10f)
                                    tempYear = newYear
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = {
                            try {
                                val newDate = LocalDate.of(tempYear.toInt(), tempMonth.toInt() + 1, tempDay.toInt())
                                onDateSelected(newDate)
                                onDismiss()
                            } catch (e: Exception) {
                                // Invalid date, ignore
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set Date", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// Helper function to get day suffix (1st, 2nd, 3rd, 4th, etc.)
fun getDaySuffix(day: Int): String {
    return when (day % 100) {
        11, 12, 13 -> "th"
        else -> when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}

// Modern Slider Time Picker Dialog Component with Smart Defaults
@Composable
fun TimePickerDialog(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    // Directly use selectedTime values - no separate state needed
    var hour by remember { mutableStateOf(selectedTime.hour.toFloat()) }
    var minute by remember { mutableStateOf(selectedTime.minute.toFloat()) }
    
    android.util.Log.d("TimePickerDialog", "Dialog opened with selectedTime=$selectedTime, hour=$hour, minute=$minute")
    
    // Smart time defaults based on current time
    val currentTime = LocalTime.now()
    val smartTimeDefaults = remember(currentTime) {
        val morningStart = 6f
        val morningEnd = 11f
        val afternoonStart = 12f
        val afternoonEnd = 17f
        val eveningStart = 18f
        val eveningEnd = 22f
        
        when {
            currentTime.hour < 6 -> listOf(8f, 9f, 10f) // Early morning -> suggest morning times
            currentTime.hour in 6..11 -> listOf(12f, 14f, 16f) // Morning -> suggest afternoon times
            currentTime.hour in 12..17 -> listOf(18f, 19f, 20f) // Afternoon -> suggest evening times
            currentTime.hour in 18..22 -> listOf(8f, 9f, 14f) // Evening -> suggest next day morning/afternoon
            else -> listOf(9f, 12f, 15f) // Late night -> suggest next day times
        }
    }
    
    // Helper functions for time formatting
    fun formatHour(h: Float): String {
        val hour24 = h.toInt()
        val displayHour = when {
            hour24 == 0 -> "12"
            hour24 <= 12 -> hour24.toString()
            else -> (hour24 - 12).toString()
        }
        val period = if (hour24 < 12) "AM" else "PM"
        return "$displayHour $period"
    }
    
    fun formatTime(h: Float, m: Float): String {
        val hour24 = h.toInt()
        val displayHour = when {
            hour24 == 0 -> "12"
            hour24 <= 12 -> hour24.toString()
            else -> (hour24 - 12).toString()
        }
        val period = if (hour24 < 12) "AM" else "PM"
        val hourInt = displayHour.toIntOrNull() ?: 12
        return String.format("%02d:%02d %s", hourInt, m.toInt(), period)
    }
    
    // Smart time suggestions based on context
    val smartTimeSuggestions = remember(currentTime) {
        val baseSuggestions = mutableListOf<Pair<String, Pair<Float, Float>>>()
        
        // Add smart defaults first
        smartTimeDefaults.forEach { h ->
            baseSuggestions.add("${h.toInt()}:00 AM".replace("AM", if (h >= 12) "PM" else "AM") to Pair(h, 0f))
        }
        
        // Add common times
        val commonTimes = listOf(
            "1:00 AM" to Pair(1f, 0f),
            "2:00 AM" to Pair(2f, 0f),
            "3:00 AM" to Pair(3f, 0f),
            "4:00 AM" to Pair(4f, 0f),
            "5:00 AM" to Pair(5f, 0f),
            "6:00 AM" to Pair(6f, 0f),
            "7:00 AM" to Pair(7f, 0f),
            "8:00 AM" to Pair(8f, 0f),
            "9:00 AM" to Pair(9f, 0f),
            "10:00 AM" to Pair(10f, 0f),
            "11:00 AM" to Pair(11f, 0f),
            "12:00 PM" to Pair(12f, 0f),
            "1:00 PM" to Pair(13f, 0f),
            "2:00 PM" to Pair(14f, 0f),
            "3:00 PM" to Pair(15f, 0f),
            "4:00 PM" to Pair(16f, 0f),
            "5:00 PM" to Pair(17f, 0f),
            "6:00 PM" to Pair(18f, 0f),
            "7:00 PM" to Pair(19f, 0f),
            "8:00 PM" to Pair(20f, 0f),
            "9:00 PM" to Pair(21f, 0f),
            "10:00 PM" to Pair(22f, 0f),
            "11:00 PM" to Pair(23f, 0f),
            "12:00 AM" to Pair(0f, 0f)
        )
        
        // Add common times that aren't already in smart defaults
        commonTimes.forEach { (label, time) ->
            if (!baseSuggestions.any { it.second == time }) {
                baseSuggestions.add(label to time)
            }
        }
        
        baseSuggestions.take(12) // Limit to 12 suggestions for better UI
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with current time preview
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "⏰ Select Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Large time display
                    Text(
                        text = formatTime(hour, minute),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Smart Time Suggestions (Compact)
                Text(
                    text = "🎯 Smart Suggestions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(smartTimeSuggestions.take(8)) { (timeLabel, timePair) ->
                        val (h, m) = timePair
                        val isSmartDefault = smartTimeDefaults.any { it == h }
                        OutlinedButton(
                            onClick = {
                                hour = h
                                minute = m
                            },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSmartDefault) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isSmartDefault) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = timeLabel.replace(":00 ", "").replace(" AM", "am").replace(" PM", "pm"),
                                fontSize = 10.sp,
                                fontWeight = if (isSmartDefault) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Hour Slider (Compact)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                     
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(6.dp)
                     ) {
                         IconButton(
                             onClick = {
                                 val newHour = (hour.toInt() - 1).coerceIn(0, 23)
                                 hour = newHour.toFloat()
                             },
                             modifier = Modifier.size(32.dp)
                         ) {
                             Text("−", style = MaterialTheme.typography.bodyMedium)
                         }
                         
                         Slider(
                             value = hour,
                             onValueChange = { hour = it },
                             valueRange = 0f..23f,
                             steps = 22,
                             modifier = Modifier.weight(1f),
                             colors = SliderDefaults.colors(
                                 thumbColor = MaterialTheme.colorScheme.primary,
                                 activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                 inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                             )
                         )
                         
                         IconButton(
                             onClick = {
                                 val newHour = (hour.toInt() + 1).coerceIn(0, 23)
                                 hour = newHour.toFloat()
                             },
                             modifier = Modifier.size(32.dp)
                         ) {
                             Text("+", style = MaterialTheme.typography.bodyMedium)
                         }
                     }
                     
                    Text(
                        text = formatHour(hour),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Minute Slider (Compact)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Minute",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                     
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(6.dp)
                     ) {
                         IconButton(
                             onClick = {
                                 val newMinute = (minute.toInt() - 1).coerceIn(0, 59)
                                 minute = newMinute.toFloat()
                             },
                             modifier = Modifier.size(32.dp)
                         ) {
                             Text("−", style = MaterialTheme.typography.bodyMedium)
                         }
                         
                         Slider(
                             value = minute,
                             onValueChange = { minute = it },
                             valueRange = 0f..59f,
                             steps = 58,
                             modifier = Modifier.weight(1f),
                             colors = SliderDefaults.colors(
                                 thumbColor = MaterialTheme.colorScheme.secondary,
                                 activeTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                 inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                             )
                         )
                         
                         IconButton(
                             onClick = {
                                 val newMinute = (minute.toInt() + 1).coerceIn(0, 59)
                                 minute = newMinute.toFloat()
                             },
                             modifier = Modifier.size(32.dp)
                         ) {
                             Text("+", style = MaterialTheme.typography.bodyMedium)
                         }
                     }
                     
                    Text(
                        text = String.format("%02d min", minute.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick Intervals (15, 30, 45 minutes)
                Text(
                    text = "Quick Intervals",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 15, 30, 45).forEach { min ->
                        OutlinedButton(
                            onClick = { minute = min.toFloat() },
                            modifier = Modifier.weight(1f).height(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (minute.toInt() == min) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                contentColor = if (minute.toInt() == min) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = "${min}m",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = {
                            onTimeSelected(LocalTime.of(hour.toInt(), minute.toInt()))
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set Time", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    viewModel: ReminderViewModel,
    speechManager: SpeechManager,
    reminderId: Int?,
    onBack: () -> Unit,
    onConfirm: (String, Long) -> Unit,
    onCalendarClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    // State for navigation to alert settings screen
    var showAlertSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // File management state
    val fileManager = remember { FileManager(context) }
    var showFileMenu by remember { mutableStateOf(false) }
    var showSavedFilesDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var saveAsFileName by remember { mutableStateOf("") }
    var isFileMode by remember { mutableStateOf(false) } // Toggle between reminder and file mode
    
    // Single content field - supports both typing and voice
    var content by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var loadedReminder by remember { mutableStateOf<Reminder?>(null) }
    
    // Priority selection
    var selectedPriority by remember { mutableStateOf(5) }
    
    // Enhanced alert configuration state
    var alertConfig by remember { mutableStateOf(AlertConfig()) }
    var repeatPattern by remember { mutableStateOf(RepeatPattern()) }
    var alertLevel by remember { mutableStateOf(AlertLevel.LOW) }
    
    // State for simplified components
    var selectedTriggerPoint by remember { mutableStateOf<TriggerPoint?>(TriggerPoint(TriggerType.AT_DUE_TIME)) }
    
    // Enhanced date/time state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now().plusMinutes(15)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTimeSuggestions by remember { mutableStateOf(false) }
    var whenDay by remember { mutableStateOf("") }
    var whenTime by remember { mutableStateOf("") }
    var focusTimeField by remember { mutableStateOf(false) }
    
    // Common time suggestions
    val timeSuggestions = listOf(
        "12:00 AM", "1:00 AM", "2:00 AM", "3:00 AM", "4:00 AM", "5:00 AM",
        "6:00 AM", "7:00 AM", "8:00 AM", "9:00 AM",
        "10:00 AM", "11:00 AM", "12:00 PM",
        "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM",
        "5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM", "9:00 PM", "10:00 PM", "11:00 PM",
        "Morning", "Afternoon", "Evening", "Night"
    )
    val priorityOptions = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val priorityLabels = mapOf(
        1 to "Very Low", 2 to "Low", 3 to "Low-Medium", 4 to "Medium-Low",
        5 to "Medium", 6 to "Medium-High", 7 to "High-Medium", 
        8 to "High", 9 to "Very High", 10 to "Urgent"
    )
    
    // Check for file picker result from MainActivity
    LaunchedEffect(Unit) {
        MainActivity.selectedFileUri?.let { uri ->
            scope.launch {
                val success = fileManager.loadFileFromUri(uri)
                if (success) {
                    content = fileManager.currentFileContent.value
                    isFileMode = true
                }
                MainActivity.selectedFileUri = null // Clear after processing
            }
        }
    }
    
    // Load existing reminder data if editing
    LaunchedEffect(reminderId) {
        reminderId?.let { id ->
            scope.launch {
                try {
                    // Add a small delay to ensure database operations are complete
                    kotlinx.coroutines.delay(100)
                    val reminder = viewModel.getReminderById(id)
                    if (reminder != null) {
                        loadedReminder = reminder
                        content = reminder.content
                        selectedPriority = reminder.importance
                        whenDay = reminder.whenDay ?: ""
                        whenTime = reminder.whenTime ?: ""
                        alertConfig = reminder.getAlertConfigData()
                        repeatPattern = reminder.getRepeatPatternData()
                        alertLevel = reminder.getAlertLevelEnum()
                        
                        // Initialize selectedTriggerPoint from reminder's trigger points
                        val triggerPoints = reminder.getTriggerPointsList()
                        selectedTriggerPoint = triggerPoints.firstOrNull()
                        android.util.Log.d("InputScreen", "Loaded whenDay='$whenDay', whenTime='$whenTime' from database")
                        android.util.Log.d("InputScreen", "Loaded alertConfig=${alertConfig.alertType}, repeatPattern=${repeatPattern.type}")
                        
                        // Restore selectedDate and selectedTime from reminderTime
                        try {
                            // Always restore date from reminderTime
                            val reminderDateTime = java.time.Instant.ofEpochMilli(reminder.reminderTime)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                            selectedDate = reminderDateTime.toLocalDate()
                            
                            // Try to parse whenTime if it exists, otherwise use reminderTime
                            if (!reminder.whenTime.isNullOrBlank()) {
                                android.util.Log.d("InputScreen", "Attempting to parse whenTime: '${reminder.whenTime}'")
                                
                                // Try multiple time formats
                                val timeFormats = listOf(
                                    Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm)", RegexOption.IGNORE_CASE),  // 3:30pm, 11:45 am
                                    Regex("(\\d{1,2})\\s*(am|pm)", RegexOption.IGNORE_CASE),           // 3pm, 11 am
                                    Regex("(\\d{1,2}):(\\d{2})"),                                   // 15:30, 09:45
                                    Regex("(\\d{1,2})")                                              // 15, 9
                                )
                                
                                var parsed = false
                                for (format in timeFormats) {
                                    val match = format.find(reminder.whenTime)
                                    if (match != null) {
                                        try {
                                            val hour = match.groupValues[1].toInt()
                                            val minute = match.groupValues.getOrNull(2)?.toInt() ?: 0
                                            val ampm = match.groupValues.getOrNull(3)?.lowercase()
                                            
                                            val parsedHour = when {
                                                ampm == "am" -> if (hour == 12) 0 else hour
                                                ampm == "pm" -> if (hour == 12) 12 else hour + 12
                                                hour > 12 -> hour // Already 24-hour format
                                                else -> if (hour <= 12 && ampm == null && hour != 12) hour + 12 else hour
                                            }
                                            
                                            selectedTime = java.time.LocalTime.of(parsedHour.coerceIn(0, 23), minute.coerceIn(0, 59))
                                            android.util.Log.d("InputScreen", "Successfully parsed selectedTime='$selectedTime' from whenTime='${reminder.whenTime}'")
                                            parsed = true
                                            break
                                        } catch (e: Exception) {
                                            android.util.Log.d("InputScreen", "Failed to parse with format: ${e.message}")
                                            continue
                                        }
                                    }
                                }
                                
                                if (!parsed) {
                                    android.util.Log.d("InputScreen", "Could not parse whenTime, using reminderTime")
                                    selectedTime = reminderDateTime.toLocalTime()
                                }
                            } else {
                                // No whenTime saved, use reminderTime
                                selectedTime = reminderDateTime.toLocalTime()
                                android.util.Log.d("InputScreen", "No whenTime saved, using selectedTime='$selectedTime' from reminderTime")
                            }
                            
                        } catch (e: Exception) {
                            // Fallback to current date/time if parsing fails
                            selectedDate = java.time.LocalDate.now()
                            selectedTime = java.time.LocalTime.NOON
                            android.util.Log.d("InputScreen", "Failed to parse time: ${e.message}, using NOON")
                        }
                        
                        android.util.Log.d("InputScreen", "Loaded reminder for editing: id=${reminder.id}, reminderTime=${reminder.reminderTime}, whenDay=${reminder.whenDay}, whenTime=${reminder.whenTime}")
                        android.util.Log.d("InputScreen", "Final selectedTime='$selectedTime', selectedDate='$selectedDate'")
                        
                        // Force UI update by adding a small delay after setting values
                        kotlinx.coroutines.delay(50)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("InputScreen", "Error loading reminder: ${e.message}")
                }
            }
        }
    }
    
    // Reload data when screen becomes visible (handle potential stale data)
    LaunchedEffect(reminderId, loadedReminder) {
        reminderId?.let { id ->
            if (loadedReminder != null) {
                scope.launch {
                    // Double-check the data after a short delay to catch any race conditions
                    kotlinx.coroutines.delay(300)
                    val freshReminder = viewModel.getReminderById(id)
                    val currentReminderTime = loadedReminder?.reminderTime ?: 0
                    val freshReminderTime = freshReminder?.reminderTime ?: 0
                    if (freshReminder != null && currentReminderTime != freshReminderTime) {
                        android.util.Log.d("InputScreen", "Detected stale data, reloading: old=${loadedReminder?.reminderTime}, new=${freshReminder.reminderTime}")
                        loadedReminder = freshReminder
                        content = freshReminder.content
                        selectedPriority = freshReminder.importance
                        whenDay = freshReminder.whenDay ?: ""
                        whenTime = freshReminder.whenTime ?: ""
                        alertConfig = freshReminder.getAlertConfigData()
                        repeatPattern = freshReminder.getRepeatPatternData()
                        alertLevel = freshReminder.getAlertLevelEnum()
                        
                        // Update date/time fields
                        try {
                            val reminderDateTime = java.time.Instant.ofEpochMilli(freshReminder.reminderTime)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                            selectedDate = reminderDateTime.toLocalDate()
                            selectedTime = reminderDateTime.toLocalTime()
                        } catch (e: Exception) {
                            selectedDate = java.time.LocalDate.now()
                            selectedTime = java.time.LocalTime.NOON
                        }
                    }
                }
            }
        }
    }
    
    // Speech states
    val isListening by speechManager.isListening.collectAsState()
    val speechResult by speechManager.speechResult.collectAsState()
    
    // Function to launch keyboard voice input
    fun launchKeyboardVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you need to remember?")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            (context as? Activity)?.startActivityForResult(intent, 1002)
        } catch (e: Exception) {
            Log.e("InputScreen", "Error launching keyboard voice input: ${e.message}")
        }
    }
    
    // File operations functions
    fun launchFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*" // Accept all file types
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, "Select a file")
            }
            (context as? Activity)?.startActivityForResult(intent, 1003)
        } catch (e: Exception) {
            Log.e("InputScreen", "Error launching file picker: ${e.message}")
        }
    }
    
    fun handleFileMenuAction(action: String) {
        when (action) {
            "new" -> {
                fileManager.createNewFile()
                content = ""
                isFileMode = true
            }
            "open" -> {
                launchFilePicker()
            }
            "saved" -> {
                showSavedFilesDialog = true
            }
            "save" -> {
                if (fileManager.currentFileUri.value != null) {
                    scope.launch {
                        fileManager.currentFileContent.value = content
                        val success = fileManager.saveToOriginalFile()
                        if (success) {
                            isFileMode = false // Switch back to reminder mode after saving
                        }
                    }
                } else {
                    showSaveAsDialog = true
                }
            }
            "save_as" -> {
                saveAsFileName = fileManager.currentFileName.value.ifBlank { "reminder.txt" }
                showSaveAsDialog = true
            }
            "discard" -> {
                scope.launch {
                    val success = fileManager.discardChanges()
                    if (success) {
                        content = fileManager.currentFileContent.value
                    }
                }
            }
            "close_file" -> {
                fileManager.currentFileName.value = ""
                fileManager.currentFileContent.value = ""
                fileManager.currentFileUri.value = null
                fileManager.isFileModified.value = false
                content = ""
                isFileMode = false
            }
        }
        showFileMenu = false
    }
    
    // Simple processing for user content
    LaunchedEffect(content) {
        if (content.isNotBlank() && !isProcessing) {
            isProcessing = true
            scope.launch {
                try {
                    // Simple processing with delay for better UX
                    kotlinx.coroutines.delay(300)
                    
                    // Extract basic info
                    val processedCategory = extractCategory(content)
                    val processedTime = extractTime(content)
                    val processedPriority = extractPriority(content)
                    
                    // Extract day and time separately
                    val extractedDay = extractDay(content)
                    val extractedTime = extractTimeOnly(content)
                    
                    // Always update day and time fields when detected in message
                    // But preserve user input if they've manually set different values
                    if (extractedDay.isNotBlank()) {
                        // Only update if the detected day is different from current
                        if (whenDay != extractedDay) {
                            whenDay = extractedDay
                            Log.d("InputScreen", "Updated whenDay from '$whenDay' to '$extractedDay' based on message content")
                            
                            // Also update selectedDate to match the new day
                            val newDate = when (extractedDay) {
                                "Today" -> LocalDate.now()
                                "Tomorrow" -> LocalDate.now().plusDays(1)
                                "Monday" -> getNextWeekday(LocalDate.now(), java.time.DayOfWeek.MONDAY)
                                "Tuesday" -> getNextWeekday(LocalDate.now(), java.time.DayOfWeek.TUESDAY)
                                "Wednesday" -> getNextWeekday(LocalDate.now(), java.time.DayOfWeek.WEDNESDAY)
                                "Thursday" -> getNextWeekday(LocalDate.now(), java.time.DayOfWeek.THURSDAY)
                                "Friday" -> getNextWeekday(LocalDate.now(), java.time.DayOfWeek.FRIDAY)
                                "Saturday" -> getNextWeekday(LocalDate.now(), java.time.DayOfWeek.SATURDAY)
                                "Sunday" -> getNextWeekday(LocalDate.now(), java.time.DayOfWeek.SUNDAY)
                                else -> selectedDate // Keep current date if no match
                            }
                            selectedDate = newDate
                            Log.d("InputScreen", "Updated selectedDate to '$newDate' based on extractedDay='$extractedDay'")
                        }
                    }
                    if (extractedTime.isNotBlank()) {
                        // Only update if the detected time is different from current
                        if (whenTime != extractedTime) {
                            whenTime = extractedTime
                            Log.d("InputScreen", "Updated whenTime from '$whenTime' to '$extractedTime' based on message content")
                            
                            // Also update selectedTime if a specific time was detected
                            val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
                            val timeMatch = timePattern.find(extractedTime)
                            if (timeMatch != null) {
                                try {
                                    val hour = timeMatch.groupValues[1].toInt()
                                    val minute = timeMatch.groupValues[2].takeIf { it.isNotBlank() }?.toInt() ?: 0
                                    val ampm = timeMatch.groupValues[3].lowercase()
                                    
                                    val parsedHour = when {
                                        ampm == "am" -> if (hour == 12) 0 else hour
                                        ampm == "pm" -> if (hour == 12) 12 else hour + 12
                                        hour <= 12 -> hour // Default to AM for single digit hours
                                        else -> hour
                                    }
                                    
                                    selectedTime = LocalTime.of(parsedHour.coerceIn(0, 23), minute.coerceIn(0, 59))
                                    Log.d("InputScreen", "Updated selectedTime to '$selectedTime' based on extractedTime='$extractedTime'")
                                } catch (e: Exception) {
                                    Log.d("InputScreen", "Failed to parse time: ${e.message}")
                                }
                            }
                        }
                    }
                    
                    // Don't auto-update selectedDate when day info is detected
                    // This preserves the date set by the user in the date picker
                    
                    Log.d("InputScreen", "Processed: category='$processedCategory', time='$processedTime', priority='$processedPriority', day='$extractedDay', timeOnly='$extractedTime'")
                    Log.d("InputScreen", "Preserved user input: whenDay='$whenDay', whenTime='$whenTime'")
                } catch (e: Exception) {
                    Log.e("InputScreen", "Error processing content: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }
    
    

    
    // Handle speech results
    LaunchedEffect(speechResult) {
        speechResult?.let { result ->
            if (!result.contains("permission") && !result.contains("not available") && !result.contains("error") && !result.contains("Try:") && !result.contains("Hey Google")) {
                content = result
                
            }
            speechManager.clearSpeechResult()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Show file name or mode indicator
                    Text(
                        text = if (isFileMode) {
                            fileManager.getDisplayFileName()
                        } else {
                            ""
                        },
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp
                    )
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        // File menu button
                        IconButton(onClick = { showFileMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "File Menu")
                        }
                    }
                },
                actions = {
                    // Mode toggle button
                    Button(
                        onClick = {
                            isFileMode = !isFileMode
                            if (!isFileMode) {
                                // Switching to file mode, create new file if needed
                                if (fileManager.currentFileName.value.isBlank()) {
                                    fileManager.createNewFile()
                                }
                                content = fileManager.currentFileContent.value
                            } else {
                                // Switching to reminder mode, update file content first
                                fileManager.updateContent(content)
                            }
                        },
                        modifier = Modifier.height(32.dp).padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFileMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isFileMode) "📝 Reminder" else "📁 File",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Home icon
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Configure button in top bar
                    IconButton(onClick = { showAlertSettings = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configure Alerts",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    // Save button in top bar
                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                // Create reminder with enhanced date/time
                                val reminderDateTime = java.time.LocalDateTime.of(selectedDate, selectedTime)
                                val reminderTimeMillis = reminderDateTime
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                
                                // Only update whenDay and whenTime if they are empty (preserve user input)
                                if (whenDay.isBlank()) {
                                    whenDay = when {
                                        selectedDate == LocalDate.now() -> "Today"
                                        selectedDate == LocalDate.now().plusDays(1) -> "Tomorrow"
                                        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE"))
                                    }
                                }
                                if (whenTime.isBlank()) {
                                    whenTime = selectedTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                                }
                                
                                // Store the selected trigger point in the reminder (will be used in notification scheduling)
                                
                                // Use new alert configuration and repeat pattern
                                val alertConfigJson = kotlinx.serialization.json.Json.encodeToString(
                                    AlertConfig.serializer(),
                                    alertConfig
                                )
                                val repeatPatternJson = kotlinx.serialization.json.Json.encodeToString(
                                    RepeatPattern.serializer(),
                                    repeatPattern
                                )
                                
                                // Store the selected trigger point as JSON using custom serialization
                                val triggerPoint = selectedTriggerPoint ?: TriggerPoint(TriggerType.AT_DUE_TIME)
                                val triggerPointsJson = org.json.JSONArray().apply {
                                    put(org.json.JSONObject().apply {
                                        put("type", triggerPoint.type.name)
                                        put("value", triggerPoint.value)
                                        put("customOffsetMs", triggerPoint.customOffsetMs)
                                        put("enableFlash", triggerPoint.enableFlash)
                                        put("enableSound", triggerPoint.enableSound)
                                        put("enableVibration", triggerPoint.enableVibration)
                                    })
                                }.toString()
                                
                                val reminder = Reminder(
                                    content = content,
                                    category = extractCategory(content),
                                    importance = selectedPriority,
                                    reminderTime = reminderTimeMillis,
                                    whenDay = whenDay.ifBlank { null },
                                    whenTime = whenTime.ifBlank { null },
                                    voiceInput = content,
                                    isProcessed = true,
                                    triggerPoints = triggerPointsJson, // Store the selected trigger point
                                    alertConfig = alertConfigJson,
                                    repeatPattern = repeatPatternJson,
                                    alertLevel = alertLevel.name
                                )
                                
                                if (reminderId != null) {
                                    // Update existing reminder
                                    val updatedReminder = reminder.copy(id = reminderId)
                                    android.util.Log.d("InputScreen", "Updating reminder: id=${reminderId}, newReminderTime=${reminder.reminderTime}, whenDay=${whenDay}, whenTime=${whenTime}")
                                    viewModel.updateReminder(updatedReminder)
                                    // Add a small delay before navigating back to ensure database update is complete
                                    scope.launch {
                                        kotlinx.coroutines.delay(200)
                                    }
                                } else {
                                    // Add new reminder
                                    android.util.Log.d("InputScreen", "Adding new reminder: reminderTime=${reminder.reminderTime}, whenDay=${whenDay}, whenTime=${whenTime}")
                                    viewModel.addReminder(reminder)
                                }
                                onBack()
                            }
                        },
                        enabled = content.isNotBlank() && !isProcessing
                    ) {
                        Text("💾 Save")
                    }
                    
                    IconButton(onClick = onCalendarClick) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    }
                }
            )
        }
    ) { paddingValues ->
    // File menu dropdown
    if (showFileMenu) {
        DropdownMenu(
            expanded = showFileMenu,
            onDismissRequest = { showFileMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("📄 New File") },
                onClick = { handleFileMenuAction("new") },
                leadingIcon = { Icon(Icons.Default.Create, contentDescription = "New File") }
            )
            DropdownMenuItem(
                text = { Text("📂 Open File") },
                onClick = { handleFileMenuAction("open") },
                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = "Open File") }
            )
            DropdownMenuItem(
                text = { Text("📁 Saved Files") },
                onClick = { handleFileMenuAction("saved") },
                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = "Saved Files") }
            )
            if (isFileMode) {
                DropdownMenuItem(
                    text = { Text("💾 Save") },
                    onClick = { handleFileMenuAction("save") },
                    leadingIcon = { Icon(Icons.Default.Save, contentDescription = "Save") }
                )
                DropdownMenuItem(
                    text = { Text("💾 Save As...") },
                    onClick = { handleFileMenuAction("save_as") },
                    leadingIcon = { Icon(Icons.Default.Save, contentDescription = "Save As") }
                )
                DropdownMenuItem(
                    text = { Text("🗑️ Discard Changes") },
                    onClick = { handleFileMenuAction("discard") },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = "Discard") }
                )
                DropdownMenuItem(
                    text = { Text("❌ Close File") },
                    onClick = { handleFileMenuAction("close_file") },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = "Close File") }
                )
            }
        }
    }
    
    // Saved files dialog
    if (showSavedFilesDialog) {
        val savedFiles = remember { fileManager.getSavedFiles() }
        
        Dialog(onDismissRequest = { showSavedFilesDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📁 Saved Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (savedFiles.isEmpty()) {
                        Text(
                            text = "No saved files found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(savedFiles) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val success = fileManager.loadSavedFile(file.name)
                                                if (success) {
                                                    content = fileManager.currentFileContent.value
                                                    isFileMode = true
                                                    showSavedFilesDialog = false
                                                }
                                            }
                                        }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${file.length()} bytes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            fileManager.deleteFile(file.name)
                                            showSavedFilesDialog = false
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSavedFilesDialog = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
    
    // Save As dialog
    if (showSaveAsDialog) {
        Dialog(onDismissRequest = { showSaveAsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💾 Save As",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = saveAsFileName,
                        onValueChange = { saveAsFileName = it },
                        label = { Text("File Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSaveAsDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    fileManager.currentFileContent.value = content
                                    val success = fileManager.saveAsNewFile(saveAsFileName)
                                    if (success) {
                                        isFileMode = false // Switch back to reminder mode after saving
                                        showSaveAsDialog = false
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Alert Level Selector - Moved to top
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "🔔 Alert Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Alert level selector
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (alertLevel) {
                                    AlertLevel.LOW -> "Low"
                                    AlertLevel.HIGH -> "High"
                                    AlertLevel.URGENT -> "Urgent"
                                },
                                color = when (alertLevel) {
                                    AlertLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                    AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                                    AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                                }
                            )
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = "Select Alert Level",
                                tint = when (alertLevel) {
                                    AlertLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                    AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                                    AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Only show built-in alert levels
                        listOf(
                            AlertLevel.LOW to "Low",
                            AlertLevel.HIGH to "High",
                            AlertLevel.URGENT to "Urgent"
                        ).forEach { (level, displayName) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = displayName,
                                        color = when (level) {
                                            AlertLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                            AlertLevel.HIGH -> MaterialTheme.colorScheme.secondary
                                            AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                },
                                onClick = {
                                    alertLevel = level
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Configure alert behavior in ⚙️ Alert Settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Main Voice Input Section - Moved voice button to top
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📝",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 8.sp // Even smaller font
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Single voice input button - MOVED TO TOP
                    Button(
                        onClick = {
                            if (isListening) {
                                speechManager.stopListening()
                            } else {
                                // Try keyboard voice input first (most reliable)
                                launchKeyboardVoiceInput()
                                // Fallback to direct mic if keyboard doesn't work
                                scope.launch {
                                    kotlinx.coroutines.delay(1000)
                                    if (content.isBlank()) {
                                        speechManager.restartSpeechRecognizer()
                                        speechManager.startListening()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop Recording" else "Start Voice Input",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isListening) "⏹️ Stop Recording" else "🎤 Tap to Speak",
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    
                    
                    if (isListening) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Single content field (typing + voice) - expandable with more lines for file content
                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            if (isFileMode) {
                                fileManager.updateContent(it)
                            }
                        },
                        label = {
                            Text(
                                text = if (isFileMode) "File Content" else "What do you need?",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp // Small font for better readability
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = if (isFileMode) 20 else 5, // More lines for file content
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp // Slightly larger font for better readability
                        ),
                        placeholder = {
                            Text(
                                text = if (isFileMode) "Start typing or load a file..." else "e.g., Call mom tomorrow at 3pm",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp // Smaller placeholder text
                                )
                            )
                        }
                    )
                    
                    // File mode indicator
                    if (isFileMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📁 File Mode: ${fileManager.getDisplayFileName()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            if (fileManager.isFileModified.value) {
                                Text(
                                    text = "• Modified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Modern Date Picker
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📅 Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (whenDay.isNotBlank()) whenDay else selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Modern Time Picker
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⏰ Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (whenTime.isNotBlank()) whenTime else selectedTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick Time Suggestions - Using Card instead of TextField to avoid keyboard
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimeSuggestions = !showTimeSuggestions },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⏰ Quick Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (whenTime.isNotBlank()) whenTime else "Tap for suggestions",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (whenTime.isNotBlank())
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = "Show Suggestions",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Time Suggestions Dropdown (appears below the card)
                    if (showTimeSuggestions) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .heightIn(max = 200.dp), // Fixed max height to prevent infinity constraints
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(timeSuggestions) { suggestion ->
                                    OutlinedButton(
                                        onClick = {
                                            whenTime = suggestion
                                            showTimeSuggestions = false
                                            
                                            // Parse and update selected time if it's a specific time
                                            val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
                                            val match = timePattern.find(suggestion)
                                            if (match != null) {
                                                val hour = match.groupValues[1].toIntOrNull() ?: 0
                                                val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
                                                val ampm = match.groupValues.getOrNull(3)?.lowercase()
                                                
                                                val parsedHour = when {
                                                    ampm == "am" -> if (hour == 12) 0 else hour
                                                    ampm == "pm" -> if (hour == 12) 12 else hour + 12
                                                    else -> hour
                                                }
                                                selectedTime = LocalTime.of(parsedHour.coerceIn(0, 23), minute.coerceIn(0, 59))
                                            }
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Optional: Manual time input field (only shown when explicitly requested)
                    if (focusTimeField) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = whenTime,
                            onValueChange = {
                                whenTime = it
                                showTimeSuggestions = false
                            },
                            label = { Text("Custom Time") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., 3:30pm, 14:30") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text
                            )
                        )
                        
                        // Add a button to hide the manual input field
                        TextButton(
                            onClick = { focusTimeField = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Hide Manual Input", fontSize = 10.sp)
                        }
                    } else {
                        // Add a small button to show manual input field
                        TextButton(
                            onClick = { focusTimeField = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Manual Input", fontSize = 10.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Priority selector with slider and +/- buttons
                    Text(
                        text = "Priority: ${priorityLabels[selectedPriority]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // Compact priority slider with +/- buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Minus button (left side)
                        IconButton(
                            onClick = {
                                if (selectedPriority > 1) {
                                    selectedPriority--
                                }
                            },
                            modifier = Modifier.size(32.dp),
                            enabled = selectedPriority > 1
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Decrease priority",
                                tint = if (selectedPriority > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        // Compact slider in the middle
                        Slider(
                            value = selectedPriority.toFloat(),
                            onValueChange = { selectedPriority = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = when (selectedPriority) {
                                    in 8..10 -> Red
                                    in 6..7 -> Color(0xFFFFA500) // Orange
                                    in 4..5 -> Blue
                                    else -> Green
                                },
                                activeTrackColor = when (selectedPriority) {
                                    in 8..10 -> Red.copy(alpha = 0.6f)
                                    in 6..7 -> Color(0xFFFFA500).copy(alpha = 0.6f)
                                    in 4..5 -> Blue.copy(alpha = 0.6f)
                                    else -> Green.copy(alpha = 0.6f)
                                }
                            )
                        )
                        
                        // Plus button (right side)
                        IconButton(
                            onClick = {
                                if (selectedPriority < 10) {
                                    selectedPriority++
                                }
                            },
                            modifier = Modifier.size(32.dp),
                            enabled = selectedPriority < 10
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increase priority",
                                tint = if (selectedPriority < 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // Priority value display (centered below)
                    Text(
                        text = "$selectedPriority / 10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    // Simplified Alert Timing Selector
                    SimplifiedAlertTimingSelector(
                        selectedTriggerPoint = selectedTriggerPoint,
                        onTriggerPointChange = { triggerPoint ->
                            selectedTriggerPoint = triggerPoint
                            // Update alertConfig to reflect the new trigger point
                            alertConfig = alertConfig.copy(
                                alertType = when (triggerPoint.type) {
                                    TriggerType.AT_DUE_TIME -> AlertType.NOTIFICATION_ONLY
                                    TriggerType.MINUTES_BEFORE -> AlertType.NOTIFICATION_ONLY
                                    TriggerType.HOURS_BEFORE -> AlertType.NOTIFICATION_ONLY
                                    TriggerType.DAYS_BEFORE -> AlertType.NOTIFICATION_ONLY
                                    TriggerType.WEEKS_BEFORE -> AlertType.NOTIFICATION_ONLY
                                    TriggerType.CUSTOM_OFFSET -> AlertType.NOTIFICATION_ONLY
                                }
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Simplified Repeat Pattern Selector
                    SimplifiedRepeatPatternSelector(
                        repeatPattern = repeatPattern,
                        onRepeatPatternChange = { pattern ->
                            repeatPattern = pattern
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Alert Settings Summary (Compact)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                Column {
                                    Text(
                                        text = "⚙️ Alert Settings",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Tap ⚙️ in top bar to customize alerts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Quick Configure Button
                                Button(
                                    onClick = { showAlertSettings = true },
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = "Configure",
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            
                            // Quick summary of current settings
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Alert: ${alertConfig.alertType.name.replace("_", " ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Repeat: ${repeatPattern.type.name}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            // Show repeat interval if configured
                            if (repeatPattern.type != RepeatType.NONE) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Every ${repeatPattern.interval} ${getIntervalUnit(repeatPattern.type)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Processing indicator - More compact
            if (isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🧠 Processing...", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            // Compact Analysis (shows extraction)
            if (content.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp), // Reduced padding
                        verticalArrangement = Arrangement.spacedBy(2.dp) // Reduced spacing
                    ) {
                        Text(
                            text = "🧠 Quick Analysis",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        Text(
                            text = "📝 ${content.replace(Regex("(remind me to|remember to)"), "").trim()}",
                            style = MaterialTheme.typography.bodySmall, // Smaller text
                            maxLines = 1
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📁 ${extractCategory(content)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = "🔥 ${extractPriority(content)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = when (extractPriority(content)) {
                                    "High" -> MaterialTheme.colorScheme.error
                                    "Medium" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📅 ${if (whenDay.isNotBlank()) whenDay else "No day"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "⏰ ${if (whenTime.isNotBlank()) whenTime else "No time"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                    }
                }
            }
            
            
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                // Always update whenDay when date is changed to reflect the new selection
                val newDay = when {
                    date == LocalDate.now() -> "Today"
                    date == LocalDate.now().plusDays(1) -> "Tomorrow"
                    else -> date.format(DateTimeFormatter.ofPattern("EEEE"))
                }
                whenDay = newDay
                
                // Update content field to replace day references
                if (content.isNotBlank()) {
                    val updatedContent = updateContentDay(content, newDay)
                    if (updatedContent != content) {
                        content = updatedContent
                        Log.d("InputScreen", "Updated content field from '$content' to '$updatedContent'")
                    }
                }
                
            },
            onDismiss = { showDatePicker = false }
        )
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                // Always update whenTime to reflect the selected time
                whenTime = time.format(DateTimeFormatter.ofPattern("h:mm a"))
            },
            onDismiss = { showTimePicker = false }
        )
    }
    
    // Alert Settings Screen Navigation
    if (showAlertSettings) {
        AlertSettingsScreenFixed(
            onBack = { showAlertSettings = false }
        )
    }
}