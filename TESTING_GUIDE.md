# Phase 1 Testing Guide 🧪

This guide helps you verify that all Phase 1 features are working correctly.

---

## Pre-Testing Setup

1. **Install the app:**
   ```bash
   cd /Users/thantthuaung/StudioProjects/LearnLog-Student-Study-Time-Manager
   ./gradlew installDebug
   ```

2. **Grant necessary permissions:**
   - Notifications
   - (Optional) Do Not Disturb access for timer

---

## 🧪 Test Plan

### 1. TIMER — State Persistence & Notifications

#### Test 1.1: Basic Timer Function
- [ ] Open Timer tab
- [ ] Select 25-minute preset
- [ ] Tap "Start" → Timer counts down
- [ ] Verify: Progress ring fills, time updates every second
- [ ] Tap "Pause" → Timer stops
- [ ] Verify: Ring dims, button shows "Resume"
- [ ] Tap "Resume" → Timer continues
- [ ] Verify: Ring brightens, button shows "Pause"

#### Test 1.2: Timer Persistence
- [ ] Start a 10-minute timer
- [ ] Wait 2 minutes
- [ ] **Press Home** button (background the app)
- [ ] Wait 1 minute
- [ ] Reopen app
- [ ] Verify: Timer still running with ~7 minutes remaining

#### Test 1.3: Process Kill Survival
- [ ] Start a 10-minute timer
- [ ] Wait 2 minutes
- [ ] **Force stop** the app from Settings
- [ ] Reopen app
- [ ] Navigate to Timer tab
- [ ] Verify: Timer restored with correct remaining time

#### Test 1.4: Screen Rotation
- [ ] Start timer
- [ ] Wait 1 minute
- [ ] **Rotate device** (portrait ↔ landscape)
- [ ] Verify: Timer continues with correct time

#### Test 1.5: Notification Actions
- [ ] Start timer
- [ ] Background the app
- [ ] Pull down notification shade
- [ ] Verify: Notification shows "Timer Running" with time
- [ ] Tap **"Pause"** in notification
- [ ] Verify: Notification updates to "Paused" with "Resume" button
- [ ] Tap **"Resume"** in notification
- [ ] Verify: Timer continues
- [ ] Tap **notification body** → App opens on Timer page

#### Test 1.6: Timer Completion
- [ ] Start a **1-minute** timer
- [ ] Wait for completion
- [ ] Verify: 
  - Sound plays (if enabled)
  - Device vibrates (if enabled)
  - Snackbar: "Session saved! Great focus session!"
  - Notification disappears

#### Test 1.7: Input Validation
- [ ] Try to start timer at 0:00
- [ ] Verify: Error message "Cannot start timer at 0:00"
- [ ] Select "Custom" preset
- [ ] Try to set 0 minutes
- [ ] Verify: Error "Minimum duration is 1 minute"
- [ ] Try to set 200 minutes
- [ ] Verify: Error "Maximum duration is 3 hours"

---

### 2. TASKS — Edit, Validation, Priority, Search

#### Test 2.1: Create Task
- [ ] Tap **"+"** FAB button
- [ ] Enter title: "Math Homework"
- [ ] Enter subject: "Mathematics"
- [ ] Select due date: Tomorrow at 11:59 PM
- [ ] Select priority: **High**
- [ ] Tap **"Save Task"**
- [ ] Verify:
  - Task appears in list
  - 🔴 Red priority dot visible
  - Snackbar: "Task created successfully"

#### Test 2.2: Edit Task
- [ ] Long-press on a task
- [ ] Select **"Edit"**
- [ ] Change title to "Updated Title"
- [ ] Change priority to **Low**
- [ ] Tap **"Update Task"**
- [ ] Verify:
  - Task updated in list
  - 🟢 Green priority dot visible
  - Snackbar: "Task updated successfully"

#### Test 2.3: Past Date Validation
- [ ] Tap **"+"** to add task
- [ ] Enter title: "Test Task"
- [ ] Select **yesterday's date**
- [ ] Verify: 
  - Error message: "Date and time cannot be in the past"
  - Save button is **disabled** (grayed out)
- [ ] Change to **tomorrow's date**
- [ ] Verify: Error clears, Save button enabled

#### Test 2.4: Priority Visuals
- [ ] Create 3 tasks with different priorities:
  - Task 1: Low → 🟢 Green dot
  - Task 2: Medium → 🟠 Amber/orange dot
  - Task 3: High → 🔴 Red dot
- [ ] Verify: All dots show correct colors

