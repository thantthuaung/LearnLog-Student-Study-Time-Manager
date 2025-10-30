#!/bin/bash

# LearnLog Phase 3 - Build Script
# Run this to build the app with all Phase 3 features

echo "🚀 Starting LearnLog Phase 3 Build..."
echo ""

# Navigate to project directory
cd /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager

# Step 1: Clean old build artifacts
echo "📦 Step 1: Cleaning old build artifacts..."
./gradlew clean

if [ $? -ne 0 ]; then
    echo "❌ Clean failed. Stopping Gradle daemon and retrying..."
    ./gradlew --stop
    sleep 2
    ./gradlew clean
fi

echo ""
echo "✅ Clean complete!"
echo ""

# Step 2: Build the project
echo "🔨 Step 2: Building project..."
./gradlew build assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ ✅ ✅ BUILD SUCCESSFUL! ✅ ✅ ✅"
    echo ""
    echo "📱 Your APK is ready at:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📊 Phase 3 Features Included:"
    echo "   ✅ Analytics Page (charts, insights)"
    echo "   ✅ Settings Page (preferences, export/import UI)"
    echo "   ✅ Daily Rollups (background optimization)"
    echo "   ✅ WorkManager (scheduled tasks)"
    echo ""
    echo "🎯 Next Steps:"
    echo "   1. Install: ./gradlew installDebug"
    echo "   2. Test existing features (Tasks, Timer, Planner)"
    echo "   3. Add navigation to Analytics/Settings"
    echo "   4. Implement export/import logic"
    echo ""
else
    echo ""
    echo "❌ BUILD FAILED"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   1. Check BUILD_READY_FINAL.md for details"
    echo "   2. Try: File → Invalidate Caches in Android Studio"
    echo "   3. Verify all files are saved"
    echo "   4. Check for typos in recent changes"
    echo ""
    echo "📋 Check these files:"
    echo "   - AppSettings.kt (should NOT have TimerPreset class)"
    echo "   - TimerPreset.kt (should have TimerPreset class)"
    echo "   - strings.xml (should have error_required_field)"
    echo "   - colors.xml (should have primary_blue)"
    echo ""
fi

echo "Done! 🎉"

