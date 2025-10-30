# Firebase Setup Guide for LearnLog

## ✅ Current Status

Your Firebase app ID **`1:762310666511:android:91fd3a81ccb65cd23cf67a`** has been **successfully configured** in:
- ✅ `app/google-services.json` (already in place)
- ✅ App is ready to use Firebase Authentication

## 🔧 Google Sign-In Configuration (Required)

To enable **Google Sign-In**, you need to add an **OAuth 2.0 Web Client ID** in the Firebase Console:

### Step 1: Go to Firebase Console
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **learnlog-1babb**

### Step 2: Enable Google Sign-In Provider
1. Navigate to **Authentication** → **Sign-in method**
2. Click on **Google** provider
3. Click **Enable**
4. You'll see a **Web SDK configuration** section

### Step 3: Add SHA-1 Certificate (Important!)
1. Go to **Project Settings** (gear icon) → **General**
2. Scroll down to **Your apps** section
3. Find your Android app (`com.example.learnlog`)
4. Click **Add fingerprint**

#### Get your SHA-1 fingerprint:

**For Debug builds (development):**
```bash
cd /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager
./gradlew signingReport
```

Look for the SHA-1 under `Variant: debug` and copy it.

**For Release builds (production):**
Use your keystore's SHA-1:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

5. Paste the SHA-1 in Firebase Console and click **Save**

### Step 4: Download Updated google-services.json
1. After adding SHA-1, download the **new** `google-services.json` file
2. Replace the existing file at:
   ```
   /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager/app/google-services.json
   ```

### Step 5: Verify Web Client ID
The new `google-services.json` should have an `oauth_client` section like this:
```json
"oauth_client": [
  {
    "client_id": "762310666511-xxxxxxxxxxxxx.apps.googleusercontent.com",
    "client_type": 3
  }
]
```

## 📱 Package Name Verification

✅ Your app's package name is: **`com.example.learnlog`**
- This matches the configuration in `google-services.json`
- This is registered with Firebase app ID: `1:762310666511:android:91fd3a81ccb65cd23cf67a`

## 🏗️ Build Configuration

All build configurations are already in place:

✅ **Root `build.gradle.kts`:**
- Google Services plugin: `com.google.gms.google-services`

✅ **App `build.gradle.kts`:**
- Plugin applied: `id("com.google.gms.google-services")`
- Firebase BOM: `com.google.firebase:firebase-bom:32.7.0`
- Firebase Auth: `com.google.firebase:firebase-auth-ktx`
- Google Sign-In: `com.google.android.gms:play-services-auth:20.7.0`

✅ **Dependency Injection:**
- `FirebaseModule.kt` configured to use auto-generated `default_web_client_id`
- Fallback handling if Web Client ID is not configured

## 🚀 Testing Google Sign-In

After completing the above steps:

1. **Clean and rebuild:**
   ```bash
   cd /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. **Install on device/emulator:**
   ```bash
   ./gradlew installDebug
   ```

3. **Test the flow:**
   - Open the app
   - Navigate to Login screen
   - Tap "Continue with Google"
   - Select your Google account
   - Should successfully sign in

## 🔒 Email/Password Authentication

Email/Password authentication is **already enabled** and ready to use:
- No additional configuration needed
- Just enable it in Firebase Console → Authentication → Sign-in method → Email/Password

## 📋 Summary

| Component | Status |
|-----------|--------|
| Firebase Project | ✅ Configured (learnlog-1babb) |
| App ID | ✅ `1:762310666511:android:91fd3a81ccb65cd23cf67a` |
| google-services.json | ✅ File in place |
| Build Configuration | ✅ Complete |
| Email/Password Auth | ⚠️ Enable in Console |
| Google Sign-In | ⚠️ Add SHA-1 + Download new google-services.json |

## 🆘 Troubleshooting

**Issue:** "Google Sign-In failed" or "Error 10"
- **Solution:** Make sure SHA-1 is added and you downloaded the updated `google-services.json`

**Issue:** Build error about `default_web_client_id`
- **Solution:** The code has fallback handling, but for full functionality, complete Google Sign-In setup

**Issue:** "API not enabled"
- **Solution:** Go to [Google Cloud Console](https://console.cloud.google.com/) and enable "Google Sign-In API"

## 📞 Need Help?

- [Firebase Documentation](https://firebase.google.com/docs/android/setup)
- [Google Sign-In Setup](https://firebase.google.com/docs/auth/android/google-signin)

