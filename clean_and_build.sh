#!/bin/bash

# Navigate to project directory
cd "$(dirname "$0")"

echo "Cleaning project..."
./gradlew clean

echo "Building debug APK..."
./gradlew assembleDebug

echo "Build complete! Check app/build/outputs/apk/debug/ for the APK"

