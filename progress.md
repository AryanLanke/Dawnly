# Dawnly — Progress

> Progress tracker for [PLAN.md](PLAN.md). **PLAN.md is still the full brief and the single source of truth.** Read it first: the requirements, design system, technical approach, and Appendix A are all there. This file records where the work stands and what to do next.
>
> Rules from PLAN.md: work one phase at a time. End each phase with a summary and the steps to verify it on a device. Ask before adding any library not listed in PLAN.md section 5. Update this file whenever a step is done.

**Current phase:** Phase 7, release prep (in progress). Phases 0–6 are done (2026-11-01). Done on 2026-11-02: the signed release build, the licence, the overlay hint, two Today-screen changes (Dismiss alarm asks first; a "Signs you're awake" bar), the new app icon, and a repo cleanup. The user wants the app made good first and **testing later as its own phase**: device tests (release walkthrough, reboot, Samsung/Xiaomi) wait until then. Also open: the CI check and hobbyist sharing. See the Phase 7 checklist. **Auto-dismiss on a locked phone works (tested 2026-11-03); the reversed "dismissed" sound and going back to the previous app are built, not yet installed (below).**
**Last updated:** 2026-11-03

---

## ▶ Auto-dismiss with the phone locked (the user's top priority)

**Result, 2026-11-03: it works.** Step 1 Run A on the Nothing Phone, release 0.1.0: screen off (power button 00:49:24, display OFF at 00:51:25), phone in a pocket, walking with Spotify playing. Watching began 00:50:00; awake at 00:50:28 (Sensitivity High, no screen use); at 00:51:28 `DismissAlarmActivity` started from the background (`BAL_ALLOW_SAW_PERMISSION`) and Google Clock dismissed the 1:05 alarm **without asking to unlock**. Google Clock then opened its alarm list behind the lock screen and Dawnly came forward over it, so after unlocking an app was open instead of Spotify.
- Gotcha for testers: an alarm is only watched if it was set **before** its window opened (`WatchPlanner`, "leaves it alone" otherwise). Set test alarms more than one window ahead.
- Run B (lock screen showing) is moved to the testing phase.

