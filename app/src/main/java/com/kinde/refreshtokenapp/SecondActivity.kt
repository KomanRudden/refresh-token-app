package com.kinde.refreshtokenapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import au.kinde.sdk.GrantType
import au.kinde.sdk.KindeSDK
import au.kinde.sdk.SDKListener
import au.kinde.sdk.model.TokenType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class SecondActivity : AppCompatActivity(), SDKListener {
    private lateinit var kindeSDK: KindeSDK
    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button
    private lateinit var backButton: Button
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var testResultText: TextView
    
    private var isLoggingOut = false
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        // Initialize views
        loginButton = findViewById(R.id.loginButton)
        logoutButton = findViewById(R.id.logoutButton)
        backButton = findViewById(R.id.backButton)
        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        scrollView = findViewById(R.id.scrollView)
        testResultText = findViewById(R.id.testResultText)

        logMessage("=== SECOND ACTIVITY CREATED ===")
        logMessage("Initializing NEW SDK instance in SecondActivity...")
        
        // Initialize Kinde SDK (separate instance from MainActivity)
        kindeSDK = KindeSDK(
            activity = this,
            loginRedirect = "kinde.sdk://koman.kinde.com",
            logoutRedirect = "kinde.sdk://koman.kinde.com",
            sdkListener = this
        )

        logMessage("SDK initialized. Checking auth state...")
        
        // Test: Check if this new SDK instance can see auth state from MainActivity
        val isAuthenticatedOnCreate = kindeSDK.isAuthenticated()
        logMessage("isAuthenticated() on onCreate: $isAuthenticatedOnCreate")
        
        if (isAuthenticatedOnCreate) {
            logMessage("✓ SUCCESS: New SDK instance detected existing auth state!")
            testResultText.text = "✓ Test Result: Auth state synced correctly"
            testResultText.setTextColor(getColor(android.R.color.holo_green_dark))
            
            // Try to get user info
            thread {
                try {
                    val user = kindeSDK.getUser()
                    val email = user?.preferredEmail ?: "N/A"
                    logMessage("✓ getUser() works: $email")
                } catch (e: Exception) {
                    logMessage("✗ getUser() failed: ${e.message}")
                }
            }
        } else {
            logMessage("⚠ New SDK instance does NOT see existing auth state")
            testResultText.text = "⚠ Test Result: Auth state not synced (expected if not logged in)"
            testResultText.setTextColor(getColor(android.R.color.holo_orange_dark))
        }

        // Set up button listeners
        loginButton.setOnClickListener {
            logMessage("Login button clicked in SecondActivity")
            kindeSDK.login(GrantType.PKCE)
        }

        logoutButton.setOnClickListener {
            logMessage("Logout button clicked in SecondActivity")
            kindeSDK.logout()
        }

        backButton.setOnClickListener {
            logMessage("Going back to MainActivity...")
            logMessage("When you return to MainActivity, check if it shows authenticated status")
            finish()
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        logMessage("=== SecondActivity onResume ===")
        
        // Test refreshState explicitly
        kindeSDK.refreshState()
        val isAuthAfterRefresh = kindeSDK.isAuthenticated()
        logMessage("After refreshState(), isAuthenticated: $isAuthAfterRefresh")
        
        updateUI()
    }

    override fun onNewToken(token: String) {
        runOnUiThread {
            if (!::kindeSDK.isInitialized) {
                Log.w(TAG, "onNewToken called but SDK not fully initialized yet")
                return@runOnUiThread
            }
            
            val tokenEnd = if (token.length >= 30) token.substring(token.length - 30) else token
            logMessage("✓ TOKEN RECEIVED in SecondActivity - Token (last 30): ...$tokenEnd")
            logMessage("✓ LOGIN SUCCESS! Now go back to MainActivity to verify sync")
            
            testResultText.text = "✓ Test: Logged in! Go back to MainActivity to verify sync"
            testResultText.setTextColor(getColor(android.R.color.holo_green_dark))
            
            updateUI()
        }
    }

    override fun onLogout() {
        runOnUiThread {
            logMessage("✓ User logged out from SecondActivity")
            if (::kindeSDK.isInitialized && !isLoggingOut) {
                isLoggingOut = true
                testResultText.text = "✓ Logged out. Go back to MainActivity to verify sync"
                testResultText.setTextColor(getColor(android.R.color.holo_orange_dark))
                updateUI()
                isLoggingOut = false
            }
        }
    }

    override fun onException(exception: Exception) {
        runOnUiThread {
            logMessage("✗ Exception in SecondActivity: ${exception.message}")
            Log.e(TAG, "SDK Exception", exception)
        }
    }

    private fun updateUI() {
        if (!::kindeSDK.isInitialized || isLoggingOut) return
        
        val isAuthenticated = kindeSDK.isAuthenticated()

        loginButton.isEnabled = !isAuthenticated
        logoutButton.isEnabled = isAuthenticated

        if (isAuthenticated) {
            statusText.text = "Status: Authenticated ✓"
        } else {
            statusText.text = "Status: Not Authenticated"
        }
    }

    private fun logMessage(message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $message\n\n"
        
        runOnUiThread {
            logText.append(logEntry)
            // Auto-scroll to bottom
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
        
        Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "SecondActivity"
    }
}
