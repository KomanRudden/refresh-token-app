# Kinde Token Refresh Test App

This Android app is designed to test the automatic token refresh functionality implemented in the Kinde Android SDK.

## Configuration

- **Client ID**: e9fd8f12f583420c93fd027d842b1873
- **Domain**: https://koman.kinde.com
- **Redirect URI**: kinde.sdk://koman.kinde.com

## Features

The app provides:
1. **Login/Logout** - Standard authentication flow
2. **Get User Info** - Displays user details and current token information
3. **Check Token** - Shows token expiration details and refresh status
4. **Activity Log** - Real-time logging of all SDK events with timestamps

## Building the App

```bash
cd /home/koman/Projects/Kinde/kotlin/312-automatic-token-refresh/refresh-token-app

# Use the build script (sets Java 11)
./build.sh

# Or manually with correct Java version
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
./gradlew assembleDebug
```

## Installing and Running

```bash
# Use the install script (recommended)
./install.sh

# Or manually
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
./gradlew installDebug

# Or install the APK directly
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.kinde.refreshtokenapp/.MainActivity

# View logs
adb logcat -s RefreshTokenApp
```

## Testing Token Refresh

The SDK implements automatic token refresh with the following behavior:

### 1. **How Token Refresh Works**

- Tokens are scheduled to refresh **5 minutes before expiration** (TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000)
- When the app is in foreground, a scheduled refresh runs automatically
- When the app comes back to foreground, it checks if the token needs refresh and reschedules if necessary
- If a token expires while the app is in background, it will be refreshed when the app resumes

### 2. **Key Events to Monitor**

The `SDKListener.onNewToken()` callback is triggered when:
- Initial login completes
- Automatic background refresh succeeds
- Manual refresh is triggered

### 3. **Testing Scenarios**

#### Scenario A: Normal Flow (Long Wait Test)
**Purpose**: Verify scheduled refresh happens automatically

1. Login to the app
2. Click "Get User Info" to see token expiration time
3. Note: "Token is valid - automatic refresh will occur in Xs"
4. **Wait** until the scheduled time (current time + X seconds)
5. Watch the Activity Log for the message: "✓ TOKEN REFRESHED - New token received"
6. The token expiration should now be extended

**Expected Result**: Token automatically refreshes 5 minutes before original expiration time

#### Scenario B: App Background/Foreground (Resume Test)
**Purpose**: Verify refresh on app resume

1. Login to the app
2. Click "Get User Info" to see token expiration time
3. Put the app in background (press Home button)
4. Wait for the token to get close to expiration (or past the 5-minute buffer)
5. Bring the app back to foreground
6. Watch the Activity Log

**Expected Result**: When app resumes, it should detect near-expiry and refresh immediately with log message "✓ TOKEN REFRESHED"

#### Scenario C: API Call with Expired Token (Force Refresh Test)
**Purpose**: Verify refresh happens on API call with expired/near-expired token

1. Login to the app
2. Wait until token is within 5 minutes of expiration (or use a short-lived token)
3. Click "Get User Info" button
4. Watch the Activity Log

**Expected Result**: 
- If token is expired/near-expiry, SDK automatically refreshes before making the API call
- You should see "✓ TOKEN REFRESHED" followed by user info

#### Scenario D: Manual Verification with Logcat
**Purpose**: See detailed logging from the SDK

```bash
# View detailed SDK logs
adb logcat -s RefreshTokenApp KindeSDK:* *:E

# Filter for token refresh events
adb logcat | grep -i "refresh\|token\|expire"
```

### 4. **What to Look For**

#### Success Indicators:
- ✓ "TOKEN REFRESHED - New token received" appears in the log
- Token expiration time updates to a future time
- No logout occurs
- User info can still be fetched successfully
- Status remains "Authenticated ✓"

#### Failure Indicators:
- ✗ "Exception:" messages appear
- Automatic logout occurs
- Status changes to "Not Authenticated"
- Token expiration time doesn't update

### 5. **Timing Details**

The app shows:
- **Current Time**: When the check was made
- **Expires At**: When the token will expire
- **Time Until Expiry**: Countdown in seconds and minutes
- **Refresh scheduled**: Confirms refresh is scheduled 5 minutes before expiry

Example log output:
```
[14:23:45.123] Token Info:
- Current Time: 14:23:45.123
- Expires At: 15:23:45.000
- Time Until Expiry: 3600s (60m)
- Refresh scheduled 5 minutes before expiry

[14:23:45.124] Token is valid - automatic refresh will occur in 3300s
```

This means:
- Token expires at 15:23:45
- Automatic refresh will happen at 15:18:45 (5 minutes before)
- You need to wait 3300 seconds (55 minutes) for the automatic refresh

### 6. **Quick Testing (For Development)**

If you want to test quickly without waiting:

1. **Option A**: Modify TOKEN_REFRESH_BUFFER_MS in KindeSDK.kt to a larger value (e.g., 50 minutes)
   ```kotlin
   private const val TOKEN_REFRESH_BUFFER_MS = 50 * 60 * 1000L // 50 minutes
   ```
   This will make refresh happen sooner after login.

2. **Option B**: Configure your Kinde application to issue short-lived tokens (e.g., 10 minutes)

3. **Option C**: Use the background/foreground test (Scenario B) which can trigger refresh on resume

### 7. **Debugging Tips**

- The Activity Log shows all events with precise timestamps
- Scroll to the bottom of the log to see the latest events
- The "Check Token" button can be clicked repeatedly to monitor the countdown
- Watch for the onNewToken callback in logcat for confirmation
- If refresh fails, check network connectivity and Kinde server status

## Code Structure

- `MainActivity.kt`: Main activity with UI and SDK integration
- `activity_main.xml`: UI layout with buttons and scrollable log
- Implements `SDKListener` interface to receive SDK events:
  - `onNewToken(token: String)` - Called when token is refreshed
  - `onLogout()` - Called when user logs out
  - `onException(exception: Exception)` - Called on errors

## Notes

- The app registers the KindeSDK as a lifecycle observer to handle pause/resume events
- Token refresh is cancelled when app goes to background to save battery
- Token refresh is rescheduled when app comes to foreground
- All events are logged with millisecond precision timestamps for accurate testing
