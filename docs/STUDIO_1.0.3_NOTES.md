# Hisab Studio 2 - 1.0.3 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 26, version name `1.0.3-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Subscription finder**
- Insights > "Subscriptions: repeating charges": Hisab reads your entries on the phone and spots repeating charges - same note, steady amount, steady weekly/monthly/yearly rhythm, at least three times.
- Each hit shows the amount, rhythm, last charge and how many times it has repeated, plus what it costs per month. The total sits on top.

**Round-up saving**
- Settings > Money > Round-up saving: round each expense up to the next 10 or the next 100.
- The spare change is counted into a "Round-up pot" shown in the setting and as a card in Insights. It is counted, never moved - no entries are created, nothing leaves any account.

## Tests

127 checks green, including new cases for the subscription finder (rhythm and amount steadiness) and the round-up pot math, plus the full screenshot suite. Test data is fully synthetic.

## Not verified

The 1.0.3 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
