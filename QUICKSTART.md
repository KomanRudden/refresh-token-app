# Quick Start Guide

## 🚀 Build & Install

```bash
cd /home/koman/Projects/Kinde/kotlin/312-automatic-token-refresh/refresh-token-app
./install.sh
```

The script will:
- ✓ Check for connected Android devices
- ✓ Build the APK if no device is connected
- ✓ Install directly if a device is available

## 📱 Installing on Device

### Option 1: USB Connection
1. Connect your Android device via USB
2. Enable USB debugging on your device:
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → Developer Options → Enable "USB Debugging"
3. Run: `./install.sh`

### Option 2: Manual APK Install
The APK is built at: `app/build/outputs/apk/debug/app-debug.apk`

You can:
- Copy it to your device and install
- Use: `adb install app/build/outputs/apk/debug/app-debug.apk`

### Option 3: Android Emulator
```bash
# List available emulators
emulator -list-avds

# Start an emulator
emulator -avd <emulator_name> &

# Wait for emulator to boot, then run
./install.sh
```

## 🧪 Testing Token Refresh

### Fastest Method: Background/Foreground Test

1. **Login**: Tap "Login" button and authenticate
2. **Check Token**: Tap "Check Token" to see expiration time
3. **Background**: Press Home button (put app in background)
4. **Wait**: Wait a few minutes
5. **Foreground**: Tap app icon to bring it back
6. **Verify**: Look for "✓ TOKEN REFRESHED" in the activity log

### What Success Looks Like

In the app's activity log, you'll see:
```
[14:23:45.123] Token Info:
- Expires At: 15:18:45.000
- Time Until Expiry: 3300s (55m)

... after backgrounding and returning ...

[15:13:45.200] ✓ TOKEN REFRESHED - New token received
[15:13:45.205] Token Info:
- Expires At: 16:13:45.000  ← New expiration!
```

## 📊 Monitoring

```bash
# Use the watch script (recommended)
./watch-logs.sh

# Or manually
adb logcat -s RefreshTokenApp

# Clear old logs first
adb logcat -c && adb logcat -s RefreshTokenApp
```

## 🔧 Troubleshooting

### "No devices connected"
- Connect Android device via USB, OR
- Start an Android emulator

### "Unauthorized device"
- Check your phone screen for USB debugging permission dialog
- Accept the connection

### Can't find emulator command
- Install Android Studio or Android Command Line Tools
- Add to PATH: `export PATH=$PATH:$ANDROID_HOME/emulator`

## 📚 More Information

- See `TESTING_GUIDE.md` for detailed testing scenarios
- See `README.md` for complete documentation
