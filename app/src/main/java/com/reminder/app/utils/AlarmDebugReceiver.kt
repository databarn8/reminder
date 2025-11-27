package com.reminder.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmDebugReceiver", "=== onReceive called with action: ${intent.action} ===")
        
        when (intent.action) {
            "com.reminder.app.TEST_SOUND_STOP" -> {
                Log.d("AlarmDebugReceiver", "=== TEST SOUND STOP BROADCAST RECEIVED ===")
                
                try {
                    // Create an intent to launch AlarmActivity with test flag
                    val testIntent = Intent(context, com.reminder.app.ui.screens.AlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("TEST_SOUND_STOP", true)
                        putExtra("alarm_title", "DEBUG TEST")
                        putExtra("alarm_content", "Testing sound stop functionality via ADB")
                        putExtra("alert_level", "LOW")
                    }
                    
                    context.startActivity(testIntent)
                    Log.d("AlarmDebugReceiver", "Started AlarmActivity with TEST_SOUND_STOP flag")
                } catch (e: Exception) {
                    Log.e("AlarmDebugReceiver", "Error starting AlarmActivity: ${e.message}")
                }
            }
            
            "com.reminder.app.DEBUG_SOUND_STOP" -> {
                Log.d("AlarmDebugReceiver", "=== DEBUG SOUND STOP BROADCAST RECEIVED ===")
                
                try {
                    // Create an intent to launch AlarmActivity with test flag
                    val testIntent = Intent(context, com.reminder.app.ui.screens.AlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("TEST_SOUND_STOP", true)
                        putExtra("alarm_title", "DEBUG TEST")
                        putExtra("alarm_content", "Testing sound stop functionality via ADB")
                        putExtra("alert_level", "LOW")
                    }
                    
                    context.startActivity(testIntent)
                    Log.d("AlarmDebugReceiver", "Started AlarmActivity with TEST_SOUND_STOP flag")
                } catch (e: Exception) {
                    Log.e("AlarmDebugReceiver", "Error starting AlarmActivity: ${e.message}")
                }
            }
            
            else -> {
                Log.d("AlarmDebugReceiver", "Unknown action: ${intent.action}")
            }
        }
    }
}