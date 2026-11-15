# Hisab Studio 2: 0.5.0 feature test

Test pre-release, application ID `in.aicon.hisab.studio2`, version code 9.
Signed with the same debug certificate as 0.4.1. Install as an update to
Hisab Studio 2. Do not uninstall it first. Other older test apps are separate.
No production release or store submission.

## Added

- A visual 15-icon picker when adding a category, also available after editing
  a category name. Existing category types and transaction references stay intact.
- Add/change/remove profile picture. Android's document picker selects a photo;
  Hisab copies a centre-cropped 320px JPEG into local settings. No broad gallery
  permission. The photo appears at Home and in Settings, and is included in backup.
- Password-protected full backup and restore using Android's document picker.
  AES-256-GCM with random 16-byte salt, random 12-byte nonce, a 128-bit tag and
  PBKDF2-HMAC-SHA256 with 210,000 iterations. Password is not stored or recoverable.
  Use a strong unique password. Backup includes all ledger tables and settings.
  Restore checks the password and basic data validity, asks before replacement,
  and uses a database transaction so failed validation rolls back. File limit:
  12 MiB; 50,000 rows per table. Restore turns SMS and reminders off until enabled.
  CSV export remains available, but is not encrypted and is not a restore file.
- 17 accents, a scrollable swatch row and a named colour list. A new
  "Match app icon to accent" option chooses the matching launcher-icon alias.
  Wallpaper accent follows Android wallpaper colour on Android 12+ (fallback Lime
  on older Android). The original orange icon is available by turning matching off.
  Launcher caching may delay updates; some Android launcher/theme settings may
  override the displayed colours. An update can leave an old pinned shortcut;
  use the icon in the app drawer if that happens. No launcher behavior is promised
  without real-device testing.
- Quick entry: `auto 60`, `tea 40`, `auto 60 yesterday`, `tea Rs. 40`,
  `salary 52000`, and explicit dates such as `tea 40 2026-11-10`.
  Tapping the check or the keyboard Done action persists one entry, with Undo.
  Unknown or ambiguous amounts are rejected rather than guessed.
- A note from AiCoN, WhatsApp channel, Telegram community and website buttons.
  URLs were read from the live AiCoN homepage, and channel pages were checked.
  Buttons open the destination app/browser; they do not join or follow for you.
- Official WhatsApp green glyph on reminder actions; generic SMS icon. Both open
  a preview confirmation, then the external app with text. Nothing sends itself.

## Bank SMS: experimental, not all-bank certification

Off by default. Explains why access is needed before requesting RECEIVE_SMS.
An optional recent scan separately requests READ_SMS, reads at most 5,000 inbox
messages from 30 days, and stores no raw messages. There is no INTERNET permission,
no server and no outgoing SMS permission. Both permissions can be denied or
restricted by the installer, Android version or device. No bypass is provided.

The user links a bank sender family plus account last-four digits to a Hisab
account. Recognised incoming balance messages update an SMS snapshot and date.
The SMS balance is displayed **separately** from the ledger balance. It does not
rewrite starting balances or create transactions, avoiding double counting of
manual entries. Old or duplicate timestamp messages do not replace a newer value.

Synthetic tests exercise four explicit-balance variants for each of 20 sender
families: SBI, HDFC, ICICI, Axis, Kotak, PNB, Canara, Union, Bank of India,
Bank of Baroda, Indian Overseas, Indian Bank, Federal, South Indian, IDFC FIRST,
YES, IDBI, AU, RBL and Bandhan. This is 80 constructed test cases, **not real
bank-message samples or verified coverage of each bank's current formats**.
The fixtures require an account number/masked ending plus an explicit
INR/Rs/rupee `balance` or `bal` amount. Sender keys are listed in `BankSms.BANKS`.

"Other bank / sender" lets the user explicitly map another alphanumeric sender
and bank name to an account. It still needs the same generic explicit-balance
pattern. That route does not mean every message from every Indian bank works.
A local preview checker shows whether a message is recognised without changing
any balance. OTP, credit-card, offer/loan and ambiguous multi-account/multi-balance
messages are rejected. Unknown sender IDs are ignored unless explicitly linked.
Messages without a balance, non-English formats, alternative sender IDs and
unrecognised wording may not parse. A sender ID is not cryptographic proof of a
bank, so verify against the bank's own app. SMS balance can be stale.

## Verification and limits

33 automated framework/render tests pass. Build and Android lint pass (0 errors,
33 warnings). Automated framework tests cover encryption round trip, wrong password, tampering,
failed-restore rollback, photo-in-backup, category persistence, quick entry through
its actual button, amount/date rejection, 80 synthetic sender-pattern cases,
OTP/unknown/ambiguous rejection, explicit SMS opt-in/link/newness requirements,
custom senders, zero balance and launcher-alias selection for every accent.
The prior keyboard-inset regression and existing logic tests remain in the suite.
Native-rendered screenshots are checked for light/dark forms, category icons,
SMS settings, community/reminder actions, and every launcher icon variant.

**Not tested on a real phone or Android emulator.** Actual SMS permission grants,
receipt and provider scanning; photo/backup file pickers; external app handoff;
and launcher refresh behavior still need a phone test. No claim that all Indian
banks are supported. Backup encryption has not had an independent security audit.
Google sign-in, Drive sync and Malayalam remain absent. Data is on the device;
if you choose a cloud folder in Android's file picker, that provider stores the
selected encrypted backup file. Uninstalling deletes local app data.
