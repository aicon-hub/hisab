# Hisab Studio 2 0.6.0 test build

This is an in-place update to Hisab Studio 2 0.4.1 or 0.5.0. Same application ID `in.aicon.hisab.studio2`, same test signing certificate, version code 10. Do not uninstall first. Other old Hisab test apps remain separate.

## Opening and theme

The in-app opening reveal uses the saved accent for the mark and colour-name chip, blends into the selected light/dark background, and crossfades into Home. Total designed reveal is about 1.15 seconds. Disabled Android animations skip it. This is the in-app reveal, not a promise to control every launcher's Android system splash. Matching launcher icon variants from 0.5.0 remain available.

## Balance privacy

Home has a Hide/Show balances control. Settings > Privacy adds persistent global and separate bank, cash, wallet and card masking. Account cards, the account management list, entry account picker, SMS balance displays and account total honour it. If any account is hidden, the combined total is hidden too. An edit form does not prefill a hidden opening balance; leaving it empty preserves the current value.

This does not hide transaction amounts, charts, budgets, CSV or encrypted backup contents after decryption. It is not an app password lock and does not change balances in storage. Per-kind choices remain active after switching the global control off.

Optional Android screen protection blocks ordinary screenshots/recording and protects recent-app previews where supported. Device and OEM behaviour needs a phone test. Turn it off when sending a support screenshot.

## Language

English is the default even on a Malayalam phone. Settings > Language / ഭാഷ offers English and Malayalam. Malayalam is beta: navigation, main screens, common controls, categories, forms and key privacy/backup/SMS warnings are translated; some dynamic and technical help falls back to English. System pickers follow the phone's settings. Data in storage is not renamed or translated. Uses the system font for Malayalam shaping and allows extra font height. Dates use the selected language; numeric entry remains Arabic digits.

Malayalam quick entry supports examples such as `ചായ 40`, `ഓട്ടോ 60` and `പാൽ 30`, alongside the English forms from 0.5.0. Date words `today` and `yesterday` are still English; ISO dates are supported. Category suggestions are rules, not an online AI model.

## Categories

30 category icons, expanded from 15. Added fitness, pets, rent, Wi-Fi, subscriptions, charity, repair, beauty, work, baby, insurance, tax, water, laundry and electronics. No insurance sales or financial products are offered; these are record labels only.

When adding or renaming a category, Hisab suggests an icon from English or selected Malayalam keywords. Tap the suggested-icon button to accept or choose any other icon. Unknown names use Other. Existing categories are not automatically rewritten. Quick entry also uses the new rules and exact custom-category name matching.

## Bank SMS

Still experimental, opt-in and on-device. A bank SMS must state its balance explicitly. Hisab cannot ask a bank for today's real-time balance or infer it from a debit amount. The newest recognised message for a linked account updates a separate dated SMS snapshot. Ledger starting balances and transactions never change because of an SMS.

Improvements:
- More explicit INR/Rs patterns, including `Avl. Bal is Rs. 1,20,000.00`, currency-after-amount, current balance, signed balances, CR and DR.
- Strict amount grouping, precision and range checks; multiple accounts/balances and ambiguous formats are rejected.
- Bank SMS balances always show INR, even when the ledger currency is USD or another option. There is no currency conversion.
- Date and time shown. Snapshots older than seven days are marked older. Even a recent SMS is not guaranteed current.
- Separate latest-bank-SMS and ledger displays in SMS settings.
- New mappings choose Bank-type Hisab accounts. Older mappings stay compatible.
- Balance privacy also masks SMS amounts. Local preview does not update balances.

The 20 sender families and 80 original synthetic fixtures from 0.5.0 remain, with additional synthetic signed/currency-position/rejection cases. This is not real-message validation, bank certification, all-bank coverage or authentication of sender identity. SMS senders can be spoofed. Verify actual available balance in the bank app. Unknown formats, OTPs, credit-card/loan/promotion/failure/hold/limit messages remain rejected. No Internet permission, SMS uploading or SMS sending. Access can be restricted by Android or installer policy; no bypass is provided.

## Related improvements

A local weekly Home nudge encourages encrypted backup for real (not sample) data. No automatic export, scheduled notification or cloud sync is added. It stops for seven days after a successful encrypted document export. A restored backup resets the local last-export record. Existing encrypted backup format and password rules remain unchanged. Backup passwords are not stored or recoverable.

Removed redundant section comments and a no-op lifecycle placeholder. Kept useful safety comments, attribution and license files.

## Verification

45 automated framework/render tests pass across bounded runs: 9 crash regressions, 12 earlier feature tests, 4 ledger logic tests, 2 simulated Android 8 checks, 11 new privacy/language/icon/SMS/reveal tests and 7 screenshot tests. The final small reveal-elevation and bank-picker changes were followed by another 12 new-feature/render checks and 27 core checks. Build and lint pass: 0 errors, 33 warnings.

Inspected actual rendered pixels for the selected-colour reveal, balance masking, privacy options, Malayalam Home and Settings, expanded icon picker, and SMS balance display. Sample data only.

Not run on a real phone or emulator. Actual animation frame pacing, SMS reception/permissions/inbox scanning, screenshot/recording protection, document pickers, external app handoff, launcher icon refresh, OEM fonts and accessibility need phone testing. Malayalam has not had an independent native-speaker review. Encryption has not had an independent security audit. Debug-signed pre-release only, no store submission, no server, no Google sign-in or Drive sync.
