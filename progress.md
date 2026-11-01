# Dawnly — Progress

> Progress tracker for [PLAN.md](PLAN.md). **PLAN.md is still the full brief and the single source of truth.** Read it first: the requirements, design system, technical approach, and Appendix A are all there. This file records where the work stands and what to do next.
>
> Rules from PLAN.md: work one phase at a time. End each phase with a summary and the steps to verify it on a device. Ask before adding any library not listed in PLAN.md section 5. Update this file whenever a step is done.

**Current phase:** Phase 1 (feasibility test), passed on the Nothing Phone. Samsung and Xiaomi skipped for now (user's decision). Phase 2 is next.
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
1. [x] Tested on the Nothing Phone with the user (2026-11-01). Reading and dismissing both work; see the results table.
2. [x] Samsung and Xiaomi: the user has neither phone. **The user decided (2026-11-01) to skip them for now and start Phase 2.** Test them before sharing the app (PLAN.md section 9).
3. [x] Repeating-alarm test: dismiss skips one ring and leaves the alarm on. Recorded in the table.
4. [ ] Ask before committing and pushing Phase 1. Nothing from Phase 1 is committed yet.
5. [ ] Start Phase 2.

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
| 2 | Watch window: settings, exact-alarm scheduling, foreground service | ⬜ Not started |
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
         ├─ MainActivity.kt
         ├─ data/alarm/NextAlarm.kt, NextAlarmSource.kt, ClockApp.kt
         └─ ui/feasibility/FeasibilityScreen.kt, ui/theme/Theme.kt
```
`data/alarm/` is meant to be reused in later phases: `NextAlarmSource` reads the next alarm and follows it as a Flow, and `ClockApp` handles the dismiss intent, the handler lookup, and the open-Clock fallback. `ClockApp.dismissNextAlarmIntent()` is what Phase 4's notification `PendingIntent` should wrap. `ui/feasibility/` is a throwaway test screen that Home replaces in Phase 5. The `domain/` and `service/` packages will be created in the phases that need them.

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

### Phase 2 — Watch window
- [ ] DataStore settings: window length (30 min / 1 hr / 2 hr / custom).
- [ ] Schedule an exact alarm at (alarm time − window). Handle `SCHEDULE_EXACT_ALARM` and fall back gracefully if it's denied.
- [ ] Foreground service with a low-priority ongoing notification. Choose the Android 14+ `foregroundServiceType` and **explain the choice to the user**.
- [ ] Stop the service when the alarm time passes or the user dismisses. Reschedule when the next alarm changes and after a reboot.

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
- Can the user get hold of a Samsung and a Xiaomi phone for Phase 1 testing?
- Not v1: what should happen if the user falls back asleep after dismissing? (PLAN.md section 4)

## Environment notes (this PC)
- Windows 11. Tools available: PowerShell, Git Bash, winget, git, Node, Python 3.11.
- Nothing Android-related was installed before 2026-10-29. Android Studio and its SDK are the only additions.
- Test phone: Nothing Phone (2a) (A142), Android 16, Google Clock (`com.google.android.deskclock`, confirmed in Phase 1).
- The project lives at `C:\Users\aryan\dev\Dawnly`, outside OneDrive. It moved there on 2026-10-31 because OneDrive locked files in `app/build/`, so incremental builds failed with "Unable to delete directory ... merged.dir\values". Keep it out of OneDrive.
