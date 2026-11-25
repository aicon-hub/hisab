# Hisab Studio 2 - 1.0.9 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 32, version name `1.0.9-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Savings rate goal**
- Settings > Money > Savings rate goal: pick the share of income you want to keep each month.
- Home shows how much of this month's income you have kept so far, and says when you are on track.

**Entry streaks**
- Home shows how many days in a row you have logged at least one entry, from two days up.
- A streak stays alive through today until you have logged something, so it only breaks after a full day with nothing.

## Tests

136 checks green, including a new case for streaks and the savings goal. Test data is fully synthetic.

## Not verified

The 1.0.9 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
