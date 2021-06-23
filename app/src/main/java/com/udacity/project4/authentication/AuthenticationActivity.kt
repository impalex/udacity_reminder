package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import timber.log.Timber

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().currentUser?.let {
            Timber.d("User ${it.uid} is logged in already")
            startMainActivity()
            return@onCreate
        }
        launchSignIn()

        val binding = DataBindingUtil.setContentView<ActivityAuthenticationBinding>(this, R.layout.activity_authentication)
        binding.loginButton.setOnClickListener { launchSignIn() }
    }

    private val authLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { checkSignInResult(it) }

    private fun launchSignIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        authLauncher.launch(
            AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
        )
    }

    private fun checkSignInResult(result: FirebaseAuthUIAuthenticationResult?) {
        Timber.d("Firebase auth result: $result")
        if (result?.resultCode == RESULT_OK) {
            Timber.d("Successfully logged in")
            startMainActivity()
        } else {
            Timber.e("Login failed")
            Toast.makeText(this, R.string.login_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, RemindersActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

}
