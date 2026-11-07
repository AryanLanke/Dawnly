# Dawnly: how it progressed

Dawnly is at **1.7.8**. This file tells how it got there, version by version, what each step
taught, and what is still untested. The short list of changes per version is in [CHANGELOG.md](CHANGELOG.md);
the test plan for the later testing phase is [TESTING.md](TESTING.md).

## Where it stands

| | |
|---|---|
| Version | 1.7.8 (versionCode 26), app ID `com.aryanlanke.dawnly`, Kotlin namespace `com.dawnly.app` |
| Works on | Nothing Phone (2a), Android 16, Google Clock (the phone it was built on). Testers ran it on Motorola, Redmi, OPPO and OnePlus phones. |
| Not tested yet | Samsung Clock and Xiaomi Clock, a reboot during a watch window, real nights (TESTING.md Rounds 6 and 7), the 1.7 checks (ringing reminders, setting alarms from Today) |
| Code | About 12,700 lines of Kotlin (with tests), 148 unit tests, CI on every push (tests, debug build, lint) |
| Not done on purpose | A "Still awake?" nudge for falling back asleep after dismissing (the hook is `WakeWatcher.onAwake`) |

## How it progressed

### 31 Oct to 1 Nov: does the idea work at all?
- **Phase 0, setup.** A Compose project that runs on the phone, with light and dark mode checked.
- **Phase 1, feasibility.** Can an app read the next alarm and dismiss it? Yes: `getNextAlarmClock()` reads it and `ACTION_DISMISS_ALARM` dismisses it, even three hours early on Google Clock. Everything else depended on this answer.
- **Phase 2, the watch window.** An exact alarm starts a quiet foreground service when the window opens. Learned: without "Alarms & reminders" Android delayed the fallback alarm by up to an hour, so onboarding makes the case for the permission. Alarms that first appear inside their window are skipped, because the user is plainly awake then.
- **Phase 3, wake detection.** Screen use, apps opened, steps, movement and music add up to one score; the user's rule that music never counts alone came from here. A debug screen shows the live values.
- **Phase 4, the prompt.** "Up already?" with Dismiss alarm and Not yet. `DismissGuard` refuses a stale prompt, so a late tap can never skip tomorrow's alarm.
- **Phase 5, the look.** The glass design system, Home, Settings, onboarding, and 17 notification sounds.
- **Phase 6, portfolio finish.** README with a demo, CI, the app icon.

### 1 to 3 Nov: making it real
- **Release build.** Signed with a private key, R8 and resource shrinking on. Debug code moved out of the release app.
- **Auto-dismiss on a locked phone.** The top priority, and it works. Starting another app's activity from the background is blocked, so it goes through a full-screen notification. After a dismiss the phone is left as it was: back to the app you were in, no Clock screen open, and the sound plays backwards so you know by ear.
- **Ask me, Warn first, Right away.** Three ways to answer "When you're up".
- **A living sky.** Morning clouds, night stars, a moon; light, dark, System, and Clock (night 7 pm to 6 am). Dropping the blur and baking each glass edge into a bitmap is what made scrolling hold 120 Hz.
- **0.2.0, shared with testers.** Their phones showed four problems that shaped the app:
  - Android 13+ locks Usage access and "Display over other apps" for apps installed from a file. Dawnly now needs nothing locked during setup: Usage access is optional and the automatic modes use a full-screen notification. It also shows step-by-step help when a lock does appear.
  - A Redmi swallowed the one-tap battery dialog, so each settings permission has a list of screens to try in turn.
  - A Redmi's Clock opened but did not dismiss, so the dismiss request has three forms, remembers the one that worked, and says "Your alarm is still on" if none does.
  - OPPO, OnePlus and realme got their own battery step.
- **0.2.1.** The app ID became `com.aryanlanke.dawnly` and was registered with Google.

### 3 to 4 Nov: 0.3.0 to 1.5.4, more than an alarm helper
- **0.3.0.** Reminders on Today, with a glass editor and time wheels. Learning from your mornings: Not yet and quick Dismiss answers steer how cautious Dawnly is, and it learns usual weekday and weekend times.
- **1.3 to 1.4.** A sound per reminder; custom repeats and a custom watching window on hours-and-minutes wheels (a slider was tried and dropped).
- **1.5.0, smarter detection.** Testers found setup cost too much for the payoff, so accuracy came from cheap things: the charger being unplugged as a sign, activity that has to keep going for a few minutes, and goals that are lower near the alarm and higher far from it.
- **1.5.1 to 1.5.4.** The sun and the moon became the same soft glow, the night got blacker, and the "Alarm's off" notice stopped saying "good morning" at night.

### 5 to 6 Nov: 1.6.4 to 1.6.6
- **1.6.4 and 1.6.5.** "Which days" for a reminder (every day, today, tomorrow, a date, or a range) and a glass date wheel.
- **1.6.6, the cleanup.** Learning stopped recording when switched off, reminders are set again before Android can stop the app after a reboot, backups leave out state that belongs to one phone, wrong in-app texts were fixed, large files were split into focused ones, shared helpers replaced copies, and the tests grew from 122 to 135. The README, privacy policy and test plan were brought up to date, the screenshots and demo were recorded again on the phone (cropped to the app, rounded corners, an animated WebP at 60 fps), and the code went under the MIT licence.

