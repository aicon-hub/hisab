# Hisab Studio 2 - 1.0.10 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 33, version name `1.0.10-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Money diary**
- Settings > Money > Money diary: tap + to write a note for today, or tap an older note to edit or delete it. Up to 500 characters a day.
- Notes live in the same local database as your entries, so the encrypted backup and Drive backup carry them.

**Monthly summary notification**
- Settings > Money > Monthly summary notification (off by default).
- The first time you open the app in a new month, it shows last month's spending, income and the category you spent most on. Nothing runs in the background, so it appears when you open the app, not at midnight on the 1st.

## Tests

137 checks green, including a new case for the diary and monthly summary. Test data is fully synthetic.

## Not verified

The 1.0.10 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
