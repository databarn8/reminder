# Voice Recognition Fix for Reminder App

## Files to modify:

### 1. AndroidManifest.xml
Add these lines after the permissions section (around line 13):

```xml
<!-- Required for Android 11+ speech recognition -->
<queries>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
    <intent>
        <action android:name="android.speech.action.RECOGNIZE_SPEECH" />
    </intent>
</queries>
```

### 2. SpeechManager.kt

Replace the `startIntentBasedSpeechRecognition()` method with:

```kotlin
private fun startIntentBasedSpeechRecognition() {
    Log.d("SpeechManager", "Trying intent-based speech recognition")
    
    try {
        // Use the specific Google app component that we know works
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your reminder...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // Set specific component that handles speech recognition
            setClassName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.intentapi.IntentApiActivity"
            )
            
            // Android 11+ specific handling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Ensure we're not using secure mode
                putExtra(RecognizerIntent.EXTRA_SECURE, false)
            }
        }
        
        activity?.let { act ->
            act.startActivityForResult(intent, SPEECH_REQUEST_CODE)
            _isListening.value = true
            Log.d("SpeechManager", "Started speech recognition with Google app")
        } ?: run {
            Log.e("SpeechManager", "Activity not available for speech recognition")
            _speechResult.value = "Speech recognition not available - please restart the app"
        }
    } catch (e: SecurityException) {
        Log.e("SpeechManager", "Security exception in speech recognition: ${e.message}")
        _isListening.value = false
        _speechResult.value = "Permission denied for speech recognition"
    } catch (e: Exception) {
        Log.e("SpeechManager", "Error starting speech recognition: ${e.message}")
        _isListening.value = false
        _speechResult.value = "Speech recognition failed: ${e.message}"
    }
}
```

Replace the `isGoogleSpeechServicesAvailable()` method with:

```kotlin
private fun isGoogleSpeechServicesAvailable(): Boolean {
    return try {
        val pm = context.packageManager
        
        // Check specifically for Google app speech recognition
        val googleIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            setClassName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.intentapi.IntentApiActivity"
            )
        }
        
        val googleAvailable = googleIntent.resolveActivity(pm) != null
        Log.d("SpeechManager", "Google speech recognition available: $googleAvailable")
        
        if (googleAvailable) {
            return true
        }
        
        // Fallback to general check
        val packages = pm.queryIntentActivities(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
        )
        Log.d("SpeechManager", "Found ${packages.size} general speech recognition activities")
        packages.forEach { 
            Log.d("SpeechManager", "Available speech app: ${it.activityInfo.packageName}")
        }
        packages.isNotEmpty()
    } catch (e: Exception) {
        Log.e("SpeechManager", "Error checking speech services availability: ${e.message}")
        false
    }
}
```

Replace the `startListening()` method with:

```kotlin
fun startListening() {
    Log.d("SpeechManager", "startListening() called")
    Log.d("SpeechManager", "hasAudioPermission: ${hasAudioPermission()}")
    Log.d("SpeechManager", "SpeechRecognizer available: ${SpeechRecognizer.isRecognitionAvailable(context)}")
    Log.d("SpeechManager", "Google speech services available: ${isGoogleSpeechServicesAvailable()}")
    Log.d("SpeechManager", "Android version: ${Build.VERSION.SDK_INT}")
    
    if (!hasAudioPermission()) {
        Log.w("SpeechManager", "Audio permission not granted")
        _permissionNeeded.value = true
        _speechResult.value = "Please grant microphone permission"
        return
    }
    
    // Check if any speech recognition is available at all
    if (!isGoogleSpeechServicesAvailable()) {
        Log.e("SpeechManager", "No speech recognition services available")
        _speechResult.value = "Voice input not available. Please install 'Google Voice Search' from Play Store."
        return
    }
    
    // Always use intent-based speech recognition as it's more reliable
    startIntentBasedSpeechRecognition()
}
```

## Build Instructions:
1. Apply these changes to your Mac development environment
2. Build the app with Android Studio
3. Install and test on device

## Expected Result:
Voice prompt should now appear when tapping the microphone button in the reminder app.