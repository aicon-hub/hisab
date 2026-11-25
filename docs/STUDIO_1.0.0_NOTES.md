# Hisab Studio 2 - 1.0.0 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 23, version name `1.0.0-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Trash bin**
- Deleting an entry now moves it to a trash bin instead of deleting it at once (Settings > Your data > Trash bin).
- Restore any trashed entry, or empty the bin yourself. Entries left in the bin are purged after 30 days, and their bill photos go with them.
- All lists, totals, budgets and reports ignore trashed entries while they rest in the bin.

**Tags on entries**
- Every entry takes free-text tags (tap an entry > Tags, or add them while adding the entry).
- Ledger search matches tags too, so "trip" or "amma" finds everything you tagged.

**Ledger search filters**
- The Ledger search row gains a filter button: pick a category, a from/to date range, and a minimum/maximum amount.
- Active filters show as a badge on the button and combine with text search.

**Year report**
- Insights gains a year report: month-by-month pace bars, the year's totals, the top five categories, and how the year compares with the one before.

**Settings, tidied**
- Settings is now a short hub - profile on top, then six sections: Look, Language and privacy, Money, Reminders, Your data, About. Each opens its own page. Far less scrolling.

**Malayalam numerals**
- Quick add understands Malayalam-Indic digits: "ചായ ൪൦" saves Food 40, the same as "ചായ 40".

## Tests

122 checks green, including new cases for the trash bin round trip and purge, tags, ledger filters, the year report, the Settings hub and Malayalam digits, plus the full screenshot suite. Test data is fully synthetic.

## Not verified

The 1.0.0 changes are code-reviewed and unit-tested but this exact build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
