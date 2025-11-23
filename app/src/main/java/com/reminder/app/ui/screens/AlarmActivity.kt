package com.reminder.app.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.ui.theme.ReminderAppTheme
import com.reminder.app.data.AlertLevel
import com.reminder.app.data.AlertLevelConfig
import com.reminder.app.data.AlertLevelOption
import com.reminder.app.data.AlertConfig
import com.reminder.app.data.VibrationConfig
import com.reminder.app.data.VibrationPattern
import com.reminder.app.data.SoundConfig
import com.reminder.app.utils.ScreenFlashManager
import com.reminder.app.data.Reminder
import com.reminder.app.utils.NotificationScheduler
import kotlinx.coroutines.delay

class AlarmActivity : ComponentActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null
    private var isAlarmDismissed = false
    private var alarmCount = 0
    private val maxAlarms = 5
    private var isReleased = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set flags to show alarm over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        val title = intent.getStringExtra("alarm_title") ?: "Reminder"
        val content = intent.getStringExtra("alarm_content") ?: "Your reminder is due!"
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val alertLevelString = intent.getStringExtra("alert_level") ?: "LOW"
        val customProfileName = intent.getStringExtra("custom_profile_name")
        val alertLevel = try {
            AlertLevel.valueOf(alertLevelString)
        } catch (e: Exception) {
            AlertLevel.LOW
        }
        
        // Load alert level config to get custom profile settings if needed
        val alertLevelConfig = loadAlertLevelConfig(this)
        
        // Get alert config (already loaded above)
        val alertConfig = getAlertConfigForLevel(alertLevelConfig, alertLevel, customProfileName)
        
        handler = Handler(Looper.getMainLooper())
        
        setContent {
            ReminderAppTheme {
                AlarmScreen(
                    title = title,
                    content = content,
                    alertLevel = alertLevel,
                    alertConfig = alertConfig,
                    onDismiss = { dismissAlarm() },
                    alarmCount = alarmCount,
                    maxAlarms = maxAlarms,
                    customProfileName = customProfileName
                )
            }
        }
        
        // Start repeating alarm
        startRepeatingAlarm(alertConfig)
        
        // Check if this is a repeat occurrence and schedule next one if needed
        val triggerType = intent.getStringExtra("trigger_type") ?: "AT_DUE_TIME"
        if (triggerType == "REPEAT_OCCURRENCE") {
            val reminderId = intent.getIntExtra("reminder_id", -1)
            val reminderJson = intent.getStringExtra("reminder_json")
            
            if (reminderJson != null) {
                try {
                    // Parse reminder from JSON manually since parseReminderFromJson is private
                    val jsonObject = org.json.JSONObject(reminderJson)
                    val reminder = Reminder(
                        id = jsonObject.getInt("id"),
                        content = jsonObject.getString("content"),
                        category = jsonObject.getString("category"),
                        importance = jsonObject.getInt("importance"),
                        reminderTime = jsonObject.getLong("reminderTime"),
                        whenDay = jsonObject.getString("whenDay").takeIf { it.isNotEmpty() },
                        whenTime = jsonObject.getString("whenTime").takeIf { it.isNotEmpty() },
                        repeatType = jsonObject.getString("repeatType"),
                        repeatInterval = jsonObject.getInt("repeatInterval"),
                        createdAt = jsonObject.getLong("createdAt")
                    )
                    // Schedule next occurrence after a delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        scheduleNextRepeatOccurrence(this, reminder)
                    }, 5000) // 5 second delay
                } catch (e: Exception) {
                    android.util.Log.e("AlarmActivity", "Error scheduling next repeat: ${e.message}")
                }
            }
        }
    }
    
    private fun startRepeatingAlarm(alertConfig: AlertConfig) {
        if (isAlarmDismissed) return
        
        // Apply alert configuration
        if (alertConfig.vibration.enabled) {
            triggerVibration(alertConfig.vibration)
        }
        
        if (alertConfig.sound.enabled) {
            playAlarmSound(alertConfig.sound)
        }
        
        if (alertConfig.series.enabled && alertConfig.series.escalationEnabled) {
            triggerScreenFlash()
        }
        
        alarmCount++
        
        // Schedule next alarm based on alert configuration
        if (!isAlarmDismissed && alarmCount < alertConfig.series.maxAttempts) {
            handler?.postDelayed({
                startRepeatingAlarm(alertConfig)
            }, (alertConfig.series.intervalMinutes * 60 * 1000).toLong())
        }
    }
    
    private fun triggerVibration(vibrationConfig: VibrationConfig) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = when (vibrationConfig.pattern) {
                    VibrationPattern.SINGLE -> android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    VibrationPattern.DOUBLE -> android.os.VibrationEffect.createWaveform(longArrayOf(200, 100, 200), intArrayOf(android.os.VibrationEffect.DEFAULT_AMPLITUDE, 0, android.os.VibrationEffect.DEFAULT_AMPLITUDE), -1)
                    VibrationPattern.TRIPLE -> android.os.VibrationEffect.createWaveform(longArrayOf(200, 100, 200, 100, 200), intArrayOf(android.os.VibrationEffect.DEFAULT_AMPLITUDE, 0, android.os.VibrationEffect.DEFAULT_AMPLITUDE, 0, android.os.VibrationEffect.DEFAULT_AMPLITUDE), -1)
                    VibrationPattern.LONG -> android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    VibrationPattern.PULSE -> android.os.VibrationEffect.createWaveform(longArrayOf(200, 100, 200, 100, 200, 100, 200), intArrayOf(android.os.VibrationEffect.DEFAULT_AMPLITUDE, 0, android.os.VibrationEffect.DEFAULT_AMPLITUDE, 0, android.os.VibrationEffect.DEFAULT_AMPLITUDE, 0, android.os.VibrationEffect.DEFAULT_AMPLITUDE), -1)
                    else -> android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Vibration failed: ${e.message}")
        }
    }
    
    private fun triggerScreenFlash() {
        try {
            ScreenFlashManager.triggerFlash(
                this,
                androidx.compose.ui.graphics.Color.Red,
                300,
                1,
                200
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Screen flash failed: ${e.message}")
        }
    }
    
    private fun playAlarmSound(soundConfig: SoundConfig) {
        try {
            // Stop any existing sound
            mediaPlayer?.stop()
            mediaPlayer?.release()
            isReleased = true
            
            // Get default alarm sound
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                isLooping = false
                setAudioStreamType(AudioManager.STREAM_ALARM)
                setVolume(soundConfig.volume, soundConfig.volume)
                prepare()
                start()
                
                // Auto-stop after 10 seconds
                handler?.postDelayed({
                    stop()
                    release()
                    isReleased = true
                }, 10000)
            }
        } catch (e: Exception) {
            // Fallback to system sound
            try {
                val ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                ringtone?.play()
                
                handler?.postDelayed({
                    ringtone?.stop()
                }, 10000)
            } catch (e2: Exception) {
                android.util.Log.e("AlarmActivity", "Could not play alarm sound: ${e2.message}")
            }
        }
    }
    
    private fun dismissAlarm() {
        isAlarmDismissed = true
        
        // Stop sound
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error stopping media player: ${e.message}")
        }
        mediaPlayer = null
        isReleased = true
        
        // Cancel any pending alarms
        try {
            handler?.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error removing callbacks: ${e.message}")
        }
        
        // Finish activity
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dismissAlarm()
    }
    
    private fun scheduleNextRepeatOccurrence(context: Context, reminder: Reminder) {
        try {
            // Get next occurrence after current reminder time
            val nextOccurrence = reminder.getNextOccurrence(
                java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(reminder.reminderTime),
                    java.time.ZoneId.systemDefault()
                )
            )
            
            nextOccurrence?.let { next ->
                val nextTime = next.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val currentTime = System.currentTimeMillis()
                
                if (nextTime > currentTime) {
                    android.util.Log.d("AlarmActivity", "Scheduling next repeat occurrence at: ${java.util.Date(nextTime)}")
                    
                    // Schedule the next occurrence
                    // Create reminder JSON manually since reminderToJson is private
                    val reminderJson = org.json.JSONObject().apply {
                        put("id", reminder.id)
                        put("content", reminder.content)
                        put("category", reminder.category)
                        put("importance", reminder.importance)
                        put("reminderTime", reminder.reminderTime)
                        put("whenDay", reminder.whenDay ?: "")
                        put("whenTime", reminder.whenTime ?: "")
                        put("repeatType", reminder.repeatType)
                        put("repeatInterval", reminder.repeatInterval)
                        put("createdAt", reminder.createdAt)
                    }.toString()
                    
                    val intent = android.content.Intent(context, NotificationScheduler::class.java).apply {
                        putExtra("reminder_title", reminder.content)
                        putExtra("reminder_content", reminder.content)
                        putExtra("reminder_id", reminder.id)
                        putExtra("reminder_json", reminderJson)
                        putExtra("trigger_type", "REPEAT_OCCURRENCE")
                        putExtra("enable_flash", true)
                        putExtra("enable_sound", true)
                        putExtra("enable_vibration", true)
                    }
                    
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        "${reminder.id}_next_repeat".hashCode(),
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (!alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setAndAllowWhileIdle(
                                android.app.AlarmManager.RTC_WAKEUP,
                                nextTime,
                                pendingIntent
                            )
                            android.util.Log.d("AlarmActivity", "Next repeat scheduled with setAndAllowWhileIdle")
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                android.app.AlarmManager.RTC_WAKEUP,
                                nextTime,
                                pendingIntent
                            )
                            android.util.Log.d("AlarmActivity", "Next repeat scheduled with setExactAndAllowWhileIdle")
                        }
                    } else {
                        alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            nextTime,
                            pendingIntent
                        )
                        android.util.Log.d("AlarmActivity", "Next repeat scheduled with setExact")
                    }
                } else {
                    android.util.Log.d("AlarmActivity", "No future occurrences to schedule")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error scheduling next repeat: ${e.message}")
        }
    }
    
    override fun onBackPressed() {
        // Handle back button as dismiss
        dismissAlarm()
    }
}

