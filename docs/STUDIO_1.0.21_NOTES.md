# Hisab Studio 2 - 1.0.21 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 44, version name `1.0.21-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Spoken feedback**
- Uses the phone's own text-to-speech voice; nothing is sent anywhere. Malayalam needs a Malayalam voice installed on the phone, else English.
- Voice entries confirmed by voice; optional speak after saving; Read out buttons on Home, Insights, Coming up and trips; shared bill split spoken.
- Settings > Language and privacy > Spoken feedback.
- Verba TTS is not used: it has no published licence or download.

## Tests

150 checks green. Test data is fully synthetic.

## Not verified

These changes were checked on rendered screenshots only; this build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
