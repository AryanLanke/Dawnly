# Putting Dawnly in the Xiaomi and Samsung stores

**Why bother?** Apps from a phone's own store install without Android's "unknown app" warnings. Since the 2026-11-03 changes, a file install no longer runs into the restricted settings lock during setup either (it can still appear later if you turn on Apps opened or Display over other apps; Dawnly shows how to unlock it). So the stores are about a cleaner, more trusted install, not about making Dawnly work.

**What every store asks for:**
- **Privacy policy link:** https://github.com/AryanLanke/Dawnly/blob/main/docs/privacy-policy.md. It only works once that file is pushed to GitHub.
- **The app file:** ask the developer for a release build. The APK is `app/build/outputs/apk/release/app-release.apk`; Google Play would use the `.aab`.
- **Icon, screenshots and description:** the icon is `design/app-icon/egg-512.png`, screenshots can be taken on the phone (the README shows the download site, `docs/website-phone.gif`), and the description can reuse the README's opening paragraph (add a line about reminders).
- **Category:** Tools (or Lifestyle). **Price:** free. **Ads:** none. **In-app purchases:** none.

## Xiaomi GetApps (Xiaomi, Redmi and POCO phones)
**Checked on 2026-11-03: companies only.** The developer sign-up form requires a company name, the company region, a business licence number and an uploaded company registration certificate. The certificate must show the business may offer internet services, and personal ID isn't accepted. An individual can't register until there's a registered business. The steps below are for then.

1. Make a Xiaomi account at https://account.xiaomi.com (or sign in).
2. Go to https://global.developer.mi.com → **Console** → apply to become a developer. Choose **Individual** if it's offered (see the note above: for now it isn't). Expect to give your real name, an ID number and a phone number; you can skip payout details, because Dawnly is free.
3. Wait for review (a few days).
4. Console → **Create app**. Package name: `com.aryanlanke.dawnly`. Upload `app-release.apk`. Fill in the details above and submit for review.
5. Each new version: raise `versionCode` (the developer does it), build, then upload under **Update**.

## Samsung Galaxy Store (Samsung phones)
**Probably blocked for now.** Even free apps need "commercial seller" status. Samsung asks for proof of a business for that: a D-U-N-S number, or business papers where D-U-N-S isn't available. Individual developers without a company report being turned down, and these reports are from 2023 onwards. Samsung's page doesn't charge a fee.

Worth one try, since it costs nothing:
1. Make a Samsung account. **Pick the right country:** it can't be changed later.
2. Go to https://seller.samsungapps.com and register as a **Private Seller**.
3. Seller Portal → **Profile** → **Request commercial seller status**. If it asks for a D-U-N-S number: in the US, Dun & Bradstreet gives sole proprietors one for free, but it can take about 30 days. Elsewhere it may cost money; stop there.
4. Once approved: **Add new application** → Android. Upload `app-release.apk`, fill in the details and submit.

Samsung users can still install from the file. Dawnly's first setup now asks for nothing Android locks.

## Other phones
OnePlus (outside China), Motorola, Google Pixel and Nothing phones only come with the Google Play Store. Install from the file. If Play is ever affordable, `docs/play-internal-testing.md` has the steps.
