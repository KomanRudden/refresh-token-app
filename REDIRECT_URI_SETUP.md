# Fixing Error 1656 - Redirect URI Setup

## The Problem

Error 1656 from Kinde means the redirect URI used by the app is not registered in your Kinde application settings.

## The Solution

You need to add the redirect URI to your Kinde application:

### Step 1: Log into Kinde Dashboard

Go to: https://koman.kinde.com

### Step 2: Navigate to Your Application

1. Go to **Settings** → **Applications**
2. Find and open the application with Client ID: `e9fd8f12f583420c93fd027d842b1873`

### Step 3: Add Redirect URIs

In the application settings, find the **Allowed callback URLs** section and add:

```
kinde.sdk://koman.kinde.com
```

Also add to **Allowed logout redirect URLs**:

```
kinde.sdk://koman.kinde.com
```

### Step 4: Save Changes

Click **Save** to apply the changes.

### Step 5: Test Again

1. Launch the app on your emulator
2. Tap the "Login" button
3. Enter your credentials
4. You should now be redirected back to the app successfully

## Understanding the Redirect URI

- **Scheme**: `kinde.sdk`
- **Host**: `koman.kinde.com`
- **Full URI**: `kinde.sdk://koman.kinde.com`

This URI tells Kinde where to redirect after authentication. The Android system uses this to open your app.

## Verification

After adding the redirect URI, when you login:

1. ✅ Browser opens with Kinde login page
2. ✅ You enter credentials
3. ✅ Kinde redirects to `kinde.sdk://koman.kinde.com`
4. ✅ Android opens your app
5. ✅ App receives the authentication token
6. ✅ You see "Status: Authenticated ✓"

## Still Having Issues?

### Check the Redirect URI Format

Make sure you entered exactly:
```
kinde.sdk://koman.kinde.com
```

Common mistakes:
- ❌ `kinde.sdk:/koman.kinde.com` (single slash)
- ❌ `kinde.sdk//koman.kinde.com` (missing colon)
- ❌ `kinde.sdk://koman.kinde.com/` (trailing slash)
- ✅ `kinde.sdk://koman.kinde.com` (correct)

### Check Application Configuration

In your Kinde application settings, verify:
- **Application type** should support OAuth 2.0
- **Grant types** should include "Authorization Code" with PKCE
- The client ID matches: `e9fd8f12f583420c93fd027d842b1873`

### View App Logs

If still having issues, check the logs:
```bash
./watch-logs.sh
```

Look for any error messages or redirect issues.

## Alternative: Use Standard Redirect URI Pattern

If you prefer, you can use the package-based redirect URI pattern:

1. Change the redirect URI to:
   ```
   com.kinde.refreshtokenapp:/oauth2redirect
   ```

2. Update the manifest at `app/src/main/AndroidManifest.xml`:
   ```xml
   <data
       android:scheme="com.kinde.refreshtokenapp"
       android:host="oauth2redirect" />
   ```

3. Update MainActivity.kt:
   ```kotlin
   kindeSDK = KindeSDK(
       activity = this,
       loginRedirect = "com.kinde.refreshtokenapp:/oauth2redirect",
       logoutRedirect = "com.kinde.refreshtokenapp:/oauth2redirect",
       sdkListener = this
   )
   ```

4. Add this URI to Kinde dashboard
5. Rebuild: `./install.sh`

But the current setup with `kinde.sdk://koman.kinde.com` should work fine once registered!
