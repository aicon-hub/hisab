# Hisab Studio 2 - 0.8.0 live bank SMS (test build)

Opt-in "Live and automatic" mode in Bank SMS. On-device only; no SMS text is stored, uploaded or sent. GPL-3.0.

## What it does
- A background receiver reads each new SMS when it arrives, even when Hisab is closed.
- Only recognised bank sender IDs (or senders you added) are read. Phone numbers, shops, OTPs, mandates, offers, card dues and ambiguous messages are ignored.
- The first message for an unseen account ending creates a Bank account such as "HDFC ••1234". Its opening balance is set so the ledger equals the bank balance after the message is saved.
- Balance snapshots are saved with their date. Clear debits become Expense entries with the balance in the note. Person-to-person transfers are filed under People.
- Duplicate alerts for one transaction collapse (UTR, or account, day, amount and person).
- A quiet local notification shows what was added. Amounts are hidden for private accounts.
- The balance mismatch warning and "Add a missing entry" remain for transactions that never sent an SMS.

## Android limits
- Needs RECEIVE_SMS and READ_SMS (plus notification permission on Android 13+). Google Play restricts SMS permissions for store apps; this is a sideloaded test build.
- Android 13+ can block "restricted" permissions for sideloaded apps until you allow them in App info.
- Battery savers, force-stop or restricted standby can delay or stop delivery. Missed messages are caught up by Scan last 30 days or the automatic catch-up on open.
- Not tested on a real phone. Bank formats are synthetic patterns; coverage is not guaranteed.

Reference project pennywiseai-tracker is AGPL-3.0; ideas only, no code copied.
