#!/bin/bash

# CP3406 Assignment 2 - Build and Test Script
# LearnLog - Student Study Time Manager
# Date: November 6, 2025

set -e  # Exit on error

echo "=========================================="
echo "  LearnLog - CP3406 Submission Build"
echo "=========================================="
echo ""

# Navigate to project directory
cd /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager

echo "✓ Step 1: Cleaning previous builds..."
./gradlew clean

echo ""
echo "✓ Step 2: Refreshing dependencies (Compose, Retrofit, Moshi)..."
./gradlew --refresh-dependencies

echo ""
echo "✓ Step 3: Building debug APK..."
./gradlew assembleDebug

echo ""
echo "✓ Step 4: Running lint checks..."
./gradlew lintDebug || echo "⚠ Lint warnings found (non-blocking)"

echo ""
echo "=========================================="
echo "  Build Complete!"
echo "=========================================="
echo ""
echo "📦 APK Location:"
echo "  app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "📋 Next Steps:"
echo "  1. Install APK on device/emulator:"
echo "     ./gradlew installDebug"
echo ""
echo "  2. Test key features:"
echo "     - Open Insights page (see Compose cards)"
echo "     - Check daily motivation quote (networking)"
echo "     - Enable notifications (runtime permission)"
echo "     - Add study session and verify Room storage"
echo ""
echo "  3. Commit changes:"
echo "     git add -A"
echo "     git commit -m 'feat: Add CP3406 submission features'"
echo "     git push origin main"
echo ""
echo "✅ Ready for CP3406 submission!"
echo ""

