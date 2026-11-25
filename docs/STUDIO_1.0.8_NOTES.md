# Hisab Studio 2 - 1.0.8 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 31, version name `1.0.8-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Weekly budgets**
- Settings > Money > Budgets, tap a category, choose Weekly limit.
- The row shows this Monday-to-Sunday week's spend against the weekly limit, next to the monthly figure.

**Budget carry-over**
- Per category, switch on Carry unspent to next month.
- Whatever you did not spend of last month's limit is added to this month's limit (one month back, it does not stack). Home's safe-to-spend amount and the over-budget alert use the carried total.

## Tests

135 checks green, including a new case for weekly limits and carry-over. Test data is fully synthetic.

## Not verified

The 1.0.8 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
