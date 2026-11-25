# Hisab Studio 2 - 1.0.16 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 39, version name `1.0.16-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Fixes from the first phone test**
- The PIN pad no longer gets cut off on narrow phones: all keys are visible and the lock screen text is centred.
- Vibration uses the vibration motor with a stronger pulse and an alarm-type vibration, so the phone's touch-vibration switch does not mute it.
- The 7-day chart hint fits on one line.

## Tests

142 checks green, including a case that checks every PIN pad key fits a narrow screen. Test data is fully synthetic.

## Not verified

The 1.0.16 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
