# Hisab Studio 2 - 1.0.12 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 35, version name `1.0.12-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Income sources**
- Settings > Money > Income sources: your income grouped by category (or the entry note when an entry has no category), biggest first, each with its share of the total.
- A month-by-month list sits underneath. The calendar button at the top picks this month, last 3 months, this year or all time.
- Everything is worked out on the phone from your own entries.

## Tests

139 checks green, including a new case for income grouping, shares and the page. Test data is fully synthetic.

## Not verified

The 1.0.12 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
