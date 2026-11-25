# Hisab Studio 2 - 1.0.17 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 40, version name `1.0.17-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**PDF import of a Hisab export table**
- A PDF or screenshot of Hisab's own exported table is recognised and imported with type, account, category and note. Rows already in the ledger are skipped. Use Settings > Import entries (CSV) when you still have the CSV, as it is exact.

**Profiles**
- The profile name on Home is a one-tap switch button, and the switch fades out and in with a short buzz.

## Tests

144 checks green, including cases for the exported table reader and the profile switch button. Test data is fully synthetic.

## Not verified

The 1.0.17 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
