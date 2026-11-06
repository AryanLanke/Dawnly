# Sharing Dawnly through Google Play (internal testing)

Testers who install the APK from a chat or a file get two kinds of warnings:

- **Play Protect** ("unsafe app", "this app may harm your device").
- **Android's restricted settings lock** (Android 13+). It blocks Usage access and Display over other apps with "this can put your personal and financial info at risk".

Neither comes from anything Dawnly does. Both come from how the app got onto the phone. An app installed from Google Play gets neither. Internal testing lets up to 100 people install it from Play with a link, usually within minutes of uploading, before Dawnly is public.

Other ways, until then:

- **No computer:** the phone's own unlock. Open Dawnly's App info, tap ⋮ at the top right, then Allow restricted settings. Dawnly shows these steps when the lock stops a permission.
- **With a computer:** [`tools/setup-phone.bat`](../tools/setup-phone.bat) installs over USB and turns everything on. The steps are in the file.

## One time: the developer account
1. Go to https://play.google.com/console and sign in with the Google account Dawnly should belong to.
2. Choose **Personal** account, pay the one-time US$25 fee, and verify your identity. Google can take a few days to verify.

## One time: create the app
1. Play Console → **Create app**. Name: Dawnly. Default language: English. App or game: **App**. Free. Tick the declarations → **Create app**.
2. Left menu → **Test and release → Testing → Internal testing** → **Testers** tab → **Create email list**. Add each tester's Gmail address → **Save**.

## Each version
1. Raise `versionCode` in `app/build.gradle.kts`; Play refuses the same number twice. Ask the developer to do it.
2. Build the bundle; the developer can run it: `.\gradlew.bat bundleRelease`. The file is `app/build/outputs/bundle/release/app-release.aab`.
3. Internal testing → **Create new release**. The first time, accept **Play App Signing**: Google keeps the key that signs what testers download, and your own key only uploads.
4. Drag in `app-release.aab`, write a line in **Release notes** → **Next** → **Save and publish**.
5. **Testers** tab → **Copy link**, and send it to your testers. They open it on their phone, tap **Accept invite**, then **Download it on Google Play**.

## Forms Play asks for
Play may not let you publish until these are filled in (left menu → **Policy → App content**). Use the answers below.

- **Privacy policy:** a URL is needed because Dawnly reads usage stats and physical activity. Use the policy in this repo, https://github.com/AryanLanke/Dawnly/blob/main/docs/privacy-policy.md. It covers what Dawnly reads, what it keeps on the phone (settings, reminders, what it learned) and that nothing leaves the phone.
- **Data safety:** no data is collected or shared. Everything stays on the device. (Feedback only opens the user's own email app.)
- **Foreground service:** type **Special use**. Reason: the same text as the manifest's `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` ("Runs only during a short, user-chosen window before the user's next alarm, watching for signs they woke up early so it can offer to dismiss that alarm."). Play may ask for a short video of the watch window running.
- **Ads:** no ads. **Target audience:** 18+ or 13+, your choice. **Content rating:** fill in the questionnaire (no violence etc.).

Play also asks why Dawnly uses these sensitive permissions. Suggested reasons:
- **`USE_FULL_SCREEN_INTENT`** (restricted on Android 14+): "Warn first and Right away dismiss the user's alarm while the phone is locked in a pocket. A full-screen notification is the only way to start the Clock app from the background."
- **`SCHEDULE_EXACT_ALARM`**: "The watch window must start at the minute the user chose before their alarm, and reminders must come at the time the user set. Without it Android can delay them by up to an hour."
- **`PACKAGE_USAGE_STATS`** (optional, off by default): "Counts how many apps the user opens during the watch window, and finds the app they were in so Dawnly can take them back to it after dismissing."
- **`SYSTEM_ALERT_WINDOW`** (optional): "Lets Dawnly open the Clock app's dismiss request without an extra tap while the phone is in use. Dawnly never draws over other apps."

If a review question comes up about **battery optimization** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), the reason is: "Dawnly's core job is watching in the background during a window before the user's alarm. If the phone stops it, the feature fails."
