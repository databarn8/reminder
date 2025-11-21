package com.reminder.app.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.reminder.app.data.RepeatPattern
import com.reminder.app.data.RepeatType
import java.time.DayOfWeek

// Data class for repeat options - used across all repeat pattern selectors
data class RepeatOption(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val type: RepeatType,
    val interval: Int = 1,
    val daysOfWeek: List<DayOfWeek>? = null
)

// Helper functions for repeat pattern operations
fun createRepeatPatternFromOption(option: RepeatOption): RepeatPattern {
    return RepeatPattern(
        type = option.type,
        interval = option.interval,
        daysOfWeek = option.daysOfWeek
    )
}

fun formatRepeatPattern(pattern: RepeatPattern): String {
    return when (pattern.type) {
        RepeatType.NONE -> "No repeat"
        RepeatType.MINUTELY -> "Every ${pattern.interval} minute${if (pattern.interval > 1) "s" else ""}"
        RepeatType.HOURLY -> "Every ${pattern.interval} hour${if (pattern.interval > 1) "s" else ""}"
        RepeatType.DAILY -> "Every ${pattern.interval} day${if (pattern.interval > 1) "s" else ""}"
        RepeatType.WEEKLY -> {
            if (pattern.daysOfWeek != null) {
                val days = pattern.daysOfWeek!!.take(3).joinToString(", ") { it.name.take(3) }
                "Every week on $days..."
            } else {
                "Every ${pattern.interval} week${if (pattern.interval > 1) "s" else ""}"
            }
        }
        RepeatType.MONTHLY -> "Every ${pattern.interval} month${if (pattern.interval > 1) "s" else ""}"
        RepeatType.YEARLY -> "Every ${pattern.interval} year${if (pattern.interval > 1) "s" else ""}"
        else -> "Custom pattern"
    }
}

fun getIntervalUnit(type: RepeatType): String {
    return when (type) {
        RepeatType.MINUTELY -> "minutes"
        RepeatType.HOURLY -> "hours"
        RepeatType.DAILY -> "days"
        RepeatType.WEEKLY -> "weeks"
        RepeatType.MONTHLY -> "months"
        RepeatType.YEARLY -> "years"
        else -> "units"
    }
}