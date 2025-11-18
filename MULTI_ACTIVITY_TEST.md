# Multi-Activity Authentication State Test

This test app validates that authentication state syncs correctly across multiple activities using SharedPreferences.

## What Was Fixed

**Issue #27**: When using the SDK in multiple activities, the authentication state wasn't syncing properly. If you logged in on Activity 2, Activity 1 would still show as unauthenticated because it was using an in-memory state object.

**Solution**: 
1. Added `refreshState()` method to explicitly reload state from SharedPreferences
2. Made `isAuthenticated()` check SharedPreferences without side effects
3. Added thread safety with `stateLock` for concurrent access
4. Automatically call `refreshState()` in `onResume()` lifecycle

## Test Scenarios

### Scenario 1: Login in Activity 2, Return to Activity 1 ✅

**Steps:**
1. Launch app → Activity 1 shows "Not Authenticated"
2. Click "Go to Activity 2" button
3. In Activity 2, click "Login" and authenticate
4. After successful login, click "Back to Activity 1"
5. **Expected:** Activity 1 now shows "Authenticated ✓"

**What This Tests:** 
- `isAuthenticated()` reads from SharedPreferences
- `onResume()` calls `refreshState()` automatically
- Multiple SDK instances can coexist

### Scenario 2: Already Authenticated, Navigate Between Activities ✅

**Steps:**
1. Login in Activity 1
2. Navigate to Activity 2
3. **Expected:** Activity 2 immediately shows "Authenticated ✓"
4. Navigate back to Activity 1
5. **Expected:** Activity 1 still shows "Authenticated ✓"

**What This Tests:**
- New SDK instances detect existing auth state
- State persists across activity creation/destruction

### Scenario 3: Logout in Activity 2, Return to Activity 1 ✅

**Steps:**
1. Be authenticated in Activity 1
2. Navigate to Activity 2 (shows authenticated)
3. Click "Logout" in Activity 2
4. Return to Activity 1
5. **Expected:** Activity 1 now shows "Not Authenticated"

**What This Tests:**
- Logout state syncs across activities
- SharedPreferences cleared correctly

## How to Run

### Option 1: Build and Install from Command Line

```bash
cd testapp
./gradlew :app:installDebug
adb shell am start -n com.kinde.refreshtokenapp/.MainActivity
```

### Option 2: Android Studio

1. Open the `kinde-sdk-android` project in Android Studio
2. Select `testapp.app` from the run configuration dropdown
3. Click Run (or Shift+F10)

### Option 3: Build from Root Project

```bash
# From kinde-sdk-android root
./gradlew :testapp:app:installDebug
```

## Watch Logs

To see detailed logging:

```bash
adb logcat -s MainActivity:D SecondActivity:D RefreshTokenApp:D
```

Or use the included script:

```bash
cd testapp
./watch-logs.sh
```

## Expected Log Output

**When navigating from Activity 1 to Activity 2 (already authenticated):**

```
[MainActivity] Navigating to SecondActivity...
[SecondActivity] === SECOND ACTIVITY CREATED ===
[SecondActivity] Initializing NEW SDK instance in SecondActivity...
[SecondActivity] SDK initialized. Checking auth state...
[SecondActivity] isAuthenticated() on onCreate: true
[SecondActivity] ✓ SUCCESS: New SDK instance detected existing auth state!
[SecondActivity] ✓ Test Result: Auth state synced correctly
```

**When returning to Activity 1 after login in Activity 2:**

```
[MainActivity] === MainActivity onResume ===
[MainActivity] Checking auth state after returning from other activity...
[MainActivity] isAuthenticated in onResume: true
[MainActivity] ✓ SUCCESS: MainActivity detected auth state from SecondActivity!
```

## Key Implementation Details

### What Changed in KindeSDK.kt

1. **Added `refreshState()` method:**
   ```kotlin
   fun refreshState() {
       synchronized(stateLock) {
           val stateJson = store.getState()
           if (!stateJson.isNullOrBlank()) {
               state = AuthState.jsonDeserialize(stateJson)
           }
       }
   }
   ```

2. **Modified `isAuthenticated()` to avoid side effects:**
   ```kotlin
   fun isAuthenticated(): Boolean {
       synchronized(stateLock) {
           val stateJson = store.getState()
           if (stateJson.isNullOrBlank()) return false
           val currentState = AuthState.jsonDeserialize(stateJson)
           return currentState.isAuthorized && checkTokenWithState(currentState)
       }
   }
   ```

3. **Added `onResume()` lifecycle hook:**
   ```kotlin
   override fun onResume(owner: LifecycleOwner) {
       super.onResume(owner)
       isPaused = false
       refreshState()  // Sync state when returning to foreground
       if (isAuthenticated()) {
           scheduleTokenRefresh()
       }
   }
   ```

## Troubleshooting

**Issue:** Both activities show "Not Authenticated" even after login
- Check that you're using the correct redirect URIs
- Verify meta-data in AndroidManifest.xml
- Check logcat for authentication errors

**Issue:** Activity 1 doesn't update after returning from Activity 2
- Verify that `onResume()` is being called (check logs)
- Ensure you're not calling `finish()` on Activity 1 before navigating

**Issue:** Concurrent modification exceptions
- This should be fixed with the `stateLock` synchronization
- Report if you still see race conditions

## Configuration

The test app is configured with:
- **Domain:** `koman.kinde.com`
- **Client ID:** `e9fd8f12f583420c93fd027d842b1873`
- **Redirect URI:** `kinde.sdk://koman.kinde.com`

Update these in `AndroidManifest.xml` and the activity files if testing with your own Kinde instance.
