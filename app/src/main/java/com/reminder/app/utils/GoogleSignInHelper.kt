package com.reminder.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleSignInHelper(private val context: Context) {
    
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestIdToken(context.getString(com.reminder.app.R.string.default_web_client_id))
        .build()
    
    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
    
    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()
    
    enum class SignInState {
        Idle, SigningIn, Success, Error, SignedOut
    }
    
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    fun isSignedIn(): Boolean {
        return getSignedInAccount() != null
    }
    
    fun signIn(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        _signInState.value = SignInState.SigningIn
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }
    
    fun signOut() {
        googleSignInClient.signOut()
            .addOnCompleteListener {
                _signInState.value = SignInState.SignedOut
            }
    }
    
    fun handleSignInResult(task: Task<GoogleSignInAccount>): Boolean {
        return try {
            val account = task.getResult(ApiException::class.java)
            _signInState.value = SignInState.Success
            true
        } catch (e: ApiException) {
            _signInState.value = SignInState.Error
            false
        }
    }
    
    fun getAccountEmail(): String? {
        return getSignedInAccount()?.email
    }
    
    fun getAccountName(): String? {
        return getSignedInAccount()?.displayName
    }
    
    fun getAccountPhoto(): String? {
        return getSignedInAccount()?.photoUrl?.toString()
    }
}