# Hisab Studio 2, 1.0.31 test build

Google Drive backup (hidden app-data folder of the user's own Drive, no server of ours).

- Backup is automatic and follows every change (batched, at most one upload a minute), as before.
- New switch "Include photos and PDFs" (default on).
- New switch "Encrypt my backup with a password" (default off). Off: restore needs only the Google sign-in. On: the Drive copy is AES-256-GCM encrypted (PBKDF2, 210000 rounds). The password is kept only in the app's private storage on the phone, never in the backup, and cannot be recovered.
- Restore: if the Drive copy is encrypted, the stored password is tried first, then the app asks for it. A wrong password changes nothing.
- If Google asks for sign-in again (the Testing consent screen expires sign-ins after about 7 days), a manual backup now opens Google's prompt and retries.
- Unverified on a real phone. Tests use invented data only.
- Hisab AI answers questions about Hisab itself (creator card, licence, capabilities per section following the Read and Write switches, privacy) from built-in text, not from the model.
