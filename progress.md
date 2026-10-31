# Dawnly — Progress

> Progress tracker for [PLAN.md](PLAN.md). **PLAN.md is still the full brief and the single source of truth.** Read it first: the requirements, design system, technical approach, and Appendix A are all there. This file records where the work stands and what to do next.
>
> Rules from PLAN.md: work one phase at a time. End each phase with a summary and the steps to verify it on a device. Ask before adding any library not listed in PLAN.md section 5. Update this file whenever a step is done.

**Current phase:** Phase 0 (setup), almost done. The debug build works; the phone still needs to be connected.
**Last updated:** 2026-10-31

---

## Phase overview

| Phase | Goal | Status |
|---|---|---|
| 0 | Setup: SDK, Compose project, mockups, Hello screen running on the phone | 🟡 In progress |
| 1 | Feasibility: read the next alarm and dismiss it, tested on 3 phones | ⬜ Not started |
| 2 | Watch window: settings, exact-alarm scheduling, foreground service | ⬜ Not started |
| 3 | Wake detection: signals, scoring + unit tests, debug screen | ⬜ Not started |
| 4 | Notification and one-tap dismiss, plus the fallback | ⬜ Not started |
| 5 | UI polish: glass design system, all screens, motion, dark mode, onboarding | ⬜ Not started |
| 6 | Portfolio finish: README, GIF, challenges section, CI, app icon | ⬜ Not started |

---

## Phase 0 — Setup

### Done
- [x] Android Studio 2026.2.2 installed through winget. Its bundled JDK is **JBR 25**, at `C:\Program Files\Android\Android Studio\jbr`. Use it as `JAVA_HOME`; no separate JDK is installed.
- [x] Appendix A saved exactly as-is to `design/mockups.html`.
- [x] Compose project scaffolded (details under "Project facts" below).
- [x] Gradle wrapper 9.8.2 added. The `gradle-wrapper.jar` SHA-256 matches Gradle's published checksum, and `distributionSha256Sum` is set.
- [x] Hello screen: `ui/HelloScreen.kt` shows "Hello from Dawnly" with a minimal light/dark `DawnlyTheme`.
- [x] `git init` done and `.gitignore` added. **Nothing is committed yet.**

- [x] Studio setup wizard run (2026-10-31). The SDK is at `C:\Users\aryan\AppData\Local\Android\Sdk` with platform `android-37.0`, build-tools 36.0.0, and the licenses accepted.
- [x] `local.properties` created. The backslashes must be doubled in the file; a single `\U` silently breaks the path and gives "Invalid file path".
- [x] `assembleDebug` succeeds (2026-10-31). Fixed `settings.gradle.kts`: the regex escapes `"com\.android.*"` must be written `"com\\.android.*"` in Kotlin. None of the "Risks" below came up.

- [x] Phone connected (2026-10-31): Nothing Phone (2a), model A142, Android 16, serial `00069348R000717`. `adb devices` shows it as `device`.
- [x] `installDebug` works. "Hello from Dawnly" shows on the phone in **dark mode** (verified by screenshot).

### Remaining
- [ ] **User:** check light mode by hand. `adb shell cmd uimode night no` had no effect, probably because Nothing's Bedtime mode was on and forcing dark. `Theme.kt` follows `isSystemInDarkTheme()`, so this should just work.
- [x] User env vars set (2026-10-31): `JAVA_HOME` = Studio's `jbr`, `ANDROID_HOME` = the SDK, and `%ANDROID_HOME%\platform-tools` appended to the user `PATH` (kept as `REG_EXPAND_SZ`). They apply only to terminals opened after this change.
- [x] First commit made on branch `main`, with `.gitattributes` added so `gradlew` keeps LF line endings.
- [ ] Push to GitHub. Waiting on the user for the repo URL; `gh` isn't installed.
- [ ] Mark Phase 0 done here and write the phase summary.

### Risks to check on the first build
- **AGP 9 has built-in Kotlin.** The project does *not* apply `org.jetbrains.kotlin.android`. The Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose` 2.4.21) is declared in the root build file so it pulls KGP 2.4.21 onto the classpath. If the build complains about the Kotlin version, look here first.
- `compileSdk = 37` uses the old-style DSL. If AGP 9.4 requires the new `compileSdk { version = release(37) }` form, switch to it.
- `org.gradle.configuration-cache=true` is on. If a plugin is incompatible with the configuration cache, turn it off.
- JDK 25 (the JBR) with Gradle 9.8.2 should work. If it doesn't, add a Gradle toolchain.

---

## Project facts (decisions made so far)

| Item | Value |
|---|---|
| Package / applicationId | `com.dawnly.app`. Confirm with the user before Phase 1; it can't change after the app is published. |
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
- Is the package name `com.dawnly.app` OK?
- Make the first git commit after Phase 0 is verified?
- Not v1: what should happen if the user falls back asleep after dismissing? (PLAN.md section 4)

## Environment notes (this PC)
- Windows 11. Tools available: PowerShell, Git Bash, winget, git, Node, Python 3.11.
- Nothing Android-related was installed before 2026-10-29. Android Studio is the only addition.
- The project lives in OneDrive (`Desktop\Dawnly`). If OneDrive sync locks or slows the `build/` and `.gradle/` folders, consider moving the project out of OneDrive.
