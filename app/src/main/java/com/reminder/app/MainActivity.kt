package com.reminder.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.reminder.app.data.ReminderDatabase
import com.reminder.app.data.Reminder
import com.reminder.app.repository.ReminderRepository
import com.reminder.app.ui.screens.CalendarScreen
import com.reminder.app.ui.screens.ConfirmationScreen
import com.reminder.app.ui.screens.EmailSettingsScreen
import com.reminder.app.ui.screens.InputScreen
import com.reminder.app.ui.screens.ReminderListScreen
import com.reminder.app.ui.screens.AlertSettingsScreenFixed
import com.reminder.app.ui.screens.ArchiveRestoreScreen
import com.reminder.app.ui.screens.BackupSettingsScreen
import com.reminder.app.ui.screens.SettingsScreen
import com.reminder.app.ui.screens.TaskCompletionScreen
import com.reminder.app.ui.theme.ReminderAppTheme
import com.reminder.app.utils.EnhancedEmailService
import com.reminder.app.utils.GoogleSignInHelper
import com.reminder.app.utils.NotificationScheduler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import com.reminder.app.utils.ScreenFlashManager
import com.reminder.app.utils.ScreenFlashOverlay
import com.reminder.app.utils.SpeechManager
import com.reminder.app.viewmodel.ReminderViewModel
import com.reminder.app.viewmodel.ArchiveRestoreViewModel
import com.reminder.app.viewmodel.TaskCompletionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import com.reminder.app.data.EmailPreferencesManager

class MainActivity : ComponentActivity() {
    private lateinit var speechManager: SpeechManager
    private lateinit var emailService: EnhancedEmailService
    private lateinit var emailPreferencesManager: EmailPreferencesManager
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    
    // File picker launcher for file attachments
    var filePickerLauncher: ActivityResultLauncher<Intent>? = null
    
    companion object {
        // Static properties to hold file picker results
        var selectedFileUri: Uri? = null
        var selectedFileUris: List<Uri>? = null
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        speechManager.onPermissionResult(isGranted)
    }
    
    private val emailIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // For email intents, we want to capture the user's choice regardless of result code
        // since RESULT_OK might not be returned when user just selects an app
        result.data?.let { intent ->
            // Update email preference based on user's choice
            emailService.updateEmailPreference(this@MainActivity, intent)
        }
        
        // Always return to the reminder app regardless of result
        // This prevents getting stuck in email client
        // Check if we're currently in a different app (like email client)
        val currentApp = result.data?.`package` ?: result.data?.component?.packageName
        val isReminderApp = currentApp == packageName
        
        android.util.Log.d("EmailTest", "Email result - resultCode: ${result.resultCode}, data: ${result.data}, isReminderApp: $isReminderApp")
        
        // If we're not in the reminder app, bring it back to front
        if (!isReminderApp) {
            val bringToFrontIntent = Intent(this@MainActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(bringToFrontIntent)
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle notification permission result
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission GRANTED")
            // Test notification system immediately
            NotificationScheduler.testAlarm(this)
        } else {
            android.util.Log.w("MainActivity", "Notification permission DENIED - reminders may not work properly")
            // Show toast to user about importance of notifications
            android.widget.Toast.makeText(this, "Notifications are required for reminders to work properly", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    private val systemAlertWindowPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "SYSTEM_ALERT_WINDOW permission granted")
        } else {
            android.util.Log.w("MainActivity", "SYSTEM_ALERT_WINDOW permission denied")
        }
    }
    
