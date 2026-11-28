# Phone test, build 1.0.38 (Stage 1)

Mark each line PASS, FAIL or NOT TESTED, and add a short note. Make a backup first.

1. Install over 1.0.37 (no uninstall). Open the app. Expected: all entries still there. [ ]
2. Set a PIN, close the app, reopen, enter it. Expected: opens. [ ]
3. Enter a wrong PIN 5 times. Expected: a wait message of about 30 seconds, even for the right PIN. Close the app fully and reopen: the wait is still there. [ ]
4. Split a bill: total 101, you plus 2 people picked from People, "They owe you". Expected: your share 34, first person 34, second person 33 (sum 101.00). Then total 100 with 3 people (you plus 2): 33.34, 33.33, 33.33. [ ]
5. Repeats: add a monthly repeat on the 31st. Confirm it three times. Expected: 28 Feb (or 29 in a leap year), then 31 Mar, then 30 Apr. [ ]
6. Backup error: turn on airplane mode, tap Back up now to Drive. Expected: error message stays until you tap Dismiss. [ ]
7. Wrong backup password on restore. Expected: message stays until Dismiss, data unchanged. [ ]
8. Home, Ledger, Insights, People: scroll to the bottom. Expected: last row is fully above the dock on each screen. Try with gesture navigation and with 3-button navigation. [ ]
9. Empty screens (use a second profile or Settings, sample off): Home, Insights, Ledger, Goals, Repeats each show one button that opens the right form. [ ]
10. Quick add hint on a small phone: the hint reads "Type it: tea 40" and is not cut off. [ ]
11. Turn off Google voice typing (or use a phone without it), tap the mic. Expected: message tells you to turn it on. [ ]
12. Settings, Last problem: opens, says no problem recorded. [ ]
13. Switch language to Malayalam: Dismiss, empty-state buttons and the hint show Malayalam. [ ]

Regression (short): add an expense, undo it; open Drive backup and restore; share a PDF statement; SMS parse of a redacted bank message; voice input; TalkBack on Home and Add entry; large font; animations on a low-end phone; open every Settings screen and press Back from each.
NOT TESTED by me: everything above. Nothing was run on a phone.

Paste back like this: Build 1.0.38: 1 PASS, 2 PASS, 3 FAIL (what I saw), 4 NOT TESTED ...
