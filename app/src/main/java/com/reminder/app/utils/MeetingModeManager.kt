package com.reminder.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages meeting mode state and settings for the reminder app.
 * In meeting mode, only vibration and notification alerts are enabled (no flash).
 * When meeting mode is off, sound and flash alerts are enabled.
 */
class MeetingModeManager private constructor(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "meeting_mode_prefs"
        private const val KEY_MEETING_MODE_ENABLED = "meeting_mode_enabled"
        private const val KEY_SOUND_TIMES = "sound_duration_seconds" // Keep same key for backward compatibility
        private const val DEFAULT_SOUND_TIMES = 2 // 2 times default
        
        @Volatile
        private var INSTANCE: MeetingModeManager? = null
        
        fun getInstance(context: Context): MeetingModeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeetingModeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Check if meeting mode is currently enabled
     */
    fun isMeetingModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_MEETING_MODE_ENABLED, false)
    }
    
    /**
     * Enable or disable meeting mode
     */
    fun setMeetingMode(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_MEETING_MODE_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Toggle meeting mode state
     */
    fun toggleMeetingMode(): Boolean {
        val currentState = isMeetingModeEnabled()
        val newState = !currentState
        setMeetingMode(newState)
        return newState
    }
    
    /**
     * Get the sound duration in seconds for meeting mode
     */
    fun getSoundDurationSeconds(): Int {
        return sharedPreferences.getInt(KEY_SOUND_TIMES, DEFAULT_SOUND_TIMES)
    }
    
    /**
     * Set the sound duration in seconds for meeting mode
     */
    fun setSoundDurationSeconds(duration: Int) {
        sharedPreferences.edit()
            .putInt(KEY_SOUND_TIMES, duration)
            .apply()
    }
    
    /**
     * Truncate message to first 10 words for meeting mode
     */
    fun truncateMessage(message: String): String {
        if (!isMeetingModeEnabled()) {
            return message
        }
        
        val words = message.split("\\s+".toRegex())
        return if (words.size <= 10) {
            message
        } else {
            words.take(10).joinToString(" ") + "..."
        }
    }
    
    /**
     * Check if full notification should be shown (true even in meeting mode)
     */
    fun shouldShowFullNotification(): Boolean {
        return true // Always show notifications, even in meeting mode
    }
    
    /**
     * Check if flash should be enabled (false in meeting mode, true when meeting mode is off)
     */
    fun shouldEnableFlash(): Boolean {
        return !isMeetingModeEnabled() // Flash only when meeting mode is OFF
    }
    
    /**
     * Check if sound should be enabled (true in normal mode, false in meeting mode)
     */
    fun shouldEnableSound(): Boolean {
        return !isMeetingModeEnabled() // Sound only when meeting mode is OFF
    }
    
    /**
     * Check if vibration should be enabled (true in meeting mode, always enabled otherwise)
     */
    fun shouldEnableVibration(): Boolean {
        return true // Vibration always enabled, but primary when meeting mode is ON
    }
}