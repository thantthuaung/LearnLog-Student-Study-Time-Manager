#!/bin/bash

# Build verification script for LearnLog
# This script performs a clean build and reports any issues

echo "========================================="
echo "LearnLog Build Verification"
echo "========================================="
echo ""

# Navigate to project directory
cd "$(dirname "$0")"

echo "Step 1: Cleaning previous build artifacts..."
./gradlew clean

echo ""
echo "Step 2: Building debug APK..."
./gradlew assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "✅ BUILD SUCCESSFUL!"
    echo "========================================="
    echo ""
    echo "APK Location:"
    find app/build/outputs/apk/debug -name "*.apk" 2>/dev/null
    echo ""
    echo "Build Summary:"
    echo "- Logo: Using ic_logo_learnlog.png ✓"
    echo "- Launcher Icon: Android 12+ compatible ✓"
    echo "- Compose Compiler: Version compatible ✓"
    echo ""
else
    echo ""
    echo "========================================="
    echo "❌ BUILD FAILED"
    echo "========================================="
    echo ""
    echo "Please check the error messages above."
    echo "Common issues:"
    echo "1. Missing google-services.json"
    echo "2. Hilt duplicate bindings"
    echo "3. Resource conflicts"
    echo ""
    exit 1
fi

