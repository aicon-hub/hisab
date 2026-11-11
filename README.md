# Hisab

An original, on-device Android expense tracker. Work in progress, not a finished release.

The app has no internet permission, analytics or ads. It stores records in local SQLite. English and Malayalam interface options are included; category hints use English-only local keywords and earlier exact-note matches, with user confirmation. There is no AI model.

## Current scope

Manual expense, income and transfer entries; fixed Cash/Bank/Wallet accounts; budgets; savings goals; current-month reports; entry search/edit/delete; equal bill splitting; CSV export; passphrase-encrypted file backup/restore implementation; light/dark themes.

Google Drive backup, receipt/voice entry, notification capture, shared ledgers and recurring bills are not implemented.

## Build

Use Android Studio with JDK 17, Android SDK 35, and Gradle 8.9. Open the project and build the debug app. No wrapper binary is included in this prepared source. This Gradle configuration has not been exercised yet. The accompanying APK was built directly with Android build tools 35.0.0 and Java 17 from the same Java/resources, using d8 and apksigner, not from this Gradle configuration.

## Verification status

The ledger helpers passed 34,570 JVM assertions, and category hints passed 14 synthetic rule/abstention examples. These are not real-world categorisation benchmarks. Earlier app screens rendered on an Android 8 emulator, including Malayalam and dark mode. The final renamed and re-iconed APK has NOT completed device visual verification, save/export/restore workflow testing, process-death tests, or Android 15 testing. The emulator repeatedly failed to complete system/package-manager work. Do not describe this draft as validated, secure, perfect, or ready for public use.

The backup implementation uses AES-GCM with a random salt/nonce and PBKDF2-HMAC-SHA256, 210,000 iterations. Its Android file workflow has not been verified. Loss of the passphrase means no recovery. CSV exports are unencrypted. Uninstalling deletes local data, and OS backup is disabled.

## Identity

Hisab, by AiCoN. Other apps already use the name Hisab; no trademark clearance is claimed. The developer website is https://aicon-hub.github.io/ . The repository link is https://github.com/aicon-hub/hisab and appears only in About.

## Distribution

Private test package `in.aicon.pocket.test`. Debug/test signing only. It cannot replace the live AiCoN reader. No release or production signing key is in this source. Do not add credentials, personal data or private keys to a repository.

No open-source licence has been selected yet. Publishing source does not itself grant a licence; the owner must select one before claiming the project is open source.

## Source archive

`Hisab-source.zip` contains the full original Java and resource tree, project files, and helper tests. Unzip it to work on the project. It includes no licence and no signing keys.

## Downloads

[Untested pre-release](https://github.com/aicon-hub/hisab/releases/tag/v0.1.0-test). Download `Hisab-0.1.0-test.apk` for the test APK and `Hisab-source.zip` for the complete source project. GitHub automatic Source code ZIP/TAR assets contain the repository README only, not the app source. Use the separately attached `Hisab-source.zip` instead.
