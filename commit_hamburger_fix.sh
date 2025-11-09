#!/bin/bash

# Commit script for hamburger button fix

cd "$(dirname "$0")"

echo "🔧 Committing Hamburger Button Fix..."
echo ""

# Stage all changes
git add -A

# Show what's being committed
echo "📋 Changes to commit:"
git status --short

echo ""
echo "💾 Creating commit..."

git commit -m "fix: Hamburger button now opens drawer on tap

✅ Fixed hamburger button - works with single tap (no hold/swipe)
✅ Made Tasks FAB blue to match Planner

Root cause: All main fragments were extending Fragment instead of BaseFragment,
so hamburger button click handler was never set up.

Changes:
- BaseFragment: Added constructor to accept layout ID parameter
- TasksFragment: Changed Fragment → BaseFragment
- PlannerFragment: Changed Fragment → BaseFragment
- TimerFragment: Changed Fragment → BaseFragment
- InsightsFragment: Changed Fragment → BaseFragment
- fragment_tasks.xml: Added blue FAB color (backgroundTint nav_blue)
- top_bar.xml: Improved hamburger button (padding, clickable attributes)
- MainActivity: Enhanced openDrawer() with post and animation

Testing:
- Tap hamburger on any page → Drawer opens immediately ✅
- Tasks FAB is now blue like Planner ✅
- Settings shows back arrow (not hamburger) ✅

Closes hamburger button not working issue."

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Commit created successfully!"
    echo ""
    echo "📊 Commit details:"
    git log -1 --stat
    echo ""
    echo "🚀 Next step: Push to GitHub"
    echo "   Run: git push origin main"
    echo ""
else
    echo ""
    echo "❌ Commit failed!"
    echo "Please check the error messages above."
    echo ""
fi

