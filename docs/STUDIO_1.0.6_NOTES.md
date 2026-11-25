# Hisab Studio 2 - 1.0.6 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 29, version name `1.0.6-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Home screen widget**
- Long-press the home screen, add the Hisab widget: today's spend on top, total expense below.
- The plus button opens a new expense sheet straight from the home screen.
- Numbers refresh after every change you make in the app.

**Month-end forecast**
- Home shows one quiet line: at this pace, this month ends around this much.
- Appears after the third day of the month, once there is spending to read.

## Tests

133 checks green, including new cases for the widget amounts and the forecast maths, plus the full screenshot suite. Test data is fully synthetic.

## Not verified

The 1.0.6 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test, and the widget especially wants a look on a real launcher. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
