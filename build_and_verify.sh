#!/bin/bash
# Build verification script for UI Light Theme fix
# Run this from the project root

echo "🔧 Starting clean build..."
./gradlew clean

echo ""
echo "🏗️  Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "📱 APK Location:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "🚀 Next steps:"
    echo "   1. Install APK on physical device"
    echo "   2. Verify light theme appears correctly"
    echo "   3. Check cards are white (not gray)"
    echo "   4. Verify chips show correct colors"
    echo "   5. Confirm calendar is readable"
    echo "   6. Ensure bottom nav is blue with white icons"
else
    echo ""
    echo "❌ BUILD FAILED"
    echo ""
    echo "Check the error messages above"
    exit 1
fi