**Done, 2026-11-03 (Step 2a, with the user's new rules; see memory "dismiss-leaves-phone-as-is"):**
- Every dismiss (auto, the prompt's button, Today's button) posts **"Alarm dismissed"** (`DismissedNotice`) with the chosen sound **played backwards** (`res/raw/sound_*_reversed.ogg`, made with ffmpeg `areverse` + leading-silence trim, 340 KB for all 17; Phone default borrows the reversed flute). Its own per-sound channels `dismissed_<sound>`, swapped with the prompt's in `AwakeNotifier.useSound`. Unlocked it clears itself after a minute; locked it waits on the lock screen. Replaces the old toast.
- **No app left open:** `QuietDismiss` now goes back to the app that was in front before the dismiss (Usage access events; `ReturnPlan`, unit tested), the home screen if unknown, or Dawnly when dismissed from Dawnly's Today screen. Manifest `<queries>` gains MAIN/LAUNCHER so the app can be relaunched.
- Settings text and README say it works with the phone locked.
- **Second locked test passed** (01:14–01:15, walking only, no music): awake 26 s after watching began, dismissed while locked, reversed sound played, back to the app that was open (`Back to App(...)` in the log), no Clock screen.
- **Sounds with a fading tail** (the user's ask: like an iPhone sound, not stopping dead): every sound, forward and reversed, now ends with a soft echo that fades over about a second. The masters moved to `design/sounds/source/`; `design/sounds/make_sounds.sh` (ffmpeg) makes the 34 OGG files in `res/raw` (1.4 MB, was 3.7 MB). Same resource names, so saved choices and channels keep working. The user listened to the reversed ones on the phone (`Download/Dawnly reversed sounds v2`) and asked for the same on the forward ones.
- **To test later (testing phase):** locked auto-dismiss lands back in Spotify (or home) after unlock; the reversed sound plays in a pocket; Today's button and the prompt's button play it too; Run B with the lock screen showing; without Usage access it goes home.

### The plan as written before the test

**What the user wants (2026-11-02):** with **Dismiss automatically** on, the alarm should go away by itself when they're already up and busy, e.g. working out, walking, or listening to music, with the phone **locked** in a pocket or on the floor. They shouldn't have to unlock the phone for it. Do this before any other work.

**What we know already**
- Auto-dismiss has only been tested with the phone **unlocked** (home screen, 2026-11-01, see "Device test: auto-dismiss" in Phase 6). Nothing in the code handles the lock screen yet.
- Detection works with the phone locked: steps, movement and music are read by the running service whatever the lock state. Screen use only counts while unlocked, so a locked workout relies on steps + movement (+ music).
- The 1-minute countdown is an exact alarm (`setExactAndAllowWhileIdle`, in `AwakeNotifier`), so it fires even in Doze, screen off.
- When the minute is up, `AutoDismissReceiver` starts `DismissAlarmActivity` (allowed from the background by "Display over other apps"). That activity sends `ACTION_DISMISS_ALARM` to the Clock app, and `QuietDismiss` then brings Dawnly back to the front with a toast.
- **The open question is the Clock app.** Android's open-source Clock (AOSP DeskClock, `HandleApiCalls.kt`, read 2026-11-02) has **no lock-screen check**: it dismisses the next alarm in the background (pre-dismiss for an upcoming alarm) and opens its alarm list. Google Clock is closed source and may differ (e.g. ask to unlock first); Samsung and Xiaomi clocks are unknown. Only a test on the phone can tell.

**Step 1: test on the Nothing Phone (about 15 minutes, needs the user's OK and the phone)**
Exception to "testing later": this is a yes/no feasibility question like Phase 1, and the plan depends on it.
1. Set a Google Clock alarm about 10 minutes ahead and a short watch window (Settings → custom, e.g. 10 min). Turn on **Dismiss automatically** (and "Display over other apps").
2. Lock the phone and let the screen go off. Get "awake" without unlocking: walk around with music playing (the real use case; Sensitivity High makes it quicker). Alternative: the adb trigger `adb shell am broadcast -a com.dawnly.app.debug.USER_AWAKE -p com.dawnly.app` only exists in the debug build, which can't install over the release build without uninstalling (settings start fresh).
3. Leave the phone locked for the whole minute. Then check, without unlocking first, whether the alarm still shows on the lock screen, then unlock and check Google Clock.
4. Logs: `adb logcat -s AwakeNotifier AutoDismiss DismissAlarmActivity QuietDismiss ClockApp` plus `adb shell dumpsys alarm | grep -A2 "next alarm clock"` (or Dawnly's Home) to see whether the next alarm moved.
5. Repeat once with the lock screen showing (screen on, not unlocked), since Android treats that differently from screen off.

**Step 2a: if Google Clock dismisses while locked (most likely, going by AOSP)**
- In `QuietDismiss`: when the phone is locked (`KeyguardManager.isKeyguardLocked`), don't bring Dawnly to the front or show a toast (nobody sees them, and Dawnly would be sitting there after unlock). Post a quiet notification instead: "Your 5:30 alarm was dismissed. You were up and moving." so it's on the lock screen when they look.
- Keep `DismissAlarmActivity` doing its work in `onCreate` (it runs behind the lock screen); check the log shows it running while locked.
- Unit test the locked/unlocked choice (pure function, like `AutoDismissPlan`).
- Settings' auto-dismiss text and README: say it works with the phone locked.

**Step 2b: if Google Clock asks to unlock first**
Choose with the user (don't decide alone):
- **Dismiss at the next unlock:** if the countdown ends while locked, wait; the moment they unlock (`USER_PRESENT`), dismiss, if it's still before the alarm. If they don't unlock in time, the alarm rings as normal. Safe, but it doesn't cover a workout where they never unlock.
- **Preferred fallback: press Google Clock's own "Dismiss" button.** Google Clock posts an "Upcoming alarm" notification with a **Dismiss** action before each alarm (Samsung Clock has one too). With "Notification access" (a `NotificationListenerService`, a special permission the user turns on), Dawnly can find that notification and send its Dismiss action's `PendingIntent` itself. That is the Clock app's own dismiss, so it should work with the phone locked and without the Clock screen appearing. To check: when Google Clock posts the notification (it must be up when the countdown ends; watch windows are at most a few hours), and that sending the action works while locked. Needs a Play policy reason later, like the other special permissions.
- **Not recommended:** muting the alarm sound instead of dismissing it. It could make them miss a real alarm and it's not a real dismiss.
- Either way, the countdown notification should say so ("Unlock to dismiss your 5:30 alarm").

**Step 3: detection during a workout with the phone locked**
Check from the logs of the Step 1 walk how fast the score reaches 100% without screen use (steps + movement + music). If it's too slow at Medium, consider counting music more strongly when it plays together with steps. Discuss before changing the scoring.

**Later:** Samsung and Xiaomi clocks in the testing phase (Phase 1 table), locked and unlocked.

## ▶ Where things stand (2026-11-02)

**Open items:**
- **Testing phase (later):** the release device test, the reboot test (Phase 2), Samsung and Xiaomi (Phase 1), and checking the new icon on the phone. Details in the Phase 7 checklist.
- **Delete the two test alarms** in Google Clock, "Dawnly demo" (11:19 pm) and "Dawnly test" (11:40 pm). Both are off but still in the list.
- **Release key** (2026-11-02): `C:\Users\aryan\dawnly-keys\dawnly-release.jks`, password in the git-ignored `keystore.properties`. On this PC only, not backed up (the user's choice). If it's lost, Dawnly can't be updated on testers' phones or registered with Google.
- **Old OneDrive copy:** `C:\Users\aryan\OneDrive\Desktop\Dawnly` is a stale copy from before the move to `C:\Users\aryan\dev\Dawnly` (2026-10-31; OneDrive locked build files). Everything in it is in this repo, so the user can delete it.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and `SYSTEM_ALERT_WINDOW` need a Play policy justification before any Play Store release (fine for limited distribution).

**Git:** only `main`, on https://github.com/AryanLanke/Dawnly. Everything up to the locked auto-dismiss work and the new sounds is pushed (2026-11-03). Push only when the user says so. The user is the only author and contributor of every commit (the history was rewritten on 2026-11-02 to make it so).

**Useful commands** (new terminals have `JAVA_HOME`, `ANDROID_HOME`, and adb on `PATH`; restart VS Code if they're missing). On a busy PC add the low-memory flags `--max-workers=2 "-Pkotlin.compiler.execution.strategy=in-process"`.
```
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug   # the full check (CI runs the same)
.\gradlew.bat assembleRelease                              # signed release APK
adb install -r app\build\outputs\apk\release\app-release.apk
```

### History: the parallel phase
Phases 3, 4 and 5 ran in parallel on 2026-11-01 in three sessions (A: wake detection, B: notification and dismiss, C: UI), each in its own git worktree and branch, building on shared "contract" files on `main` with a file-ownership split. All three merged into `main` fast-forward; the worktrees and branches are gone.

---

## Phase overview

| Phase | Goal | Status |
|---|---|---|
| 0 | Setup: SDK, Compose project, mockups, Hello screen running on the phone | ✅ Done (2026-10-31) |
| 1 | Feasibility: read the next alarm and dismiss it, tested on 3 phones | ✅ Passed on Nothing Phone (2026-11-01). Samsung and Xiaomi not tested yet |
| 2 | Watch window: settings, exact-alarm scheduling, foreground service | ✅ Done (2026-11-01). Reboot not tested yet |
| 3 | Wake detection: signals, scoring + unit tests, debug screen | ✅ Done (2026-11-01), with the user's changes: no unlock sign; apps opened, movement, music added; additive score. Real detection worked on the phone |
| 4 | Notification and one-tap dismiss, plus the fallback | ✅ Done (2026-11-01). Follow-up: "Up already?" copy, opt-in auto-dismiss (device-tested), Clock screen cut to a flicker |
| 5 | UI polish: glass design system, all screens, motion, dark mode, onboarding | ✅ Done (2026-11-01), merged second. Includes the notification sound picker; the prompt now plays the chosen sound |
| 6 | Portfolio finish: README, GIF, challenges section, CI, app icon | ✅ Done (2026-11-01). README with screenshots, GIF and challenges; CI; icon; default sound Flute |
| 7 | Release prep: remaining checks, Samsung/Xiaomi, signed release, hobbyist sharing | 🔶 In progress (2026-11-02). Signed 0.1.0 release built and R8-checked, licence, overlay hint, Today confirm + "Signs you're awake" bar, new app icon, repo cleanup done. Open: CI check, hobbyist sharing; device tests wait for a later testing phase |

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
- [x] First commit `4481a1b` pushed to https://github.com/AryanLanke/Dawnly (`origin`; `main` tracks `origin/main`). `gh` isn't installed; plain `git push` works through the user's Git Credential Manager login.

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

### File layout
```
Dawnly/
├─ PLAN.md (the brief), progress.md (this log), README.md, LICENSE
├─ .github/workflows/ci.yml          tests, debug build and lint on every push
├─ design/                           see design/README.md
│  ├─ mockups.html                   approved UI mockups (PLAN.md Appendix A)
│  └─ app-icon/                      icon masters, preview.html, generator/ (Python, renders with Chrome)
├─ docs/                             README screenshots and demo.gif
├─ gradle/libs.versions.toml         version catalog
└─ app/src/
   ├─ main/java/com/dawnly/app/
   │  ├─ DawnlyApp.kt, MainActivity.kt   app-wide singletons (manual wiring, no DI library)
   │  ├─ data/        alarm/ (next alarm, Clock app), settings/, sound/, onboarding/, wake/, watch/
   │  ├─ domain/      wake/ (signals, scoring, tracker), watch/ (window planning)
   │  ├─ service/     WatchScheduler, WatchService, WakeWatcher, receivers
   │  ├─ notify/      "Up already?" prompt, dismiss, auto-dismiss
   │  └─ ui/          glass/ (design system), theme/, home/, settings/, onboarding/, permissions/, format/
   ├─ main/res/       strings_*.xml per area, mipmap-*/ launcher icon layers, raw/ alert sounds
   ├─ debug/          debug-only: wake detection screen (own launcher icon), adb test receiver
   └─ test/           unit tests, mirroring main's packages
```
Screens are split into the screen and its parts: `ui/home/HomeScreen.kt` + `HomeCards.kt`, `ui/settings/SettingsScreen.kt` + `SettingsSections.kt`. `WatchScheduler.reconcile()` is the single entry point for the watch window; see Phase 2.

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

### Phase 3 — Wake detection ✅ Done (session A's branch `phase-3-wake`, finished in the Phase 6 session, 2026-11-01)

#### Summary
While the watch window is open, `WatchService` runs a `WakeWatcher`. It feeds phone events into the pure `WakeTracker`, publishes the readings to `WatchSession` (Home and the debug screen), and calls `AwakeNotifier.onUserAwake()` once `WakeScore` says the user is up. That happens at most once per window, even across a process restart, because progress is saved in `WakeProgressStore`.

**The signs** (the user's changes from the Phase 5 test applied):
| Sign | Source | Goal (Low / Medium / High) | Default |
|---|---|---|---|
| Screen use | `USER_PRESENT` / `SCREEN_ON` without a lock screen, until `SCREEN_OFF` | 10 / 5 / 3 min | On |
| Apps opened | `UsageStatsManager` resume events; launchers, system UI and Dawnly don't count | 5 / 3 / 2 apps | Off until Usage access is allowed |
| Steps | `TYPE_STEP_COUNTER` (wake-up variant if the phone has one) | 100 / 50 / 30 | Off until Physical activity is allowed |
| Movement | Accelerometer, ~5 samples a second, batched 10 s. A second counts when the acceleration strays more than 0.6 m/s² from gravity | 3 / 2 / 1 min | On |
| Music playing | `AudioManager.isMusicActive` every 5 s | 1 min, capped by movement | On |

**Scoring (`WakeScore`):** each counted sign's progress (0 to 1) adds up; awake at 1. Music adds at most as much as movement, so it never counts alone. "Phone unlocked" is gone (it still starts the screen-use clock). Home's "You're up early" now uses the same scorer, so Home and the prompt agree.

**Decisions:**
- **Scoring changed from "every sign must reach its goal" to "progress adds up to 1".** With five signs, requiring all of them would mean nobody counts as awake without music playing. With adding up, any one sign at its goal is enough, and a mix works too.
- **FGS type stays `specialUse`.** The step counter and accelerometer work under any foreground service type, and `health` would need Physical activity granted before every start.
- **Apps opened** counts switches to a different app, so screens inside one app don't count, but going back to an app from the home screen does. Usage access is a new onboarding step (`AppPermission.UsageAccess`); allowing it turns the sign on.
- The old `use_unlock` setting is removed on the next settings write.
- 62 unit tests in total (`WakeScoreTest` 10, `WakeTrackerTest` 15, plus the updated Home and Sensitivity tests).

- [x] Screen use, apps opened, steps, movement, music.
- [x] Pure-Kotlin scoring and tracking in `domain/wake/` with unit tests. A quick 3 AM time check doesn't trigger it.
- [x] Publish live readings to `WatchSession`, and call `AwakeNotifier.onUserAwake()` at most once per window.
- [x] FGS type revisited: stays `specialUse` (see above).
- [x] Debug-only screen with live values (`ui/debug/DebugScreen.kt`), opened by the "Dawnly debug" launcher icon. It can switch signs and change goals for quick tests.
- [x] Phase 6 CI: `.github/workflows/ci.yml` runs `testDebugUnitTest assembleDebug lintDebug` on push and PR.
- [x] Clean hook for the future "Still awake?" nudge (`WakeWatcher.onAwake`).
- [x] Device test (Nothing Phone (2a), 2026-11-01, 5-min window, Medium): someone used the phone during the window, and at 23:06:10 Dawnly counted them awake from 3 min 10 s of screen use (63%) plus 45 s of movement (38%). The prompt went out once.

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

#### Phase 4 follow-up (session B's list, done in the Phase 6 session, 2026-11-01)
The user asked the Phase 6 session to do B's and A's lists itself.
- **Prompt copy:** "Up already?" / "Looks like you're awake. Dismiss your 5:30 alarm?" (the user's preferred wording). The buttons stay **Dismiss alarm** and **Not yet**.
- **Dismiss automatically** (Settings, off by default). When the user seems awake, the prompt says "Dismissing your 5:30 alarm in 1 minute" with a live countdown, **Dismiss alarm**, and **Not yet**. Swiping it away counts as Not yet. When the minute is up, `AutoDismissReceiver` (our own exact alarm) runs `DismissGuard` again and opens `DismissAlarmActivity`. Dawnly asks the normal way instead when: the alarm is under 70 s away (`AutoDismissPlan`, 4 unit tests), Display over other apps is off, exact alarms are off (the minute could stretch to an hour), or notifications are off (no warning could show).
- **Display over other apps** (`SYSTEM_ALERT_WINDOW`): Android 10+ only lets an app start the Clock app from the background with it. It's a new `AppPermission.Overlay`, not in onboarding (`askedUpFront = false`). Turning the switch on without it shows a one-line reason and an Allow pill; the switch turns on once it's granted. Settings → Permissions lists it only while auto-dismiss is on.
- **Hiding Google Clock:** the dismiss request now carries `EXTRA_SKIP_UI`. `QuietDismiss` waits up to 5 s for the next alarm to move on, then brings Dawnly back to the front with a "Your 5:30 alarm is dismissed" toast. If the alarm doesn't move, the Clock app stays open so the user sees why. Used by the prompt, auto-dismiss, and Home's Dismiss alarm.
- `SettingsRepository` gained `autoDismiss` (it was frozen only during the parallel phases). PLAN.md sections 3, 4 and 7 updated.
- **Device test (Nothing Phone (2a), Google Clock, 2026-11-01):** the new prompt copy shows. Dismiss alarm from the prompt: the guard said Ok, Google Clock dismissed the alarm, and Dawnly came back to the front 80 ms after the Clock screen appeared (23:06:47.12 → 47.20). Android allowed that through its grace period (`BAL_ALLOW_GRACE_PERIOD`), so it works without "Display over other apps". The "Your 11:19 pm alarm is dismissed" toast showed. `EXTRA_SKIP_UI` is ignored by Google Clock, as expected. Auto-dismiss was tested the same night; see "Device test: auto-dismiss" in Phase 6.

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
- Settings → **Notification sound** opens a picker: **Phone default** plus 17 Mixkit sounds the user chose, under our own short names (Flute, Sparkle, Bubble, Fizz, Pop, Bloop, Simmer, Potion, Timer, Beep, Click, Level up, Retro, Score, Jump, Jackpot, Pixel). Tapping one selects and previews it on the notification stream. Default: **Flute** (the user's pick in the Phase 6 session; it was Phone default until then).
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

### Phase 6 — Portfolio finish ✅ Done (2026-11-01)

#### Summary
Dawnly has a portfolio README with light and dark screenshots, a demo GIF, a "Challenges I solved" section, and build steps. CI runs on GitHub Actions. The app icon came from session C. Session B's and A's follow-up lists were done first, in this same session.

- [x] Checked that A and B had merged: they hadn't. The user then asked this session to do both lists itself (see the Phase 3 and Phase 4 sections).
- [x] **README.md**: the problem, how it works (with the sign table), architecture, tech stack, screenshots (`docs/screenshots/`, Home watching, Settings and the prompt, each in light and dark), the demo GIF (`docs/demo.gif`, 9 s, 3 MB), "Challenges I solved" (background limits, exact alarms and the 1-hour delay, Samsung and Xiaomi battery killers, FGS types, the Skipped rule, one channel per sound, the wrong-alarm guard, auto-dismiss from the background), and how to build and run.
- [x] Screenshots with `adb exec-out screencap`, the GIF with `adb shell screenrecord`, converted with ffmpeg (installed with the user's OK: `winget install Gyan.FFmpeg`, at `%LOCALAPPDATA%\Microsoft\WinGet\Packages\Gyan.FFmpeg_…\ffmpeg-9.0.2-full_build\bin`; open a new terminal to have it on `PATH`). A throwaway "Dawnly demo" alarm was set with `SET_ALARM` and dismissed in the GIF.
- [x] **Default notification sound: Flute** (user's choice; Retro was picked first, then changed to Flute). `AlertSound.DEFAULT`; nothing saved, or a removed sound, gives Flute.
- [x] Removed the Phase 1/2 feasibility strings from `strings.xml`. Lint now only reports `BatteryLife` (known, see Phase 5) and a newer AGP.
- [x] Fixed on the way: settings saved before the new signs existed now get the apps and movement goals of the user's sensitivity level, so Settings still shows that level after an update. `Sensitivity` moved to `domain/wake`.
- [x] Worktrees `Dawnly-ui`, `Dawnly-notify`, `Dawnly-wake` removed (all clean and merged first).
- [x] `.gitignore` now ignores JVM crash logs (`hs_err_pid*.log`, `replay_pid*.log`). The PC ran low on memory during builds while another big Python job was running; building with `--max-workers=2 -Pkotlin.compiler.execution.strategy=in-process` worked.
- [x] Pushed to GitHub (`11b09c4`). The CI result couldn't be read without the user's login (see Phase 7).
- [ ] Reboot test (Phase 2): skipped by the user.

#### Device test: auto-dismiss (Nothing Phone (2a), Google Clock, 2026-11-01, 23:30–23:39)
| Test | Result |
|---|---|
| Turn on **Dismiss automatically** | ✅ The one-line reason and **Allow** appeared; the switch stayed off. Allow opened Android's "Display over other apps" screen. On Android 11+ that's the list of all apps, not Dawnly's own page, so the user has to tap **Dawnly** first. Back in Dawnly, the switch turned on by itself, and Settings → Permissions listed "Display over other apps" |
| Countdown | ✅ "Up already?" / "Dismissing your 11:40 pm alarm in 1 minute." with a live 00:57 countdown, **Dismiss alarm** and **Not yet** |
| Swipe the countdown away (from the shade) | ✅ "Auto-dismiss stopped" at 23:36:00, ten seconds before it was due. Nothing fired; the alarm stayed on |
| **Not yet** | ✅ Stopped at 23:36:40. Nothing fired; the alarm stayed on |
| Let the minute run out, with Dawnly in the background (home screen) | ✅ At 23:38:51.4, exactly 60 s after the warning: `DismissGuard` Ok → Google Clock dismissed the 11:40 pm alarm → Dawnly back in front 0.5 s later (`BAL_ALLOW_SAW_PERMISSION`). Next alarm moved to tomorrow 4:40 pm |

After the test, auto-dismiss was turned off again and "Display over other apps" reset, so the phone is as it was.

### Phase 7 — Release prep (in progress, started 2026-11-02)
Everything Phases 0–6 left open, plus PLAN.md section 9: share the app through Google's free limited distribution account first, maybe the Play Store later.

**Checks to finish**
- [ ] **CI on GitHub** (the user checks; the repo is private and `gh` isn't installed): github.com/AryanLanke/Dawnly → **Actions** tab → the top run should have a green tick. If it's red, open it, click the failed step, and copy the error into a session.
- [x] **Auto-dismiss device test:** passed (2026-11-01), see "Device test: auto-dismiss" in Phase 6.
- [x] **Overlay hint** (2026-11-02): on Android 11+ the reason under **Dismiss automatically** now ends "Allow opens a list of apps: tap Dawnly there and turn it on." (`settings_auto_dismiss_find`). Android 8–10 open Dawnly's own page, so they don't show it. Not yet seen on the phone. Usage access (onboarding) also opens the app list; it has no hint yet.
- [ ] **Delete the test alarms:** Clock app → **Alarm** tab → touch and hold the 11:19 pm "Dawnly demo" alarm → **Delete**. Do the same for the 11:40 pm "Dawnly test" alarm.
- [ ] **Reboot test** (Phase 2), if wanted: restart the phone during a watch window and check watching carries on.

**Before sharing the app**
- [ ] **Samsung and Xiaomi:** run the Phase 1 test steps (read and dismiss the next alarm) and the onboarding battery screens on each. Record the results in the Phase 1 table.
- [x] **Signing key** (2026-11-02): `C:\Users\aryan\dawnly-keys\dawnly-release.jks` (PKCS12, RSA 4096, alias `dawnly`, CN=Aryan Lanke, valid to 2054). SHA-256 `DC:6D:70:4D:EA:FD:FF:B7:6B:32:2B:87:D0:1F:88:C1:0D:32:1E:AA:21:6D:CE:9F:28:49:89:72:F3:4E:0E:BE`. Its path and the generated password are in `keystore.properties` at the repo root (git-ignored, as are `*.jks` and `*.keystore`). `app/build.gradle.kts` signs release builds with it when that file exists and leaves them unsigned otherwise (CI only builds debug). The user is to copy the password into a password manager. Kept on this PC only (the user's choice); a second copy somewhere safe is still advisable.
- [x] **Version:** 0.1.0, versionCode 1 (the user's choice; already set). Bump versionCode for every build sent to testers.
- [x] **Release build** (2026-11-02): `assembleRelease` → `app/build/outputs/apk/release/app-release.apk`, 5.1 MB (debug is 17 MB), APK signature v2 with the key above, not debuggable. R8 checks, from the APK and `mapping.txt`: every manifest component keeps its class name (`DawnlyApp`, `MainActivity`, `WatchService`, `WatchAlarmReceiver`, `SystemEventsReceiver`, `DismissAlarmActivity`, `NotYetReceiver`, `AutoDismissReceiver`); the debug-only receivers aren't in it; all 17 `raw/sound_*` and `font/figtree` survive resource shrinking with their names; log tags are string constants, so `adb logcat -s …` works the same. DataStore and Haze bring their own R8 rules, nothing in the app uses reflection, and enums are saved by `name`, which R8 keeps. `proguard-rules.pro` needed nothing.
- [x] **Fixed on the way:** bundled sounds were handed to notification channels as `android.resource://…/<resource number>`. A channel keeps its sound for good, and resource numbers can shift between versions, so an update could have played the wrong sound. `AlertSound.uri()` now uses `…/raw/sound_flute` (names survive R8, checked above).
- [ ] **Release device test** (needs the user's OK). The release key differs from the debug key, so Android won't install over the debug build: uninstall Dawnly first (settings and onboarding start fresh, and the "Dawnly debug" icon goes). Then: onboarding (all steps); Settings (change every setting, close Dawnly from Recents, reopen: settings kept); a 5-min watch window with the phone locked (the watching notification appears, and goes at the alarm); use the phone in the window (the "Up already?" prompt with the Flute sound); **Dismiss alarm** (Google Clock dismisses it, Dawnly comes back); **Not yet**; change the alarm with the prompt showing (the prompt goes); sound picker previews; auto-dismiss with the new hint. Logs: `adb logcat -s WatchScheduler WakeWatcher AwakeNotifier DismissAlarmActivity QuietDismiss AutoDismiss ClockApp`.
  - **Moved to a later testing phase** (the user's decision, 2026-11-02): "we should just make the app, and the testing phase will come later. We have to first make it good." Don't run device-test rounds while building; collect what needs testing here instead.
  - **What the user saw, explained from the logs:** no "Up already?" prompt in either try. Notifications, the channel (high importance, `raw/sound_flute`), the ringer and DND were all fine; the prompt was never posted because detection never reached 100% (2.5 min of watching the first time, about 1 min after the restart the second time; Medium needs e.g. 5 min of screen use). Both dismisses came from the Today screen's **Dismiss alarm** button (the only path that logs `ClockApp` without `DismissAlarmActivity`; the 8:08:40 one started from `MainActivity` 0.8 s after the shade closed over it). Auto-dismiss never ran. The user felt the first one happened by itself; the button pops in as watching starts and dismissed on one tap, so it now asks first (below).
  - **Partial result (release 0.1.0 installed with the user's OK, 2026-11-02, from the logs):** onboarding ran at 19:46, including Alarms & reminders. Dawnly's process restarted at 19:47:41 and the 5-min window setting was kept. An 8:00 pm alarm got its window at 7:55; the exact alarm started watching at 19:55:00.014. **Dismiss alarm** on Home at 19:57:30 went to Google Clock; the next alarm moved 0.17 s later and watching stopped. No crashes. Not covered yet: the "Up already?" prompt and its sound (detection didn't trigger in 2.5 min), Not yet, the hint, and the reboot test. The phone disconnected from adb after 19:57.
- [x] **Today screen, from the release try (the user's picks, 2026-11-02):**
  - **Dismiss alarm asks first.** A tap swaps the button, in place, for "Dismiss your 8:00 pm alarm?" with **Dismiss alarm** / **Cancel** (`DismissAction` in `HomeScreen.kt`). Inline rather than a dialog, so it stays on the glass and the flowing background.
  - **"Signs you're awake" bar** at the top of the signs card while watching: a percentage, a thin bar (`ProgressBar` in `ui/glass/ProgressRing.kt`) and "Dawnly asks once this fills". It shows `WakeScore.value`, the same score that sends the prompt (`HomeStatus.Watching.wakeProgress`, 1 new unit test, 63 in total).
  - Not seen on the phone yet (testing comes later).
- [x] **New app icon** (2026-11-02): "Floor Light", a slanted arched window in liquid glass on blue (the user's pick from 40 concepts; masters, a preview page and the generator are in `design/app-icon/`). The launcher uses the **night** version (the user's choice of one static icon) as bitmap layers in `mipmap-{mdpi…xxxhdpi}/ic_launcher_{background,foreground,monochrome}.png`, because the glass needs blur, which vector drawables can't do. To change it, edit `design/app-icon/generator/icon.py` and run `python build.py --android night` in that folder (see `design/README.md`). The old vector icon drawables were removed. Release 0.1.0 with the new icon installed over the old one with the user's OK (2026-11-02, settings kept). Not checked on the phone yet: the home screen, the app drawer, and themed icons on.
- [x] **Repo cleanup** (2026-11-02, the user asked for a tidy, engineer-style repo):
  - **Debug-only code out of the release app:** `DebugScreen.kt` moved from `main` to `app/src/debug/` (only `DebugActivity` uses it), with its 44 strings (`strings_wake.xml` → `debug/res/values/strings_debug_screen.xml`). The release APK no longer contains the debug screen or its text (checked with aapt2).
  - **Big screens split into parts:** `HomeScreen.kt` (432 lines) → `HomeScreen.kt` (screen + previews) and `HomeCards.kt` (alarm ring, signs card, dismiss button, notes); `SettingsScreen.kt` (460) → `SettingsScreen.kt` and `SettingsSections.kt` (window, signs, auto-dismiss, permissions). No behaviour change.
  - **Icon design files:** the 40-concept exploration folder `design/logo/` replaced by `design/app-icon/` (masters, `preview.html`, a 3-file generator). The generator's output was checked to be byte-identical. `design/README.md` explains the folder.
  - **Removed:** the merged branches `phase-3-wake`, `phase-4-notify`, `phase-5-ui`; the stale root `build/` and empty `.kotlin/` folders; `__pycache__/` is now git-ignored.
  - **GitHub contributors:** the commit history was rewritten so the user is the only contributor, and force-pushed (the user's instruction). Commit hashes before 2026-11-02 changed as a result.
  - Checks after the cleanup: `testDebugUnitTest assembleDebug assembleRelease lintDebug` pass (63 tests; lint shows only `BatteryLife` and the AGP 9.5.0 notice).
- [x] **Licence** (2026-11-02): all rights reserved (the user's choice over MIT and Apache 2.0). `LICENSE` says so and lists the font's and sounds' own licences; the README has a Licence section.
- [ ] **Hobbyist sharing:** see "Sharing with testers" below.

**Sharing with testers (researched 2026-11-02)**
Google's "hobbyist account" is now the **limited distribution account** in the Android Developer Console (`https://android.google.com/developerconsole`). It's free and needs no ID: a Google Account with 2-Step Verification, a Google payments profile (legal name and address) and a contact email. It registers package names and shares an app with **up to 50 devices** (Google's guide says 50; its FAQ still says 20) through a QR code or link that each tester accepts on their phone. Sources: developer.android.com/developer-verification/guides/limited-distribution and …/guides/faq.
- **What's enforced now:** since 2026-09-30, unregistered apps can't be installed on certified phones in Brazil, Indonesia, Singapore and Thailand only. Everywhere else a sideloaded APK installs as before until the global rollout in 2027. adb installs are never affected.
- **So, today:** testers can install `app-release.apk` directly (e.g. from a Google Drive link, with "Install unknown apps" allowed for the browser or Files app).
- **For 2027:** register `com.dawnly.app` with the release key's SHA-256 in the console's **Package names** tab. If the key is lost, the package can't be registered.
- [ ] Account created · [ ] `com.dawnly.app` registered · [ ] build sent to testers

**Before the Play Store (later)**
- [ ] Play policy reasons for `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, and the `specialUse` foreground service.
- [ ] A privacy policy (Dawnly is fully offline and collects nothing, but Play still asks for one), store listing, and screenshots (the README ones can be reused).
- [ ] Not v1, from PLAN.md section 4: a "Still awake?" nudge for falling back asleep after dismissing. The hook is `WakeWatcher.onAwake`.

**Build notes:** in PowerShell, quote the Kotlin flag or it splits at the dot: `.\gradlew.bat --max-workers=2 "-Pkotlin.compiler.execution.strategy=in-process" assembleRelease`. Final check 2026-11-02: `testDebugUnitTest assembleDebug lintDebug` passes (63 tests; lint shows only `BatteryLife` and the AGP 9.5.0 notice).

---

## Open questions for the user
- Can the user get hold of a Samsung and a Xiaomi phone for Phase 1 testing? (Skipped for now; test before sharing the app.)
- Not v1: what should happen if the user falls back asleep after dismissing? (PLAN.md section 4)

## Environment notes (this PC)
- Windows 11. Tools available: PowerShell, Git Bash, winget, git, Node, Python 3.11.
- Nothing Android-related was installed before 2026-10-29. Android Studio and its SDK are the only additions.
- Test phone: Nothing Phone (2a) (A142), Android 16, Google Clock (`com.google.android.deskclock`, confirmed in Phase 1).
- The project lives at `C:\Users\aryan\dev\Dawnly`, outside OneDrive. It moved there on 2026-10-31 because OneDrive locked files in `app/build/`, so incremental builds failed with "Unable to delete directory ... merged.dir\values". Keep it out of OneDrive.
