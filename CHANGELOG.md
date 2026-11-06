# Changelog

What changed in each version of Dawnly, newest first. Dates are 2026.

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
- First version shared with friends: works on Motorola, Redmi, OPPO and OnePlus phones installed from a file, with nothing Android locks during setup.

## Before 0.2.0 (31 Oct to 2 Nov)
- Reading the next alarm and dismissing it; the watch window and its foreground service; wake detection; the "Up already?" prompt with 17 sounds; the three choices for **When you're up** (Ask me, Warn first, Right away), which work with the phone locked; the glass look over a living sky; the Sunny Egg icon; CI; a signed release build.
