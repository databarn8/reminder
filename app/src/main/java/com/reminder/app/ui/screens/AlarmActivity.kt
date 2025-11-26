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
import com.reminder.app.data.SoundType
import com.reminder.app.utils.ScreenFlashManager
import com.reminder.app.data.Reminder
import com.reminder.app.utils.NotificationScheduler
import com.reminder.app.R
import kotlinx.coroutines.delay

class AlarmActivity : ComponentActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null
    private var isAlarmDismissed = false
    private var alarmCount = 0
    private val maxAlarms = 5
    private var isReleased = false
    private var isActivityInitialized = false
    
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
        val alertLevel = try {
            AlertLevel.valueOf(alertLevelString)
        } catch (e: Exception) {
            AlertLevel.LOW
        }
        
        // Load alert level config
        val alertLevelConfig = loadAlertLevelConfig(this)
        
        // Get alert config for the alert level
        val alertConfig = getAlertConfigForLevel(alertLevelConfig, alertLevel)
        
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
                    maxAlarms = maxAlarms
                )
            }
        }
        
        // Start repeating alarm
        startRepeatingAlarm(alertConfig, alertLevel)
        
        // REMOVED: Don't schedule repeat alarms from AlarmActivity
        // This was causing the 4x repetition issue
        // Repeat alarms are now handled by NotificationScheduler.scheduleRepeatAlarms()
        // which is called when reminders are first created
    }
    
    private fun startRepeatingAlarm(alertConfig: AlertConfig, alertLevel: AlertLevel) {
        if (isAlarmDismissed) return
        
        // Apply alert configuration once for this reminder
        if (alertConfig.vibration.enabled) {
            triggerVibration(alertConfig.vibration)
        }
        
        if (alertConfig.sound.enabled) {
            playAlarmSound(alertConfig.sound, alertLevel)
        }
        
        // Always trigger screen flash - it's a key visual alert feature
        triggerScreenFlash(alertLevel)
        
        alarmCount = 1 // Set to 1 since this is a single alarm for this reminder
        
        // Don't schedule next alarm - this should be a single alarm per reminder
        // The repetition should only happen for recurring reminders (daily, weekly, etc.)
        // which is handled by the scheduleNextRepeatOccurrence function
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
    
    private fun triggerScreenFlash(alertLevel: AlertLevel) {
        try {
            // Use different colors based on alert level
            val flashColor = when (alertLevel) {
                AlertLevel.LOW -> androidx.compose.ui.graphics.Color(0xFFFFFF00) // Yellow
                AlertLevel.HIGH -> androidx.compose.ui.graphics.Color(0xFFFF8C00) // Dark Orange
                AlertLevel.URGENT -> androidx.compose.ui.graphics.Color(0xFF8B0000) // Dark Red
            }
            
            android.util.Log.d("AlarmActivity", "Triggering flash for $alertLevel with color: $flashColor")
            
            ScreenFlashManager.triggerFlash(
                this,
                flashColor,
                300,
                1,
                200
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Screen flash failed: ${e.message}")
        }
    }
    
    private fun playAlarmSound(soundConfig: SoundConfig, alertLevel: AlertLevel) {
        try {
            // Stop any existing sound
            mediaPlayer?.stop()
            mediaPlayer?.release()
            isReleased = true
            
            // First try to use the configured sound type
            val alarmUri = if (soundConfig.enabled) {
                when (soundConfig.type) {
                    SoundType.CHIME -> {
                        // Try to use custom chime sound
                        try {
                            android.net.Uri.parse("android.resource://${packageName}/${com.reminder.app.R.raw.gentle_chime}")
                        } catch (e: Exception) {
                            android.util.Log.e("AlarmActivity", "Could not load chime sound: ${e.message}")
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        }
                    }
                    SoundType.GENTLE -> {
                        // Try to use gentle sound
                        try {
                            android.net.Uri.parse("android.resource://${packageName}/${com.reminder.app.R.raw.soft_bell}")
                        } catch (e: Exception) {
                            android.util.Log.e("AlarmActivity", "Could not load gentle sound: ${e.message}")
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        }
                    }
                    SoundType.ALARM -> {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    }
                    SoundType.URGENT -> {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    }
                    else -> {
                        // Fallback to alert level based sounds
                        val soundResourceId = when (alertLevel) {
                            AlertLevel.LOW -> com.reminder.app.R.raw.gentle_chime
                            AlertLevel.HIGH -> com.reminder.app.R.raw.soft_bell
                            AlertLevel.URGENT -> com.reminder.app.R.raw.urgent_alarm
                        }
                        try {
                            android.net.Uri.parse("android.resource://${packageName}/${soundResourceId}")
                        } catch (e: Exception) {
                            android.util.Log.e("AlarmActivity", "Could not load custom sound: ${e.message}")
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        }
                    }
                }
            } else {
                // Sound disabled, use fallback based on alert level
                val soundResourceId = when (alertLevel) {
                    AlertLevel.LOW -> com.reminder.app.R.raw.gentle_chime
                    AlertLevel.HIGH -> com.reminder.app.R.raw.soft_bell
                    AlertLevel.URGENT -> com.reminder.app.R.raw.urgent_alarm
                }
                try {
                    android.net.Uri.parse("android.resource://${packageName}/${soundResourceId}")
                } catch (e: Exception) {
                    android.util.Log.e("AlarmActivity", "Could not load custom sound: ${e.message}")
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                isLooping = false
                setAudioStreamType(AudioManager.STREAM_ALARM)
                setVolume(soundConfig.volume, soundConfig.volume)
                prepare()
                start()
                
                android.util.Log.d("AlarmActivity", "Playing custom sound for $alertLevel: $alarmUri")
                
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
                
                android.util.Log.e("AlarmActivity", "Fallback to system alarm sound: ${e.message}")
                
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
        
        // Clear the alarm handling state
        val prefs = getSharedPreferences("alarm_activity_state", Context.MODE_PRIVATE)
        prefs.edit().remove("current_handling_id").apply()
        
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
    maxAlarms: Int
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
        AlertLevel.HIGH -> Color(0xFFFF6B35) // Dark Orange
        AlertLevel.URGENT -> Color(0xFF8B0000) // Dark Red
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
                
                // Alarm icon and level
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏰",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "$alertLevel Level Alert",
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
                    text = "Tap DISMISS to stop this reminder alarm",
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

private fun getAlertConfigForLevel(levelConfig: AlertLevelConfig, level: AlertLevel): AlertConfig {
    return when (level) {
        AlertLevel.LOW -> levelConfig.lowLevel
        AlertLevel.HIGH -> levelConfig.highLevel
        AlertLevel.URGENT -> levelConfig.urgentLevel
    }
}