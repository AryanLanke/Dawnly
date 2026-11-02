# Project Plan: Dawnly (Android)

> App name: **Dawnly**
> This file is the brief for the AI coding assistant working in this repo. Read it fully before writing any code.

## 1. How to work on this project
- This is a **portfolio project built to production standards**: clean architecture, readable code, tests, and a polished UI. Treat it like a real product, not a tutorial app.
- Work **one phase at a time** (see section 8). Don't jump ahead. At the end of each phase, summarize what was built and how to verify it on a device.
- Ask before adding any library not listed in section 5.
- **This plan and the mockups can be changed if needed.** They're a strong starting point, not a fixed spec. If you find a better approach, an Android limitation, or a newer API, adapt the plan or design, briefly explain why, and update this file so it stays the single source of truth.
- The UI mockups are in **Appendix A** at the end of this file. In Phase 0, save that code as `design/mockups.html` and use it as the visual reference for every screen.

## 2. The problem
I set an alarm for 5:30 AM, wake up at 5:00 on my own, pick up my phone and start working, and the alarm still rings at 5:30. Annoying.

## 3. What the app does
1. Reads the time of the **next alarm the user set in their normal Clock app** (Google Clock, Samsung Clock, Xiaomi Clock). The user does NOT set alarms inside our app.
2. Before that alarm, during a **watch window** the user chooses (e.g. 30 min, 1 hr, 2 hr), the app quietly watches for signs the user is awake.
3. If the user seems awake, the app shows a **gentle notification**: "Up already? Looks like you're awake. Dismiss your 5:30 alarm?"
4. One tap on the notification asks the Clock app to dismiss that alarm. (Optional, off by default: auto-dismiss after a one-minute warning; see section 4.)

