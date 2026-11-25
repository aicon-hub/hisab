# Hisab Studio 2 - 1.0.7 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 30, version name `1.0.7-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Search by amount**
- Type a number in the Ledger search box ("499", "1,234" or "499.50") and every entry of exactly that amount shows, alongside any note, tag or category matches.

**Tag manager**
- Settings > Money > Tags lists every tag with how many entries use it.
- Rename a tag everywhere; give it the name of another tag and the two merge.
- Remove a tag from every entry (entries are never deleted).
- Show jumps to the Ledger filtered to that tag.

## Tests

134 checks green, including new cases for amount search and for rename, merge and remove of tags. Test data is fully synthetic.

## Not verified

The 1.0.7 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
