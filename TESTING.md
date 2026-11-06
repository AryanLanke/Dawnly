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
| Charger unplugged | Power disconnected | You unplug the phone | Adds 40%, never enough alone (less an hour or more before the alarm) |
| Music | Media playing | Any music or video is playing | Only counts together with movement, never alone |

The signs add up. One full sign is enough, and so are two half signs.

**Three rules change the timings below.** (1) Activity has to keep going: steps and movement need at least about 3 minutes of it, so a short burst doesn't count. (2) Closeness: goals are 20% lower in the last 10 minutes and grow to 40% higher two hours out, so tests close to the alarm ask sooner. (3) Learning from you: earlier Not yet answers make Dawnly a bit more cautious. For clean timings, switch **Learning from you** off first (More options) or tap **Start over**.

**A quick test alarm, the same every time:**
1. In Dawnly: **Settings** → **How early should Dawnly start watching?** → **Custom** → set the wheels to **0 hr 05 min** → **Done**.
2. **Settings** → **More options** → **Sensitivity** → **High** (short goals make tests quicker).
3. In the **Clock** app: **Alarm** tab → **+** → pick a time **at least 10 minutes from now** → **OK**.
4. Dawnly starts watching 5 minutes before that alarm. Today shows "On watch from …" until then.

Why at least 10 minutes: Dawnly only watches an alarm that was set *before* its watch time started. An alarm set too close shows "Dawnly lets it ring as usual".

**Saving time:** for Rounds 3 to 5, set several alarms at once, **10 minutes apart** (for example 2:20, 2:30, 2:40, 2:50). Each one gets its own 5-minute watch, so you can run one test per alarm without waiting. Delete the leftover alarms after the round.

---

## Round 1 — Fresh install and first start (about 10 min)

Removes Dawnly and installs it again, like a new user would. Your Dawnly settings reset; your alarms in the Clock app are not touched.

