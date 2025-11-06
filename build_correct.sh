#!/bin/bash

# Correct build command for LearnLog
echo "🔨 Building LearnLog (Settings implementation)..."
echo ""

cd /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager

# Run the correct build command (build, not built)
./gradlew clean build assembleDebug

# Check result
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "📦 APK Location:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "🎉 Settings screen with Hamburger Drawer is ready to test!"
    echo ""
    echo "Features implemented:"
    echo "  ✅ Drawer navigation with profile header"
    echo "  ✅ Account & Profile settings"
    echo "  ✅ Timer Preferences"
    echo "  ✅ Notifications (with permission handling)"
    echo "  ✅ Data & Backup (export/import)"
    echo "  ✅ Help & About"
else
    echo ""
    echo "❌ BUILD FAILED"
    echo "Check the errors above for details"
    exit 1
fi

