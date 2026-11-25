# Hisab Studio 2 - 1.0.13 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 36, version name `1.0.13-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Travel currency**
- On the Add entry screen (spent or income), tap Foreign. Enter the amount in the other currency, its three-letter code and the rate (how much of your currency one unit is worth).
- Hisab fills the amount in your own currency, rounded to the nearest paisa, and adds the original to the note, for example `USD 12.50 @ 83.5`.
- The code and rate are remembered for next time. Rates are typed by you and never fetched online, so check them before saving.
- Entries are stored in your own currency as before, so totals, backups and exports are unchanged.

## Tests

140 checks green, including a new case for the conversion maths and rate checks. Test data is fully synthetic.

## Not verified

The 1.0.13 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
