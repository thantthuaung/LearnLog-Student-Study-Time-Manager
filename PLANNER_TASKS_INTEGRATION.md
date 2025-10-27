# Planner vs Tasks: How They Work Together

## Purpose & Design Philosophy

### Tasks Page
**Purpose**: Task management and completion tracking
- **View**: Linear list of all tasks
- **Focus**: Current tasks, what needs to be done NOW
- **Actions**: Create, edit, complete, start timer, filter, sort
- **Organization**: By status, priority, or due date (user's choice)
- **Use Case**: "What do I need to work on today/this week?"

### Planner Page
**Purpose**: Calendar-based scheduling and time management
- **View**: Monthly calendar grid + selected day's tasks
- **Focus**: WHEN tasks are due, scheduling conflicts, planning ahead
- **Actions**: Reschedule, view by date, see monthly overview
- **Organization**: By date (calendar view)
- **Use Case**: "When is everything due? What's coming up? Do I need to reschedule?"

## Complementary Workflow

### Typical User Flow
1. **Tasks Page** → Create/view all tasks, see what's urgent
2. **Planner Page** → See calendar overview, reschedule conflicts
3. **Tasks Page** → Work on tasks, start timer, mark complete
4. **Planner Page** → Review upcoming week, adjust schedule
5. Repeat...

### Syncing Behavior
- **Single Source of Truth**: Room database (TaskEntity)
- **Real-time Updates**: Both pages observe the same Flow<List<TaskEntity>>
- **Instant Reflection**: Changes in either page immediately appear in the other
- **Examples**:
  - Create task in Tasks → dot appears on Planner calendar
  - Reschedule in Planner → task moves to new date in Tasks list
  - Complete in Tasks → calendar dot turns gray in Planner
  - Delete in Planner → task removed from Tasks list

## Feature Comparison Matrix

| Feature | Tasks Page | Planner Page |
|---------|-----------|--------------|
| **View All Tasks** | ✅ Complete list | 🔵 Filtered by date |
| **Calendar Overview** | ❌ No calendar | ✅ Month grid view |
| **Create Task** | ✅ Main action | ✅ Prefills selected date |
| **Edit Task** | ✅ Full edit | ✅ Full edit (same dialog) |
| **Complete Task** | ✅ Checkbox | ✅ Checkbox |
| **Delete Task** | ✅ With confirmation | ✅ With confirmation |
| **Start Timer** | ✅ Opens popup | ✅ Opens popup (same) |
| **Reschedule** | 🔵 Edit due date | ✅ Quick reschedule sheet |
| **Filter** | ✅ All/Pending/Completed | ✅ Same + Overdue |
| **Sort** | ✅ By due/priority/title | ⚪ Auto (priority → due) |
| **Visual Indicators** | ✅ Status line, priority dot | ✅ Calendar dots, colors |
| **Task Details** | ✅ Full card view | ✅ Same card view |
| **Empty State** | ✅ "Add first task" | ✅ "No tasks for date" |

## When to Use Each Page

### Use **Tasks Page** When:
- ✅ You want to see ALL tasks at once
- ✅ You need to sort/filter by status or priority
- ✅ You're working through your task list sequentially
- ✅ You want quick access to start timers
- ✅ You're focused on completing tasks TODAY

### Use **Planner Page** When:
- ✅ You want to see what's due THIS WEEK or THIS MONTH
- ✅ You need to reschedule tasks due to conflicts
- ✅ You're planning your study schedule ahead of time
- ✅ You want to see if certain days are overloaded
- ✅ You're checking if you have free time on a specific date

## Visual Indicators Explained

### Tasks Page
- **Thin progress line**: Time-to-due progress (blue → orange → red)
- **Priority dot**: Green (low) / Amber (medium) / Red (high)
- **Status text**: PENDING / IN_PROGRESS / COMPLETED / OVERDUE
- **Checkbox**: Quick complete/uncomplete

### Planner Page
- **Calendar dots**: 1-3 dots per day showing task count
- **Dot colors**:
  - 🔴 Red = Overdue or high priority
  - 🔵 Blue = Normal tasks
  - ⚪ Gray = All completed
- **Red underline**: Day has overdue tasks
- **Blue border**: Selected date
- **Light blue bg**: Today
- **Faded text**: Days from other months

## Integration Points

### Both Pages Share:
1. **AddEditTaskBottomSheet** - Same dialog for creating/editing
2. **TaskTimerBottomSheet** - Same popup timer
3. **TaskEntityAdapter** - Same task card design
4. **TaskRepository** - Same database layer
5. **Real-time sync** - Observe same Flow

### Unique to Each:
- **Tasks**: FilterAdapter, HeaderAdapter, sort menu, swipe actions
- **Planner**: CalendarAdapter, RescheduleTaskBottomSheet, month navigation

## Example Scenarios

### Scenario 1: Planning a Study Week
1. Open **Planner** → see this week on calendar
2. Notice Tuesday has 5 tasks (3 dots) → click Tuesday
3. See all Tuesday tasks listed below
4. Long-press a task → Reschedule → Move to Thursday
5. Calendar updates: Tuesday now shows 2 dots, Thursday shows 4
6. Go to **Tasks** → verify the task now shows Thursday due date

### Scenario 2: Working Through Today's Tasks
1. Open **Tasks** → see all pending tasks
2. Start working, check off completed tasks
3. Realize tomorrow is overloaded → switch to **Planner**
4. Click tomorrow → see 7 tasks
5. Reschedule 3 of them to next week
6. Go back to **Tasks** → continue working on today's tasks

### Scenario 3: Creating a New Task with a Future Due Date
1. Open **Planner** → navigate to next week
2. Click Monday → see only 1 task
3. Tap FAB → Add Task dialog opens with Monday prefilled
4. Fill in task details → Save
5. Calendar immediately shows 2 dots on Monday
6. Go to **Tasks** → new task appears in the list

## Best Practices

### For Users:
- Use **Tasks** daily for execution
- Use **Planner** weekly for planning
- Reschedule conflicting tasks as early as possible
- Check **Planner** every Sunday to plan the week ahead

### For Developers:
- Always use the shared TaskRepository methods
- Never mutate task data directly in adapters
- Ensure both pages observe the same Flow
- Keep UI updates reactive (no manual refresh calls)
- Test changes in BOTH pages to ensure sync works

---

**TL;DR**: Tasks = "Do", Planner = "When". Both sync perfectly via Room database. Use Tasks for daily work, Planner for weekly scheduling.

