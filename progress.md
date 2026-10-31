# Dawnly — Progress

> Progress tracker for [PLAN.md](PLAN.md). **PLAN.md is still the full brief and the single source of truth.** Read it first: the requirements, design system, technical approach, and Appendix A are all there. This file records where the work stands and what to do next.
>
> Rules from PLAN.md: work one phase at a time. End each phase with a summary and the steps to verify it on a device. Ask before adding any library not listed in PLAN.md section 5. Update this file whenever a step is done.

**Current phase:** Phase 1 (feasibility test), not started. Phase 0 is done.
**Last updated:** 2026-10-31

---

## ▶ Next session: start here (tasks for 2026-11-01)

**Before starting (user):**
- [ ] Open the Clock app and set an alarm for sometime later that day, so Dawnly has one to read and dismiss.
- [ ] Plug in the Nothing Phone over USB and run `adb devices`. It should show `00069348R000717  device`.

**The plan, in order:**
1. [ ] Read PLAN.md and this file. Run `git status`: the working tree should be clean, with everything pushed to `origin main` on 2026-10-31.
2. [ ] Confirm the package name with the user. Current answer (2026-10-31): keep `com.dawnly.app` unless the user owns or plans to buy `dawnly.app`, in which case use `app.dawnly`. They asked about "app.dawnly.com"; that is the domain `com.dawnly.app` already corresponds to.
3. [ ] Build the Phase 1 test screen (checklist under "Phase 1" below): show the next alarm time and the app that owns it, plus a "Dismiss next alarm" button with a fallback.
4. [ ] `gradlew installDebug`, then test it on the Nothing Phone with the user. Fill in the Nothing Phone row of the results table.
5. [ ] Ask the user about access to a Samsung and a Xiaomi phone for the other two rows. Phase 2 must not start until the table is filled in, or the user explicitly decides to skip a phone.
6. [ ] Update this file, write the Phase 1 summary, and ask before committing and pushing.

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
| 1 | Feasibility: read the next alarm and dismiss it, tested on 3 phones | ⬜ Next |
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
| Package / applicationId | `com.dawnly.app`. The user is leaning towards keeping it; get final confirmation at the start of Phase 1 (see "Next session"). It can't change after the app is published. |
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
         └─ ui/HelloScreen.kt, ui/theme/Theme.kt
```
The `data/`, `domain/`, and `service/` packages from PLAN.md section 5 will be created in the phases that need them.

---

## Later phases: checklists

### Phase 1 — Feasibility test (most important)
- [ ] Screen showing the next alarm time from `AlarmManager.getNextAlarmClock()`, refreshed on `ACTION_NEXT_ALARM_CLOCK_CHANGED`.
- [ ] Also show which app owns the alarm (`AlarmClockInfo.showIntent` → creator package). This helps with OEM testing.
- [ ] "Dismiss next alarm" button: `AlarmClock.ACTION_DISMISS_ALARM` with `EXTRA_ALARM_SEARCH_MODE = ALARM_SEARCH_MODE_NEXT`, plus the `com.android.alarm.permission.SET_ALARM` permission.
- [ ] Fallback: if no app handles the intent, open the Clock app and tell the user.
- [ ] Test on the Nothing Phone (Google Clock), a Samsung (Samsung Clock), and a Xiaomi (Xiaomi Clock). Fill in the results table below **before starting Phase 2**.

| Phone | Clock app | Reads next alarm? | Dismiss works? | Notes |
|---|---|---|---|---|
| Nothing Phone | Google Clock | | | |
| Samsung | Samsung Clock | | | |
| Xiaomi | Xiaomi Clock | | | |

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
- Final confirmation of the package name `com.dawnly.app` (or `app.dawnly` if they own `dawnly.app`).
- Can the user get hold of a Samsung and a Xiaomi phone for Phase 1 testing?
- Not v1: what should happen if the user falls back asleep after dismissing? (PLAN.md section 4)

## Environment notes (this PC)
- Windows 11. Tools available: PowerShell, Git Bash, winget, git, Node, Python 3.11.
- Nothing Android-related was installed before 2026-10-29. Android Studio and its SDK are the only additions.
- Test phone: Nothing Phone (2a) (A142), Android 16, Google Clock assumed; confirm the clock app in Phase 1.
- The project lives in OneDrive (`Desktop\Dawnly`). If OneDrive sync locks or slows the `build/` and `.gradle/` folders, consider moving the project out of OneDrive.
