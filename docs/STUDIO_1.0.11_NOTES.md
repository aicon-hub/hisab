# Hisab Studio 2 - 1.0.11 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 34, version name `1.0.11-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Share statement**
- On a person's page, tap Share statement to make a PDF of everything lent, borrowed and repaid with that person, with the running balance after each entry.
- It is written on the phone and handed to the share sheet. Nothing is uploaded by Hisab.

**Pay by UPI**
- Shown when you owe the person. Tap Pay by UPI, enter their UPI id once (it is remembered for that person on this phone), and your UPI app opens with the id, amount and name filled in.
- Hisab never moves money. You review and confirm in your UPI app, and if no UPI app is installed you get a message instead.

## Tests

138 checks green, including a new case for the person statement and UPI link. Test data is fully synthetic.

## Not verified

The 1.0.11 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
