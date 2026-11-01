# Dawnly — Progress

> Progress tracker for [PLAN.md](PLAN.md). **PLAN.md is still the full brief and the single source of truth.** Read it first: the requirements, design system, technical approach, and Appendix A are all there. This file records where the work stands and what to do next.
>
> Rules from PLAN.md: work one phase at a time. End each phase with a summary and the steps to verify it on a device. Ask before adding any library not listed in PLAN.md section 5. Update this file whenever a step is done.
>
> **Exception (user's decision, 2026-11-01):** Phases 3, 4, and 5 run **in parallel** in three coding sessions, one git worktree each. See "Parallel work" below.

**Current phase:** Phases 4 and 5 merged into `main` (2026-11-01). Next: a session B follow-up, then session A (Phase 3 + changes), then a new Phase 6 session. See "Handoff" below.
**Last updated:** 2026-11-01

> **The project moved on 2026-10-31** from `C:\Users\aryan\OneDrive\Desktop\Dawnly` to **`C:\Users\aryan\dev\Dawnly`**. OneDrive locked files in `app/build/` mid-build, so every incremental build failed with "Unable to delete directory". Open VS Code in the new folder. Once the new copy is confirmed working, delete the OneDrive copy (it has the same uncommitted changes, so nothing is lost).

---

## ▶ Handoff (2026-11-01, end of session C): the user's plan from here

Session C is closed: Phase 5 and the app icon are merged and pushed. The user's order:

1. **Session B** (`C:\Users\aryan\dev\Dawnly-notify`): Phase 4 follow-up changes, then merge.
2. **Session A** (`C:\Users\aryan\dev\Dawnly-wake`): finish Phase 3 with the changes below, then merge.
3. **A new Phase 6 session** (in `C:\Users\aryan\dev\Dawnly`, today or tomorrow): the portfolio finish.

**Before anything else in B or A: `git rebase main`.** `main` has moved (Phase 4 + Phase 5). Phase 5's `ui/` files are no longer locked to session C. Edit them as needed, but build with the components in `ui/glass/`, use the colours in `ui/theme/`, and keep UI strings in `strings_ui.xml`. Phase 5's summary (further down) explains how the UI is built.

### Session B: Phase 4 follow-up
- **Optional auto-dismiss** (user approved): a Settings switch, **off by default**. When the user seems awake, post "Dismissing your 5:30 alarm in 1 minute" with **Not yet**, then dismiss. Starting the Clock app from the background is blocked on Android 10+, so this needs "Display over other apps" (`SYSTEM_ALERT_WINDOW`). Ask for it only when the switch is turned on, with a short reason. Keep `DismissGuard`'s wrong-alarm check.
- **Hide Google Clock during a dismiss:** try `AlarmClock.EXTRA_SKIP_UI` on `ACTION_DISMISS_ALARM` (documented only for setting alarms, so it may be ignored), then bring Dawnly back or finish straight away. The user wants no Clock screen; a flicker may be the best possible. Applies to both the prompt and Home's "Dismiss alarm" (`ClockApp.dismissNextAlarm`).
- Prompt copy: the user liked "Up already?" / "Looks like you're awake. Dismiss your 5:30 alarm?". Decide with them.
- The prompt already plays the sound chosen in Settings (`AwakeNotifier.useSound`, one channel per sound). Keep that working.
- Update PLAN.md section 4 ("Never dismiss silently" becomes "never without asking, unless the user turns on auto-dismiss").

### Session A: Phase 3 with the user's changes
- **Drop "Phone unlocked" as a sign** (user's finding: the phone can be unlocked while they sleep).
- **Add:** movement (accelerometer or significant motion; no permission needed), music playing (`AudioManager.isMusicActive`; counts **only together with movement**, since people fall asleep to music), and active phone use through Usage access (`UsageStatsManager`, the closest safe stand-in for "touches"; an accessibility service was rejected). Keep steps and screen use.
- Contract change: `WakeSignals` / `WakeSignalSettings` get the new fields and lose unlock. Update the users in the same branch: `ui/home/HomeUiState.kt` (+ its tests), `ui/settings/SettingsScreen.kt` (switch rows) and `Sensitivity.kt` (the `Sign` enum), the onboarding and Settings permission lists (`ui/permissions/AppPermission.kt`) for Usage access, and the debug screen.
- Switch Home's "You're up early" hint (`HomeStatus.Watching.allSignsSeen`) to the real wake scorer, so Home and the prompt agree.
- Update PLAN.md section 4 ("Awake means...") and section 7.
- Phase 6 CI is also A's (already on `phase-3-wake`).

### New Phase 6 session (after A merges)
- README with screenshots, a demo GIF, and "Challenges I solved" (background limits, exact alarms, OEM differences, FGS types, the "Skipped" rule, channel sounds).
- Pick Dawnly's house default sound (the user will choose; it's "Phone default" until then).
- Remove the leftover feasibility strings from `res/values/strings.xml`. Lint lists them as unused.
- Optional: the reboot test from Phase 2.
- Remove the worktrees once all branches are merged: `git worktree remove ..\Dawnly-ui` (C is done, so it can go now), `..\Dawnly-notify`, `..\Dawnly-wake`.

## Earlier: Parallel work (Phases 3, 4, 5)

Phase 2 is committed and pushed. The shared "contract" files the three sessions build on are on `main` (see below). Each session has its own git worktree and branch, already created:

| Session | Work | Folder (open VS Code here) | Branch |
|---|---|---|---|
| **A** | Phase 3 wake detection + Phase 6 CI | `C:\Users\aryan\dev\Dawnly-wake` | `phase-3-wake` |
| **B** | Phase 4 notification and dismiss | `C:\Users\aryan\dev\Dawnly-notify` | `phase-4-notify` |
| **C** | Phase 5 UI polish + Phase 6 app icon | `C:\Users\aryan\dev\Dawnly-ui` | `phase-5-ui` |

The original folder `C:\Users\aryan\dev\Dawnly` keeps `main` checked out. Don't work in it; it's where merges land.

### Shared contracts (on `main`; don't change their signatures)
- `domain/wake/WakeSignals.kt`: `WakeSignals` (live readings) and `WakeSignalSettings` (which signals count, and the goals). A may add fields **with defaults**.
- `service/WatchSession.kt`: `WatchSession.signals: StateFlow<WakeSignals?>`. A publishes to it from `WatchService`; C reads it on Home. null when not watching.
- `notify/AwakeNotifier.kt`: `AwakeNotifier.onUserAwake(context, alarmAt)`. A calls it **at most once per watch window**; B implements it.
- `data/settings/SettingsRepository.kt`: `watchWindow`, `wakeSignals`, and `appearance` (with setters). C builds the Settings screen on these; A reads `wakeSignals`. Treat this file as frozen.
- `ui/debug/DebugScreen.kt`: a stub. A fills it in.
- Manifest: `ACTIVITY_RECOGNITION` is already declared, so C's onboarding can request it before A merges.

### File ownership (only edit your own; anything else is add-only and needs a reason in the commit message)
| Owner | Files |
|---|---|
| A | `domain/wake/` (scoring + tests), `service/WatchService.kt`, `ui/debug/`, `app/src/debug/` (`DebugActivity` with its own "Dawnly debug" launcher icon, so A needs nothing from C's navigation), `.github/workflows/`, `res/values/strings_wake.xml` |
| B | `notify/` (everything for Phase 4), `service/WatchScheduler.kt` (e.g. cancel the prompt when the next alarm changes), `res/values/strings_notify.xml`, manifest entries for B's own components. B's debug trigger: an `adb`-callable receiver in `app/src/debug/AndroidManifest.xml` (A adds to that file too; whoever merges second keeps both entries) |
| C | `ui/` except `ui/debug/`, `MainActivity.kt`, `ui/theme/`, `res/font/`, launcher icon resources, `res/values/themes.xml`, `res/values/strings_ui.xml`, `ic_notification.xml` (artwork only), the manifest `icon`/`roundIcon`/`theme` attributes. C deletes `ui/feasibility/` once Home and Settings cover it |
| Shared, add-only | `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `DawnlyApp.kt`, `res/values/strings.xml` (don't edit; use your own strings file) |
| Frozen | `domain/watch/`, `data/` (except as above), `SettingsRepository.kt`, `WatchSession.kt`, `AwakeNotifier`'s signature |

### Rules for all three sessions
1. **One phone, one app.** All three builds install as `com.dawnly.app`, so each install replaces the last. Build and unit test as much as you like, but **ask the user before `installDebug`**, and tell them what to test.
2. **progress.md:** edit only your own phase section below. Don't touch the overview table or this block; the merge step updates them.
3. Commit on your branch whenever a step works (no need to ask). **Ask the user before merging into `main` and pushing.**
4. Before merging, run `.\gradlew.bat testDebugUnitTest assembleDebug lintDebug`, and get the user to confirm the device test.
5. End with a phase summary and device test steps, as PLAN.md asks.

### How to merge (first finished, first merged; any order works)
Run in your own worktree once the user says go. `main` is shared by all worktrees, so there's nothing to fetch.
```
git rebase main                     # take in whatever merged first; fix conflicts (usually strings, manifest, progress.md)
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
git -C C:\Users\aryan\dev\Dawnly merge --ff-only <your-branch>
git -C C:\Users\aryan\dev\Dawnly push origin main
```
Then update your row in the overview table and push that too. Fast-forward only keeps history linear, with no merge commits. After all three merge, remove the worktrees: `git worktree remove ..\Dawnly-wake` and the same for the other two.

### After all three have merged (one session, any of them)
Phase 6 finish: README with screenshots, a demo GIF, and a "Challenges I solved" section (background limits, exact alarms, OEM differences, FGS types, the "Skipped" rule), then the optional reboot test from Phase 2.

### Earlier history
- Phase 1 done, committed as `a42e43e` (2026-11-01). Samsung and Xiaomi skipped for now (user's decision); test them before sharing the app (PLAN.md section 9).
- Phase 2 done, committed as `16b7cd5` (2026-11-01).

**Useful commands** (new terminals have `JAVA_HOME`, `ANDROID_HOME`, and adb on `PATH`; restart VS Code if they're missing):
```
.\gradlew.bat assembleDebug        # build
.\gradlew.bat installDebug         # build + install on the phone
adb shell am start -n com.dawnly.app/.MainActivity   # launch
```

---

## Phase overview

| Phase | Goal | Status |
|---|---|---|
| 0 | Setup: SDK, Compose project, mockups, Hello screen running on the phone | ✅ Done (2026-10-31) |
| 1 | Feasibility: read the next alarm and dismiss it, tested on 3 phones | ✅ Passed on Nothing Phone (2026-11-01). Samsung and Xiaomi not tested yet |
| 2 | Watch window: settings, exact-alarm scheduling, foreground service | ✅ Done (2026-11-01). Reboot not tested yet |
| 3 | Wake detection: signals, scoring + unit tests, debug screen | ⬜ Not started |
| 4 | Notification and one-tap dismiss, plus the fallback | ✅ Done (2026-11-01), merged first. Tested through the debug adb trigger; the real trigger arrives with Phase 3 |
| 5 | UI polish: glass design system, all screens, motion, dark mode, onboarding | ✅ Done (2026-11-01), merged second. Includes the notification sound picker; the prompt now plays the chosen sound |
| 6 | Portfolio finish: README, GIF, challenges section, CI, app icon | 🟡 App icon done (session C). CI with session A; README after all three merge |

---

## Phase 0 — Setup ✅ Done

### Summary
The Compose project builds and runs on the user's Nothing Phone (2a). "Hello from Dawnly" follows the system theme: dark mode was verified by screenshot (`#050A1A` background) and light mode by the user. The code is committed and pushed to https://github.com/AryanLanke/Dawnly.

### Done
- [x] Android Studio 2026.2.2 installed through winget. Its bundled JDK is **JBR 25**, at `C:\Program Files\Android\Android Studio\jbr`. Used as `JAVA_HOME`; no separate JDK is installed.
- [x] Appendix A saved exactly as-is to `design/mockups.html`.
- [x] Compose project scaffolded (details under "Project facts" below). Gradle wrapper 9.8.2; the `gradle-wrapper.jar` SHA-256 is verified and `distributionSha256Sum` is set.
- [x] Hello screen: `ui/HelloScreen.kt` with a minimal light/dark `DawnlyTheme`.
- [x] Studio setup wizard run (2026-10-31). The SDK is at `C:\Users\aryan\AppData\Local\Android\Sdk` with platform `android-37.0`, build-tools 36.0.0, and the licenses accepted.
- [x] `local.properties` created. The backslashes must be doubled in the file; a single `\U` silently breaks the path and gives "Invalid file path".
- [x] First build fixed: in `settings.gradle.kts`, the regex `"com\.android.*"` must be written `"com\.android.*"` in Kotlin. None of the planned first-build risks (AGP 9's built-in Kotlin, the `compileSdk` DSL, the configuration cache, JDK 25) came up.
- [x] Phone connected: Nothing Phone (2a), model A142, Android 16, serial `00069348R000717`. `installDebug` works.
- [x] Light and dark mode both verified on the phone. Note: `adb shell cmd uimode night no` does nothing while Nothing's Bedtime mode is on, so check light mode by hand.
- [x] User env vars set: `JAVA_HOME` = Studio's `jbr`, `ANDROID_HOME` = the SDK, and `%ANDROID_HOME%\platform-tools` appended to the user `PATH` (kept as `REG_EXPAND_SZ`).
- [x] Branch renamed `master` → `main`. `.gitattributes` added so `gradlew` keeps LF line endings for Linux and CI.
- [x] First commit `2108df3` pushed to https://github.com/AryanLanke/Dawnly (`origin`; `main` tracks `origin/main`). `gh` isn't installed; plain `git push` works through the user's Git Credential Manager login.

---

## Project facts (decisions made so far)

| Item | Value |
|---|---|
| Package / applicationId | `com.dawnly.app`, confirmed final by the user on 2026-10-31. |
| AGP | 9.4.1. 9.5.0 exists but was skipped because its release notes weren't published yet. |
| Gradle | 9.8.2 (wrapper) |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| core-ktx / activity-compose | 1.19.1 / 1.13.0 |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 (Android 17) |
| Java target | 17 |
| Version catalog | `gradle/libs.versions.toml` |
| Release build | R8 minify and resource shrinking on |
| Window theme | `android:Theme.Material.Light.NoActionBar`. All real theming is done in Compose. |

### File layout so far
```
Dawnly/
├─ PLAN.md, progress.md
├─ design/mockups.html
├─ settings.gradle.kts, build.gradle.kts, gradle.properties
├─ gradle/libs.versions.toml, gradle/wrapper/*
├─ gradlew, gradlew.bat
└─ app/
   ├─ build.gradle.kts, proguard-rules.pro
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ res/values/strings.xml, themes.xml
      └─ java/com/dawnly/app/
         ├─ DawnlyApp.kt, MainActivity.kt
         ├─ data/SafeData.kt
         ├─ data/alarm/NextAlarm.kt, NextAlarmSource.kt, ClockApp.kt
         ├─ data/settings/SettingsRepository.kt, data/watch/ArmedAlarmStore.kt
         ├─ domain/watch/WatchState.kt (WatchPlanner), WatchWindow.kt
         ├─ service/WatchScheduler.kt, WatchService.kt, WatchNotifications.kt, Receivers.kt
         └─ ui/feasibility/FeasibilityScreen.kt, WatchWindowSection.kt, ui/theme/Theme.kt
   └─ src/test/java/com/dawnly/app/domain/watch/WatchPlannerTest.kt
```
`data/alarm/` is meant to be reused in later phases: `NextAlarmSource` reads the next alarm and follows it as a Flow, and `ClockApp` handles the dismiss intent, the handler lookup, and the open-Clock fallback. `ClockApp.dismissNextAlarmIntent()` is what Phase 4's notification `PendingIntent` should wrap. `ui/feasibility/` is a throwaway test screen that Home replaces in Phase 5. `DawnlyApp` holds the app-wide singletons (manual wiring, no DI library). `WatchScheduler.reconcile()` is the single entry point for the watch window; see Phase 2.

---

## Later phases: checklists

### Phase 1 — Feasibility test (most important) ✅ Passed on the Nothing Phone

#### Summary
Dawnly can read the next alarm set in the Clock app and get the Clock app to dismiss it, with no extra permissions beyond the normal `SET_ALARM`. On the Nothing Phone (2a) with Google Clock:
- The next alarm shows up straight away and updates live when it changes.
- An alarm 3 hours away and one 2 hours away were both dismissed, which the Clock app confirmed. So every planned watch window length works.
- A daily repeating alarm only had today's ring skipped; it stayed on for the other days. That's the behaviour we want.

Samsung and Xiaomi aren't tested yet; the user has neither phone. They decided to go ahead to Phase 2 and test those before sharing the app. One risk for Phase 4 came out of testing: a stale notification could dismiss the following day's alarm (see the guard in the Phase 4 checklist).

**How to verify on a device:** see "Phase 1 test steps" below the table.

- [x] Screen showing the next alarm time from `AlarmManager.getNextAlarmClock()`, refreshed on `ACTION_NEXT_ALARM_CLOCK_CHANGED`. The receiver is registered with `RECEIVER_NOT_EXPORTED`, which system broadcasts still reach. The screen shows the day and a countdown, and follows the phone's 12/24-hour setting.
- [x] Also show which app owns the alarm (`AlarmClockInfo.showIntent` → creator package), and which app a dismiss request would go to.
- [x] "Dismiss next alarm" button: `AlarmClock.ACTION_DISMISS_ALARM` with `EXTRA_ALARM_SEARCH_MODE = ALARM_SEARCH_MODE_NEXT`, plus the `com.android.alarm.permission.SET_ALARM` permission. After a tap, the screen says whether the next alarm has changed since then. That's how we tell the dismiss really worked.
- [x] Fallback: if no app handles the intent, open the Clock app (`ACTION_SHOW_ALARMS`, or else the alarm owner's launch intent) and explain on screen. There's also a manual "Open Clock app" button.
- [x] Manifest `<queries>` for `DISMISS_ALARM` and `SHOW_ALARMS`. Android 11+ needs these to find the Clock app and show its name.
- [x] Built and launched on the Nothing Phone (no alarm set yet). The dismiss handler is `com.google.android.deskclock/com.android.deskclock.HandleApiCalls`.
- [ ] Test on the Nothing Phone (Google Clock), a Samsung (Samsung Clock), and a Xiaomi (Xiaomi Clock). Fill in the results table below **before starting Phase 2**.

| Phone | Clock app | Reads next alarm? | Dismiss works? | Notes |
|---|---|---|---|---|
| Nothing Phone (2a), Android 16 | Google Clock | ✅ Yes, updates live | ✅ Yes: an alarm 3 h away and one 2 h away were both dismissed, which the Clock app confirmed | Repeating alarm (daily at 12:47 pm): dismiss skipped only today's ring, and the alarm stayed on for the other days ✅ |
| Samsung | Samsung Clock | Not tested | Not tested | User has no Samsung yet. Test before sharing the app |
| Xiaomi | Xiaomi Clock | Not tested | Not tested | User has no Xiaomi yet. Test before sharing the app |

**What this means for the plan:** Google Clock dismisses an upcoming alarm even 3 hours early, so the 30 min / 1 hr / 2 hr watch windows all work there. Other brands' Clock apps are still unknown, so Phase 4's open-Clock fallback has to stay solid.

#### Phase 1 test steps (for each phone)
1. Install with `.\gradlew.bat installDebug` (or sideload `app\build\outputs\apk\debug\app-debug.apk` on borrowed phones), then open Dawnly.
2. **Read:** set an alarm in the Clock app for **3+ hours from now**, then switch back. Dawnly should show that time with the Clock app under "Set by". Change the alarm time and check that Dawnly updates by itself.
3. **Dismiss a far-off alarm:** tap "Dismiss next alarm" while the alarm is 3+ hours away. Note what the Clock app shows, and whether Dawnly says "The next alarm has changed". In the Clock app, check whether the alarm is now off or skipped.
4. **Dismiss a near alarm:** set an alarm **less than 2 hours away** and repeat step 3. Some clock apps (possibly including Google Clock) only dismiss an upcoming alarm in a short window before it rings. That would cap the longest watch window, so record both results.
5. Repeat with a repeating alarm. Dismissing it should skip only the next ring, not turn the alarm off.
6. If something looks wrong, check the logs: `adb logcat -s ClockApp`.

### Phase 2 — Watch window ✅ Done (2026-11-01)

#### Summary
Dawnly starts a quiet foreground service when the watch window opens, and stops it when the alarm time passes or the alarm is dismissed. The window length is a saved setting. Tested on the Nothing Phone with the screen locked: the window opened and closed to the second.

**How it works:** `WatchScheduler.reconcile()` is the only entry point. It reads the next alarm, the window setting, and the armed alarm, and asks the pure `WatchPlanner.decide()` for a `WatchState` (Idle / Waiting / Watching / Skipped). Then it sets the single pending check alarm and starts or stops `WatchService` to match. Every trigger calls it: app resume, a settings change, `NEXT_ALARM_CLOCK_CHANGED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, the exact-alarm permission being granted, and our own check alarm.

**Decisions:**
- **Foreground service type `specialUse`** (explained to the user). `health` needs `ACTIVITY_RECOGNITION` granted before every start, and the user can turn the steps signal off. `dataSync` doesn't fit and has a 6-hour daily cap on Android 15. `shortService` stops after 3 min. `specialUse` needs a Play review reason, which is in the manifest `<property>`. Phase 3 can revisit this (for example `health` when steps are on).
- **"Skipped" state.** An alarm that first appears after its window has begun isn't watched: a snooze, a short nap, or the alarm time edited mid-window. The user is clearly awake, and Phase 3 would flag them straight away. Only an alarm whose window start Dawnly scheduled ahead of time ("armed", stored in DataStore) is watched. That also lets watching resume after a reboot.
- **Never use `setAlarmClock`** for our own alarm, because it would become the "next alarm" we read. We use `setExactAndAllowWhileIdle`.
- **Starting the service from the background.** Android 12+ blocks this except right after an exact alarm fires. So when reconcile runs from a background broadcast inside the window, it schedules the check alarm for "now" and starts the service from there.
- **Fallback when "Alarms & reminders" is off** (the default on Android 14+): `setAndAllowWhileIdle`. On the Nothing Phone, Android gave that alarm a **1-hour delivery window**, so watching could start up to an hour late. The test screen says so. **Phase 5 onboarding must make a strong case for this permission.**
- Window: presets 30 min / 1 hr / 2 hr, custom 5 min to 3 h in 5-min steps, default 1 hr.
- New libraries, both covered by PLAN.md section 5: DataStore Preferences 1.3.0 and JUnit 4.13.2. `lifecycle-runtime-compose` (for `LifecycleResumeEffect`) already comes in through Compose.

- [x] DataStore settings: window length (30 min / 1 hr / 2 hr / custom).
- [x] Exact alarm at (alarm time − window), with a fallback when `SCHEDULE_EXACT_ALARM` is denied.
- [x] Foreground service (`specialUse`) with a low-priority ongoing notification: "Watching for signs you're up" / "Until your 1:30 PM alarm". Placeholder status-bar icon until Phase 6.
- [x] Stops when the alarm time passes or the alarm is dismissed. Reschedules when the next alarm changes, after an app update, and after a reboot (reboot not tested on the device yet).
- [x] Unit tests: 8 tests for `WatchPlanner` and `WatchWindow`, all passing (`.\gradlew.bat testDebugUnitTest`).
- [x] Test screen: window picker, live status, whether the service is running, and Allow buttons for notifications and Alarms & reminders.

#### Device test results (Nothing Phone (2a), 2026-11-01, 5-min window, both permissions allowed)
| Test | Result |
|---|---|
| Window opens with the phone locked | ✅ Check alarm fired at 13:25:00.013. The service started from the background and the notification showed |
| Alarm time passes | ✅ Service stopped at 13:30:00.035, notification removed, next day's window scheduled |
| Dismiss during the window (Phase 1 button) | ✅ At 13:42:14 the dismiss went to Google Clock. The next alarm moved to tomorrow, the service stopped, and the alarm didn't ring |
| Alarm first seen inside its window | ✅ Skipped (happened by chance while the user was picking a time) |
| App update while waiting | ✅ `MY_PACKAGE_REPLACED` reached the non-exported receiver and rescheduled |
| Reboot during a window | Not tested yet |

#### How to verify on a device
1. Install and open Dawnly. Under **Watch window**, allow Notifications and Alarms & reminders, then pick **Custom → 5 min**.
2. Set a Clock alarm about 8 minutes away. The status should say "Starts watching at …".
3. Lock the phone. When the window opens, the "Watching for signs you're up" notification appears.
4. Either let the alarm ring (the notification goes away at the alarm time), or tap **Dismiss next alarm** in Dawnly (it goes away at once).
5. Logs: `adb logcat -s WatchScheduler SystemEventsReceiver ClockApp`. Service: `adb shell dumpsys activity services com.dawnly.app`.

### Phase 3 — Wake detection (session A, branch `phase-3-wake`)
- [ ] Unlock: register `ACTION_USER_PRESENT` dynamically inside the service.
- [ ] Screen use: count minutes between `ACTION_SCREEN_ON` and `ACTION_SCREEN_OFF`.
- [ ] Steps: `Sensor.TYPE_STEP_COUNTER`, which needs the `ACTIVITY_RECOGNITION` permission.
- [ ] Pure-Kotlin scoring class in `domain/wake/` with unit tests (JUnit is already set up). A quick 3 AM time check must not trigger it. Use `WakeSignalSettings` for which signals count and their goals.
- [ ] Publish live readings to `WatchSession`, and call `AwakeNotifier.onUserAwake()` at most once per window.
- [ ] Revisit the FGS type now that steps need `ACTIVITY_RECOGNITION` (Phase 2 chose `specialUse`). Explain any change to the user.
- [ ] Debug-only screen showing live signal values: `ui/debug/DebugScreen.kt`, opened by a debug-only `DebugActivity` with its own launcher icon.
- [ ] Phase 6 CI (also session A): GitHub Actions workflow that runs `testDebugUnitTest assembleDebug lintDebug` on push and PR.
- [ ] Leave a clean hook for the future "Still awake?" nudge. It's not in v1.

### Phase 4 — Notification & dismiss ✅ Done (2026-11-01, session B, branch `phase-4-notify`)

#### Summary
When wake detection calls `AwakeNotifier.onUserAwake()`, Dawnly shows "You're up early" / "Dismiss your 4:40 pm alarm so it doesn't ring?" with **Dismiss alarm** and **Not yet**. Dismiss alarm only goes ahead if the next alarm is still the one the prompt is about, so a stale prompt can never skip tomorrow's alarm. Tested on the Nothing Phone with Google Clock through a debug-only adb trigger, because Phase 3 wasn't merged yet.

**How it works** (all in `notify/`):
- `AwakeNotifier` checks the alarm is still next, then posts the prompt (ID 2; the watching notification is 1). The alarm time goes in the notification extras. The prompt times out by itself at the alarm time. Tapping the body opens Dawnly; only the button dismisses.
- `DismissAlarmActivity` is opened straight from the button with `PendingIntent.getActivity`. It's an activity, not a trampoline, so Android 12+ allows it to start the Clock app. It's translucent with no UI. It closes the prompt, asks `DismissGuard`, then calls `ClockApp.dismissNextAlarm()`. If the Clock app can't dismiss, it opens the Clock app and shows a toast. If the guard refuses, it shows a toast and sends nothing.
- `DismissGuard` is a pure function with 6 unit tests. Ok only if the prompt's alarm is still next and still ahead; otherwise AlarmPassed, AlarmGone, or AlarmChanged.
- `NotYetReceiver` just closes the prompt.
- `WatchScheduler.reconcile()` calls `AwakeNotifier.cancelIfStale()` on every run, so the prompt goes as soon as the next alarm changes. It reads the alarm from the live notification's extras, so this still works after the process dies.

**Decisions:**
- **Silent heads-up channel** ("Up before your alarm", `IMPORTANCE_HIGH`, no sound or vibration). Heads-up so it's seen while the phone is in use; silent because the user is already awake. The channel is created when the first prompt is posted, so `DawnlyApp` wasn't touched.
- **Debug trigger:** `app/src/debug/` holds `notify/DebugAwakeReceiver` and its manifest entry. It's exported but needs `android.permission.DUMP`, which adb's shell has and apps can't get. Release builds don't include it.

- [x] Implement `AwakeNotifier.onUserAwake()`, tested before Phase 3 through the debug adb receiver.
- [x] "Not yet" just closes the prompt.
- [x] Gentle notification with **Dismiss alarm** and **Not yet**. The full question shows when expanded (BigTextStyle).
- [x] Dismiss uses `PendingIntent.getActivity`, with no trampoline.
- [x] Fallback: open the Clock app with a toast. Never dismisses silently.
- [x] Wrong-alarm guard: checked before posting and again before dismissing. The prompt is cancelled when the next alarm changes.

#### Device test results (Nothing Phone (2a), Google Clock, 2026-11-01)
| Test | Result |
|---|---|
| Prompt appearance | ✅ Silent heads-up, 12-hour time, both buttons. Full question readable when expanded |
| Stale prompt (prompt for 4:34 pm, real alarm 4:35 pm) → Dismiss alarm | ✅ Refused with "That alarm was already dismissed or changed…". No request sent; the alarm stayed on |
| Not yet | ✅ Prompt closed, nothing else happened |
| Alarm moved 4:35 → 4:40 pm with the prompt showing | ✅ Prompt removed 12 ms after `NEXT_ALARM_CLOCK_CHANGED` |
| Dismiss alarm (4:40 pm, daily) | ✅ Guard Ok → Google Clock dismissed it. Only today's ring was skipped; the next alarm moved to tomorrow 4:40 pm |
| Fallback (no app can dismiss) | Not tested on a device: Google Clock always handles it. Same `ClockApp` path as Phase 1's fallback |

#### How to verify on a device
1. Install a debug build. Allow notifications. Set a throwaway Clock alarm a couple of hours ahead (not one you rely on).
2. Show the prompt: `adb shell am broadcast -a com.dawnly.app.debug.USER_AWAKE -p com.dawnly.app`. Add `--ez stale true` for a prompt about an alarm one minute earlier, to test the guard.
3. Try **Not yet**, **Dismiss alarm**, and changing the alarm time while the prompt is showing.
4. Logs: `adb logcat -s AwakeNotifier DismissAlarmActivity ClockApp WatchScheduler`.
5. Once Phase 3 has merged, the real trigger is wake detection calling `onUserAwake()` during the window.

**Merge note:** A also adds to `app/src/debug/AndroidManifest.xml`. Whoever merges second keeps both `<application>` entries.

### Phase 5 — UI polish (session C, branch `phase-5-ui`) ✅ Done (2026-11-01)

#### Summary
The glass design from `design/mockups.html` is now the real app: onboarding, Home (Today), and Settings, in light and dark, with the flowing background and spring motion. The Phase 1/2 test screen (`ui/feasibility/`) is gone. Session C also made the Phase 6 app icon.

**How it's built:**
- `ui/theme/`: `DawnlyColors` (the mockup's CSS variables, light and dark), Figtree typography, `DawnlyMotion` springs, and `LocalReduceMotion` (animator scale 0).
- `ui/glass/`: the design system. `FlowingBackground` (three radial gradients on the mockup's keyframes, drawn in the draw phase only, still when motion is reduced), `GlassSurface` (Haze blur 8dp + 170% saturation + tint, a rim that is brighter along the top, an inner glow, no drop shadow), the segmented control, switch, stepper, buttons, `GlassTabBar`, `ProgressRing`, `SpringIn`, and line icons drawn in code (no icon library).
- Screens: `ui/home/` (`HomeUiState.from()` is a pure mapping with unit tests), `ui/settings/`, `ui/onboarding/`, and `ui/DawnlyRoot.kt` (theme, background, onboarding or tabs). ViewModels are created in `MainActivity` with `viewModels()`, because `lifecycle-viewmodel-compose` isn't on the classpath and isn't needed. `reconcile()` stays in `MainActivity.onResume`; Settings calls it after a window change.
- `ui/permissions/`: `AppPermission` (status, the runtime permission, the settings screen), `PermissionsController` (runtime dialogs, settings screens rechecked on resume, and a jump to settings once Android stops showing the dialog), and `OemBattery` (Samsung and Xiaomi screens from dontkillmyapp.com, falling back to app info).

**Decisions:**
- **Haze 2.0.1** (`haze` + `haze-blur`). 2.0 split Haze into modules. `haze-glass` (refraction) was skipped: it needs Android 13+ and is more than the plan's "light blur". Below Android 12 Haze draws a stronger tint instead of blur (`glassFallback`).
- **Figtree** is the single variable font from google/fonts (62 KB, weights picked with `FontVariation`), with its OFL licence in `assets/licenses/`.
- **Sensitivity** is one Low / Medium / High control that sets both goals (screen use 10/5/3 min, steps 100/50/30). Medium is the existing default. The last wake sign on can't be turned off.
- **Steps** turn on only once Physical activity is allowed. Allowing it in onboarding turns steps on.
- **Onboarding** asks only for permissions this Android has that aren't granted yet, in the order notifications, Alarms & reminders (with the "up to an hour late" case), physical activity, background use. The first step carries the mockup's welcome headline. Every step can be skipped; Settings has the same controls, and Home warns when Alarms & reminders or notifications are off.
- **Background use** step: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (manifest, add-only) for the one-tap system dialog. Lint warns this needs a Play policy justification; fine for the hobbyist release, revisit before the Play Store.
- **"You're up early"** on Home shows when every sign that's on has met its goal. It's a display hint only. **After Phase 3 merges, switch it to session A's scorer** so Home and the notification agree.
- Home shows **Dismiss alarm** while watching (solid once all signs are seen). It uses `ClockApp.dismissNextAlarm` with the open-Clock fallback and explains the fallback on screen.
- Onboarding state is in a new `data/onboarding/OnboardingStore.kt` (its own DataStore), since `SettingsRepository` is frozen.
- Window background follows day/night (`values-night/themes.xml`), so dark mode has no white flash on launch.
- The app icon is adaptive (`mipmap-anydpi`, minSdk 26), with a themed-icon monochrome layer. `ic_notification` uses the same ring-and-sun glyph.

**Notification sound (added 2026-11-01, after the user's device test):**
- Settings → **Notification sound** opens a picker: **Phone default** plus 17 Mixkit sounds the user chose, under our own short names (Flute, Sparkle, Bubble, Fizz, Pop, Bloop, Simmer, Potion, Timer, Beep, Click, Level up, Retro, Score, Jump, Jackpot, Pixel). Tapping one selects and previews it on the notification stream. Default: Phone default, until the user picks a house sound.
- Files: `res/raw/sound_*`, with silence trimmed and loudness matched, kept at 44.1 kHz stereo (about 3.6 MB). Licence: Mixkit Sound Effects Free License, credited in `assets/licenses/mixkit_sounds.txt`.
- `data/sound/AlertSound` + `SoundStore` (its own DataStore; fold into settings later).
- **Phase 4's prompt now plays the chosen sound** (user's decision; it was silent). Android fixes a channel's sound once it exists, so `AwakeNotifier.useSound()` keeps one channel per sound (`awake_<sound>`), deletes the old ones, and the app re-syncs it on start. Still no vibration. Checked on the phone: the channel followed the saved sound, and the prompt showed as heads-up.

**Next phase (user's decisions, 2026-11-01), after all three branches merge:**
- Drop "Phone unlocked" as a sign: the phone can be unlocked while the user sleeps. Add movement (motion sensors), music playing (counts only together with movement), and active phone use (Usage access) as the closest safe stand-in for touches; seeing touches outside the app would need an accessibility service, which isn't worth it.
- Optional **auto-dismiss**, off by default, with a "Dismissing your 5:30 alarm in 1 minute — Not yet" warning first. Opening the Clock app from the background needs "Display over other apps". This reverses PLAN.md's "never dismiss silently" for users who opt in, so update PLAN.md.
- Try to hide Google Clock's screen during a dismiss (`EXTRA_SKIP_UI`, which isn't documented for dismiss, then bring Dawnly back); it may only shrink to a flicker.
- The user's other UI changes.

**Left for later:**
- `strings.xml` still holds the old feasibility strings (lint lists them as unused). It's a shared file, so remove them after all three branches merge.
- The debug screen isn't linked from Settings; session A's `DebugActivity` has its own launcher icon.
- Samsung and Xiaomi battery screens are untested (no phones).
- The emulator can't run on this PC (no hypervisor driver), so visual checks happen on the phone.

- [x] Haze and Figtree approved (2026-11-01; both are in PLAN.md). Use Haze's latest stable release, and bundle Figtree in `res/font/` so the app stays offline.
- [x] Glass design system matching `design/mockups.html`: colours, glass alpha values, 28dp cards, pills.
- [x] Flowing background shapes: three radial-gradient circles animated with `rememberInfiniteTransition`. Kept still when reduce-motion is on.
- [x] Screens: onboarding (4 permission steps), Home (progress ring), Settings, floating glass tab bar.
- [x] Home reads `WatchScheduler.state` and `WatchSession.signals`; Settings uses `SettingsRepository`. `reconcile()` after a window change and in `MainActivity.onResume`.
- [x] Previews with fake `WakeSignals` (Home: watching, awake, waiting, no alarm, Clock fallback). `ui/feasibility/` deleted.
- [x] Appearance setting: Light / Dark / System, default System; status bar icons follow it. Non-blur fallback below Android 12.
- [x] OEM battery guidance for Xiaomi and Samsung.
- [x] Unit tests: 14 new (`HomeUiStateTest`, `SensitivityTest`, `OemBatteryTest`), 22 in total, all passing. `lintDebug` passes.
- [x] Checked on the Nothing Phone by screenshot: onboarding, Home (waiting), Settings, light and dark. Fixed there: the tab bar filled the screen, content clashed with the status bar, and selected pills were invisible in light mode.
- [x] User's device test (2026-11-01): onboarding, Home, Settings, light/dark, and the icon all good. The user asked for the changes listed under "Next phase".

#### How to verify on a device
1. Uninstall first for a clean onboarding: `adb uninstall com.dawnly.app`, then `.\gradlew.bat installDebug` and open Dawnly.
2. **Onboarding:** four steps with dots. Allow each one; each should move on by itself, including after you come back from the Alarms & reminders screen. Allowing Physical activity turns on "Steps walked" in Settings. "Not now" skips.
3. **Home:** set a Clock alarm about 8 minutes away and pick **Custom → 5 min** in Settings. Home shows the time, "Watching from …", and the signal goals. When the window opens, the ring starts filling and **Dismiss alarm** appears; tapping it dismisses through the Clock app.
4. **Settings:** switch Light / Dark / System (the status bar icons should follow), change sensitivity (the goals under the switches change), and try to turn off all three signs (the last one refuses, with a hint).
5. **Motion:** the background shapes drift slowly; tabs, segments, and switches spring. With Developer options → Animator duration scale → Off, everything should be still.
6. **Icon:** the launcher icon is a blue tile with a glass ring and a rising sun. Long-press the home screen → themed icons to see the monochrome layer.

### Phase 6 — Portfolio finish (CI: session A. Icon: session C. README: after all three merge)
- [ ] README with screenshots and a demo GIF, plus a "Challenges I solved" section.
- [ ] GitHub Actions CI that builds and runs the unit tests.
- [ ] App icon (session C). The manifest has no icon yet, so the default is used. Also replace the placeholder `ic_notification`.

---

## Open questions for the user
- Can the user get hold of a Samsung and a Xiaomi phone for Phase 1 testing? (Skipped for now; test before sharing the app.)
- Not v1: what should happen if the user falls back asleep after dismissing? (PLAN.md section 4)

## Environment notes (this PC)
- Windows 11. Tools available: PowerShell, Git Bash, winget, git, Node, Python 3.11.
- Nothing Android-related was installed before 2026-10-29. Android Studio and its SDK are the only additions.
- Test phone: Nothing Phone (2a) (A142), Android 16, Google Clock (`com.google.android.deskclock`, confirmed in Phase 1).
- The project lives at `C:\Users\aryan\dev\Dawnly`, outside OneDrive. It moved there on 2026-10-31 because OneDrive locked files in `app/build/`, so incremental builds failed with "Unable to delete directory ... merged.dir\values". Keep it out of OneDrive.
