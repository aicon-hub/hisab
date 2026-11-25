# Hisab Studio 2 - 1.0.19 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 42, version name `1.0.19-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Dock and Settings**
- The floating pill dock is back; the calmer look from 1.0.19 otherwise stays.
- Touch and motion and Profiles are always reachable in Settings > Your data (they were hidden unless Drive backup was on).
- New repeats and trips pick dates from a calendar instead of typed text.

## Tests

146 checks green. Test data is fully synthetic.

## Not verified

These changes were checked on rendered screenshots only; this build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
