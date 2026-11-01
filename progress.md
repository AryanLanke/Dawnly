# Dawnly — Progress

> Progress tracker for [PLAN.md](PLAN.md). **PLAN.md is still the full brief and the single source of truth.** Read it first: the requirements, design system, technical approach, and Appendix A are all there. This file records where the work stands and what to do next.
>
> Rules from PLAN.md: work one phase at a time. End each phase with a summary and the steps to verify it on a device. Ask before adding any library not listed in PLAN.md section 5. Update this file whenever a step is done.

**Current phase:** Phase 2 (watch window) done and tested on the Nothing Phone. Phase 3 (wake detection) is next.
**Last updated:** 2026-11-01

> **The project moved on 2026-10-31** from `C:\Users\aryan\OneDrive\Desktop\Dawnly` to **`C:\Users\aryan\dev\Dawnly`**. OneDrive locked files in `app/build/` mid-build, so every incremental build failed with "Unable to delete directory". Open VS Code in the new folder. Once the new copy is confirmed working, delete the OneDrive copy (it has the same uncommitted changes, so nothing is lost).

---

## ▶ Next session: start here

**Before starting (user):**
- [ ] Open VS Code in `C:\Users\aryan\dev\Dawnly` (not the old OneDrive folder).
- [ ] Plug in the Nothing Phone over USB and run `adb devices`. It should show `00069348R000717  device`.

**Done so far (2026-10-31):**
- [x] Working tree was clean and in sync with `origin/main`.
- [x] Package name confirmed by the user: **`com.dawnly.app`** (final).
- [x] Phase 1 test screen built (see the Phase 1 checklist below) and installed. It launches; with no alarm set it shows "No alarm set" and "Dismiss goes to Clock (com.google.android.deskclock)", so the Nothing Phone uses Google Clock.

**The plan, in order:**
1. [x] Phase 1 done, committed as `a42e43e` and pushed (2026-11-01). Samsung and Xiaomi skipped for now (user's decision); test them before sharing the app (PLAN.md section 9).
2. [x] Phase 2 built and tested on the Nothing Phone with the user (2026-11-01). See the Phase 2 section.
3. [ ] Ask before committing and pushing Phase 2. Nothing from Phase 2 is committed yet.
4. [ ] Optional: reboot test (reboot during a window; watching should resume).
5. [ ] Start Phase 3.

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
| 4 | Notification and one-tap dismiss, plus the fallback | ⬜ Not started |
| 5 | UI polish: glass design system, all screens, motion, dark mode, onboarding | ⬜ Not started |
| 6 | Portfolio finish: README, GIF, challenges section, CI, app icon | ⬜ Not started |

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

### Phase 3 — Wake detection
- [ ] Unlock: register `ACTION_USER_PRESENT` dynamically inside the service.
- [ ] Screen use: count minutes between `ACTION_SCREEN_ON` and `ACTION_SCREEN_OFF`.
- [ ] Steps: `Sensor.TYPE_STEP_COUNTER`, which needs the `ACTIVITY_RECOGNITION` permission.
- [ ] Pure-Kotlin scoring class in `domain/` with unit tests. A quick 3 AM time check must not trigger it. Add JUnit to the catalog; it's already covered by PLAN.md's "Tests" item.
- [ ] Hidden or debug-only screen showing live signal values.
- [ ] Leave a clean hook for the future "Still awake?" nudge. It's not in v1.

### Phase 4 — Notification & dismiss
- [ ] Gentle notification: "You're up early" / "Dismiss your 5:30 alarm so it doesn't ring?", with the actions **Dismiss alarm** and **Not yet**.
- [ ] Dismiss action uses `PendingIntent.getActivity` straight to `ACTION_DISMISS_ALARM`, with no trampoline because Android 12+ blocks those.
- [ ] Fallback when the Clock app can't dismiss. Never dismiss silently.
- [ ] **Guard against dismissing the wrong alarm** (found in Phase 1 testing). `ALARM_SEARCH_MODE_NEXT` dismisses whatever alarm is next. If today's alarm was already dismissed or has already rung, a stale notification would skip *tomorrow's* alarm. Before sending the dismiss, check that `getNextAlarmClock()` still returns the alarm the notification is about. Cancel the notification when the next alarm changes. This probably needs a small activity that checks first and then forwards the request; launch it straight from the notification, which is allowed because it isn't a trampoline.

### Phase 5 — UI polish
- [ ] Ask the user before adding Haze (it's listed in PLAN.md) and the Figtree font (Google Fonts; bundle it so the app stays offline).
- [ ] Glass design system matching `design/mockups.html`: colours, glass alpha values, 28dp cards, pills.
- [ ] Flowing background shapes: three radial-gradient circles animated with `rememberInfiniteTransition`. Keep them still when reduce-motion is on.
- [ ] Screens: onboarding (4 permission steps), Home (progress ring), Settings, floating glass tab bar.
- [ ] Appearance setting: Light / Dark / System, default System. Non-blur fallback below Android 12.
- [ ] OEM battery guidance for Xiaomi and Samsung (dontkillmyapp.com).

### Phase 6 — Portfolio finish
- [ ] README with screenshots and a demo GIF, plus a "Challenges I solved" section.
- [ ] GitHub Actions CI that builds and runs the unit tests.
- [ ] App icon. The manifest has no icon yet, so the default is used.

---

## Open questions for the user
- Can the user get hold of a Samsung and a Xiaomi phone for Phase 1 testing? (Skipped for now; test before sharing the app.)
- Not v1: what should happen if the user falls back asleep after dismissing? (PLAN.md section 4)

## Environment notes (this PC)
- Windows 11. Tools available: PowerShell, Git Bash, winget, git, Node, Python 3.11.
- Nothing Android-related was installed before 2026-10-29. Android Studio and its SDK are the only additions.
- Test phone: Nothing Phone (2a) (A142), Android 16, Google Clock (`com.google.android.deskclock`, confirmed in Phase 1).
- The project lives at `C:\Users\aryan\dev\Dawnly`, outside OneDrive. It moved there on 2026-10-31 because OneDrive locked files in `app/build/`, so incremental builds failed with "Unable to delete directory ... merged.dir\values". Keep it out of OneDrive.
