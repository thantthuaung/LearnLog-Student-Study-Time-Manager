# LearnLog - Student Study Time Manager

**Version:** 1.0.0  
**Target SDK:** Android 14 (API 34)  
**Minimum SDK:** Android 7.0 (API 24)

LearnLog is a comprehensive Android productivity app designed to help students manage their study time, track tasks, plan sessions, and gain insights into their learning progress.

---

## ✨ Features

### 📋 Task Management
- Create, edit, and delete tasks with subjects, priorities, and due dates
- Mark tasks as complete with visual feedback
- Filter tasks by subject, priority, or completion status
- Swipe to delete tasks
- Empty state guidance for new users

### 📅 Planner
- Visual calendar with color-coded study sessions
- Schedule study sessions for specific tasks
- Reschedule tasks with drag-and-drop
- View daily schedule with time blocks
- Integration with task list

### ⏱️ Timer
- Pomodoro-style study timer with preset durations (25, 45, 60 minutes)
- Task-specific timer tracking
- Session logging with automatic breaks
- Haptic feedback and notifications
- Running timer notification with controls

### 📊 Insights & Analytics
- Daily, weekly, and monthly study statistics
- Subject-wise time breakdown with pie charts
- Study streak tracking
- Top task performance analysis
- Motivational quotes integration

### 📝 Notes
- Create and manage study notes
- Categorize notes by subject
- Rich text formatting support
- Search and filter notes

### ⚙️ Settings
- Account management with Google Sign-In
- Data backup and export (JSON format)
- Import data from backups
- Notification preferences
- Timer preset customization
- Dark mode toggle (Light mode enforced in v1.0)

---

## 🏗️ Architecture

LearnLog follows **Clean Architecture** principles with **MVVM** pattern:

```
┌─────────────────────────────────────────────────┐
│                  Presentation Layer             │
│  ┌──────────────────────────────────────────┐   │
│  │  Fragments + ViewModels + UI Components │   │
│  │  - Material 3 Design                    │   │
│  │  - Jetpack Compose (Insights UI)        │   │
│  │  - Data Binding + View Binding          │   │
│  └──────────────────────────────────────────┘   │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│               Domain Layer                      │
│  ┌──────────────────────────────────────────┐   │
│  │  Repositories + Use Cases                │   │
│  │  - TaskRepository                        │   │
│  │  - PlannerRepository                     │   │
│  │  - InsightsRepository                    │   │
│  └──────────────────────────────────────────┘   │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│                  Data Layer                     │
│  ┌──────────────────────────────────────────┐   │
│  │  Room Database + DAOs                    │   │
│  │  - TaskEntity, StudySession, Note        │   │
│  │  - SubjectDao, TaskDao, SessionLogDao    │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │  Remote Data Sources                     │   │
│  │  - Firebase Auth (Google Sign-In)        │   │
│  │  - Retrofit (Quotes API)                 │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │  Local Preferences                       │   │
│  │  - DataStore (User Settings)             │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

### Core Android
- **Kotlin** - Primary language
- **Coroutines** - Asynchronous programming
- **Flow** - Reactive data streams
- **Lifecycle** - Lifecycle-aware components

### UI
- **Material 3** - Modern Material Design
- **Jetpack Compose** - Declarative UI (Insights screen)
- **Data Binding** - Declarative layouts
- **View Binding** - Type-safe view access
- **Navigation Component** - Fragment navigation

### Dependency Injection
- **Hilt** - Dependency injection framework
- **Dagger** - Compile-time DI

### Database & Persistence
- **Room** - Local database (SQLite)
- **DataStore** - Preferences storage (replacing SharedPreferences)

### Background Tasks
- **WorkManager** - Reliable background jobs
- **Foreground Service** - Timer service with notification

### Networking
- **Retrofit** - REST API client
- **OkHttp** - HTTP client with logging
- **Moshi** - JSON serialization

### Firebase
- **Firebase Auth** - Google Sign-In
- **Firestore** - Cloud storage (optional sync)

### UI Libraries
- **MPAndroidChart** - Charts and graphs
- **Glide** - Image loading

### Build & Quality
- **R8** - Code shrinking and obfuscation
- **ProGuard** - Release optimization
- **Lint** - Code quality checks

---

## 📱 Screenshots

### Main Screens
| Tasks | Planner | Timer | Insights |
|-------|---------|-------|----------|
| Task management with filters | Calendar view with sessions | Pomodoro timer | Charts and analytics |

### Features
| Notes | Settings | Empty State |
|-------|----------|-------------|
| Study notes | Account & preferences | Friendly guidance |

---

## 🚀 How to Run

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK** 17
- **Android SDK** API 34
- **Google Services JSON** (for Firebase features)

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/thantthuaung/LearnLog-Student-Study-Time-Manager.git
   cd LearnLog-Student-Study-Time-Manager
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Add Firebase Configuration** (Optional for auth features)
   - Download `google-services.json` from Firebase Console
   - Place it in `app/` directory
   - Update `strings.xml` with your Firebase Web Client ID

4. **Sync Gradle**
   - Android Studio will prompt to sync
   - Wait for dependencies to download

5. **Build and Run**
   ```bash
   ./gradlew clean assembleDebug
   ```
   Or use Android Studio's **Run** button (Shift+F10)

6. **Install APK**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Release Build
```bash
./gradlew clean assembleRelease
```
APK location: `app/build/outputs/apk/release/app-release.apk`

---

## 🔐 Permissions

LearnLog requests the following runtime permissions:

- **POST_NOTIFICATIONS** - For timer completion alerts and study reminders
- **VIBRATE** - Haptic feedback during timer
- **INTERNET** - Fetch motivational quotes
- **FOREGROUND_SERVICE** - Timer service persistence

All permissions are optional; the app remains functional if denied.

---

## 💾 Data Storage

### Local Database (Room)
- **Tasks** - Task entities with subjects, deadlines, priorities
- **Study Sessions** - Logged timer sessions
- **Notes** - User notes
- **Subjects** - Subject categories
- **Planner Entries** - Scheduled study blocks
- **Daily Rollups** - Aggregated daily statistics

### User Preferences (DataStore)
- Timer presets
- Notification settings
- Theme preferences
- Default subject colors

### Data Export/Import
- Export all data to JSON format
- Import from JSON backup
- Located in `Downloads/LearnLog_backup_[timestamp].json`

---

## 🌐 API Usage

### Quotable API
- **Endpoint:** `https://api.quotable.io/random`
- **Purpose:** Fetch motivational quotes for Insights screen
- **Fallback:** Local quote cache on network failure
- **Rate Limit:** No authentication required

