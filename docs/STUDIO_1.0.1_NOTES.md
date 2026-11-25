# Hisab Studio 2 - 1.0.1 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 24, version name `1.0.1-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Daily spending limit**
- Settings > Money > Daily spending limit: set an amount per day (0 turns it off).
- Home gains a "Left today" card: what remains of today's limit, what you have spent against it, and a slim progress bar. The card turns to "Over by ..." in red when the day runs past the limit.
- The card can be hidden from Settings > Look > Customise Home screen.

**Net worth**
- Insights gains "Net worth: accounts and dues": one page with your full position - every account's balance, the money people owe you, the money you owe, and the single net figure on top.

## Tests

124 checks green, including new cases for the Left today card and the net worth total, plus the full screenshot suite. Test data is fully synthetic.

## Not verified

The 1.0.1 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
