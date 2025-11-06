#!/bin/bash
# Release Build and Verification Script for LearnLog v1.0.0

PROJECT_DIR="/Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager"
APK_OUTPUT="$PROJECT_DIR/app/build/outputs/apk"

echo "========================================="
echo "LearnLog v1.0.0 - Release Build Script"
echo "========================================="
echo ""

cd "$PROJECT_DIR" || exit 1

# Step 1: Clean build
echo "✓ Step 1: Cleaning previous builds..."
./gradlew clean || { echo "✗ Clean failed!"; exit 1; }

echo ""
echo "✓ Step 2: Building debug APK..."
./gradlew assembleDebug || { echo "✗ Debug build failed!"; exit 1; }

echo ""
echo "✓ Step 3: Building release APK..."
./gradlew assembleRelease || { echo "✗ Release build failed!"; exit 1; }

echo ""
echo "========================================="
echo "Build Complete!"
echo "========================================="
echo ""
echo "Debug APK: $APK_OUTPUT/debug/app-debug.apk"
echo "Release APK: $APK_OUTPUT/release/app-release.apk"
echo ""

# Check if APKs exist
if [ -f "$APK_OUTPUT/debug/app-debug.apk" ]; then
    DEBUG_SIZE=$(du -h "$APK_OUTPUT/debug/app-debug.apk" | cut -f1)
    echo "✓ Debug APK size: $DEBUG_SIZE"
else
    echo "✗ Debug APK not found!"
fi

if [ -f "$APK_OUTPUT/release/app-release.apk" ]; then
    RELEASE_SIZE=$(du -h "$APK_OUTPUT/release/app-release.apk" | cut -f1)
    echo "✓ Release APK size: $RELEASE_SIZE"
    echo ""
    echo "⚠️  NOTE: Release APK is unsigned. For production, you need to sign it."
else
    echo "✗ Release APK not found!"
fi

echo ""
echo "Next steps:"
echo "1. Install debug APK:  adb install -r $APK_OUTPUT/debug/app-debug.apk"
echo "2. Test the app on a device"
echo "3. Commit changes:     ./git_commit_release.sh"
echo ""

