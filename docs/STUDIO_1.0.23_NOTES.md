# Hisab Studio 2 - 1.0.23 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 46, version name `1.0.23-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0.

## What is new

**Hisab's own keyboard** (Settings > Language and privacy > "Type with Hisab's own keyboard", on by default)
- Used in all Hisab text fields. English, Malayalam (InScript style, with a shift page) and Manglish (type "paisa", get പൈസ). The language key cycles the three. Number fields get a number pad.
- Suggestions come from your own categories, people, accounts, tags, trips and note words, plus a small built-in list of finance words.
- Password and PIN fields use the English layout only, with no suggestions.
- Manglish is rule-based and not perfect; the suggestion strip offers the correct word for common cases.

**Spoken feedback removed from view.** The "Read out" buttons and the Spoken feedback settings are gone, and the 91 MB Malayalam voice and ONNX Runtime are removed. The app is back to about 4.6 MB. Mic dictation for quick entry stays.

**Last problem** (Settings > About). If the app closes because of an error, the error is saved on the phone only (no entries or amounts). You can read it and share it by hand.

## Tests

161 checks green. Test data is fully synthetic.

## Not verified

Checked on rendered screenshots and unit tests only; this build has not run on a real phone, and the Malayalam key layout is from memory, so it may differ from what you expect. On-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission.
