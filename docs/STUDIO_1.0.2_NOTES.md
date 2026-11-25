# Hisab Studio 2 - 1.0.2 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 25, version name `1.0.2-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Spending calendar**
- Insights gains a month heatmap: each day of the month shaded by how much was spent. Darker day, heavier spending; the deepest single day is called out below.

**Weekday vs weekend**
- The "Busiest days" card now compares the averages: what a weekday costs you on average against what a weekend day costs.

**Cash or digital**
- Insights splits the month's spending into cash and digital (bank, card, wallet) by the account each entry used, with a share bar. UPI itself is not tracked - the split reads the account kind only.

**Versus last month, with percentages**
- The month-compare rows now carry the percentage change next to the amount.

## Tests

125 checks green, including a new case for the calendar, weekday-weekend line and cash/digital split, plus the full screenshot suite. Test data is fully synthetic.

## Not verified

The 1.0.2 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
