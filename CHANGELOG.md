# Changelog

What changed in each version of Dawnly, newest first. Dates are 2026.

## 1.7.9 (6 Nov)
- Code audit fixes:
- A reminder whose time has already passed (a Today reminder set for an earlier time, say) is no longer saved silently. Dawnly says so and asks you to choose a later day or Tomorrow.
- Reminders that have finished (a one-day one after its day, or a range after its last day) leave the Reminders list the next time Dawnly opens. One still waiting to ring again after a snooze stays until then.
- If a reminder's sound can't be played, Dawnly plays the default alarm sound instead, so the ring isn't silent.
- Three more automated tests (151 in all).

## 1.7.8 (6 Nov)
- Haptics only while sliding: taps on the Light / Dark / System choices, the tabs, switches, buttons, the Today ring, and wheel values are silent. Swiping between Today and Settings is silent.
- A press and hold on a segmented choice or the tab bar gives a short buzz when it lets you slide, then a tick as the finger crosses each option. Dragging a wheel ticks as each value passes the middle.

## 1.7.7 (6 Nov)
- Code audit, no change in behaviour: removed an unused helper and its test, silenced two deprecation warnings on the usage-access check with the reason noted, tidied an import and the README's description of which alarms Dawnly keeps.

## 1.7.6 (6 Nov)
- Eight more automated tests (149 in all): reminders survive saving and loading, including ones saved before the ring setting existed, and the hour and minute sent to the Clock app (midnight, noon, 23:59).

## 1.7.5 (6 Nov)
- Tap a value on any wheel to scroll it to the middle: AM to PM, 7 to 8, a month, a year. Swiping still works.
- The year on the date wheel is no longer cut off (2028 showed as 202); the wheels are one line each and the year wheel is wider.

## 1.7.4 (6 Nov)
- Set an alarm for a **Custom date**: Today, Tomorrow or Custom date. A custom date is picked on the date wheels first, then the time. Any day but today is kept by Dawnly as a one-day alarm that rings (the Clock app can't take a date).
- Changing between Today, Tomorrow, Custom date and the date and time wheels now slides across.

## 1.7.3 (6 Nov)
- Set an alarm for **Today** or **Tomorrow**: a choice above the wheels. Today goes to the Clock app, which takes only a time; if that time has already passed, Dawnly asks you to choose Tomorrow. Tomorrow is kept by Dawnly as a one-day reminder that rings like an alarm, since the Clock app can't be asked for one specific date. It's listed under Reminders as "Alarm".
- After the Clock app is left (Back or Home), Dawnly comes back to the front by itself. This needs Usage access, which is optional; without it Dawnly stays where Android leaves it.

## 1.7.2 (6 Nov)
- Simpler alarms: tapping the ring on an alarm opens the Clock app ("Tap to open in Clock"), where you change or delete it with its own controls. Dawnly's own Change and Delete are gone: Google Clock showed its screen for every change anyway, and the switch-off-then-set trick could half-fail.
- After setting an alarm, Dawnly comes back in front by itself once the Clock app has it.
- The ring visibly sinks and lights up when pressed, with a tick.
- The flute plays only when Dawnly is opened from its icon, not when it comes back from the Clock app.

## 1.7.1 (6 Nov)
- **Change or delete an alarm set from Dawnly.** Tap the ring ("Tap to change"), move the wheels and tap Done, or tap Delete alarm. Android only lets apps set and dismiss alarms, so Dawnly switches the old one off in the Clock app, waits until it's really off, then sets the new one. If the Clock app keeps the old one on, nothing new is set and the Clock app stays open to change it there.
- Alarms you made in the Clock app (they may repeat) are left to the Clock app: tapping the ring opens it.
- The ring sinks a little, ripples and ticks when you tap it.

## 1.7.0 (6 Nov)
- **Reminders can ring like an alarm.** In the editor, How it comes: Notification, or Ring like alarm. A ringing reminder fills the screen (even locked) and plays its sound again and again, each play fading out before the next, starting soft and getting louder. Stop, or Snooze 10 min. After 5 minutes it stops by itself and leaves a "Missed" notification. If Android won't let it take over the screen, the notification's sound repeats instead.
- **Set an alarm from Dawnly.** With no alarm, tap the ring ("Tap to set an alarm"); with one, tap **Add alarm**. Pick the time on the glass wheels and Dawnly sets it in your Clock app, so it shows there and rings with the Clock's own sound.

## 1.6.6 (6 Nov)
- Learning from you records nothing while it's switched off (before, it kept a history anyway).
- After a reboot or an app update, reminders are set again before Android can stop the app.
- Backups keep your settings, reminders and what Dawnly learned, and leave out state that belongs to one phone (the armed alarm, wake readings).
- Text fixes: Right away plays your sound forwards; the notification texts no longer say "Dismiss automatically" or "never dismiss on our own".
- The window behind the app is the same near-black as the night sky, so there's no flash when opening in dark mode.
- Code tidy-up with no change in behaviour: smaller files, shared notification helpers, dead code removed, more tests (135).

## 1.6.5 (6 Nov)
- Reminders can run "From one date to another". Picking a date uses a glass wheel (day, month, year).

## 1.6.4 (5 Nov)
- A reminder has a **Which days** choice: every day, just today, just tomorrow, today and tomorrow, or a particular date.

## 1.5.4 (4 Nov)
- The "Alarm's off" notice no longer says "Have a good morning" at night.

## 1.5.1 to 1.5.3 (4 Nov)
- The sun and the moon are the same soft glow; the night is blacker.

## 1.5.0 (4 Nov)
- Smarter detection: **Charger unplugged** as a sign, activity has to keep going for a few minutes, and goals are lower close to the alarm and higher far from it.

## 1.4.0 to 1.4.3 (4 Nov)
- Custom reminder repeats (1 minute to 23 hr 59 min) and a custom watching window (5 min to 3 hr 59 min) on glass wheels.
- Clearer help for Android's "Restricted setting" box.

## 1.3.0 (4 Nov)
- Each reminder has a sound, chosen from Dawnly's list.

## 0.3.0 (3 Nov)
- **Reminders** on Today, with a glass editor and time wheels.
- **Learning from you:** Dawnly asks sooner or waits longer depending on your Not yet and Dismiss answers, and learns your usual wake-up times.
- Light and dark sweep in from a corner; softer night sky; a water-like progress ring; Feedback in Settings.

## 0.2.1 (3 Nov)
- New app ID `com.aryanlanke.dawnly`.

## 0.2.0 (3 Nov)
- First version shared with testers: works on Motorola, Redmi, OPPO and OnePlus phones installed from a file, with nothing Android locks during setup.

## Before 0.2.0 (31 Oct to 2 Nov)
- Reading the next alarm and dismissing it; the watch window and its foreground service; wake detection; the "Up already?" prompt with 17 sounds; the three choices for **When you're up** (Ask me, Warn first, Right away), which work with the phone locked; the glass look over a living sky; the Sunny Egg icon; CI; a signed release build.