### 6 Nov: 1.7.0 to 1.7.7, alarms and reminders that ring
- **1.7.0.** Reminders can **ring like an alarm**: a full-screen ring that repeats its sound, each play fading out and getting louder, until Stop or Snooze 10 min. Alarms can be set from Today: the glass wheels pick the time and the Clock app gets it, so the alarm lives there like any other.
- **1.7.1 and 1.7.2.** Editing and deleting from Dawnly worked by switching the old alarm off, waiting, then setting the new one. It could half-fail and leave the old alarm on, and Google Clock showed its own screen for every change anyway. So 1.7.2 removed Dawnly's Change and Delete: tapping an alarm's ring opens the Clock app.
- **1.7.3 and 1.7.4.** Today, Tomorrow or a Custom date. Only Today goes to the Clock app, which takes a time and nothing else. Any other day is kept by Dawnly as a one-day reminder that rings like an alarm, since the Clock app can't be given a date. Dawnly comes back to the front after the Clock app is left (this needs Usage access, which is optional).
- **1.7.5.** Tapping a value on any wheel scrolls it to the middle. The year on the date wheel was cut off ("202"), now fixed.
- **1.7.6 and 1.7.7.** Tests grew to 149, then a code audit removed an unused helper and its test: 148 now, no change in behaviour.
- **Not yet done:** the 1.7 checks in TESTING.md are still to be run on the phone, so the new ring and set-alarm behaviour has no device results here yet.

## Decisions that shaped it, and what they cost
- **Read the Clock app's alarm, and set new ones only through the Clock app.** Alarms stay where users already have them. Dawnly hands a new alarm's time to the Clock app and leaves edits and deletes to it. Cost: Dawnly must never use `setAlarmClock()` for its own alarm, or it would read its own alarm as "the next alarm". A date other than today can't be given to the Clock app, so Dawnly keeps that one alarm itself as a one-day reminder.
- **Foreground service type `specialUse`.** No standard type fits "watching for signs someone woke up early". `health` needs Physical activity granted before every start; `dataSync` has a daily cap on Android 15; `shortService` stops after 3 minutes. Cost: a reason is declared for Play review.
- **"Skipped" alarms.** Only alarms whose window Dawnly scheduled ahead of time are watched. Cost: an alarm set at 4:55 for 5:00 is left alone, and Home says so.
- **One notification channel per sound.** Android fixes a channel's sound at creation. Cost: channels are swapped when the user picks a sound.
- **No blur library.** Glass is a tint plus a baked rim and shadow. Cost: less realistic glass, gain: a steady 120 Hz.
- **Full-screen notification instead of an overlay.** It works with no locked permission. Cost: with the phone in use there is one tap.
- **Fully offline.** No internet permission, no account. Cost: no crash reports, so feedback is an email draft the user sends.
- **Manual wiring, no DI library, single module.** Small enough that a library would be overhead.

## Device results
Tested on a Nothing Phone (2a), Android 16, Google Clock:

| Test | Result |
|---|---|
| Window opens with the phone locked, closes at the alarm, next day scheduled | Passed |
| Dismiss during the window moves the alarm to tomorrow; it does not ring | Passed |
| Stale prompt (alarm already changed) | Refused, nothing dismissed |
| Prompt removed when the alarm changes | Passed (12 ms) |
| App update while waiting | Passed (`MY_PACKAGE_REPLACED`) |
| Auto-dismiss with the phone locked, back to the previous app | Passed |
| Smoothness: scrolling Settings and the time wheels | 120 Hz, 0 janky frames |
| Reboot during a window | Not tested |
| Samsung Clock, Xiaomi Clock | Not tested |

## Release facts
- **What the key is for.** The release key signs every build. Android only installs an update over an app signed with the same key, and Google's 2027 registration of apps is tied to the key's fingerprint. If it is lost, the app on testers' phones can't be updated and would need uninstalling.
- **Location.** `C:\Users\aryan\dawnly-keys\dawnly-release.jks` (PKCS12, RSA 4096, alias `dawnly`, valid to 2054). The password is in the git-ignored `keystore.properties` at the repo root. Keep a backup of it somewhere safe.
- **SHA-256.** `DC:6D:70:4D:EA:FD:FF:B7:6B:32:2B:87:D0:1F:88:C1:0D:32:1E:AA:21:6D:CE:9F:28:49:89:72:F3:4E:0E:BE`
- **Google registration.** A free "limited distribution" developer account registered `com.aryanlanke.dawnly` on 3 Nov. Since 30 Sep 2026 unregistered apps can't be installed on certified phones in Brazil, Indonesia, Singapore and Thailand; the rest of the world follows in 2027. adb installs are never affected.
- **Stores.** [docs/app-stores.md](docs/app-stores.md) (Xiaomi GetApps needs a registered company; Samsung needs commercial seller status) and [docs/play-internal-testing.md](docs/play-internal-testing.md).
- **Build.** Release: `.\gradlew.bat --max-workers=2 "-Pkotlin.compiler.execution.strategy=in-process" :app:installRelease`. The phone has the release-signed app, so `installDebug` fails over it.
