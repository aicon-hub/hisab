# Phone test, Stage 2 (build 1.0.39). Not tested on a phone yet.

Back up first (Settings > Backup). Mark each item PASS, FAIL or NOT TESTED with a note.

1. Install over 1.0.38 (same release key). Steps: install the APK. Expected: opens, all entries and photos still there. Result: ____ Note: ____
2. Old photos still open. Steps: open an entry with a photo from before the update, wait one minute, open it again. Expected: photo shows both times. Result: ____
3. New photo. Steps: add a photo from camera and one from gallery. Expected: both show; Remove works. Result: ____
4. Photo after restart. Steps: close the app fully, reopen, view the photos. Expected: still show. Result: ____
5. PIN set and unlock. Steps: Settings > set PIN, lock, unlock. Expected: opens. Result: ____
6. Wrong-PIN lockout survives restart. Steps: enter a wrong PIN 5 times, force-stop the app, reopen, enter the right PIN. Expected: wait message of about 30 s, right PIN refused until it ends. Result: ____
7. Drive backup password warning. Steps: Settings > Encrypt my backup. Expected: warning that a forgotten password cannot be recovered, optional hint field. Result: ____
8. Hint. Steps: set a password and a hint, back up, then restore on Drive. Expected: password prompt shows the hint. A hint that contains the password is refused. Result: ____
9. Restore of an encrypted backup with photos. Expected: data and photos return. Result: ____
10. SMS opt-in. Steps: fresh state, do not open Bank SMS. Expected: no SMS permission popup anywhere. Turn on "Read new bank SMS": popup appears only then. Result: ____
11. About screen. Steps: Settings > About > "Install says a different certificate?". Expected: the steps text shows, also in Malayalam. Result: ____
12. Malayalam. Switch language; open Settings > About and Encrypt my backup. Expected: Malayalam text, no clipping. Result: ____

Regression (short):
R1 Back from every changed screen. R2 TalkBack on Home and Add entry. R3 large font. R4 Drive backup and restore. R5 SMS parsing with a redacted real bank message. R6 PDF statement share. R7 voice input. R8 animations on a low-end phone. R9 20,000-row speed.

Paste-back: "Build 1.0.39: 1 PASS, 2 FAIL (what I saw), 3 NOT TESTED"