@Composable
fun AlarmScreen(
    title: String,
    content: String,
    alertLevel: AlertLevel,
    alertConfig: AlertConfig,
    onDismiss: () -> Unit,
    alarmCount: Int,
    maxAlarms: Int,
    customProfileName: String? = null
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    
    val timeString = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(currentTime))
    
    val backgroundColor = when (alertLevel) {
        AlertLevel.LOW -> Color(0xFFFFA500) // Orange
        AlertLevel.MEDIUM -> Color(0xFFFF6B35) // Dark Orange
        AlertLevel.HIGH -> Color(0xFFFF0000) // Red
        AlertLevel.URGENT -> Color(0xFF8B0000) // Dark Red
        AlertLevel.CUSTOM -> Color(0xFF9C27B0) // Purple
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Current time
                Text(
                    text = timeString,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = backgroundColor,
                    textAlign = TextAlign.Center
                )
                
                // Alarm icon and count
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏰",
                        fontSize = 48.sp
                    )
                    Text(
                        text = if (alertLevel == AlertLevel.CUSTOM && !customProfileName.isNullOrBlank()) {
                            "$customProfileName - Alarm $alarmCount of $maxAlarms"
                        } else {
                            "$alertLevel Level - Alarm $alarmCount of $maxAlarms"
                        },
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Reminder title
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                // Reminder content
                Text(
                    text = content,
                    fontSize = 18.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = backgroundColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop Alarm",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISMISS ALARM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Instructions
                Text(
                    text = "Tap DISMISS to stop alarm\nNext alarm in 1 minute if not dismissed",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Helper functions for alert level configuration
private fun loadAlertLevelConfig(context: Context): AlertLevelConfig {
    val prefs = context.getSharedPreferences("alert_level_config", Context.MODE_PRIVATE)
    val json = prefs.getString("alert_level_config", null)
    return if (json != null) {
        try {
            AlertLevelConfig.Companion.fromJson(json)
        } catch (e: Exception) {
            AlertLevelConfig() // Default if parsing fails
        }
    } else {
        AlertLevelConfig() // Default
    }
}

private fun getAlertConfigForLevel(levelConfig: AlertLevelConfig, level: AlertLevel, customProfileName: String? = null): AlertConfig {
    return when (level) {
        AlertLevel.LOW -> levelConfig.lowLevel
        AlertLevel.MEDIUM -> levelConfig.mediumLevel
        AlertLevel.HIGH -> levelConfig.highLevel
        AlertLevel.URGENT -> levelConfig.urgentLevel
        AlertLevel.CUSTOM -> {
            // Use the specific custom profile name if provided, otherwise fall back to first available
            if (!customProfileName.isNullOrBlank()) {
                levelConfig.customProfiles[customProfileName] ?: AlertConfig.Companion.getMediumLevelDefaults()
            } else {
                levelConfig.customProfiles.values.firstOrNull() ?: AlertConfig.Companion.getMediumLevelDefaults()
            }
        }
    }
}