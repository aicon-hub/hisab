# Hisab Studio 2 - 0.7.0-sms test build

Branch `studio-redesign` only. Test build, not a store release. Application id `in.aicon.hisab.studio2`, versionCode 11, versionName `0.7.0-sms`, minSdk 26. Installs as an in-place update over 0.4.1 / 0.5.0 / 0.6.0 test builds (same debug signing key).

## What is new

### Automatic due reminders (People)
- Opt-in. Once a day (one inexact alarm, nothing else in the background) Hisab checks who still owes you and reminds each person at most once every 3, 7, 14 or 30 days (your choice).
- Send by WhatsApp or SMS:
  - WhatsApp: a notification arrives; tapping it opens the chat with the message written. Android does not let apps send WhatsApp messages automatically, so you press send there.
  - SMS: the phone sends the message itself when SMS is chosen and the send permission is granted. Carrier SMS charges apply. If permission is missing or sending fails, a notification opens the SMS composer instead.
- Only people who currently owe you and have a phone number are included. Settled people are never reminded. At most 3 reminders per daily check; the rest follow the next day.
- Entry points: People page and Settings > Reminder.

### Indian phone number fix
- 10-digit mobile numbers (and `0`-prefixed or `91`-prefixed variants) are normalised to `+91...` when a person is added, imported from contacts, or used for WhatsApp / SMS links. WhatsApp now opens the chat instead of an "Invite to WhatsApp" page.
- Existing saved numbers are migrated once on first launch.

### Bank SMS: fuller setup
- Enabling asks for receive + read SMS permission in one step, then walks through linking an account and offers the first 30-day scan.
- Quiet catch-up: after app start, at most every six hours, the last 3 days of inbox are re-checked locally so balances and expenses stay current without a manual scan.
- Balance mismatch flag: when the bank SMS snapshot and the ledger balance differ, the difference is shown with an "Add a missing entry" action. Transactions that arrive with no SMS never appear automatically; this is how you spot and reconcile them.

### Auto-add expenses from bank SMS (opt-in)
- Clear debit messages for linked accounts create Expense entries on this phone.
- Person-to-person transfers (UPI to a name or a `handle@bank` VPA) are filed under a new "People" expense category. Merchant debits are matched to existing categories by name keywords, falling back to Other.
- Dedupe: several alerts for one transaction (debit alert, balance, UPI confirmation) collapse into a single entry, keyed on account + day + amount + counterparty, and on the UPI UTR/reference when present.
- Rescans never duplicate. Deleting an imported entry is permanent: it is not re-added.
- OTPs, e-mandates, future debits ("will be debited"), credits, failed/declined and promotional messages are ignored. Messages with several different amounts are ignored as ambiguous.
- Imported entries are listed under "Imported from SMS" on the Bank SMS page for review.

## Privacy and licensing
- Everything stays on-device. No server, no analytics, raw SMS is never stored or uploaded. GPL-3.0.
- Parsing ideas (mandate/future-debit rejection, "credited" as the only certain incoming keyword, stop-delimited counterparty names, stable dedupe keys) were informed by pennywiseai-tracker (AGPL-3.0, github.com/sarim2000/pennywiseai-tracker). Its AGPL-3.0 licence is not copied into Hisab: no code was taken; the implementation here is original and stays GPL-3.0.

## Honest limitations
- Not tested on a real phone or emulator. Framework tests and native renders only. SMS receipt/permissions, alarm timing, notification taps, WhatsApp/SMS composer hand-off and contact picker need device testing.
- Bank SMS formats are synthetic-pattern based; coverage for every Indian bank is not guaranteed. Sender IDs can be forged: verify balances in your bank app.
- Person vs merchant detection is heuristic and can misclassify; review "Imported from SMS".
- Automatic SMS sending depends on carrier and SIM; charges are the user's.
- Malayalam remains a beta interface translation; new screens have partial translation with English fallback.
