#!/bin/bash

# Build script for LearnLog app
cd /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager

echo "🔨 Starting clean build..."
./gradlew clean assembleDebug --console=plain

if [ $? -eq 0 ]; then
    echo "✅ Build succeeded!"
    echo "📦 APK location: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Build failed. Check errors above."
    exit 1
fi

