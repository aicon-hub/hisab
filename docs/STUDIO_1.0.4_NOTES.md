# Hisab Studio 2 - 1.0.4 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 27, version name `1.0.4-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Wishlist**
- Settings > Money > Wishlist: save the things you want with their prices.
- Each wish is weighed against what you can spend: within today's safe amount when budgets are set, or against what you kept this month.
- Tap a wish to edit or remove it.

**Trips and events**
- Settings > Money > Trips and events: name a trip (dates optional), then tag its entries with trip:name in the Tags field.
- Each trip card adds up its entries, and with dates set it shows the per-day spend. Removing a trip keeps the entries.

## Tests

129 checks green, including new cases for the wishlist round trip and trip totals from tags, plus the full screenshot suite. Test data is fully synthetic.

## Not verified

The 1.0.4 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
