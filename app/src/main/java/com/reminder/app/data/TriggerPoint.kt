package com.reminder.app.data

import kotlinx.serialization.Serializable

enum class TriggerType {
    AT_DUE_TIME,           // Exactly at due time
    MINUTES_BEFORE,        // X minutes before due time
    HOURS_BEFORE,          // X hours before due time
    DAYS_BEFORE,           // X days before due time
    WEEKS_BEFORE,          // X weeks before due time
    CUSTOM_OFFSET          // Custom milliseconds offset
}

@Serializable
data class TriggerPoint(
    val type: TriggerType,
    val value: Int = 0, // Value for minutes/hours/days/weeks before
    val customOffsetMs: Long = 0L, // Custom offset in milliseconds
    val enableFlash: Boolean = true, // Enable screen flash
    val enableSound: Boolean = true, // Enable sound
    val enableVibration: Boolean = true // Enable vibration
) {
    companion object {
        fun createDefault(): TriggerPoint {
            return TriggerPoint(TriggerType.AT_DUE_TIME)
        }
    }
    
    fun calculateTriggerTime(dueTime: Long): Long {
        return when (type) {
            TriggerType.AT_DUE_TIME -> dueTime
            TriggerType.MINUTES_BEFORE -> dueTime - (value * 60 * 1000L)
            TriggerType.HOURS_BEFORE -> dueTime - (value * 60 * 60 * 1000L)
            TriggerType.DAYS_BEFORE -> dueTime - (value * 24 * 60 * 60 * 1000L)
            TriggerType.WEEKS_BEFORE -> dueTime - (value * 7 * 24 * 60 * 60 * 1000L)
            TriggerType.CUSTOM_OFFSET -> dueTime - customOffsetMs
        }
    }
    
    fun getDescription(): String {
        return when (type) {
            TriggerType.AT_DUE_TIME -> "At due time"
            TriggerType.MINUTES_BEFORE -> "$value minute${if (value != 1) "s" else ""} before"
            TriggerType.HOURS_BEFORE -> "$value hour${if (value != 1) "s" else ""} before"
            TriggerType.DAYS_BEFORE -> "$value day${if (value != 1) "s" else ""} before"
            TriggerType.WEEKS_BEFORE -> "$value week${if (value != 1) "s" else ""} before"
            TriggerType.CUSTOM_OFFSET -> "${customOffsetMs / 1000}s before"
        }
    }
}