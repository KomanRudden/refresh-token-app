# Token Refresh Testing Guide

## Quick Start

1. **Build and Install**
   ```bash
   cd /home/koman/Projects/Kinde/kotlin/312-automatic-token-refresh/refresh-token-app
   ./install.sh
   ```

2. **Launch the App**
   - Tap the "Kinde Token Refresh Test" icon on your device
   - Or use: `adb shell am start -n com.kinde.refreshtokenapp/.MainActivity`

3. **Login**
   - Tap the "Login" button
   - Complete authentication in the browser
   - You'll be redirected back to the app

## What the App Shows

The app displays:
- **Status bar**: Shows authentication status
- **Buttons**: Login, Logout, Get User Info, Check Token
- **Activity Log**: Scrollable log with timestamped events showing:
  - Login/logout events
  - Token refresh events (marked with ✓)
  - Token expiration details
  - User information
  - Any errors (marked with ✗)

## Testing Token Refresh

### Method 1: Background/Foreground Test (Quickest)

This is the **fastest way** to test token refresh:

1. Login to the app
2. Click "Check Token" to see when refresh is scheduled
3. Put app in background (press Home button)
4. Wait a few minutes
5. Bring app back to foreground
6. **Expected**: You should see "✓ TOKEN REFRESHED" in the log if token was close to expiration

### Method 2: Wait for Scheduled Refresh

This tests the automatic scheduled refresh:

1. Login to the app  
2. Click "Check Token" - note the time until refresh (shown as "automatic refresh will occur in Xs")
3. Keep the app open and in foreground
4. Wait for the scheduled time
5. **Expected**: You should see "✓ TOKEN REFRESHED - New token received" exactly at the scheduled time

### Method 3: API Call Triggers Refresh

1. Login to the app
2. Wait until token is near expiration (within 5 minutes)
3. Click "Get User Info"
4. **Expected**: Token refreshes automatically before making the API call

## Understanding the Logs

### Successful Refresh
```
[14:23:45.123] Token Info:
- Current Time: 14:23:45.123
- Expires At: 15:18:45.000
- Time Until Expiry: 3300s (55m)
- Refresh scheduled 5 minutes before expiry

[14:23:45.124] Token is valid - automatic refresh will occur in 3000s

... wait 50 minutes ...

[15:13:45.200] ✓ TOKEN REFRESHED - New token received (length: 1234)
[15:13:45.205] Token Info:
- Current Time: 15:13:45.205  
- Expires At: 16:13:45.000  <-- New expiration time
- Time Until Expiry: 3600s (60m)
```

### What to Look For

**✓ Success Indicators:**
- "✓ TOKEN REFRESHED" appears in log
- Token expiration time extends into the future
- User remains authenticated
- No logout occurs

**✗ Failure Indicators:**
- "✗ Exception:" messages
- Automatic logout
- Status changes to "Not Authenticated"
- Token expiration doesn't update

## Key Timing Details

The SDK implements these timing rules:

- **TOKEN_REFRESH_BUFFER_MS = 5 minutes**
- Token refresh is scheduled to occur **5 minutes before expiration**
- When app resumes from background, it checks if token needs refresh
- If token is within 5 minutes of expiry, refresh happens immediately
- If refresh fails, it retries after 1 minute

## Monitoring with Logcat

For detailed SDK logging:

```bash
# View app logs only
adb logcat -s RefreshTokenApp

# View with SDK internals
adb logcat -s RefreshTokenApp KindeSDK:*

# Filter for refresh events
adb logcat | grep -i "refresh\|token\|expire"

# Clear logs and start fresh
adb logcat -c && adb logcat -s RefreshTokenApp
```

## Testing with Short-Lived Tokens

If you want to test quickly without waiting:

### Option A: Modify SDK Buffer Time
Edit `KindeSDK.kt` in the SDK project and increase the buffer:
```kotlin
private const val TOKEN_REFRESH_BUFFER_MS = 50 * 60 * 1000L // 50 minutes instead of 5
```
Then rebuild both SDK and app. This makes refresh happen sooner after login.

### Option B: Configure Kinde for Short Tokens
In your Kinde dashboard:
1. Go to your application settings
2. Set token lifetime to 10-15 minutes
3. This allows faster testing cycles

### Option C: Use Background/Foreground Method
This doesn't require code changes - just use Method 1 above.

## Troubleshooting

### App doesn't refresh token
- Check that app is in foreground (refresh pauses in background to save battery)
- Verify network connectivity
- Check logcat for error messages
- Ensure Kinde server is accessible

### Can't login
- Verify client ID: `e9fd8f12f583420c93fd027d842b1873`
- Verify domain: `koman.kinde.com`
- Check that redirect URI is configured in Kinde dashboard: `kinde.sdk://koman.kinde.com`

### Build fails
- Ensure Java 11 is being used: `./build.sh` or `export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64`
- Clean build: `rm -rf .gradle build app/build`

## SDK Implementation Details

The token refresh is implemented in `KindeSDK.kt`:

1. **scheduleTokenRefresh()**: Calculates time until refresh (expiry - 5 minutes) and schedules a Handler callback
2. **onResume()**: When app comes to foreground, checks if token needs refresh and reschedules
3. **onPause()**: Cancels scheduled refresh to save battery
4. **getToken()**: Performs the actual refresh using the refresh token
5. **onNewToken()**: Callback fired when token is refreshed - this is what your app monitors

The test app implements `SDKListener` interface to receive these callbacks.
