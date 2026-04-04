# Generated Project Description README

# Productivity Buddy — Process Monitor

---

## Requirements

- Java 17+
- JavaFX 21+
- OSHI dependency (add to `pom.xml`):

---

## Configuration

`config.properties` must be placed in `root` and is bundled into the classpath at build time.

```properties
monitor.interval=3000              # process scan interval in milliseconds (min: 100)
mapping.file=process_info.json     # path to JSON persistence file, relative to working directory
snapshot.interval=60               # periodic CSV snapshot interval in seconds
snapshot.fixed_time_1=15:30:00     # fixed-time snapshot in HH:mm:ss format
snapshot.fixed_time_2=21:00:00     # multiple entries supported (fixed_time_1, fixed_time_2, ...)
```

All keys are optional — defaults are applied for any missing or invalid value.

---

## File locations

All paths are relative to the JVM working directory, which is the project root when run from an IDE.

| File | Purpose |
|------|---------|
| `process_info.json` | Persistent process state — created automatically on first save |
| `process_info.json.tmp` | Temporary write file — renamed atomically, never left in partial state |
| `snapshot_YYYY_MM_DD_HH_mm_ss_SSS.csv` | Point-in-time CSV snapshots |

> **Note:** These paths break when the application is packaged as a `.jar`, `.exe` or `.dmg`. A future upgrade will resolve paths to the platform-appropriate user data directory (`~/Library/Application Support/` on macOS, `%APPDATA%` on Windows, `~/.local/share/` on Linux).

---

## Architecture

The application follows a strict MVC pattern with a single-page application navigation model.

```
App
└── MainController
    ├── ToolbarController       toolbar actions (save, shutdown)
    ├── ProcessListController   left-panel process list
    ├── PieChartController      right-panel category pie chart + summary
    ├── ProcessDetailsController    per-process detail panel
    └── AnalyticsService        1-second tick — pushes live updates to all open views
```

### Thread model

```
scan-executor-thread   (1)   single-thread ScheduledExecutorService
                              reads config → schedules scans
                              never does file I/O

file-executor-thread-N (2)   2-thread ScheduledThreadPool
                              all file I/O: Load History, JSON saves, CSV snapshots
                              jsonLock (ReentrantLock) serialises JSON writes
                              CsvWriter uses CREATE_NEW + 1ms retry for unique filenames

analytics-thread       (1)   single-thread ScheduledExecutorService
                              fires every 1 second
                              checks fixed-time snapshot triggers
                              pushes UI updates via Platform.runLater()

watcher-thread         (1)   daemon thread
                              blocks on WatchService.take()
                              detects manual edits to process_info.json
                              suppressed during app-triggered saves

fx-thread              (1)   JavaFX Application Thread
                              never blocked
```

---

## Process tracking

### Uptime

Uptime is app-managed — it does not use OS process uptime. The `ProcessData` accumulates elapsed seconds between scan cycles into an `uptimeStore` keyed by process name. This means:

- Uptime is preserved across process restarts (same name = same bank entry)
- Uptime is preserved across app sessions (saved to and loaded from `process_info.json`)
- `getLiveUptime()` adds elapsed seconds since the last scan for smooth ticking in the UI between scan cycles

### Freeze

When a process's time tracking is frozen, the `uptimeStore` stops accumulating for that entry. Freeze state survives process exits and app restarts. A frozen process that exits and respawns resumes with its accumulated total unchanged.

### Categories

Processes can be assigned to `Work`, `Fun`, `Other`, or left as `No Category`. Category assignment is persisted to JSON. The category pie chart shows total uptime per category.

---

## Data persistence — process_info.json

### Schema

```json
{
  "processes": [
    {
      "originalName": "idea64.exe",
      "aliasName": "IntelliJ IDEA",
      "category": "Work",
      "isTrackingFreezed": false,
      "totalTimeSeconds": 4980
    }
  ]
}
```

### Save triggers

- **Manual save** — toolbar Save button
- **Graceful shutdown** — toolbar Shutdown button or window close (X)

Both paths write atomically via a temp file + rename. The save always includes:
- Currently running processes (live store values)
- Processes loaded from a previous session that never ran this session (preserved unchanged)
- Processes that ran and exited this session (moved from store to historicData on exit)

### External edit detection (WatcherService)

If `process_info.json` is manually edited outside the app, `WatcherService` detects the `ENTRY_MODIFY` event and reloads the file. Changes are applied immediately:

- **Running process** — alias, category, freeze state applied to live record; `totalTimeSeconds` is aggregated as `jsonValue + currentSessionContribution` so in-session progress is never lost
- **Inactive process** — all fields replaced in memory
- **New entry in JSON** — inserted and ready to activate when that process starts
- **Deleted entry** — removed from memory if inactive; left alive in store if currently running (preserved on next save)

App-triggered saves suppress the watcher via a `ReentrantLock` + `AtomicBoolean` flag so save events are never mistaken for external edits.

---

## CSV snapshots

Snapshots capture a point-in-time view of all running processes.

### Triggers

- **Periodic** — every `snapshot.interval` seconds (default: 60)
- **Fixed-time** — at each configured `snapshot.fixed_time_N` each day, firing exactly once per second match
- **Snapshot complete** — "✓ Snapshot saved" shown in toolbar for 3 seconds after each write

### Format

```
timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name
2026-10-12T10:00:01Z,1240,idea64.exe,12.50,1500.00,Work,IntelliJ IDEA
2026-10-12T10:00:01Z,4450,chrome.exe,5.20,800.00,Fun,YouTube
```

CPU shows `N/A` when OS data is unavailable. RAM shows `0` when unavailable.

---

## Graceful shutdown

Both the toolbar Shutdown button and the window X button follow the same path:

1. Confirm dialog shown (toolbar button only)
2. Toolbar disabled, "Saving..." shown
3. `process_info.json` written atomically
4. Services shut down in order: `AnalyticsService` → `WatcherService` → `xecutorService` → `ProcessScanService` (ForkJoinPool)
5. `Platform.exit()` called
6. `App.stop()` runs as a safety net (no-op in normal path)

If the JVM is force-killed, `App.stop()` performs a best-effort shutdown without saving.

---

## Process actions (Item Detail View)

| Action | Behaviour |
|--------|-----------|
| Kill Process | Sends `SIGTERM` via `ProcessHandle.destroy()`. Process removed from list immediately. `SecurityException` and `UnsupportedOperationException` handled gracefully. |
| Freeze Time Tracking | Pauses uptime accumulation. Button toggles to "Unfreeze". State persists across restarts. |
| Change Name | Sets an alias displayed in place of the original process name throughout the UI. |
| Change Category | Reassigns the process category. List row and chart update immediately. |

---

## Scan resilience

The ForkJoin scan handles the following per-process without crashing:

- Process terminates mid-scan — skipped silently
- OS denies access (`SecurityException`, `AccessDeniedException`) — skipped silently
- `getName()` returns null — skipped
- `getProcessID()` returns -1 — skipped
- `getProcessCpuLoadCumulative()` returns `NaN` — stored as `NaN`, displayed as `N/A`
- `getResidentSetSize()` returns 0 — stored as 0, displayed as `0.0 MB`