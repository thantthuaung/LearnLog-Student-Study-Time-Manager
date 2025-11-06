# LearnLog: Student Study Time Manager

**CP3406 Assignment 2 - Mobile Application Development**  
A comprehensive study management app built with modern Android architecture and APIs.

---

## 📱 About

LearnLog helps students track study time, manage tasks, plan study sessions, and gain insights into their learning habits. Built entirely in Kotlin using Jetpack Compose, MVVM architecture, Hilt DI, and modern Android APIs.

---

## ✨ Features

### Core Functionality
- **Task Management** - Organize assignments with subjects, priorities, and due dates
- **Pomodoro Timer** - Built-in focus timer with customizable presets
- **Study Planner** - Visual calendar for planning study sessions
- **Insights Dashboard** - Real-time analytics with Jetpack Compose cards
- **Study Notes** - Organize notes by subject with search and pinning
- **Analytics** - Detailed charts showing productivity trends and streaks

### Technical Highlights (CP3406)
- **Jetpack Compose Integration** - Modern declarative UI in Insights screen
- **Networking with Caching** - Daily motivational quotes from quotable.io API
- **Hilt Dependency Injection** - Clean architecture with comprehensive DI
- **Runtime Permissions** - Android 13+ POST_NOTIFICATIONS handling
- **Room Database** - 8 tables for persistent local storage
- **Material Design 3** - Consistent, beautiful UI throughout

---

## 🏗️ Architecture

### Pattern
- **MVVM** (Model-View-ViewModel)
- **Repository Pattern** for data abstraction
- **Clean Architecture** with separation of concerns

### Technologies
- **UI:** Jetpack Compose + XML, Material 3, View Binding
- **DI:** Hilt 2.51.1
- **Database:** Room 2.6.1 (8 DAOs)
- **Networking:** Retrofit 2.9.0 + Moshi 1.15.0 + OkHttp 4.12.0
- **Async:** Kotlin Coroutines + Flow
- **Background:** WorkManager 2.9.0, Foreground Service
- **Auth:** Firebase Authentication with Google Sign-In
- **Storage:** DataStore for preferences and caching

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- Minimum Android 7.0 (API 24)

### Build & Run

```bash
# Clone repository
git clone https://github.com/thantthuaung/LearnLog-Student-Study-Time-Manager.git
cd LearnLog-Student-Study-Time-Manager

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Or use automated script
chmod +x build_submission.sh
./build_submission.sh
```

---

## 📦 Dependencies

### Core Android
```gradle
androidx.core:core-ktx:1.13.1
androidx.appcompat:appcompat:1.7.0
com.google.android.material:material:1.12.0
```

### Jetpack Compose
```gradle
androidx.compose:compose-bom:2024.02.00
androidx.compose.material3:material3
androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5
```

### Architecture
```gradle
androidx.navigation:navigation-fragment-ktx:2.7.3
androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.5
com.google.dagger:hilt-android:2.51.1
```

### Data
```gradle
androidx.room:room-ktx:2.6.1
androidx.datastore:datastore-preferences:1.1.1
androidx.work:work-runtime-ktx:2.9.0
```

### Networking
```gradle
com.squareup.retrofit2:retrofit:2.9.0
com.squareup.retrofit2:converter-moshi:2.9.0
com.squareup.okhttp3:okhttp:4.12.0
```

### Firebase
```gradle
com.google.firebase:firebase-bom:32.7.0
com.google.firebase:firebase-auth-ktx
com.google.android.gms:play-services-auth:20.7.0
```

---

## 📸 Screenshots

*(To be added - record screenshots for promotional video)*

---

## 🎯 CP3406 Requirements Met

| Requirement | Implementation |
|-------------|----------------|
| **Kotlin** | ✅ 100% Kotlin codebase |
| **UI/UX Design** | ✅ Material Design 3, Compose + XML |
| **App Architecture** | ✅ MVVM, Repository, Hilt DI |
| **Navigation** | ✅ Jetpack Navigation + SafeArgs |
| **Room APIs** | ✅ 8 DAOs with migrations |
| **Network Connectivity** | ✅ Retrofit + quotable.io + caching |
| **Jetpack Compose** | ✅ Insights cards (3 composables) |
| **Runtime Permissions** | ✅ POST_NOTIFICATIONS handler |

---

## 📚 Documentation

- **[CP3406_SUBMISSION_READY.md](CP3406_SUBMISSION_READY.md)** - Complete implementation guide
- **[PROMOTIONAL_VIDEO_GUIDE.md](PROMOTIONAL_VIDEO_GUIDE.md)** - Video recording instructions
- **[SUBMISSION_CHECKLIST.md](SUBMISSION_CHECKLIST.md)** - Pre-submission verification
- **[IMPLEMENTATION_COMPLETE.md](IMPLEMENTATION_COMPLETE.md)** - Quick start guide

---

## 🎥 Promotional Video

Recording guide and feature showcase available in [PROMOTIONAL_VIDEO_GUIDE.md](PROMOTIONAL_VIDEO_GUIDE.md)

**Key features to demonstrate:**
- Tasks & Timer with notifications
- Study Planner with calendar
- **Insights with Compose cards** (focus time + subject chart)
- **Daily motivation quote** (networking feature)
- **Permission handling** (Android 13+)
- Analytics dashboard
- Data backup/sync

---

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run lint checks
./gradlew lintDebug

# Generate test reports
./gradlew testDebugUnitTest
```

### Manual Testing Checklist
- [ ] Add task → Verify Room persistence
- [ ] Start timer → Complete → Check notification
- [ ] Open Insights → See Compose cards
- [ ] Wait for quote → Verify network fetch
- [ ] Enable airplane mode → Quote still visible (cache)
- [ ] Settings → Notifications → Permission dialog
- [ ] Grant permission → Notifications work
- [ ] Deny permission → Graceful fallback

---

## 🔒 Permissions

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**Runtime Permissions:**
- `POST_NOTIFICATIONS` - Timer completion alerts (Android 13+)
- Handled with rationale dialog and graceful fallback

---

## 🌐 External APIs

### quotable.io
- **Purpose:** Daily motivational quotes
- **Endpoint:** `GET https://api.quotable.io/random?tags=inspirational`
- **Caching:** DataStore with 24-hour refresh
- **Fallback:** Cached quote if offline

---

## 📄 License

This project is created for CP3406 Assignment 2 educational purposes.

---

## 👤 Author

**Thant Thu Aung**  
CP3406 Mobile Application Development  
James Cook University  
November 2025

---

## 🙏 Acknowledgments

- **quotable.io** - Free quote API
- **Material Design** - Google design system
- **JetBrains** - Kotlin language
- **Android Team** - Jetpack libraries
- **PhilJay** - MPAndroidChart library

---

## 📞 Support

For issues or questions related to this assignment submission:
- Check documentation in project root
- Review implementation guides
- See commit history for change details

---

**Built with ❤️ for CP3406 Assignment 2**

