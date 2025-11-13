#!/bin/bash
# Watch app logs in real-time

echo "Watching logs for Kinde Token Refresh Test App"
echo "Press Ctrl+C to stop"
echo ""
echo "----------------------------------------"

adb logcat -c  # Clear old logs
adb logcat -s RefreshTokenApp