    private val alarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle alarm permission result
        if (!isGranted) {
            android.util.Log.d("MainActivity", "Exact alarm permission denied")
        }
    }
    
    // Initialize signInLauncher in onCreate
    private fun initSignInLauncher() {
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            googleSignInHelper.handleSignInResult(task)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        speechManager = SpeechManager(this)
        speechManager.setActivity(this)
        emailService = EnhancedEmailService()
        emailPreferencesManager = EmailPreferencesManager(this)
        googleSignInHelper = GoogleSignInHelper(this)
        initSignInLauncher()
        
        // Initialize file picker launcher
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.let { data ->
                // Check for multiple files
                val clipData = data.clipData
                if (clipData != null) {
                    // Multiple files selected
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i)?.uri?.let { uri ->
                            uris.add(uri)
                        }
                    }
                    selectedFileUris = uris
                } else {
                    // Single file selected
                    data.data?.let { uri ->
                        selectedFileUri = uri
                    }
                }
            }
        }
        
        // Check and request necessary permissions
        if (!speechManager.hasAudioPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                android.util.Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted")
            }
        }
        
        // Request exact alarm permission for Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmPermissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
        
        // Request SYSTEM_ALERT_WINDOW permission for screen flash when phone is asleep
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                android.util.Log.d("MainActivity", "Requesting SYSTEM_ALERT_WINDOW permission")
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                systemAlertWindowPermissionLauncher.launch(Manifest.permission.SYSTEM_ALERT_WINDOW)
            } else {
                android.util.Log.d("MainActivity", "SYSTEM_ALERT_WINDOW permission already granted")
            }
        }
        
        // Handle voice actions from Google Assistant and keyboard input
        handleIntent(intent)
        
        setContent {
            ReminderAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Screen flash overlay for reminder notifications
                    ScreenFlashOverlay()
                    val navController = rememberNavController()
                    val database = ReminderDatabase.getDatabase(this)
                    val repository = ReminderRepository(database.reminderDao())
                    val viewModel: ReminderViewModel = viewModel(
                        factory = ReminderViewModelFactory(repository, application)
                    )
                    
                    NavHost(
                        navController = navController,
                        startDestination = "reminder_list"
                    ) {
                        composable("reminder_list") {
                            ReminderListScreen(
                                viewModel = viewModel,
                                onAddReminder = { navController.navigate("input_screen") },
                                onReminderClick = { reminder ->
                                    navController.navigate("input_screen?reminderId=${reminder.id}")
                                },
                                onEditClick = { reminder ->
                                    navController.navigate("input_screen?reminderId=${reminder.id}")
                                },
                                onCalendarClick = {
                                    android.util.Log.d("CalendarTest", "Calendar button clicked!")
                                    navController.navigate("calendar")
                                },
                                onEmailClick = { reminder ->
                                    // Always use the launcher to capture user choice
                                    // This ensures we can update the preference regardless of whether
                                    // a preferred client is already set or not
                                    emailService.sendReminderEmailWithLauncher(
                                        this@MainActivity,
                                        reminder,
                                        emailIntentLauncher
                                    )
                                },
                                onAlertSettingsClick = {
                                    android.util.Log.d("AlertTest", "Alert settings button clicked!")
                                    navController.navigate("alert_settings")
                                },
                                onEmailSettingsClick = {
                                    android.util.Log.d("EmailSettingsTest", "Email settings button clicked!")
                                    navController.navigate("email_settings")
                                },
                                onBackupSettingsClick = {
                                    android.util.Log.d("BackupTest", "Backup settings button clicked!")
                                    navController.navigate("backup_settings")
                                },
                                onTaskCompletionClick = {
                                    android.util.Log.d("TaskCompletionTest", "Task Completion button clicked!")
                                    navController.navigate("task_completion")
                                },
                                onArchiveRestoreClick = {
                                    android.util.Log.d("ArchiveTest", "Archive/Restore button clicked!")
                                    navController.navigate("archive_restore")
                                },
                                onSettingsClick = {
                                    android.util.Log.d("SettingsTest", "General settings button clicked!")
                                    navController.navigate("settings")
                                },
                                onHomeClick = {
                                    android.util.Log.d("HomeTest", "Home button clicked!")
                                    // Already on home screen
                                }
                            )
                        }
                        
                        composable("calendar") {
                            android.util.Log.d("CalendarTest", "Calendar screen navigated!")
                            CalendarScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onAddReminder = { navController.navigate("input_screen") },
                                onReminderClick = { reminder ->
                                    navController.navigate("input_screen?reminderId=${reminder.id}")
                                },
                                onAddReminderWithDate = { date ->
                                    navController.navigate("input_screen?selectedDate=${date.toString()}")
                                },
                                onHomeClick = {
                                    android.util.Log.d("HomeTest", "Home button clicked from calendar!")
                                    navController.popBackStack("reminder_list", false)
                                }
                            )
                        }
                        
                        composable("input_screen?reminderId={reminderId}&selectedDate={selectedDate}") { backStackEntry ->
                            val reminderId = backStackEntry.arguments?.getString("reminderId")?.toIntOrNull()
                            val selectedDateString = backStackEntry.arguments?.getString("selectedDate")
                            val selectedDate = selectedDateString?.let {
                                try {
                                    java.time.LocalDate.parse(it)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            InputScreen(
                                viewModel = viewModel,
                                speechManager = speechManager,
                                reminderId = reminderId,
                                onBack = { navController.popBackStack() },
                                onConfirm = { _, _ ->
                                    // InputScreen handles the reminder creation internally
                                    // This callback is only used for navigation back
                                    navController.popBackStack()
                                },
                                onCalendarClick = { navController.navigate("calendar") },
                                onHomeClick = {
                                    android.util.Log.d("HomeTest", "Home button clicked from input screen!")
                                    navController.popBackStack("reminder_list", false)
                                }
                            )
                        }
                        
                        composable("confirmation_screen/{text}") { backStackEntry ->
                            val text = backStackEntry.arguments?.getString("text") ?: ""
                            ConfirmationScreen(
                                initialText = text,
                                speechManager = speechManager,
                                onConfirm = { confirmedText ->
                                    // Save reminder to database
                                    val reminder = Reminder(
                                        content = confirmedText,
                                        category = "Personal",
                                        importance = 5,
                                        reminderTime = System.currentTimeMillis() + 15 * 60 * 1000, // 15 minutes from now
                                        repeatType = "none",
                                        repeatInterval = 1
                                    )
                                    viewModel.addReminder(reminder)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() },
                                onCalendarClick = { navController.navigate("calendar") }
                            )
                        }
                        
                        composable("alert_settings") {
                            android.util.Log.d("AlertTest", "Alert settings screen navigated!")
                            AlertSettingsScreenFixed(
                                onBack = { navController.popBackStack() },
                                onHomeClick = {
                                    android.util.Log.d("HomeTest", "Home button clicked from alert settings!")
                                    navController.popBackStack("reminder_list", false)
                                }
                            )
                        }
                        
                        composable("email_settings") {
                            android.util.Log.d("EmailSettingsTest", "Email settings screen navigated!")
                            EmailSettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable("backup_settings") {
                            android.util.Log.d("BackupTest", "Backup settings screen navigated!")
                            BackupSettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable("settings") {
                            android.util.Log.d("SettingsTest", "Settings screen navigated!")
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onAlertSettingsClick = {
                                    android.util.Log.d("SettingsTest", "Navigating to alert settings from settings screen!")
                                    navController.navigate("alert_settings")
                                },
                                onEmailSettingsClick = {
                                    android.util.Log.d("SettingsTest", "Navigating to email settings from settings screen!")
                                    navController.navigate("email_settings")
                                },
                                onBackupSettingsClick = {
                                    android.util.Log.d("SettingsTest", "Navigating to backup settings from settings screen!")
                                    navController.navigate("backup_settings")
                                },
                                onHomeClick = {
                                    android.util.Log.d("HomeTest", "Home button clicked from settings!")
                                    navController.popBackStack("reminder_list", false)
                                }
                            )
                        }
                        
                        composable("archive_restore") {
                            android.util.Log.d("ArchiveTest", "Archive/Restore screen navigated!")
                            val database = ReminderDatabase.getDatabase(this@MainActivity)
                            val repository = ReminderRepository(database.reminderDao())
                            val archiveViewModel: ArchiveRestoreViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                factory = com.reminder.app.ReminderViewModelFactory(repository, this@MainActivity.application)
                            )
                            ArchiveRestoreScreen(
                                viewModel = archiveViewModel,
                                onReminderRestored = { viewModel.refreshReminders() },
                                onBack = { navController.popBackStack() },
                                onHomeClick = {
                                    android.util.Log.d("HomeTest", "Home button clicked from archive restore!")
                                    navController.popBackStack("reminder_list", false)
                                }
                            )
                        }
                        
                        composable("task_completion") {
                            android.util.Log.d("TaskCompletionTest", "Task Completion screen navigated!")
                            val database = ReminderDatabase.getDatabase(this@MainActivity)
                            val repository = ReminderRepository(database.reminderDao())
                            val taskCompletionViewModel: TaskCompletionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                factory = com.reminder.app.ReminderViewModelFactory(repository, this@MainActivity.application)
                            )
                            val mainViewModel: ReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                factory = com.reminder.app.ReminderViewModelFactory(repository, this@MainActivity.application)
                            )
                            TaskCompletionScreen(
                                viewModel = taskCompletionViewModel,
                                onBack = { navController.popBackStack() },
                                onReminderRestored = {
                                    // Go directly to reminder list instead of archive page
                                    navController.popBackStack("reminder_list", false)
                                },
                                onHomeClick = {
                                    android.util.Log.d("HomeTest", "Home button clicked from task completion!")
                                    navController.popBackStack("reminder_list", false)
                                }
                            )
                        }
                        
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let { 
            when (it.action) {
                "android.intent.action.CREATE_NOTE",
                "com.google.android.gms.actions.CREATE_NOTE",
                "com.reminder.app.CREATE_REMINDER",
                "android.intent.action.SEND",
                "android.intent.action.SENDTO" -> {
                    val text = it.getStringExtra(Intent.EXTRA_TEXT) ?: 
                               it.getStringExtra("android.intent.extra.TEXT") ?: return
                    if (text.isNotBlank()) {
                        createReminderFromVoice(text)
                        // Show confirmation that reminder was created
                        showReminderCreatedConfirmation()
                    }
                }
                "com.reminder.app.VOICE_INPUT" -> {
                    val voiceInput = it.getStringExtra("voice_input")
                    if (!voiceInput.isNullOrBlank()) {
                        // Navigate to input screen with the voice input pre-filled
                        // This will be handled by the navigation system
                        // For now, create's reminder directly
                        createReminderFromVoice(voiceInput)
                        showReminderCreatedConfirmation()
                    }
                }
            }
        }
    }
    
    private fun showReminderCreatedConfirmation() {
        // You could show a Toast or Snackbar here
        // For now, reminder appearing in the list is confirmation enough
    }
    
    private fun createReminderFromVoice(text: String) {
        val reminder = Reminder(
            content = text,
            category = "Personal",
            importance = 5,
            reminderTime = System.currentTimeMillis() + 15 * 60 * 1000, // 15 minutes from now
            repeatType = "none",
            repeatInterval = 1
        )
        
        // Get ViewModel and save reminder
        val database = ReminderDatabase.getDatabase(this)
        val repository = ReminderRepository(database.reminderDao())
        val viewModel = ReminderViewModel(repository, application)
        viewModel.addReminder(reminder)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle speech manager results
        speechManager.handleActivityResult(requestCode, resultCode, data)
        
        // Handle keyboard voice input results
        if (requestCode == 1002 && resultCode == Activity.RESULT_OK && data != null) {
            val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                // Navigate to input screen with the voice result
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("voice_input", matches[0])
                    action = "com.reminder.app.VOICE_INPUT"
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }
}