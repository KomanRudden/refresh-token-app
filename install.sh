#!/bin/bash
# Install script for Kinde Token Refresh Test App

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# Check if any devices are connected
DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep -v "^$" | wc -l)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "No Android devices or emulators connected."
    echo ""
    echo "Building APK instead..."
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✓ Build successful!"
        echo ""
        echo "APK location:"
        echo "  app/build/outputs/apk/debug/app-debug.apk"
        echo ""
        echo "To install:"
        echo "  1. Connect your Android device via USB (enable USB debugging)"
        echo "  2. Run: adb install app/build/outputs/apk/debug/app-debug.apk"
        echo "  3. Or run this script again: ./install.sh"
        echo ""
        echo "To start an emulator:"
        echo "  emulator -list-avds  # List available emulators"
        echo "  emulator -avd <name> # Start an emulator"
    fi
else
    echo "Found $DEVICE_COUNT connected device(s)"
    echo "Installing app..."
    ./gradlew installDebug
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✓ App installed successfully!"
        echo ""
        echo "To launch:"
        echo "  adb shell am start -n com.kinde.refreshtokenapp/.MainActivity"
        echo ""
        echo "To view logs:"
        echo "  adb logcat -s RefreshTokenApp"
    fi
fi
