#!/bin/bash

# 🚀 Quick Build & Test Script for Navigation Fixes
# This script builds the app and provides instructions for testing

set -e  # Exit on error

PROJECT_DIR="/Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager"
cd "$PROJECT_DIR"

echo "════════════════════════════════════════════════════════════════"
echo "  🔧 LearnLog - Navigation & Profile Fixes - Build & Test"
echo "════════════════════════════════════════════════════════════════"
echo ""

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "❌ Error: gradlew not found. Are you in the right directory?"
    exit 1
fi

echo "📋 Step 1: Cleaning previous builds..."
./gradlew clean

echo ""
echo "🔨 Step 2: Building debug APK..."
echo "(This may take 1-2 minutes...)"
./gradlew assembleDebug

# Check if build succeeded
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  📱 APK Location:"
    echo "════════════════════════════════════════════════════════════════"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo "  File: $APK_PATH"
        echo "  Size: $APK_SIZE"
    fi
    echo ""

    echo "════════════════════════════════════════════════════════════════"
    echo "  🎯 Next Steps:"
    echo "════════════════════════════════════════════════════════════════"
    echo ""
    echo "1️⃣  Install on connected device/emulator:"
    echo "    adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "2️⃣  Or install via Android Studio:"
    echo "    - Open project in Android Studio"
    echo "    - Click Run ▶️  (Shift+F10)"
    echo ""
    echo "3️⃣  Test the navigation fixes:"
    echo "    See: TESTING_CHECKLIST_NAVIGATION.md"
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  ✨ What's Fixed:"
    echo "════════════════════════════════════════════════════════════════"
    echo "  ✅ Hamburger opens drawer from Tasks/Planner/Timer/Insights"
    echo "  ✅ Drawer items navigate to Settings with correct section"
    echo "  ✅ Settings shows back arrow (not hamburger)"
    echo "  ✅ Back arrow returns to previous page"
    echo "  ✅ Drawer locked on Settings (can't swipe open)"
    echo "  ✅ Profile data persists to DataStore"
    echo "  ✅ Drawer header updates instantly after save"
    echo "  ✅ No more 'student@learnlog.app' placeholder"
    echo "  ✅ Email is optional (but validated if provided)"
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  🧪 Quick Test (30 seconds):"
    echo "════════════════════════════════════════════════════════════════"
    echo "  1. Open app → Tap hamburger (☰) → Drawer opens ✅"
    echo "  2. Tap 'Account & Profile' → Goes to Settings ✅"
    echo "  3. See back arrow (←) not hamburger ✅"
    echo "  4. Tap back arrow → Returns to Tasks ✅"
    echo "  5. Settings → Account → Enter name/email → Save ✅"
    echo "  6. Open drawer → See updated profile ✅"
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo ""
    echo "📚 Documentation created:"
    echo "  • NAVIGATION_COMPLETE.md         - Main summary"
    echo "  • NAVIGATION_PROFILE_FIXES.md    - Detailed changes"
    echo "  • NAVIGATION_FLOW_DIAGRAM.md     - Visual architecture"
    echo "  • TESTING_CHECKLIST_NAVIGATION.md - 40+ test cases"
    echo ""
    echo "💾 To commit these changes:"
    echo "  ./commit_navigation_fixes.sh"
    echo ""
    echo "🚀 Happy testing! 🎉"
    echo ""
else
    echo ""
    echo "❌ BUILD FAILED!"
    echo ""
    echo "Please check the error messages above."
    echo "Common issues:"
    echo "  • Internet connection (Gradle needs to download dependencies)"
    echo "  • Java/JDK version mismatch"
    echo "  • Android SDK not properly configured"
    echo ""
    echo "Try:"
    echo "  ./gradlew clean build --info"
    echo ""
    exit 1
fi

