# Hisab Studio 2 - 1.0.18 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 41, version name `1.0.18-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Full visual redesign**
A restrained, flat look instead of the generic app style.
- **Flat bottom bar** replaces the floating pill dock: hairline top edge, five equal tabs with small labels, a thin mark under the active tab and a plain ink add button.
- **Home**: the big coloured hero card is gone. Balance and month figures sit directly on the page in ink and grey, with a thin pace line and dividers.
- **Cards and shapes**: hairline outlines, tighter corner radii, no heavy fills.
- **Colour used sparingly**: selected chips are ink, not lime. Transaction icons are plain grey badges with ink icons instead of pastel circles.
- **Insights**: the long outlined buttons are now a clean list with chevrons (Year report, Net worth, Subscriptions, Share month summary).
No features were removed.

## Tests

144 checks green. Test data is fully synthetic.

## Not verified

The redesign was checked on rendered screenshots only; this build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