#### Test 2.5: Search Functionality
- [ ] Create tasks:
  - "Math Homework" (subject: Mathematics)
  - "Physics Lab Report" (subject: Physics)
  - "English Essay" (subject: English)
- [ ] In search field, type **"math"**
- [ ] Verify: Only "Math Homework" shows
- [ ] Clear search, type **"phys"**
- [ ] Verify: Only "Physics Lab Report" shows
- [ ] Clear search
- [ ] Verify: All tasks reappear

#### Test 2.6: Delete Confirmation
- [ ] Long-press on a task
- [ ] Select **"Delete"**
- [ ] Verify: Dialog appears with:
  - Title: "Delete Task?"
  - Message: "Are you sure..."
  - Buttons: "Delete" / "Cancel"
- [ ] Tap **"Cancel"** → Dialog closes, task remains
- [ ] Repeat, tap **"Delete"** → Task removed
- [ ] Verify: Snackbar "Task deleted" with **"UNDO"** action
- [ ] Tap **"UNDO"** → Task restored

---

### 3. EMPTY STATES — Tasks, Planner, Insights

#### Test 3.1: Tasks Empty State
- [ ] Delete all tasks (or fresh install)
- [ ] Go to **Tasks** tab
- [ ] Verify:
  - Icon displayed (checklist icon)
  - Text: "No tasks yet"
  - Hint: "Tap + to add a task"
  - **"Add Task"** button visible
- [ ] Tap **"Add Task"** button
- [ ] Verify: Add Task dialog opens

#### Test 3.2: Planner Empty State
- [ ] Go to **Planner** tab
- [ ] Select a date with no tasks
- [ ] Verify:
  - Text: "No tasks for this date"
  - Hint: "Tap + to add a task"
  - **"Add Task"** button visible
- [ ] Tap button
- [ ] Verify: Add Task dialog opens with **selected date prefilled**

#### Test 3.3: Insights Empty State
- [ ] Go to **Insights** tab (fresh install or no sessions)
- [ ] Verify:
  - Total Focus Time shows: "No focus time yet — start a 25m session"
  - Pie chart section shows: "No sessions yet"

---

### 4. ERROR HANDLING & FEEDBACK

#### Test 4.1: Success Messages
- [ ] Create task → Snackbar: "Task created successfully"
- [ ] Edit task → Snackbar: "Task updated successfully"
- [ ] Mark complete → Snackbar: "Task marked as completed"
- [ ] Duplicate task → Snackbar: "Task duplicated"
- [ ] Complete timer → Snackbar: "Session saved! Great focus session!"

#### Test 4.2: Confirmation Dialogs
- [ ] All destructive actions (delete) show confirmation
- [ ] Dialogs have clear messaging and action buttons
- [ ] Cancel preserves data, Confirm executes action

---

## ✅ Test Results Summary

After completing all tests, fill out this checklist:

### Timer Tests:
- [ ] Basic start/pause/resume works
- [ ] Timer survives backgrounding
- [ ] Timer survives process kill
- [ ] Timer survives screen rotation
- [ ] Notification shows with actions
- [ ] Notification actions work (pause/resume/stop)
- [ ] Timer completion plays sound/vibrate
- [ ] Session logged in Insights
- [ ] Input validation prevents invalid timers

### Tasks Tests:
- [ ] Create task works
- [ ] Edit task works
- [ ] Past date validation works
- [ ] Priority dots show correct colors
- [ ] Search filters tasks correctly
- [ ] Delete requires confirmation
- [ ] Undo restore works
- [ ] All success messages appear

### Empty States:
- [ ] Tasks empty state shows with CTA
- [ ] Planner empty state shows with CTA (prefills date)
- [ ] Insights empty state shows helpful messages

### Error Handling:
- [ ] Confirmation dialogs appear for destructive actions
- [ ] Success snackbars appear for all operations
- [ ] No crashes or unhandled exceptions

---

## 🐛 Bug Report Template

If you find issues, report them with this format:

```
**Bug Title:** [Short description]

**Severity:** Critical / High / Medium / Low

**Steps to Reproduce:**
1. Go to...
2. Tap on...
3. Observe...

**Expected Behavior:**
[What should happen]

**Actual Behavior:**
[What actually happens]

**Device Info:**
- Android version: 
- Device model: 
- App version: 

**Screenshots/Video:**
[Attach if possible]
```

---

## ✅ Sign-Off

**Tester Name:** _________________

**Date:** _________________

**All tests passed?** ☐ Yes  ☐ No (see bug reports)

**Ready for production?** ☐ Yes  ☐ No

**Notes:**
_____________________________________
_____________________________________
_____________________________________

