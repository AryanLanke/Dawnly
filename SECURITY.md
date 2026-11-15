# Security policy

Dawnly reads your next alarm, watches for signs you're awake, and can dismiss an alarm in your
Clock app. It does all of that on the phone, with no account and no internet permission, so a
report about it matters.

## Reporting a vulnerability

Please don't open a public issue for a security problem. Use GitHub's private reporting instead:

1. Open the repository's **Security** tab.
2. Choose **Report a vulnerability**.
3. Describe what you found, the Android version and phone you saw it on, and the steps to repeat it.

You'll get an answer within a week, and a fix or a written explanation of why the behavior is
intended. Once a fix ships, the report can be credited to you in the changelog, or kept private if
you prefer.

## Supported versions

Only the latest release is supported. Fixes ship as a new signed APK on
[dawnlyapp.pages.dev](https://dawnlyapp.pages.dev).

| Version | Supported |
|---|---|
| 1.8.x | Yes |
| Older | No. Update to the latest release first. |

## What's in scope

- Bypassing the app's protections around dismissing an alarm (dismissing the wrong alarm, or one
  the user hasn't confirmed).
- Reading or writing Dawnly's stored data (alarms, reminders, settings, learned wake behavior)
  from another app.
- Escalating through one of Dawnly's components: only the launcher activity is exported, and every
  other activity, service and receiver is `exported="false"`.
- Spoofing or forging the broadcasts Dawnly listens for to trigger a behavior without the user.
- Notification or full-screen intent abuse: showing a ring, prompt or alarm screen that the user
  didn't set.
- Anything in the release APK that sends data off the device. Dawnly declares no `INTERNET`
  permission and has no networking code, so this would be a serious finding.

## What's not in scope

- Android's own warnings for apps installed from outside the Play Store. Dawnly is distributed as a
  signed APK, so those warnings are expected.
- The battery exemption and other permissions Dawnly asks for in onboarding; each is explained in
  the app and in the README.
- Reports that need a rooted phone, an unlocked bootloader or a modified Android build to work.
- Social engineering that requires the user to install a modified copy of Dawnly.
