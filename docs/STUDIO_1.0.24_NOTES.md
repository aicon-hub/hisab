# Hisab Studio 2 - 1.0.24 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 47, version name `1.0.24-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0.

## What is new

**Ask Hisab (rule-based chat)** - Settings > Ask Hisab, or Insights > Ask Hisab. No AI model, no download, no network: it reads your own entries with fixed rules, in English, Malayalam and Manglish.
- Questions: spending (today, yesterday, this/last week, month, year, a named month, last 7/30 days, all time), by category, by note word (for example "tea"), income, what is left, top categories, biggest expenses, last N entries, how many entries, daily average, compare with last month, budgets, account balances, who owes me / does Ravi owe me.
- Changes (always shown first, nothing is saved until you tap Confirm): add an entry ("tea 20", "add auto 60 yesterday", "ശമ്പളം 5000"), delete the last entry (goes to Trash), change the amount of the last entry ("change last to 500").
- Examples: "ee maasam ethra chilavu", "ഈ മാസം എത്ര ചെലവായി?", "ഇന്ന് ഭക്ഷണത്തിന് എത്ര", "ravi kadam undo".
- It does not guess: if it does not understand, it says so and shows examples.

## Tests

Rule-based chat checked on synthetic entries (English, Manglish, Malayalam, add/delete/edit confirmation). Full suite green. Test data is fully synthetic.

## Not verified

Checked on unit tests only; this build has not run on a real phone. Phrasings beyond the list above may not be understood yet. While the consent screen is in Google's Testing mode, only registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission.
