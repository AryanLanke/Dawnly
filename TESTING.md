# Dawnly — Test plan

The full check of Dawnly on a real phone before sharing it. Work through it in rounds. Each round can be a separate sitting. Keep the phone plugged into the laptop with the USB cable during a round so the developer can watch Dawnly's log and tell you what it saw.

Tick each box when the result matches **Expect**. When it doesn't, write what happened next to it (or just report it). Bugs get fixed in batches between rounds, and the affected tests get run again.

**Phone:** Nothing Phone (2a), Android 16, Google Clock. Samsung and Xiaomi come in Round 7.

## Before you start

**One build for the whole round.** Test the build the developer installs at the start of the round, and don't change the app while a round is running. Otherwise a result can't be trusted.

**How the sensors work** (so you know what you're testing):

| Sign | Sensor | Counts when | Goal on High / Medium / Low |
|---|---|---|---|
| Screen use | Screen on/off and unlock | The phone is unlocked and the screen is on | 3 / 5 / 10 min |
| Apps opened | Usage access | You open a different app (the home screen and Dawnly don't count) | 2 / 3 / 5 apps |
| Steps | Step counter | You walk, screen on or off | 30 / 50 / 100 steps |
| Movement | **Accelerometer** (not the gyroscope; Dawnly doesn't use it) | The phone is picked up, carried or shaken, counted per second | 1 / 2 / 3 min |
| Music | Media playing | Any music or video is playing | Only counts together with movement, never alone |

The signs add up. One full sign is enough, and so are two half signs.

**A quick test alarm, the same every time:**
1. In Dawnly: **Settings** → **How early should Dawnly start watching?** → **Custom** → slide all the way left to **5 min**.
2. **Settings** → **More options** → **Sensitivity** → **High** (short goals make tests quicker).
3. In the **Clock** app: **Alarm** tab → **+** → pick a time **at least 10 minutes from now** → **OK**.
4. Dawnly starts watching 5 minutes before that alarm. Today shows "On watch from …" until then.

Why at least 10 minutes: Dawnly only watches an alarm that was set *before* its watch time started. An alarm set too close shows "Dawnly lets it ring as usual".

**Saving time:** for Rounds 3 to 5, set several alarms at once, **10 minutes apart** (for example 2:20, 2:30, 2:40, 2:50). Each one gets its own 5-minute watch, so you can run one test per alarm without waiting. Delete the leftover alarms after the round.

---

## Round 1 — Fresh install and first start (about 10 min)

Removes Dawnly and installs it again, like a new user would. Your Dawnly settings reset; your alarms in the Clock app are not touched.

- [ ] **1.1** Uninstall Dawnly and install the test build. **Expect:** the Dawnly icon (the sunny egg) appears in the app drawer.
- [ ] **1.2** Open Dawnly. **Expect:** a welcome screen, then one permission per screen with a reason for each: Notifications, Alarms & reminders, Physical activity, Usage access, Battery. Dots at the bottom show the step.
- [ ] **1.3** **Notifications** → **Allow notifications** → Android asks → **Allow**. **Expect:** it moves to the next step by itself.
- [ ] **1.4** **Alarms & reminders**: tap the button → Android's page opens → turn on **Allow setting alarms and reminders** → press Back. **Expect:** Dawnly moves on.
- [ ] **1.5** **Physical activity** → **Allow**.
- [ ] **1.6** **Usage access**: tap the button → a list of apps opens → tap **Dawnly** → turn on **Permit usage access** → Back, Back. **Expect:** Dawnly moves on.
- [ ] **1.7** **Battery** → Android asks to let Dawnly run in the background → **Allow**. On a Nothing phone there may be an extra battery step; follow it.
- [ ] **1.8** **Expect:** you land on **Today**. Nothing crashed, no step was stuck.
- [ ] **1.9** Close Dawnly (swipe it away in Recents) and open it again. **Expect:** onboarding doesn't come back.
- [ ] **1.10** Skip test: tell the developer, who reinstalls, and this time tap **Skip** (or "Not now") on Physical activity and Usage access. **Expect:** onboarding still finishes; later, **Settings** → **More options** shows "2 need you" in blue. Then allow both from there.

## Round 2 — Look, feel and settings (about 15 min, no alarm needed)

**Today and moving around**
- [ ] **2.1** With no alarm set: Today says "Nothing to watch yet" and a hint to set one in the Clock app.
- [ ] **2.2** Set an alarm for tomorrow morning in Clock. **Expect:** Today shows it within a few seconds, with "On watch from …" and the right time.
- [ ] **2.3** Swipe left and right. **Expect:** Today and Settings follow your finger; the tab pill follows the swipe.
- [ ] **2.4** Tap each tab. **Expect:** it switches smoothly.
- [ ] **2.5** Press and hold the tab bar, slide across, let go. **Expect:** the pill follows your finger and the page changes as you cross over.
- [ ] **2.6** On Settings, press Back. **Expect:** it goes to Today. Back on Today closes Dawnly.
- [ ] **2.7** The funny line above the tabs: tap it. **Expect:** a different line. Close and reopen Dawnly: another one.
- [ ] **2.8** Smoothness: scroll Settings up and down and swipe pages for a while. Then the developer reads the frame times from the phone. **Expect:** no stutter you can feel.

**Every setting**
- [ ] **2.9** **How early**: tap each preset, then **Custom**. Slide the slider and use **−** / **+** (also hold them: they repeat faster). **Expect:** the time on Today's "On watch from" changes to match.
- [ ] **2.10** **When you're up**: tap **Ask me**, **Warn first**, **Right away**. **Expect:** the line under it changes each time. Picking Warn first or Right away for the first time asks for **Display over other apps** → **Allow** → a list of apps → **Dawnly** → turn on → Back. **Expect:** the choice sticks once allowed. If you don't allow it, it stays on Ask me.
- [ ] **2.11** Hold and slide on **When you're up**. **Expect:** the pill follows your finger, the choice is made when you let go.
- [ ] **2.12** **Notification sound**: tap each sound. **Expect:** each plays once, with a soft fading ending, and gets the blue tick.
- [ ] **2.13** **Appearance**: Light, Dark, System, Clock. **Expect:** Light = morning sky with clouds; Dark = night sky with stars and a moon; System follows the phone's dark mode; Clock = night from 7 pm to 6 am. Text is easy to read in both.
- [ ] **2.14** **More options** → signs: turn each one off and on. Try to turn all of them off. **Expect:** at least one must stay on ("Music alone doesn't count").
- [ ] **2.15** **More options** → **Sensitivity**: Low, Medium, High.
- [ ] **2.16** **More options** → permissions: each shows whether it's allowed; tapping a missing one opens the right Android page.
- [ ] **2.17** Change several settings, swipe Dawnly away in Recents, reopen. **Expect:** every setting kept.
- [ ] **2.18** Turn the phone sideways (with auto-rotate on). **Expect:** nothing is cut off or overlapping. Turn the phone's font size up (Settings → Display → Font size, largest). **Expect:** still readable, nothing cut off. Put it back after.
- [ ] **2.19** Home screen: the icon looks right. Turn on themed icons (long-press the home screen → Wallpaper & style → Themed icons). **Expect:** Dawnly gets a themed version, not a blank or square.

## Round 3 — Each sign on its own: the sensors (about 1 hour)

Set 9 alarms 10 minutes apart (see "Saving time"). Sensitivity **High**, When you're up **Ask me**. Before each test, in **More options**, turn on **only** the sign(s) named. Watch Today's **Signs you're awake** bar while you test.

Each "asks" result means: the **Up already?** notification with your sound, and Dawnly's Today showing "Look who's up early". Each "doesn't ask" result means: no notification, and the alarm rings as normal at its time (stop it in the Clock app).

- [ ] **3.1 Screen use** (only Screen use on): when watching starts, unlock and use the phone for 3 minutes. **Expect:** the bar fills steadily; asks at about 3 min. Tap **Dismiss alarm**.
- [ ] **3.2 Quick looks don't count** (only Screen use on): unlock, look for 10 seconds, lock. Do this 5 times over the window. **Expect:** the bar barely moves; doesn't ask.
- [ ] **3.3 Apps opened** (only Apps opened on): open 2 different apps (e.g. Chrome, then WhatsApp) from the home screen. **Expect:** asks within about 10 seconds of the 2nd app. Opening the same app twice, or Dawnly, doesn't count.
- [ ] **3.4 Steps** (only Steps on): lock the phone, put it in your pocket, walk about 50 steps. **Expect:** asks within about half a minute of reaching 30 steps (the step counter reports in batches).
- [ ] **3.5 Movement, in your hand** (only Movement on): keep the phone in your hand, screen on, walking around or turning it over, for 1 minute. **Expect:** asks at about 1 min of moving.
- [ ] **3.6 Movement, in your pocket, screen off** (only Movement on): lock, pocket, walk around for 2 minutes. **Expect:** asks. (This is the case to watch: with the screen off, the phone may sleep the accelerometer. If it doesn't ask, the developer checks the log for how much movement got through.)
- [ ] **3.7 Lying still doesn't count** (all signs on): phone face down on a table, don't touch it for the whole window. **Expect:** the bar stays at 0%; doesn't ask; alarm rings.
- [ ] **3.8 In bed doesn't count** (all signs on): phone on the pillow or mattress next to you, turn over a couple of times. **Expect:** doesn't ask, or the bar moves only a little.
- [ ] **3.9 Music alone doesn't count** (Movement and Music on): phone still on the table, play music for the whole window. **Expect:** doesn't ask.
- [ ] **3.10 Music plus movement** (Movement and Music on): play music, then carry the phone around for 30 seconds. **Expect:** asks after about 30 s of movement (half from movement, half from music).
- [ ] **3.11 A mix** (all signs on, Medium): 2 minutes of screen use plus 20 steps. **Expect:** asks (the parts add up).

## Round 4 — The three choices for "When you're up" (about 1 hour)

Same setup: alarms 10 minutes apart, Sensitivity High, all signs on. The quickest way to get "awake" is to use the phone for 3 minutes, or to walk with it for a minute.

After **every** dismiss, check these three things; they apply to every test in this round:
- **(a)** "Alarm's off" notification, with your sound **played backwards**.
- **(b)** You're back where you were (the app you had open, or the home screen). **No Clock app** left open.
- **(c)** In the Clock app, the alarm is dismissed (shows as off for today / moved to tomorrow). It does not ring.

**Ask me**
- [ ] **4.1** Unlocked, in another app (e.g. YouTube): get awake → **Up already?** → **Dismiss alarm**. **Expect:** (a)(b)(c), you're back in YouTube.
- [ ] **4.2** Locked: get awake by walking with the phone in your pocket → take it out, don't unlock → tap **Dismiss alarm** on the lock screen. **Expect:** it may ask you to unlock; then (a)(b)(c).
- [ ] **4.3** **Not yet**: get awake → tap **Not yet**. **Expect:** the notification goes and doesn't come back in this window; the alarm rings as normal.
- [ ] **4.4** Ignore the prompt. **Expect:** it disappears by itself when the alarm rings.
- [ ] **4.5** Today's button: during a watch window, in Dawnly, tap **Dismiss alarm** → it asks "Dismiss your … alarm?" → **Cancel**. **Expect:** nothing happens. Then **Dismiss alarm** → **Dismiss alarm**. **Expect:** (a)(c), and you're back in Dawnly.

**Warn first**
- [ ] **4.6** Unlocked, in another app: get awake → "Dismissing your … alarm in 1 minute" → wait the minute. **Expect:** (a)(b)(c).
- [ ] **4.7** Stop it: get awake → tap **Not yet** in the warning. **Expect:** nothing dismissed; the alarm rings.
- [ ] **4.8** Dismiss early: get awake → tap **Dismiss alarm** in the warning. **Expect:** (a)(b)(c) straight away.
- [ ] **4.9** Locked, screen off, in your pocket, walking (with music if you like). Don't touch the phone for 2 minutes after it buzzes. **Expect:** (a) on the lock screen; after unlocking, (b) the app you had open, and (c).
- [ ] **4.10** Too close to the alarm: set an alarm so that you get awake in the last minute before it. **Expect:** Dawnly asks instead of warning (there's no time for a 1-minute warning).

**Right away**
- [ ] **4.11** Unlocked, in another app: get awake. **Expect:** no question, (a)(b)(c) within a few seconds.
- [ ] **4.12** Locked, screen off, pocket, walking. **Expect:** (a) on the lock screen; after unlocking, (b)(c).
- [ ] **4.13** Lock screen showing: get awake, then press the power button once so the lock screen shows, and leave it there (don't unlock) until it dismisses. **Expect:** (a)(c); after unlocking, (b).
- [ ] **4.14** On the home screen (no app open): get awake. **Expect:** back on the home screen, not in Dawnly or Clock.

## Round 5 — When things go wrong (about 45 min)

- [ ] **5.1 Alarm set too late:** set an alarm 3 minutes ahead (inside the 5-minute window). **Expect:** Today says Dawnly lets it ring as usual; it never asks.
- [ ] **5.2 Alarm changed while asking:** get the **Up already?** prompt, then move that alarm to a later time in Clock. **Expect:** the prompt goes away.
- [ ] **5.3 Alarm deleted while watching:** delete the alarm during its watch window. **Expect:** the watching notification goes; Today updates.
- [ ] **5.4 Two alarms close together:** alarms 3 minutes apart (e.g. 3:00 and 3:03). Get awake before the first. **Expect:** only the first is dismissed; the second still rings.
- [ ] **5.5 Dawnly swiped away during a window:** once watching starts, swipe Dawnly away in Recents, then get awake. **Expect:** it still asks (the watching notification stays).
- [ ] **5.6 Restart during a window:** once watching starts, restart the phone; unlock it. **Expect:** watching carries on (watching notification back), and it still asks when you get awake.
- [ ] **5.7 Display over other apps turned off:** set Right away, then in Android settings turn off Display over other apps for Dawnly. Get awake. **Expect:** it asks instead (the normal Up already? prompt).
- [ ] **5.8 No Usage access:** turn off Usage access for Dawnly. Right away, in another app, get awake (by screen use). **Expect:** (a)(c), and you land on the home screen. Turn Usage access back on.
- [ ] **5.9 Notifications off:** turn off Dawnly's notifications in Android settings. **Expect:** Today shows "Dawnly can't say hi" with a way to turn them on. Then, with **Warn first**, get awake. **Expect:** nothing is dismissed (a warning nobody can see can't be stopped), and the alarm rings. With **Right away**: the alarm is dismissed but silently, since the "Alarm's off" notification can't show. Turn notifications back on.
- [ ] **5.10 Silent and Do Not Disturb:** phone on silent, get awake. **Expect:** the notification arrives silently (vibrate only or nothing). Same with Do Not Disturb on.
- [ ] **5.11 Battery saver:** turn on battery saver, run a window, get awake. **Expect:** works the same.
- [ ] **5.12 Alarm turned off in Clock:** during a window, turn the alarm's switch off in Clock. **Expect:** watching stops.

## Round 6 — Real nights (3 nights)

The real thing: your normal morning alarm, default settings (Medium, a 30-minute or 1-hour window), the phone where you normally keep it.
- [ ] **6.1 Night 1, Ask me:** sleep normally. If you wake before the alarm and start your day, does it ask? If you sleep through, it must **not** ask, and the alarm must ring.
- [ ] **6.2 Night 2, Warn first.**
- [ ] **6.3 Night 3, Right away.**
- [ ] **6.4** Each morning, report what happened (and plug in the phone); the log shows the score through the night. **Expect:** no false alarm (never dismissed while you were asleep), and battery use reasonable (Android settings → Battery → Dawnly).

## Round 7 — Other phones (when you can borrow them)

On a Samsung and a Xiaomi phone, each with its own Clock app:
- [ ] **7.1** Round 1 (install, onboarding, especially the battery steps).
- [ ] **7.2** Dawnly reads the next alarm (Today shows it).
- [ ] **7.3** Right away, unlocked and locked (4.11, 4.12): the phone's own Clock app dismisses the alarm.
- [ ] **7.4** One sign test (3.1 or 3.4).

---

## Found while writing this plan (to fix)

- The Android notification setting for "Up before your alarm" still says "…only dismisses it on its own if you turn on Dismiss automatically", the old switch's name. It should name **When you're up**.
- The Notifications reason (onboarding and More options) says "We never dismiss it on our own." That's no longer true with Warn first or Right away.
