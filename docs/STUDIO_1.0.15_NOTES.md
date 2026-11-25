# Hisab Studio 2 - 1.0.15 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 38, version name `1.0.15-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Hisab's own PIN pad**
- The lock screen and the set, change and turn-off PIN screens use Hisab's own number pad with PIN dots instead of the phone's keyboard, so no keyboard app sees the digits. The correct PIN opens the app as soon as it is typed.

**Vibration**
- A tick on every tap, key and switch, a firmer buzz on save, a double buzz on a wrong PIN. It uses the vibration motor directly. Settings > Touch and motion turns it off.

**Motion**
- Page content slides in with a short stagger, PIN dots pop, and the dots shake on a wrong PIN.

**7-day chart**
- Swipe instead of arrow buttons. The chart follows the finger.

## Tests

141 checks green, including the lock-screen case rewritten for the PIN pad. Test data is fully synthetic.

## Not verified

The 1.0.15 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
