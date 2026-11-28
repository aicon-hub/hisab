# Hisab Studio 2, 1.0.39 test build (Stage 2: security and signing)

- S3: constant-time PIN hash compare. Per-install salt and delay after wrong PINs that survives restart (already there since 1.0.36), now tested.
- S1: attached photos and PDFs are encrypted on the phone (AES-256-GCM, key held by the Android Keystore). Older photos are sealed on next start. APK size change under 0.05 MB.
- S4: docs/SIGNING.md and Gradle signing from environment or local.properties. Release build stops with a clear message if the key is missing.
- S6: SMS permissions are asked only inside the SMS features. They stay declared in the manifest because Android requires it.
- S7: warning and optional hint for the Drive backup password. The saved password is sealed on the phone.
- S11: model and engine SHA-256 values are pinned in source (test added).
- Settings > About: steps for the one-time update after the signing key changed, with Malayalam.
Not done: SQLCipher and photo sealing are untested on a phone. Drive zip still holds plain photo bytes (optionally inside the password-encrypted file).
