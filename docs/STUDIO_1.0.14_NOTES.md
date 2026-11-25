# Hisab Studio 2 - 1.0.14 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 37, version name `1.0.14-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Second profile**
- Settings > Your data > Profiles: add a second profile (called Work by default) and switch between the two. Tap the active profile to rename it.
- The first profile keeps its original database file and is not copied, moved or changed. The second profile is a separate database that starts empty, with no demo data.
- Entries, accounts, categories, budgets and settings are not shared between profiles. The widget, SMS capture and reminders follow whichever profile is active.
- Encrypted backup, CSV export and Drive backup cover the active profile only. The second profile's Drive backup uses its own file name, so it never overwrites the first profile's backup.
- Home shows the active profile's name once a second profile exists. There is no delete-profile option in this release.

## Tests

141 checks green, including a new case that adds a second profile, checks it starts empty and that the first profile is intact after switching back. Test data is fully synthetic.

## Not verified

The 1.0.14 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