- [ ] **1.1** Uninstall Dawnly and install the test build. **Expect:** the Dawnly icon (the sunny egg) appears in the app drawer.
- [ ] **1.2** Open Dawnly. **Expect:** a welcome screen, then one permission per screen with a reason for each: Notifications, Alarms & reminders, Physical activity, Background use (battery). Dots at the bottom show the step.
- [ ] **1.3** **Notifications** → **Allow notifications** → Android asks → **Allow**. **Expect:** it moves to the next step by itself.
- [ ] **1.4** **Alarms & reminders**: tap the button → Android's page opens → turn on **Allow setting alarms and reminders** → press Back. **Expect:** Dawnly moves on.
- [ ] **1.5** **Physical activity** → **Allow**.
- [ ] **1.6** (Usage access isn't asked during setup any more. You can allow it later from Settings → More options → Apps opened.)
- [ ] **1.7** **Battery** → Android asks to let Dawnly run in the background → **Allow**. On a Nothing phone there may be an extra battery step; follow it.
- [ ] **1.8** **Expect:** you land on **Today**. Nothing crashed, no step was stuck.
- [ ] **1.9** Close Dawnly (swipe it away in Recents) and open it again. **Expect:** onboarding doesn't come back.
- [ ] **1.10** Skip test: tell the developer, who reinstalls, and this time tap **Skip** (or "Not now") on Physical activity. **Expect:** onboarding still finishes; later, **Settings** → **More options** shows "1 needs you" in blue. Then allow it from there.

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
- [ ] **2.9** **How early**: tap each preset, then **Custom**. Custom opens hours and minutes wheels (hours 0 to 3). **Expect:** the time on Today's "On watch from" changes to match.
- [ ] **2.10** **When you're up**: tap **Ask me**, **Warn first**, **Right away**. **Expect:** the line under it changes each time. Warn first and Right away stick without any extra permission when full-screen notifications are allowed. **Display over other apps** is optional: when asked, **Allow** → a list of apps → **Dawnly** → turn on → Back; it only saves a tap with the phone in use.
- [ ] **2.11** Hold and slide on **When you're up**. **Expect:** the pill follows your finger, the choice is made when you let go.
- [ ] **2.12** **Notification sound**: tap each sound. **Expect:** each plays once, with a soft fading ending, and gets the blue tick.
- [ ] **2.13** **Appearance**: Light, Dark, System, Clock. **Expect:** Light = morning sky with clouds; Dark = night sky with stars and a moon; System follows the phone's dark mode; Clock = night from 7 pm to 6 am. Text is easy to read in both.
- [ ] **2.14** **More options** → signs: turn each one off and on. Try to turn all of them off. **Expect:** at least one must stay on ("Keep at least one on. Music and the charger alone don't count.").
- [ ] **2.15** **More options** → **Sensitivity**: Low, Medium, High.
- [ ] **2.16** **More options** → permissions: each shows whether it's allowed; tapping a missing one opens the right Android page.
- [ ] **2.17** Change several settings, swipe Dawnly away in Recents, reopen. **Expect:** every setting kept.
- [ ] **2.18** Turn the phone sideways (with auto-rotate on). **Expect:** nothing is cut off or overlapping. Turn the phone's font size up (Settings → Display → Font size, largest). **Expect:** still readable, nothing cut off. Put it back after.
- [ ] **2.19** Home screen: the icon looks right. Turn on themed icons (long-press the home screen → Wallpaper & style → Themed icons). **Expect:** Dawnly gets a themed version, not a blank or square.

## Round 3 — Each sign on its own: the sensors (about 1 hour)

Set 9 alarms 10 minutes apart (see "Saving time"). Sensitivity **High**, When you're up **Ask me**. Before each test, in **More options**, turn on **only** the sign(s) named. Watch Today's **Signs you're awake** bar while you test.

Each "asks" result means: the **Up already?** notification with your sound, and Dawnly's Today showing "Look who's up early". Each "doesn't ask" result means: no notification, and the alarm rings as normal at its time (stop it in the Clock app).

- [ ] **3.1 Screen use** (only Screen use on): when watching starts, unlock and use the phone for 3 minutes. **Expect:** the bar fills steadily; asks at about 3 min (a little sooner close to the alarm). Tap **Dismiss alarm**.
- [ ] **3.2 Quick looks don't count** (only Screen use on): unlock, look for 10 seconds, lock. Do this 5 times over the window. **Expect:** the bar barely moves; doesn't ask.
- [ ] **3.3 Apps opened** (only Apps opened on): open 2 different apps (e.g. Chrome, then WhatsApp) from the home screen. **Expect:** asks within about 10 seconds of the 2nd app, or once the activity has kept going for a few minutes. Opening the same app twice, or Dawnly, doesn't count.
- [ ] **3.4 Steps** (only Steps on): lock the phone, put it in your pocket, walk about 50 steps. **Expect:** asks once the walking has gone on for at least about 3 minutes (a short burst doesn't count, and the step counter reports in batches).
- [ ] **3.5 Movement, in your hand** (only Movement on): keep the phone in your hand, screen on, walking around or turning it over, for 1 minute. **Expect:** asks after at least about 3 minutes of moving.
- [ ] **3.6 Movement, in your pocket, screen off** (only Movement on): lock, pocket, walk around for 3 to 4 minutes. **Expect:** asks. (This is the case to watch: with the screen off, the phone may sleep the accelerometer. If it doesn't ask, the developer checks the log for how much movement got through.)
- [ ] **3.7 Lying still doesn't count** (all signs on): phone face down on a table, don't touch it for the whole window. **Expect:** the bar stays at 0%; doesn't ask; alarm rings.
- [ ] **3.8 In bed doesn't count** (all signs on): phone on the pillow or mattress next to you, turn over a couple of times. **Expect:** doesn't ask, or the bar moves only a little.
- [ ] **3.9 Music alone doesn't count** (Movement and Music on): phone still on the table, play music for the whole window. **Expect:** doesn't ask.
- [ ] **3.10 Music plus movement** (Movement and Music on): play music, then carry the phone around for 3 minutes. **Expect:** asks once the movement has gone on a few minutes (music adds to it, never alone).
- [ ] **3.11 A mix** (all signs on, Medium): 2 minutes of screen use plus 20 steps. **Expect:** asks (the parts add up).

## Round 4 — The three choices for "When you're up" (about 1 hour)

Same setup: alarms 10 minutes apart, Sensitivity High, all signs on. The quickest way to get "awake" is to use the phone for 3 minutes, or to walk with it for a minute.

After **every** dismiss, check these three things; they apply to every test in this round:
- **(a)** "Alarm's off" notification, with your sound **played backwards** (with **Right away** it plays forwards).
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
- [ ] **5.7 Display over other apps turned off:** set Right away, then in Android settings turn off Display over other apps for Dawnly. Get awake. **Expect:** it still dismisses through the full-screen notification (it opens by itself on a locked phone; unlocked, you tap **Dismiss alarm**).
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

Friends' phones, 2026-11-03 (fixed in code, not yet tested). Install from a file, the way the friends did, not with adb:
- [ ] **7.5 Motorola, Usage access:** tap Allow, see Android's "Restricted setting" box, come back. **Expect:** the "Android locked Usage access" card with three steps. Open App info, then ⋮, then Allow restricted settings; tap Allow again and it turns on.
- [ ] **7.6 Redmi, Background use:** tap Allow in onboarding and in More options. **Expect:** something always opens: Android's dialog, else Xiaomi's battery saver screen, else App info. Log tag `Permissions` says which.
- [ ] **7.7 Redmi, Warn first:** the countdown runs out. **Expect:** the alarm goes off the list. If not, "Your alarm is still on" appears; tapping it opens Clock. Log tags `QuietDismiss` and `ClockApp` show which form Xiaomi's Clock took (`Next`, `ByTime`, `ByTimeShown`), or that none worked.
- [ ] **7.8 Redmi, early dismiss:** a 2-hour window, awake early. If the first try fails, the notice says "will try again at …", and 55 minutes before the alarm it tries once more.
- [ ] **7.9 Settings, long scroll:** open More options and scroll up and down slowly. **Expect:** cards fade out above the funny line and the tabs; no text overlaps.
- [ ] **7.10 tools/setup-phone.bat** on a friend's PC: installs, and every permission shows Allowed in More options.
- [ ] **7.11 No locks on first setup (any phone, installed from a file):** onboarding never asks for Usage access, and no "Restricted setting" box appears anywhere in setup.
- [ ] **7.12 Warn first without Display over other apps, phone locked in a pocket:** the alarm is dismissed by itself (full-screen notification), and the reversed sound plays. Log: `Dismissing the alarm at ... through a full-screen notification`.
- [ ] **7.13 The same, phone unlocked and in use:** a pop-up says "You're up, so your 7:00 alarm can go"; one tap on Dismiss alarm turns it off. Watch for whether the Clock app is left open afterwards (going back to the previous app may be blocked without Display over other apps).
- [ ] **7.14 OPPO / OnePlus / realme:** the battery step shows "OPPO and OnePlus battery settings", and Open reaches Auto-launch or App info.

---

## UI polish (branch ui-polish, 2026-11-03)

- [ ] **8.1 Press:** hold a sound in Dawnly's sound, a row in More options, and the sound row itself. **Expect:** a soft light fades over the whole row, edge to edge, words included; it fades away on release. No ripple circle.
- [ ] **8.2 Funny line:** swipe from Today to Settings slowly. **Expect:** the line above the tabs fades out with the swipe and is gone on Settings; tapping where it was does nothing there.
- [ ] **8.3 Night sky:** Appearance Dark. **Expect:** the moon and stars look soft, like the morning clouds; shooting stars glide about half as fast, with a little four-point star at the head.
- [ ] **8.4 Watch ring:** while watching. **Expect:** the ring fills smoothly (no steps every few seconds), looks like translucent blue water with a shine, and a gentle wave flows along it.
- [ ] **8.5 Custom window:** Custom opens the wheels. **Expect:** the minimum is 5 min; under 8 min a note says it's short. (See 8.20.)
- [ ] **8.6 Clock appearance by default:** clear Dawnly's data. **Expect:** Appearance shows Clock selected.
- [ ] **8.7 Opening sound:** open Dawnly from the launcher with the phone's sound on. **Expect:** the flute plays once; not again when the screen turns; silent in silent mode.
- [ ] **8.8 Right away sound:** Right away, get detected awake. **Expect:** "Alarm's off" plays the sound forwards. With Ask me or Warn first it still plays backwards.

- [ ] **8.9 Feedback:** Settings, Feedback. **Expect:** Gmail opens a new email to redischarmender@gmail.com, subject "Dawnly feedback", with the version and phone at the bottom.
- [ ] **8.10 Notification icon:** any Dawnly notification shows the fried egg, not the ring and sun.
- [ ] **8.11 Music while asleep:** Warn first or Right away, some movement plus music only. **Expect:** "Up already?" asks instead of dismissing; leave it and the alarm rings.
- [ ] **8.12 Reminders:** Today, Add a reminder: "Drink water", Every 15 min, from now until an hour later. **Expect:** a notification every 15 minutes with the name and why; none outside the times; the switch stops them; editing and deleting work; they come back after a reboot.
- [ ] **8.13 Daily reminder:** At a time, 2 minutes from now. **Expect:** it comes once at that time, and again the next day.

- [ ] **8.14 Theme sweep:** switch Light and Dark. **Expect:** going to Light, the new look sweeps in from the top-right corner; going to Dark, from the bottom-left; under a second, nothing else jumps; smooth. With the phone's reduce-animations setting on, it just switches.
- [ ] **8.15 Learning, answers:** answer Not yet twice on two mornings. **Expect:** More options, Learning from you: "Waits for a little more than at first" and "Learned from 2 mornings".
- [ ] **8.16 Learning, usual time:** three weekday mornings answered Dismiss alarm. **Expect:** "You're usually up around ..." with about that time.
- [ ] **8.17 Learning, off and Start over:** the switch off makes Dawnly behave as it ships; Start over clears the list.

- [ ] **8.18 Reminder sound:** a reminder left on its default sound. **Expect:** it rings with Bubble, not the phone's own sound. Change it to another sound in the editor (Sound row): tapping a sound plays it; the next reminder rings with that one.

- [ ] **8.19 Custom repeat:** in the editor, pick Every, then Custom. **Expect:** an hours and minutes wheel in a glass circle; set 0 hr 05 min, Done, and the row reads "Repeats every  5 min"; Today shows "Every 5 min, ...". Try 6 min and 2 hr too; reopening the reminder shows Custom still picked with its value. Picking 15/20/30 min or 1 hr goes back to that choice.
- [ ] **8.20 Custom watching window:** Settings, How early should Dawnly start watching?, Custom. **Expect:** the same hours and minutes wheels as a reminder's Custom, over a frosted screen; hours go 0 to 3. Set 0 hr 06 min, Done: the row reads "Custom  6 min" and the "That's short" note shows. 1 hr 05 min, 2 hr 03 min and 3 hr 02 min stay exactly as picked; 0 hr 02 min becomes 5 min. Tapping around the circle or Back closes it unchanged.

- [ ] **8.21 Charger unplugged:** phone charging when the window opens; unplug it. **Expect:** Today's Charger unplugged row turns to the check, and the Are you up? bar jumps by about 40% (less an hour or more before the alarm). Unplugging alone never asks. Turned off in Settings, the row goes and unplugging adds nothing.
- [ ] **8.22 A burst that stops:** in the window, walk about 50 steps in under a minute, then put the phone down and stay still. **Expect:** no Up already?, even with the bar full. Walk again for 3 minutes or more: it asks. Using the phone for 5 minutes (Medium) still asks at 5 minutes, no later.
- [ ] **8.23 Closer to the alarm counts for more:** the same 4 minutes of screen use 10 minutes before the alarm asks; an hour or more before, it doesn't yet. The debug screen shows Activity stretch and Kept going for tuning.
- [ ] **8.24 Restricted setting, explained (Android 13+, installed from a file):** Settings, When you're up, Right away, before allowing Display over other apps. **Expect:** the hint under Allow says a "restricted setting" message is normal and Dawnly will show how to unlock it. Tap Allow, tap Dawnly, see Android's "Restricted setting" box, come back: Dawnly's "Android locked Display over other apps" card shows the first time, is easy to read (no Settings text showing through), and its four steps work: Open App info, ⋮ More, Allow restricted settings, confirm, come back and tap Allow.

## Reminders and other 1.6 checks

- [ ] **9.1 Which days:** in the editor, pick each of Every day, Just today, Just tomorrow, Today and tomorrow, A particular date. **Expect:** Today's row names the choice ("Today", "Tomorrow", "Today and tomorrow", or the date); reopening the reminder shows the same choice.
- [ ] **9.2 Date range:** From one date to another, 3 days from now to 5 days from now. **Expect:** nothing before the first day, a reminder each day in the range, and nothing after the last day (the alarm is cancelled).
- [ ] **9.3 Date wheel:** open it for a month with 31 days and set the day to 31, then change the month to February. **Expect:** the day drops to the last day of February (28 or 29). The year choices run about 6 years from this one.
- [ ] **9.4 Last day:** a reminder set "Just today" for a time 2 minutes away. **Expect:** it comes once; after that it does not come tomorrow.
- [ ] **9.5 Past midnight:** Every 60 min from 10 pm until 2 am. **Expect:** reminders at 10, 11, 12 and 1, then none until 10 pm.
- [ ] **9.6 Phone default sound:** a reminder with Sound set to Phone default. **Expect:** it rings with the phone's own notification sound.
- [ ] **9.7 Reboot:** restart the phone with a reminder on. **Expect:** it still comes at its next time (reminders are set again at boot).
- [ ] **9.8 Learning lines:** after three weekday and two weekend mornings answered, More options → Learning from you. **Expect:** lines for the usual weekday time, the usual weekend time, and which sign Dawnly trusts most, plus "Waits for a little more" or "Asks a little sooner" depending on your answers.
- [ ] **9.9 Learning off records nothing:** switch Learning off, get asked once, answer. Switch it on again. **Expect:** the count of mornings didn't go up.
- [ ] **9.10 Feedback without Gmail:** with no email app, tap Feedback. **Expect:** a short message with the address to write to.
- [ ] **9.11 Clock appearance at the changeover:** with Clock selected and Dawnly open at 6:59 pm (or 5:59 am). **Expect:** the look changes by itself a minute later.
- [ ] **9.12 Wrong texts fixed:** Android's notification settings for "Up before your alarm" mention **Warn first or Right away**; the Notifications reason in More options says Dawnly only dismisses on its own if you choose one of those; Right away's line says the sound plays forwards.
- [ ] **9.13 Backups:** after restoring to a new phone, reminders and settings are there, and no stale watch window or alarm carries over.


## 1.7.0: ringing reminders and setting alarms

- [ ] **10.1 Ring like alarm, unlocked:** a reminder set to Ring like alarm, 2 minutes away, with Dawnly closed. **Expect:** a full-screen ring (or a pop-up on top of the app you're in) with the name, the why, Stop and Snooze 10 min. The sound plays, fades, pauses, and plays again, starting soft and getting louder. The phone vibrates.
- [ ] **10.2 Ring like alarm, locked:** the same with the screen off and locked. **Expect:** the screen turns on and the ring shows over the lock screen.
- [ ] **10.3 Stop:** tap Stop. **Expect:** sound and vibration stop at once, the screen closes, and the reminder's next time is still set.
- [ ] **10.4 Snooze:** tap Snooze 10 min. **Expect:** it rings again 10 minutes later, and its regular next time hasn't moved.
- [ ] **10.5 From the notification:** pull down the shade while it rings and use Snooze or Stop there. **Expect:** same as 10.3 and 10.4.
- [ ] **10.6 Nobody answers:** let it ring. **Expect:** it stops after 5 minutes and leaves "Missed: <name>".
- [ ] **10.7 Back button:** press Back on the ring screen. **Expect:** it keeps ringing; only Stop or Snooze ends it.
- [ ] **10.8 Silent mode and Do Not Disturb:** with the ringer on silent, it still rings (it's on the alarm volume). With Do Not Disturb set to allow alarms, it rings too.
- [ ] **10.9 Notification stays a notification:** a reminder left on Notification. **Expect:** plays once, as before.
- [ ] **10.10 Old reminders:** after updating from 1.6.6, existing reminders show Notification in the editor.
- [ ] **10.11 Turned off while snoozed:** snooze a ringing reminder, then switch it off on Today. **Expect:** it doesn't ring 10 minutes later.
- [ ] **10.12 Set an alarm, none yet:** with no alarm, tap the ring, pick 7:30 am, Done. **Expect:** a toast "Alarm set for 7:30 am in Clock"; the alarm is in the Clock app's list; Today shows it.
- [ ] **10.13 Add alarm:** with an alarm already set, tap Add alarm and set another, earlier one. **Expect:** it shows in the Clock app, and Today switches to it if it's the next one.
- [ ] **10.14 Close without setting:** open the wheels and tap around them or press Back. **Expect:** no alarm is set.
- [ ] **10.15 24-hour phone:** with 24-hour time, the wheels show 0 to 23 and the alarm lands at the right hour.
- [ ] **10.16 Other Clock apps:** on Samsung or Xiaomi, setting an alarm either sets it quietly or opens the Clock app's own screen to finish it.
- [ ] **10.17 Touch effect:** tap the ring. **Expect:** it sinks a little, ripples and gives a small tick.
- [ ] **10.18 Change an alarm:** set an alarm from Dawnly, then tap the ring ("Tap to change"), move it 10 minutes and tap Done. **Expect:** the Clock app opens briefly, Dawnly comes back with "Alarm set for ...", and the Clock app's list has the new time with the old one switched off.
- [ ] **10.19 Delete an alarm:** tap the ring, Delete alarm. **Expect:** "Alarm deleted"; the Clock app shows it switched off; Today shows the next alarm, or "No alarm yet".
- [ ] **10.20 Far-off alarm:** change an alarm set for tomorrow. **Expect:** it works, or, if the Clock app won't switch it off, "The Clock app kept the old alarm on" and no second alarm is set.
- [ ] **10.21 Clock-made alarm:** with the next alarm made in the Clock app, tap the ring. **Expect:** the Clock app opens; no "Tap to change" under the time.
