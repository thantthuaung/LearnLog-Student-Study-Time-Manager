#!/bin/bash

echo "🔧 Starting build process..."
echo ""

cd "$(dirname "$0")"

echo "📦 Cleaning project..."
./gradlew clean

echo ""
echo "🏗️  Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "📱 APK location:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "🚀 Next steps:"
    echo "   1. Install: adb install app/build/outputs/apk/debug/app-debug.apk"
    echo "   2. Or open in Android Studio and click Run"
else
    echo ""
    echo "❌ BUILD FAILED"
    echo "Please check the error messages above"
fi

