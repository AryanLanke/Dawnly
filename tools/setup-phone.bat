@echo off
rem Installs Dawnly on a phone plugged into this PC and turns on everything it needs, with no
rem warnings. An install through adb doesn't count as "installed from a file", so Android's
rem restricted settings lock (Usage access, Display over other apps) never applies.
rem
rem Before running:
rem   1. On the phone: Settings > About phone > tap "Build number" 7 times, then
rem      Settings > System > Developer options > turn on "USB debugging".
rem   2. Plug the phone in and tap "Allow" on the "Allow USB debugging?" box.
rem   3. Put dawnly.apk next to this file (or drag the APK onto this file), then double-click it.
rem
rem Needs adb: Android Studio's platform-tools, or Google's "SDK Platform Tools" download.

setlocal
set PKG=com.aryanlanke.dawnly

set ADB=adb
where adb >nul 2>nul || set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
%ADB% version >nul 2>nul || (
    echo Couldn't find adb. Install Google's "SDK Platform Tools" and try again.
    goto :end
)

set APK=%~1
if "%APK%"=="" set APK=%~dp0dawnly.apk
if not exist "%APK%" (
    echo Couldn't find the app: %APK%
    echo Put dawnly.apk next to this file, or drag the APK onto it.
    goto :end
)

echo Waiting for the phone. Tap "Allow" on the phone if it asks about USB debugging...
%ADB% wait-for-device

echo Installing Dawnly...
%ADB% install -r "%APK%" || (
    echo The install failed. If Dawnly is already on the phone from somewhere else, uninstall it first.
    goto :end
)

echo Turning on Dawnly's permissions...
rem Notifications and steps (Android 13+ / 10+; older phones just skip these).
%ADB% shell pm grant %PKG% android.permission.POST_NOTIFICATIONS 2>nul
%ADB% shell pm grant %PKG% android.permission.ACTIVITY_RECOGNITION 2>nul
rem Usage access, for the apps-opened sign.
%ADB% shell appops set %PKG% GET_USAGE_STATS allow
rem Lifts the restricted settings lock, in case Dawnly was installed from a file before.
%ADB% shell appops set %PKG% ACCESS_RESTRICTED_SETTINGS allow 2>nul
rem Alarms and reminders, so watching starts on time.
%ADB% shell appops set %PKG% SCHEDULE_EXACT_ALARM allow 2>nul
rem Display over other apps, for "Warn me" and "Right away".
%ADB% shell appops set %PKG% SYSTEM_ALERT_WINDOW allow
rem Background use: keeps the phone from stopping Dawnly.
%ADB% shell dumpsys deviceidle whitelist +%PKG% >nul

echo.
echo Done. Open Dawnly on the phone.
echo On Xiaomi, Redmi and POCO phones, also turn on Autostart: Dawnly's App info ^> Autostart.

:end
echo.
pause
