#!/bin/bash

# Quick Test Script for Hamburger & FAB Fixes
# Tests both the hamburger button tap functionality and FAB color

set -e

PROJECT_DIR="/Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager"
cd "$PROJECT_DIR"

echo "════════════════════════════════════════════════════════════════"
echo "  🔧 Hamburger Button & FAB Color - Test Instructions"
echo "════════════════════════════════════════════════════════════════"
echo ""

echo "📋 Changes Made:"
echo "  ✅ Hamburger button now opens drawer on simple tap"
echo "  ✅ Tasks FAB is now blue (same as Planner)"
echo ""

echo "════════════════════════════════════════════════════════════════"
echo "  🔨 Building APK..."
echo "════════════════════════════════════════════════════════════════"
echo ""

./gradlew clean assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""

    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo "📦 APK: $APK_PATH ($APK_SIZE)"
    fi

    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  🧪 TESTING INSTRUCTIONS"
    echo "════════════════════════════════════════════════════════════════"
    echo ""

    echo "1️⃣  Install on device:"
    echo "    adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo ""

    echo "2️⃣  Test Hamburger Button (CRITICAL TEST):"
    echo "    ┌─────────────────────────────────────────────────┐"
    echo "    │ a. Open app (should land on Tasks page)        │"
    echo "    │ b. TAP hamburger icon (☰) ONCE                 │"
    echo "    │    ✅ Drawer should open IMMEDIATELY           │"
    echo "    │    ❌ Should NOT need press-and-hold           │"
    echo "    │    ❌ Should NOT need swipe gesture            │"
    echo "    │                                                 │"
    echo "    │ c. Repeat on other pages:                      │"
    echo "    │    - Planner: Tap ☰ → Drawer opens ✅         │"
    echo "    │    - Timer: Tap ☰ → Drawer opens ✅           │"
    echo "    │    - Insights: Tap ☰ → Drawer opens ✅        │"
    echo "    └─────────────────────────────────────────────────┘"
    echo ""

    echo "3️⃣  Test FAB Color:"
    echo "    ┌─────────────────────────────────────────────────┐"
    echo "    │ a. Go to Tasks page                             │"
    echo "    │ b. Look at Add Task button (+ FAB)             │"
    echo "    │    ✅ Should be BLUE (nav_blue color)          │"
    echo "    │                                                 │"
    echo "    │ c. Go to Planner page                          │"
    echo "    │ d. Look at Add button (+ FAB)                  │"
    echo "    │    ✅ Should be SAME blue as Tasks             │"
    echo "    │                                                 │"
    echo "    │ Both FABs should be identical blue color!      │"
    echo "    └─────────────────────────────────────────────────┘"
    echo ""

    echo "════════════════════════════════════════════════════════════════"
    echo "  ✅ Expected Results"
    echo "════════════════════════════════════════════════════════════════"
    echo "  • Single tap on hamburger opens drawer instantly"
    echo "  • No need to press-and-hold or swipe"
    echo "  • Drawer opens smoothly with animation"
    echo "  • Tasks FAB is blue (same color as Planner)"
    echo "  • Icon on FAB is white"
    echo ""

    echo "════════════════════════════════════════════════════════════════"
    echo "  🐛 If Issues Occur"
    echo "════════════════════════════════════════════════════════════════"
    echo ""
    echo "Issue: Hamburger still doesn't work with single tap"
    echo "  → Clear app data: adb shell pm clear com.example.learnlog"
    echo "  → Reinstall: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo "  → Try on different page (Tasks vs Planner)"
    echo ""
    echo "Issue: FAB not blue"
    echo "  → Check you're on Tasks page (not another page)"
    echo "  → Compare side-by-side with Planner FAB"
    echo "  → Should see nav_blue color (#2196F3 or similar)"
    echo ""

    echo "════════════════════════════════════════════════════════════════"
    echo "  📝 Report Results"
    echo "════════════════════════════════════════════════════════════════"
    echo ""
    echo "After testing, confirm:"
    echo "  [ ] Hamburger opens drawer on single tap? (YES/NO)"
    echo "  [ ] No press-and-hold needed? (YES/NO)"
    echo "  [ ] Tasks FAB is blue like Planner? (YES/NO)"
    echo ""
    echo "If all YES → Success! 🎉"
    echo "If any NO → Report specific issue for further debugging"
    echo ""

else
    echo ""
    echo "❌ BUILD FAILED!"
    echo "Please check error messages above."
    echo ""
fi

