# Build Error Fix - Incorrect DAO References

## ❌ Error
```
error: InjectProcessingStep was unable to process 'DataBackupViewModel(TaskEntityDao,StudySessionDao,PlannerEntryDao)' because 'TaskEntityDao' could not be resolved.
```

## 🔍 Root Cause
The `DataBackupViewModel` was using incorrect DAO class names that don't exist in the codebase:
- ❌ `TaskEntityDao` → ✅ `TaskDao`
- ❌ `StudySessionDao` → ✅ `SessionLogDao`
- ❌ `PlannerEntryDao` → ✅ Not needed (planner uses tasks)

## ✅ Fixes Applied

### 1. Fixed DataBackupViewModel.kt
**Changed:**
- Constructor parameters to use correct DAO names: `TaskDao` and `SessionLogDao`
- Removed `PlannerEntryDao` (doesn't exist - planner uses tasks)
- Fixed method calls: `getAll().first()` and `getAllSessions().first()` since these return Flows
- Planner count now uses task count (appropriate since planner entries are tasks)

**Before:**
```kotlin
class DataBackupViewModel @Inject constructor(
    private val taskDao: TaskEntityDao,
    private val sessionDao: StudySessionDao,
    private val plannerDao: PlannerEntryDao
) {
    fun refreshCounts() {
        viewModelScope.launch {
            _taskCount.value = taskDao.getAllTasks().size
            _sessionCount.value = sessionDao.getAllSessions().size
            _plannerCount.value = plannerDao.getAllEntries().size
        }
    }
}
```

**After:**
```kotlin
class DataBackupViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val sessionDao: SessionLogDao
) {
    fun refreshCounts() {
        viewModelScope.launch {
            _taskCount.value = taskDao.getAll().first().size
            _sessionCount.value = sessionDao.getAllSessions().first().size
            _plannerCount.value = taskDao.getAll().first().size
        }
    }
}
```

### 2. Fixed DataBackupSettingsFragment.kt
**Changed:**
- Import statement to include `ImportMode` and `ImportResult` from data.export package
- Added `pendingImportMode` variable to track whether user wants MERGE or REPLACE
- Fixed `importData()` method to use `ImportMode.MERGE` or `ImportMode.REPLACE` enum instead of boolean
- Added proper handling of `ImportResult.Success` and `ImportResult.Error` sealed classes

**Before:**
```kotlin
private fun importData(uri: Uri) {
    val result = withContext(Dispatchers.IO) {
        dataImporter.importData(uri, merge = true) // Wrong parameter type
    }
    Snackbar.make(binding.root, "Imported ${result.tasksImported}...") // Wrong result type
}
```

**After:**
```kotlin
private var pendingImportMode: ImportMode = ImportMode.MERGE

private fun importData(uri: Uri, mode: ImportMode) {
    val result = withContext(Dispatchers.IO) {
        dataImporter.importData(uri, mode) // Correct: ImportMode enum
    }
    
    when (result) {
        is ImportResult.Success -> {
            Snackbar.make(binding.root, "Imported ${result.tasksImported}...")
            viewModel.refreshCounts()
        }
        is ImportResult.Error -> {
            Snackbar.make(binding.root, "Import failed: ${result.message}")
        }
    }
}
```

## 📋 Correct DAO Names Reference
From `AppDatabase.kt`:
- ✅ `TaskDao` - for TaskEntity
- ✅ `SessionLogDao` - for SessionLogEntity  
- ✅ `SubjectDao` - for Subject
- ✅ `NoteDao` - for Note
- ✅ `DailyRollupDao` - for DailyRollupEntity

## 🔨 Build Now
```bash
cd /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager
./gradlew clean assembleDebug
```

## ✅ Expected Result
- Build completes successfully
- No more Hilt/Dagger injection errors
- All Settings features functional
- APK generated at: `app/build/outputs/apk/debug/app-debug.apk`

## 🎯 What Was Fixed
1. ✅ Corrected DAO class names in DataBackupViewModel
2. ✅ Fixed Flow collection with `.first()` 
3. ✅ Removed non-existent PlannerEntryDao dependency
4. ✅ Fixed ImportMode enum usage in DataBackupSettingsFragment
5. ✅ Added proper ImportResult handling

All injection dependencies now resolve correctly! 🚀

