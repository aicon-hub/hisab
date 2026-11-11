# Hisab V2 preview source

Original Android personal ledger, separate from the AiCoN reader. This source checkpoint is not a release. Read STATUS.md for actual implemented features, test coverage and remaining blockers. The launcher remains local-only. Account UI, foreground sync and background orchestration are compiled behind CloudConfig.ACCOUNT_UI_ENABLED=false. The manifest intentionally has no INTERNET permission. The new account screens and background coordinator have framework/fake tests, not real Google/Drive or OS scheduling verification.

## Build

Use Java 17, Gradle 8.14.3, Android SDK platform 36 and the build tools selected by Android Gradle Plugin 8.13.2. Set ANDROID_HOME or create an untracked local.properties with sdk.dir pointing to your SDK. Then run `gradle :app:assembleDebug` from this directory. The first build downloads dependencies from Google's Maven repository and Maven Central. No SDK, dependency cache, signing key or credentials are included.

`gradle :app:testDebugUnitTest` runs the Android-framework simulation tests. They do not replace real-device, Google sign-in, Drive, keyboard or launcher QA. The root Java *Test.java programs are independent pure checks in the default Java package; compile them with UTF-8 alongside the relevant pure source classes (Identifiers, Ledger, CategoryHints, SyncModel, AccountScope, LedgerIntegrity SyncEngine and ResetBarrier).

The registered Android OAuth client is tied to a specific private preview signing certificate and package in.aicon.pocket.test. An ordinary developer debug key is not interchangeable with it. The internal checkpoint APK certificate has been verified against that registration. Rebuilt APKs must still have their actual certificate verified before attempting account tests. No client secret or private signing material belongs in this archive.

Do not publish or call this V2 ready. Existing public 0.1.0 is a separate untouched checkpoint.


## Current preview appearance and login status

Dynamic colour has a light/dark choice and a pure-black toggle for OLED/AMOLED. Turn the toggle off for a softer dark surface on LCD. Display type is not detected automatically. Liquid Glass is a separate dark-only appearance with true-black ground and original static glass-edge rendering. Accent colours, launcher colours, categories, accounts, people, budgets, goals and entry defaults remain editable. Rendering is a preview, not a claim of perfect glass or measured power savings.

The Google login/account screen and own-Drive integration are implemented in source but **disabled in this preview**. Actual Android Google sign-in, Drive sync and background execution have not been verified. The manifest has no INTERNET permission. No working sign-in is promised by this upload.

See STATUS.md for newer focused checks and unresolved work. VERIFICATION.md records the earlier 78-test checkpoint, not a complete final-theme regression run. No open-source licence has been selected; source publication alone does not grant a licence.
