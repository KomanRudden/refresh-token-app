package com.kinde.refreshtokenapp

import android.content.Intent
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

class MainActivity : AppCompatActivity(), SDKListener {
    private lateinit var kindeSDK: KindeSDK
    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button
    private lateinit var statusText: TextView
    private lateinit var countdownText: TextView
    private lateinit var logText: TextView
    private lateinit var scrollView: ScrollView
    
    private var isLoggingOut = false
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private var countdownRunnable: Runnable? = null
    private var nextPollTime: Long = 0
    private val POLL_INTERVAL_MS = 60_000L // 1 minute

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        loginButton = findViewById(R.id.loginButton)
        logoutButton = findViewById(R.id.logoutButton)
        statusText = findViewById(R.id.statusText)
        countdownText = findViewById(R.id.countdownText)
        logText = findViewById(R.id.logText)
        scrollView = findViewById(R.id.scrollView)

        // Initialize Kinde SDK
        kindeSDK = KindeSDK(
            activity = this,
            loginRedirect = "kinde.sdk://koman.kinde.com",
            logoutRedirect = "kinde.sdk://koman.kinde.com",
            sdkListener = this
        )

        // Set up button listeners
        loginButton.setOnClickListener {
            logMessage("Login button clicked")
            kindeSDK.login(GrantType.PKCE)
        }

        logoutButton.setOnClickListener {
            logMessage("Logout button clicked")
            stopPolling()
            kindeSDK.logout()
        }

        updateUI()
        logMessage("App started - SDK initialized")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        logMessage("onNewIntent called")
    }

    override fun onNewToken(token: String) {
        runOnUiThread {
            if (!::kindeSDK.isInitialized) {
                Log.w(TAG, "onNewToken called but SDK not fully initialized yet")
                return@runOnUiThread
            }
            
            val tokenEnd = if (token.length >= 30) token.substring(token.length - 30) else token
            logMessage("✓ TOKEN RECEIVED - Token (last 30): ...$tokenEnd")
            displayTokenInfo()
            updateUI()
            
            // Start polling if authenticated and not already polling
            if (kindeSDK.isAuthenticated() && pollingRunnable == null) {
                startPolling()
            }
        }
    }

    override fun onLogout() {
        runOnUiThread {
            logMessage("✓ User logged out")
            stopPolling()
            if (::kindeSDK.isInitialized && !isLoggingOut) {
                isLoggingOut = true
                updateUI()
                isLoggingOut = false
            }
        }
    }

    override fun onException(exception: Exception) {
        runOnUiThread {
            logMessage("✗ Exception: ${exception.message}")
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
            displayTokenInfo()
        } else {
            statusText.text = "Status: Not Authenticated"
            countdownText.visibility = View.GONE
            logMessage("Not authenticated - please login")
        }
    }
    
    private fun startPolling() {
        stopPolling()
        logMessage("Started automatic getUser() polling every 60 seconds")
        scheduleNextPoll()
    }
    
    private fun scheduleNextPoll() {
        nextPollTime = System.currentTimeMillis() + POLL_INTERVAL_MS
        countdownText.visibility = View.VISIBLE
        startCountdown()
        
        pollingRunnable = Runnable {
            callGetUser()
            if (kindeSDK.isAuthenticated()) {
                scheduleNextPoll()
            }
        }
        handler.postDelayed(pollingRunnable!!, POLL_INTERVAL_MS)
    }
    
    private fun startCountdown() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        
        countdownRunnable = object : Runnable {
            override fun run() {
                if (!kindeSDK.isAuthenticated()) {
                    countdownText.visibility = View.GONE
                    return
                }
                
                val remaining = (nextPollTime - System.currentTimeMillis()) / 1000
                if (remaining > 0) {
                    countdownText.text = "Next getUser() call in: ${remaining}s"
                    handler.postDelayed(this, 1000)
                } else {
                    countdownText.text = "Calling getUser()..."
                }
            }
        }
        handler.post(countdownRunnable!!)
    }
    
    private fun callGetUser() {
        thread {
            try {
                logMessage("Calling getUser()...")
                val token = kindeSDK.getToken(TokenType.ACCESS_TOKEN) ?: "null"
                val tokenEnd = if (token.length >= 30) token.substring(token.length - 30) else token
                logMessage("Current Token (last 30): ...$tokenEnd")
                
                val user = kindeSDK.getUser()
                val email = user?.preferredEmail ?: "N/A"
                val name = "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim().ifEmpty { "N/A" }
                logMessage("✓ getUser() SUCCESS - Name: $name, Email: $email")
            } catch (e: Exception) {
                logMessage("✗ getUser() FAILED - ${e.javaClass.simpleName}: ${e.message}")
                Log.e(TAG, "getUser failed", e)
            }
        }
    }
    
    private fun stopPolling() {
        pollingRunnable?.let { handler.removeCallbacks(it) }
        pollingRunnable = null
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        runOnUiThread {
            countdownText.visibility = View.GONE
        }
        logMessage("Stopped automatic polling")
    }
    
    private fun displayTokenInfo() {
        try {
            // Get token expiration
            val expClaim = kindeSDK.getClaim("exp", TokenType.ACCESS_TOKEN)
            if (expClaim.value != null) {
                // Handle both Long and String types for exp claim
                val expireEpochSeconds = when (val value = expClaim.value) {
                    is Long -> value
                    is String -> value.toLongOrNull() ?: return
                    is Number -> value.toLong()
                    else -> return
                }
                val expireEpochMillis = expireEpochSeconds * 1000
                val currentTimeMillis = System.currentTimeMillis()
                val remainingSeconds = (expireEpochMillis - currentTimeMillis) / 1000
                
                val expireDate = Date(expireEpochMillis)
                val currentDate = Date(currentTimeMillis)
                
                logMessage("""
                    Token Info:
                    - Current Time: ${dateFormat.format(currentDate)}
                    - Expires At: ${dateFormat.format(expireDate)}
                    - Time Until Expiry: ${remainingSeconds}s (${remainingSeconds / 60}m)
                """.trimIndent())

                if (remainingSeconds < 300) {
                    logMessage("⚠ Token will expire soon (< 5 minutes)")
                }
            }
        } catch (e: Exception) {
            logMessage("Error getting token info: ${e.message}")
            Log.e(TAG, "Error getting token info", e)
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
    
    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    companion object {
        private const val TAG = "RefreshTokenApp"
    }
}
