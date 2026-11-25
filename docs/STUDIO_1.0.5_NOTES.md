# Hisab Studio 2 - 1.0.5 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 28, version name `1.0.5-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Share a ledger**
- Settings > Your data > Share a ledger: makes a shared-ledger file of all your entries that another Hisab user can import on their phone.
- Unlike the CSV export it keeps tags (trips included) and entry kinds exactly.
- Import a shared ledger reads a file someone sent you. Entries that already exist are skipped, so sharing back and forth between two phones never duplicates. Nothing is uploaded anywhere; the file travels however you send it.

**Year in review**
- Settings > Money > Year in review: one card for the year - income, spent and kept, how many days you kept notes, your biggest spending day, and where the money went with shares of the year's spend.

**Quick-add notification**
- Settings > Reminders > Quick-add notification: a quiet, always-on notification; one tap opens a new expense sheet from anywhere.

**Malayalam words, typed in English letters**
- The quick-add bar now reads everyday Malayalam words written in Latin letters: chaya 40, paal 55, marunnu 120, innale auto 60, shambalam 50000. Works offline, on the phone only.

## Tests

132 checks green, including new cases for the shared-ledger round trip with tags, the year-in-review maths, and transliterated quick-add, plus the full screenshot suite. Test data is fully synthetic.

## Not verified

The 1.0.5 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
