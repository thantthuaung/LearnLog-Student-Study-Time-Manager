# LearnLog – Student Study Time Manager

An Android app that helps students plan their work, focus with a timer, and see where their study time goes.

## Features

- **Tasks** – assignments, exams and projects grouped into Overdue / Today / Tomorrow / Upcoming / Completed. Search, filter, sort, swipe right to complete, swipe left to delete (with undo).
- **Planner** – month calendar (swipe to change month) with each day's tasks; add a task straight onto any day.
- **Focus timer** – one app-wide timer that keeps running when you switch tabs or lock the phone (live countdown notification with Pause / Finish). Link a task to log time against it and its subject. Daily focus goal.
- **Insights** – focus time, sessions, day streak, task completion, time by subject, planned vs actual, most-focused tasks, quote of the day.
- **Analytics** – trends over the last 7 / 30 / 90 days.
- **Notes** – searchable, pinnable, tagged notes in a two-column grid.
- **Settings** – profile photo and name, light/dark theme, timer presets, alerts (sound, vibration), JSON / CSV backup and restore.

## Tech stack

Kotlin · MVVM · Hilt · Room · DataStore · Coroutines/Flow · Navigation · WorkManager · Material 3 (XML views + a Jetpack Compose card) · MPAndroidChart · Firebase Authentication · Retrofit/Moshi (ZenQuotes)

## Build & run

Requirements: Android Studio (or JDK 17 + Android SDK 34). Min SDK 24, target SDK 34.

```bash
./gradlew assembleDebug                      # build
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Sign in with email and password. **Google sign-in** additionally needs your machine's debug SHA-1 registered for the Android app in the Firebase console (`./gradlew signingReport` prints it); then download the updated `google-services.json` into `app/`.

## Project layout

```
app/src/main/java/com/example/learnlog/
  auth/        Firebase auth wrapper
  data/        Room entities, DAOs, repositories, DataStore preferences, backup import/export
  di/          Hilt modules (database, networking, Firebase)
  timer/       FocusTimer (app-wide timer), foreground service, alerts
  ui/          Screens: tasks, planner, timer, insights, analytics, notes, settings, auth, shared UI helpers
  worker/      Nightly WorkManager job (daily rollups)
```

Each signed-in account has its own local database file, so accounts never see each other's data on a shared device.

## Notes for development

If the project lives in an iCloud-synced folder (e.g. Desktop), keep build output out of sync: `build`, `app/build` and `.gradle` are symlinks to `*.nosync` folders, which iCloud ignores.
