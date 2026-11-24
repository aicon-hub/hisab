# Hisab Studio 2 - 0.9.0 production-readiness build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 13, version name `0.9.0-pro`. Keep an encrypted backup before updating (Settings > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**App lock (PIN)**
- Privacy > App lock: set a 4-8 digit PIN. Hisab asks for it when the app opens in a fresh session.
- Only a salted SHA-256 check is stored on the phone, never the PIN. Wrong-PIN tries just shake you back to the field.
- Change or turn off needs the current PIN. If the PIN is forgotten, clearing app storage is the only way back in - keep an encrypted backup first.
- Back button on the lock screen leaves the app instead of slipping past it.

**Monthly statement (PDF)**
- Settings > Monthly statement (PDF): pick any of the last six months, get a tidy PDF with income, spent, kept, lent, borrowed and every entry of the month. Share it anywhere.
- Generated on-device into the app's private folder and shared with a read grant. Nothing is uploaded.

**CSV import**
- Settings > Import entries from CSV reads back the exact format Hisab exports.
- Accounts, categories and people named in the file are created when missing. Account kind is guessed from the name (cash/card/wallet/bank).
- Rows identical to an existing entry are skipped, so importing the same file twice adds nothing. Bad rows are skipped and counted.
- Note: starting balances are not part of the CSV - set them again in Accounts after a fresh import.

**Bills and repeats, upgraded**
- Weekly, monthly or yearly repeats.
- Pick the account and category when adding.
- Pause a repeat without deleting it; resume any time. Paused repeats show in the list and stay off the Home "Coming up" card.

**Full customisation (Hisab's hallmark)**
- Custom accent colour: three sliders, any colour you like, with live preview. Named palettes stay.
- Text size: Compact, Standard or Large across the whole app.
- Customise Home screen: switch every Home section on or off (top card, quick add, shortcuts, coming up, accounts, budgets, week chart, people, recent entries). "Show everything" resets.
- Date format: 24 Nov, Nov 24 or 24.11.2026.

**Small things**
- Entry detail now has Duplicate: one tap copies an entry (with undo).
- People can be edited (name and phone) instead of deleted and re-added.
- Insights has Share month summary: a short text recap (income, spent, kept, top categories) for any chat app.
- Long-press the launcher icon for Add expense / Add income shortcuts. A Hisab quick-settings tile opens the app from the pull-down shade. (Shortcuts live on the default launcher icon; if you switch the themed icon alias, re-pin them.)

## Tests

87 checks green: the new ProTest suite (PIN hashing and lock flow, CSV round-trip with quoting, duplicate-safe re-import, bad-row skipping, statement math, weekly/monthly/yearly repeat advance, pause persistence, month summary text, date formats, text scale, custom accent, home section flags, full-page smoke) plus every earlier suite (SmsLive 8, SmsTx 14, logic, features, polish, legacy, crash, shots). Lint clean.

## Not verified

Not tested on a real phone: the lock screen against every OEM, the quick-settings tile, launcher shortcuts on every launcher, PDF rendering of Malayalam notes in external viewers, and FileProvider sharing to every target app. Bank SMS coverage stays synthetic-pattern, experimental. Framework tests and view renders pass; they are not device testing.

## Previous builds

- 0.8.0-live: live automatic bank SMS (opt-in RECEIVE_SMS), docs/STUDIO_0.8.0_LIVE.md
- 0.7.0-sms: bank SMS balances and expense import
- 0.6.0-privacy: privacy controls, encrypted backup
- 0.5.0-features: goals, budgets, repeats
- 0.4.1-crashfix: keyboard-inset crash fix
