# Hisab Studio 2 - 1.0.20 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 43, version name `1.0.20-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Bank SMS and tags**
- Bank SMS setup is three steps, with the advanced options behind More options.
- Tap-to-pick tag chips (trips and most used tags) under the Tags field when saving an entry.
- The floating pill dock matches 1.0.17 exactly; the corner tightening from 1.0.18 is removed.

## Tests

148 checks green. Test data is fully synthetic.

## Not verified

These changes were checked on rendered screenshots only; this build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
