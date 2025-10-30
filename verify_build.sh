#!/bin/zsh
# Build Verification Script for LearnLog Phase 1

echo "🚀 LearnLog - Phase 1 Build Verification"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

echo "📦 Step 1: Cleaning previous builds..."
./gradlew clean

echo ""
echo "🔨 Step 2: Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "📱 Debug APK location:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "🎉 Phase 1 Implementation Complete!"
    echo ""
    echo "Next steps:"
    echo "  1. Install APK: ./gradlew installDebug"
    echo "  2. Run app on device/emulator"
    echo "  3. Test Phase 1 features (see PHASE_1_COMPLETE.md)"
    echo ""
else
    echo ""
    echo "❌ BUILD FAILED"
    echo ""
    echo "Please check the error messages above and:"
    echo "  1. Fix any compilation errors"
    echo "  2. Ensure all dependencies are synced"
    echo "  3. Run: ./gradlew clean build --stacktrace"
    echo ""
    exit 1
fi