---

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

---

## 📦 APK Download

**Latest Release:** v1.0.0  
[Download APK](https://github.com/thantthuaung/LearnLog-Student-Study-Time-Manager/releases/latest)

**Minimum Requirements:**
- Android 7.0 (API 24) or higher
- 50 MB free storage
- Internet connection (optional, for quotes and auth)

---

## 📄 Privacy

LearnLog respects your privacy:

- ✅ All study data stored **locally** on your device
- ✅ No data collection or analytics
- ✅ Google Sign-In data used only for authentication
- ✅ Firestore sync is **opt-in** (disabled by default)
- ✅ Network requests limited to quotes API (quotable.io)
- ✅ No third-party tracking

### Data Deletion
To delete all data:
1. Go to **Settings → Account**
2. Tap **Delete Local Data**
3. Confirm deletion

Or uninstall the app to remove all local storage.

---

## 🛣️ Roadmap

### Upcoming Features
- [ ] Cloud sync across devices
- [ ] Recurring tasks
- [ ] Group study sessions
- [ ] Dark mode support
- [ ] Widgets for quick timer start
- [ ] CSV export for analysis
- [ ] Advanced analytics (weekly reports)
- [ ] Custom timer sounds
- [ ] Wear OS companion app

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable/function names
- Add KDoc comments for public APIs
- Write unit tests for new features

---

## 📜 License

```
Copyright 2025 LearnLog Team

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 👨‍💻 Author

**Thant Thu Aung**  
📧 Email: thantthuaung@example.com  
🔗 GitHub: [@thantthuaung](https://github.com/thantthuaung)

---

## 🙏 Acknowledgments

- [Material Design](https://m3.material.io/) - Design system
- [Quotable API](https://quotable.io/) - Motivational quotes
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - Charting library
- [Firebase](https://firebase.google.com/) - Backend services
- [JetBrains](https://www.jetbrains.com/) - Android Studio

---

## 📞 Support

For bug reports, feature requests, or questions:

- **Issues:** [GitHub Issues](https://github.com/thantthuaung/LearnLog-Student-Study-Time-Manager/issues)
- **Discussions:** [GitHub Discussions](https://github.com/thantthuaung/LearnLog-Student-Study-Time-Manager/discussions)
- **Email:** support@learnlog.app

---

**Made with ❤️ by students, for students**

