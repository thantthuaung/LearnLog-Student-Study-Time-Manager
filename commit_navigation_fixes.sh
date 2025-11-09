#!/bin/bash

# Navigation & Profile Binding Fixes - Git Commit Script
# Run this to commit all the navigation and profile binding improvements

cd "$(dirname "$0")"

echo "📋 Staging all changes..."
git add -A

echo ""
echo "📊 Git status:"
git status

echo ""
echo "💾 Creating commit..."
git commit -m "fix: Navigation drawer, back button behavior, and profile binding

✅ Fixed hamburger → Settings navigation with proper drawer behavior
✅ Settings now shows back arrow instead of hamburger
✅ Drawer locked on Settings page (can't swipe open)
✅ Back button returns to previous page from Settings
✅ Removed hardcoded profile placeholders (student@learnlog.app)
✅ Profile updates persist and reflect immediately in drawer header
✅ Email validation improved (optional but format-checked)
✅ Removed duplicate settingsFragment in nav_graph

Changes:
- MainActivity: Fixed AppBarConfiguration, removed Settings from top-level
- MainActivity: Added drawer lock behavior on Settings
- MainActivity: Updated profile binding with proper empty states
- BaseFragment: Smart hamburger/back arrow switching based on destination
- nav_graph.xml: Removed duplicate settingsFragment definition
- nav_header.xml: Removed hardcoded placeholder text
- AccountSettingsFragment: Improved validation (email optional)
- strings.xml: Added navigate_up accessibility string

Navigation now works correctly:
- Top-level pages (Tasks/Planner/Timer/Insights): Hamburger opens drawer
- Settings page: Back arrow navigates up, drawer locked
- Profile data: Saves to DataStore, updates drawer instantly
- No more student@learnlog.app placeholder

Documentation:
+ NAVIGATION_PROFILE_FIXES.md: Complete change summary
+ NAVIGATION_FLOW_DIAGRAM.md: Visual navigation architecture"

echo ""
echo "✅ Commit created successfully!"
echo ""
echo "🚀 To push to GitHub, run:"
echo "   git push origin main"