## 4. Decisions already made
- **Platform:** Android only. No iPhone version.
- **Alarms:** stay in the user's default Clock app. Our app only reads the next alarm and requests a dismiss.
- **Dismiss style:** one tap on a gentle notification. Never dismiss without asking, unless the user turns on **Dismiss automatically** in Settings (off by default, user's decision 2026-11-01). Then Dawnly first shows "Dismissing your 5:30 alarm in 1 minute" with **Not yet**; swiping it away also stops it. If the warning can't be shown or can't finish before the alarm, Dawnly asks instead.
- **"Awake" means a mix of signals** (user's changes, 2026-11-01): minutes of screen use, apps opened (Usage access), steps walked, the phone moving, and music playing. Each sign's progress toward its goal adds up, and the user counts as awake at 1: any one sign at its goal, or a mix (3 of 5 minutes on screen plus 20 of 50 steps). Music never counts alone, because people fall asleep to music: it adds at most as much as movement has. "Phone unlocked" was dropped: the phone can be unlocked while the user sleeps. A quick 3 AM time check adds only a sliver.
- **Watch window:** chosen by the user in settings.
- **Works fully offline.** No server, no login, no API calls.
- **Battery-friendly:** only run during the watch window, never all night.
- **Open (not v1):** what happens if the user falls back asleep after dismissing. Leave a clean place in the code to add a "Still awake?" nudge later.

## 5. Tech stack
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3 as the base, heavily customised — see section 6)
- **Glass/blur effects:** Haze library (frosted translucent layers)
- **Architecture:** MVVM, single module, clear packages (`ui`, `data`, `domain`, `service`)
- **Settings storage:** Jetpack DataStore
- **Dependency management:** Gradle version catalog (`libs.versions.toml`)
- **Tests:** unit tests for the wake-detection scoring logic
- **Min SDK 26, target the latest stable SDK.** Real blur works on Android 12+; provide a clean non-blur fallback below that.
- **Editor:** VS Code with an AI assistant. Android Studio still needs to be installed once to get the Android SDK, emulator, and drivers. Build and install with `./gradlew` commands from the terminal.

## 6. UI direction (very important)
- Inspired by Apple's newest iOS "Liquid Glass" design: frosted translucent layers, soft depth, large rounded corners, smooth spring animations. **Inspired by, not a copy of iOS.**
- Must NOT look like a generic AI-generated or default Material app.
- Rules:
  - One font family, one accent color, a small neutral palette. Support light and dark mode.
  - Real motion: springy cards, a ring that fills as the watch window progresses, smooth screen transitions.
  - Generous spacing, strong visual hierarchy, very little text on screen.
- Screens:
  1. **Onboarding & permissions:** friendly step-by-step explanation of each permission and why it's needed.
  2. **Home:** big glass card showing the next alarm time, watch window status (waiting / watching / you're awake), and the progress ring.
  3. **Settings:** watch window length, which wake signals to use, sensitivity, appearance (Light / Dark / System).
  4. **Debug screen** (hidden or dev-only): shows live signal values (minutes of use, apps opened, steps, movement, music) so I can test detection.
- **Approved mockups:** see Appendix A (saved as `design/mockups.html`). Match these screens closely in Compose, in both light and dark mode.

### Design system (from the mockups)
- **Style:** simple, translucent liquid glass. No themed gradients or illustrations. Keep it minimal.
- **Two modes, light and dark.** Settings has an Appearance option: Light, Dark, or System (follows the phone). Default: System.
- **Background:** plain white in light mode (`#F7F9FE`), near-black navy in dark mode (`#050A1A`). On top of it, **three soft blurred light-blue shapes slowly flow around** (drifting, growing and shrinking, 15–25 second loops). This movement behind the glass is the signature of the app.
- **Flowing shapes in Compose:** draw them as radial-gradient circles (already soft, so no expensive blur needed), animated with `rememberInfiniteTransition`. Only animate while the screen is visible, and keep them still when the system "reduce animations" setting is on. Use light, soft blues. Light mode: `#A9CCFF`, `#C7E2FF`, `#8FB8FF`. Dark mode: `#5E95FF`, `#8CBCFF`, `#4A7FF0`.
- **Glass:** very transparent, slightly blue-tinted, light blur (~8dp) with boosted saturation via Haze, so the flowing shapes are clearly visible through it. Light mode: white at ~14% (~42% for selected items), white border at ~75%. Dark mode: light blue at ~8% (~20% selected), white border at ~32%. Bright highlight on the top edge, soft inner glow, almost no drop shadow. Never frosted or milky.
- **Colors:** light mode uses dark navy text `#0E1E45` and blue `#1A4FD6` for main buttons, switches, and the progress ring. Dark mode uses white text and white main buttons with blue text. Everything stays in the blue family.
- **Font:** Figtree (Google Fonts) only. Light 300 for big times, Regular 400 for body, SemiBold 600 for titles and buttons.
- **Shapes:** cards 28dp corners, buttons 16–20dp, pills and tab bar fully rounded.
- **Home:** a glass circle with a thin progress ring showing how much of the watch window has passed, the alarm time in the middle.
- **Bottom navigation:** floating glass pill with two tabs, Today and Settings.
- **Motion:** gentle spring animations for the ring, toggles, and cards. Respect the system "reduce animations" setting.
- **Copy:** short, sentence case, plain words. The dismiss button always says "Dismiss alarm".

## 7. Technical approach (guidance; verify against current Android docs)
- **Read next alarm:** `AlarmManager.getNextAlarmClock()`. Listen for `ACTION_NEXT_ALARM_CLOCK_CHANGED` to update when the user changes alarms.
- **Start the watch window:** schedule an exact alarm at (alarm time − window). Handle the `SCHEDULE_EXACT_ALARM` permission and fall back gracefully if it's denied.
- **During the window:** run a foreground service with an ongoing low-priority notification. Choose the correct `foregroundServiceType` for Android 14+ and explain the choice to me.
- **Wake signals:**
  - Screen use: minutes from unlocking (`ACTION_USER_PRESENT`, registered dynamically inside the service because it can't be in the manifest) until `ACTION_SCREEN_OFF`. Lighting up the lock screen doesn't count.
  - Apps opened: `UsageStatsManager` events (Usage access, granted in system settings), counting each switch to a different app, not the home screen or Dawnly. It's the closest safe stand-in for touches; seeing touches in other apps would need an accessibility service, which was rejected.
  - Steps: `Sensor.TYPE_STEP_COUNTER` (needs the `ACTIVITY_RECOGNITION` permission).
  - Movement: the accelerometer at its slowest standard rate, batched; no permission needed. Counts the seconds in which the acceleration strays from gravity.
  - Music: `AudioManager.isMusicActive`, checked every few seconds. Counts only together with movement.
  - Put the scoring in a pure Kotlin class so it's easy to unit test.
- **Dismiss:** notification action uses `PendingIntent.getActivity` that launches `AlarmClock.ACTION_DISMISS_ALARM` with search mode "next alarm". Requires `com.android.alarm.permission.SET_ALARM`. Use an activity PendingIntent directly because Android 12+ blocks starting activities from notification trampolines.
- **Fallback:** if the Clock app doesn't support dismissing, open the Clock app instead and tell the user.
- **Auto-dismiss (opt-in):** after the warning minute, our own exact alarm fires and starts the dismiss from the background. Android 10+ blocks background activity starts unless the user grants "Display over other apps" (`SYSTEM_ALERT_WINDOW`), so ask for it only when the switch is turned on, with a short reason.
- **Hiding the Clock app:** the dismiss request carries `EXTRA_SKIP_UI` (documented only for setting alarms; Google Clock ignores it and opens its alarm list). Once the next alarm moves on, Dawnly brings itself back to the front, so the Clock app only flickers.
- **Permissions to handle:** notifications (Android 13+), exact alarms, activity recognition, battery optimization exemption.
- **Phone brands:** Xiaomi and Samsung aggressively kill background apps. Guide the user to the right battery settings (see dontkillmyapp.com).
- Stop the service as soon as the alarm time passes or the user dismisses.

## 8. Build phases
**Phase 0 — Setup**
Install Android Studio (for the SDK), create the Compose project, save Appendix A as `design/mockups.html`, connect my phone with USB debugging, and run a "Hello" screen on it.

**Phase 1 — Feasibility test (most important)**
A screen with two things: the next alarm time read from the Clock app, and a "Dismiss next alarm" button. I'll test it on three phones: **Nothing Phone (Google Clock), Samsung (Samsung Clock), Xiaomi (Xiaomi Clock)**. Record which ones work before going further.

**Phase 2 — Watch window**
Settings for window length, scheduling the window start, and the foreground service starting and stopping correctly.

**Phase 3 — Wake detection**
Screen use, apps opened, steps, movement, and music signals, plus the scoring logic with unit tests and the debug screen.

**Phase 4 — Notification & dismiss**
The gentle notification with the one-tap dismiss and the fallback.

**Phase 5 — UI polish**
Build the glass design system from `design/mockups.html`, all screens, animations, dark mode, onboarding.

**Phase 6 — Portfolio finish**
README with screenshots and a demo GIF, a "Challenges I solved" section (background limits, battery, OEM differences, exact alarms), GitHub Actions CI that builds and runs tests, and an app icon.

## 9. Distribution (later)
First share via Google's free **limited distribution account** in the Android Developer Console (no ID or fee; up to 50 devices that each accept the app by QR code or link, per Google's guide in Nov 2026). Unregistered sideloaded apps are blocked only in Brazil, Indonesia, Singapore and Thailand until the global rollout in 2027, so testers elsewhere can install the signed APK directly for now; register `com.dawnly.app` with the release key before 2027. Possibly the Play Store later (one-time $25 fee).

## Appendix A — UI mockups (`design/mockups.html`)
Approved design reference. Save exactly as-is to `design/mockups.html`. Open it in a browser to view; it has a Light/Dark toggle and the flowing background animation.

```html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Dawnly – Blue glass UI mockups</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root{
  --page-bg:#E9EDF5; --page-text:#14203F; --page-muted:#5A6684;
  --wall:#F7F9FE; --blob1:#A9CCFF; --blob2:#C7E2FF; --blob3:#8FB8FF;
  --text:#0E1E45; --text-soft:rgba(14,30,69,.6);
  --glass:rgba(255,255,255,.14); --glass-strong:rgba(255,255,255,.42); --edge:rgba(255,255,255,.75);
  --line:rgba(14,30,69,.10); --track:rgba(14,30,69,.12);
  --accent:#1A4FD6; --on-accent:#FFFFFF;
  color-scheme:light;
}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){
  --page-bg:#070B17; --page-text:#E8ECF7; --page-muted:#97A2C0;
  --wall:#050A1A; --blob1:#5E95FF; --blob2:#8CBCFF; --blob3:#4A7FF0;
  --text:#FFFFFF; --text-soft:rgba(255,255,255,.68);
  --glass:rgba(190,220,255,.08); --glass-strong:rgba(210,230,255,.2); --edge:rgba(255,255,255,.32);
  --line:rgba(255,255,255,.14); --track:rgba(255,255,255,.18);
  --accent:#FFFFFF; --on-accent:#1A4FD6;
  color-scheme:dark;}}
:root[data-theme="dark"]{
  --page-bg:#070B17; --page-text:#E8ECF7; --page-muted:#97A2C0;
  --wall:#050A1A; --blob1:#5E95FF; --blob2:#8CBCFF; --blob3:#4A7FF0;
  --text:#FFFFFF; --text-soft:rgba(255,255,255,.68);
  --glass:rgba(190,220,255,.08); --glass-strong:rgba(210,230,255,.2); --edge:rgba(255,255,255,.32);
  --line:rgba(255,255,255,.14); --track:rgba(255,255,255,.18);
  --accent:#FFFFFF; --on-accent:#1A4FD6;
  color-scheme:dark;}
*{box-sizing:border-box;margin:0}
body{font-family:Figtree,system-ui,sans-serif;background:var(--page-bg);color:var(--page-text);padding:28px 16px 48px}
header{max-width:1280px;margin:0 auto 24px}
header h1{font-size:1.6rem;font-weight:600}
header .modes{display:inline-flex;gap:4px;margin-top:14px;padding:4px;border-radius:999px;background:var(--glass-strong);border:1px solid var(--line)}
header .modes button{font:inherit;font-size:.85rem;font-weight:500;border:0;border-radius:999px;padding:8px 16px;background:transparent;color:var(--page-text);cursor:pointer}
header .modes button[aria-pressed="true"]{background:var(--accent);color:var(--on-accent)}
header p{color:var(--page-muted);margin-top:6px;max-width:62ch;line-height:1.5}
.row{display:flex;flex-wrap:wrap;gap:28px;justify-content:center;max-width:1280px;margin:0 auto}
figure{display:flex;flex-direction:column;align-items:center;gap:10px}
figcaption{font-size:.9rem;color:var(--page-muted)}
.phone{width:300px;height:630px;border-radius:44px;padding:10px;background:#03050b;box-shadow:0 24px 60px rgba(0,0,0,.25)}
.screen{position:relative;isolation:isolate;width:100%;height:100%;border-radius:36px;overflow:hidden;color:var(--text);background:var(--wall)}
/* Flowing blue shapes */
.flow{position:absolute;inset:0;z-index:-1;pointer-events:none}
.flow i{position:absolute;border-radius:50%;filter:blur(18px);opacity:.9}
.flow i:nth-child(1){width:240px;height:240px;left:-60px;top:40px;background:radial-gradient(circle,var(--blob1),transparent 68%);animation:a 16s ease-in-out infinite alternate}
.flow i:nth-child(2){width:280px;height:280px;right:-90px;top:260px;background:radial-gradient(circle,var(--blob2),transparent 68%);animation:b 19s ease-in-out infinite alternate}
.flow i:nth-child(3){width:220px;height:220px;left:10px;bottom:-60px;background:radial-gradient(circle,var(--blob3),transparent 68%);animation:c 22s ease-in-out infinite alternate}
@keyframes a{50%{transform:translate(140px,120px) scale(1.15)}100%{transform:translate(60px,320px) scale(.9)}}
@keyframes b{50%{transform:translate(-150px,-160px) scale(.9)}100%{transform:translate(-60px,140px) scale(1.2)}}
@keyframes c{50%{transform:translate(120px,-220px) scale(1.2)}100%{transform:translate(160px,-40px) scale(.95)}}
figure:nth-child(2) .flow i{animation-delay:-6s}
figure:nth-child(3) .flow i{animation-delay:-11s}
figure:nth-child(4) .flow i{animation-delay:-15s}
@media (prefers-reduced-motion:reduce){.flow i{animation:none}}
.status{display:flex;justify-content:space-between;padding:12px 22px 0;font-size:.72rem;font-weight:600}
.glass{background:var(--glass);border:1px solid var(--edge);
  backdrop-filter:blur(8px) saturate(170%);-webkit-backdrop-filter:blur(8px) saturate(170%);
  box-shadow:inset 0 1px 1px rgba(255,255,255,.9),inset 0 -1px 1px rgba(255,255,255,.3),inset 0 0 14px rgba(255,255,255,.18),0 4px 16px rgba(10,30,90,.08)}
.soft{color:var(--text-soft)}
.card{border-radius:28px;margin:0 14px;padding:16px 18px}
.ring{position:relative;width:210px;height:210px;margin:28px auto 22px;border-radius:50%;display:grid;place-items:center}
.ring svg{position:absolute;inset:0}
.ring .base{stroke:var(--track)} .ring .prog{stroke:var(--accent)}
.ring b{display:block;font-size:3.6rem;font-weight:300;letter-spacing:-.04em;line-height:1;text-align:center}
.ring span{display:block;font-size:.78rem;text-align:center;margin-top:6px}
.row2{display:flex;justify-content:space-between;align-items:center;padding:10px 0;font-size:.9rem}
.row2+.row2{border-top:1px solid var(--line)}
.row2 small{display:block;font-size:.74rem;margin-top:2px}
.check{width:22px;height:22px;border-radius:50%;display:grid;place-items:center;font-size:.7rem;background:var(--accent);color:var(--on-accent)}
.tabs{position:absolute;left:50%;bottom:18px;transform:translateX(-50%);display:flex;gap:4px;border-radius:999px;padding:5px}
.tabs span{padding:9px 20px;border-radius:999px;font-size:.82rem;font-weight:500}
.tabs .on{background:var(--glass-strong)}
.lock{text-align:center;padding-top:44px}
.lock .d{font-size:.9rem;font-weight:500}
.lock .t{font-size:5rem;font-weight:300;letter-spacing:-.05em;line-height:1.05}
.notif{border-radius:28px;margin:150px 12px 0;padding:15px 16px}
.app{display:flex;gap:8px;align-items:center;font-size:.72rem}
.icon{width:20px;height:20px;border-radius:7px;background:var(--glass-strong);border:1px solid var(--edge)}
.notif h3{font-size:1rem;font-weight:600;margin-top:9px}
.notif p{font-size:.86rem;margin-top:3px}
.btns{display:flex;gap:8px;margin-top:13px}
.btns button{flex:1;font:inherit;font-size:.85rem;font-weight:600;border:1px solid var(--edge);border-radius:16px;padding:11px;color:var(--text);background:var(--glass-strong)}
.btns button.main{background:var(--accent);color:var(--on-accent);border-color:transparent}
.title{font-size:1.7rem;font-weight:600;padding:22px 20px 14px;letter-spacing:-.02em}
.label{font-size:.78rem;padding:0 22px 8px}
.seg{display:flex;border-radius:18px;padding:4px;margin:0 14px 20px}
.seg span{flex:1;text-align:center;padding:9px 0;border-radius:14px;font-size:.82rem;font-weight:500}
.seg .on{background:var(--glass-strong);box-shadow:0 2px 8px rgba(10,30,90,.15)}
.sw{width:44px;height:26px;border-radius:13px;background:var(--accent);position:relative;flex:none}
.sw:after{content:"";position:absolute;right:3px;top:3px;width:20px;height:20px;border-radius:50%;background:var(--on-accent);box-shadow:0 1px 3px rgba(0,0,0,.2)}
.sw.off{background:var(--track)}.sw.off:after{right:auto;left:3px;background:#fff}
.ob{position:absolute;left:0;right:0;bottom:0;padding:0 14px 22px}
.steps{display:flex;gap:6px;justify-content:center;margin-bottom:16px}
.steps i{width:22px;height:5px;border-radius:5px;background:var(--track)}
.steps i.on{background:var(--text)}
.ob h2{font-size:1.6rem;font-weight:600;letter-spacing:-.02em;line-height:1.15;text-align:center}
.lead{text-align:center;font-size:.9rem;margin:10px 10px 18px;line-height:1.45}
.perm{display:flex;gap:12px;align-items:flex-start;margin:0 0 12px}
.perm .icon{width:36px;height:36px;border-radius:12px;flex:none}
.perm h4{font-size:.92rem;font-weight:600}
.perm p{font-size:.8rem;margin-top:3px;line-height:1.4}
.cta{width:100%;font:inherit;font-weight:600;font-size:.95rem;border:0;border-radius:20px;padding:15px;background:var(--accent);color:var(--on-accent)}
.skip{text-align:center;font-size:.84rem;margin-top:12px}
</style>
</head>
<body>
<header>
  <h1>Dawnly – Blue glass UI mockups</h1>
  <p>Clear blue glass over a white (light mode) or near-black (dark mode) background, with soft blue shapes slowly flowing behind. Use the buttons to switch between light and dark mode.</p>
  <div class="modes" role="group" aria-label="Mode"><button data-m="light">Light</button><button data-m="dark">Dark</button></div>
</header>
<div class="row">

<figure><div class="phone"><div class="screen"><div class="flow"><i></i><i></i><i></i></div>
  <div class="status"><span>4:58</span><span>5G 82%</span></div>
  <div class="ring glass">
    <svg viewBox="0 0 210 210" aria-hidden="true">
      <circle class="base" cx="105" cy="105" r="96" fill="none" stroke-width="5"/>
      <circle class="prog" cx="105" cy="105" r="96" fill="none" stroke-width="5" stroke-linecap="round" pathLength="100" stroke-dasharray="47 100" transform="rotate(-90 105 105)"/>
    </svg>
    <div><b>5:30</b><span class="soft">Watching since 4:30</span></div>
  </div>
  <div class="card glass">
    <div class="row2"><span>Phone unlocked</span><span class="check">✓</span></div>
    <div class="row2"><span>Screen use</span><span class="soft">3 of 5 min</span></div>
    <div class="row2"><span>Steps</span><span class="soft">42 of 50</span></div>
  </div>
  <div class="tabs glass"><span class="on">Today</span><span>Settings</span></div>
</div></div><figcaption>Home, watching</figcaption></figure>

<figure><div class="phone"><div class="screen"><div class="flow"><i></i><i></i><i></i></div>
  <div class="status"><span></span><span>5G 81%</span></div>
  <div class="lock"><div class="d">Saturday, 31 October</div><div class="t">5:04</div></div>
  <div class="notif glass">
    <div class="app"><span class="icon"></span><span class="soft">Dawnly</span><span class="soft" style="margin-left:auto">now</span></div>
    <h3>You’re up early</h3>
    <p class="soft">Dismiss your 5:30 alarm so it doesn’t ring?</p>
    <div class="btns"><button class="main">Dismiss alarm</button><button>Not yet</button></div>
  </div>
</div></div><figcaption>The gentle notification</figcaption></figure>

<figure><div class="phone"><div class="screen"><div class="flow"><i></i><i></i><i></i></div>
  <div class="status"><span>9:12</span><span>5G 64%</span></div>
  <div class="title">Settings</div>
  <div class="label soft">Start watching before the alarm</div>
  <div class="seg glass"><span>30 min</span><span class="on">1 hr</span><span>2 hr</span><span>Custom</span></div>
  <div class="label soft">Signs you’re awake</div>
  <div class="card glass" style="padding:4px 16px">
    <div class="row2"><span>Phone unlocked</span><span class="sw"></span></div>
    <div class="row2"><span>Screen use<small class="soft">At least 5 minutes</small></span><span class="sw"></span></div>
    <div class="row2"><span>Steps walked<small class="soft">At least 50 steps</small></span><span class="sw off"></span></div>
  </div>
  <div class="label soft" style="margin-top:20px">Appearance</div>
  <div class="seg glass"><span>Light</span><span>Dark</span><span class="on">System</span></div>
  <div class="tabs glass"><span>Today</span><span class="on">Settings</span></div>
</div></div><figcaption>Settings</figcaption></figure>

<figure><div class="phone"><div class="screen"><div class="flow"><i></i><i></i><i></i></div>
  <div class="status"><span>9:00</span><span>5G 90%</span></div>
  <div class="ob">
    <div class="steps"><i class="on"></i><i></i><i></i><i></i></div>
    <h2>Up before your alarm?<br>We’ll notice.</h2>
    <p class="lead soft">Dawnly watches for signs you’re awake, only before your alarm.</p>
    <div class="card glass perm"><span class="icon"></span><div><h4>Notifications</h4><p class="soft">So we can ask before dismissing your alarm. We never dismiss it on our own.</p></div></div>
    <button class="cta">Allow notifications</button>
    <div class="skip soft">Not now</div>
  </div>
</div></div><figcaption>Onboarding, step 1 of 4</figcaption></figure>

</div>
<script>
const btns=document.querySelectorAll('.modes button');
function set(m){document.documentElement.dataset.theme=m;btns.forEach(b=>b.setAttribute('aria-pressed',b.dataset.m===m));}
set(matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light');
btns.forEach(b=>b.onclick=()=>set(b.dataset.m));
</script>
</body>
</html>
```
