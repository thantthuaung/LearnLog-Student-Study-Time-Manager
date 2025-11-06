#!/bin/bash
# Git Commit and Push Script for LearnLog v1.0.0

PROJECT_DIR="/Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager"

echo "========================================="
echo "LearnLog v1.0.0 - Git Commit & Push"
echo "========================================="
echo ""

cd "$PROJECT_DIR"

# Show status
echo "Current git status:"
git status --short
echo ""

# Add all changes
echo "✓ Adding all changes..."
git add -A

# Commit with detailed message
echo "✓ Committing changes..."
git commit -m "feat: Release hardening v1.0.0 - Production ready

FEATURES:
- Android 12+ splash screen with brand blue background
- Comprehensive ProGuard/R8 optimization for release builds
- Detailed README.md with architecture, setup, and documentation

FIXES:
- Fixed duplicate Dagger/Hilt bindings in DI modules
- Consolidated all DAOs in DatabaseModule with @Singleton scope
- Fixed ImportResult property references in DataBackupSettingsFragment
- Moved UserPreferences and SettingsRepository to DataStoreModule

IMPROVEMENTS:
- Reduced OkHttp logging to BASIC level for release
- Updated .gitignore to exclude temporary MD documentation
- Version updated to 1.0.0
- Light theme enforced (dark mode disabled)
- All themes use consistent brand colors

DOCUMENTATION:
- Complete README with features, architecture diagram, tech stack
- Setup instructions and build commands
- Privacy policy and permissions documentation
- Contributing guidelines and roadmap

Build Configuration:
- minSdk: 24 (Android 7.0)
- targetSdk: 34 (Android 14)
- versionCode: 1
- versionName: 1.0.0
- ProGuard: Enabled for release
- R8 optimization: Enabled
- Resource shrinking: Enabled"

# Push to main
echo "✓ Pushing to GitHub..."
git push origin main

# Create and push tag
echo "✓ Creating release tag v1.0.0..."
git tag -a v1.0.0 -m "Release v1.0.0 - Production ready build

LearnLog - Student Study Time Manager

Features:
- Task management with priorities and deadlines
- Visual planner with calendar integration
- Pomodoro timer with session tracking
- Analytics and insights with charts
- Notes management
- Settings with data backup/export
- Firebase Google Sign-In
- Material 3 Design

Technical:
- Clean Architecture + MVVM
- Hilt dependency injection
- Room database
- Jetpack Compose (Insights)
- WorkManager for background tasks
- Retrofit for networking
- DataStore for preferences

APK: See releases page for download"

git push origin v1.0.0

echo ""
echo "========================================="
echo "✓ Git operations complete!"
echo "========================================="
echo ""
echo "Repository: https://github.com/thantthuaung/LearnLog-Student-Study-Time-Manager"
echo "Release tag: v1.0.0"
echo ""
echo "Next steps:"
echo "1. Go to GitHub repository"
echo "2. Navigate to 'Releases' section"
echo "3. Create a new release from tag v1.0.0"
echo "4. Upload APK files"
echo "5. Add release notes"
echo ""

