# Store readiness, 12 November 2026

Preparation only. No submission, public listing request or acceptance is claimed. The owner must give a separate go before any request. GPL version 3 was selected for the original Hisab code/assets; bundled fonts retain OFL notices and third-party dependencies retain their licences.

## Prepared

- English Fastlane title, 72-character short description, full description, per-versionCode change notes, original icon and four native-render screenshots under `fastlane/metadata/android/en-US/`.
- Screenshot data is synthetic; images are framework renders, not evidence of a tested physical device. Replace/recheck against the eventual store build.
- Gradle 8.14.3 wrapper with distribution SHA-256, fixed AGP/dependency versions, and ignores for keys/local build output.
- Build command without private signing/dependency overrides succeeded on the existing developer environment: `:app:assembleDebug`. No clean-machine or independent byte-comparison result exists.

## F-Droid: not ready to submit

1. Current Google sign-in depends on `play-services-auth`, Credentials Play Services auth and Google Identity. F-Droid forbids proprietary Play Services. Prepare a separate libre/offline build variant that removes those libraries, Google connection/refresh/revoke paths and Google UI, while preserving the local ledger/export/backup flows. A runtime off switch is insufficient because the libraries remain packaged. This variant has not been implemented or built yet.
2. Audit every dependency and transitive dependency plus original asset/font provenance. Retain applicable licence notices and verify the finished APK with repository scanners. No scanner-clean claim exists yet.
3. Choose a stable production application ID and signing/release strategy with the owner. Current ID and certificate are preview-only. Do not silently change the ID or break existing test data upgrades. A final production build must not be debuggable/testOnly.
4. Record and pin the exact FLOSS JDK, SDK/build-tools and environment. Add dependency verification/locks after reviewing the resolved graph, not by copying unchecked checksums. Build twice from a fresh clone at one immutable commit in independently prepared environments. Compare unsigned APK content and report differences. Wrapper pinning alone is not reproducibility.
5. Reproducible upstream-signature distribution additionally needs an independent rebuild and supported APK-signature-copy verification. Never publish the preview signing key or any credentials.
6. Complete real-phone functional/backup/restore/upgrade/privacy/background checks before advertising usable functionality. Review the history/maintenance requirement (three months, with listed exceptions). Inclusion is a maintainer decision.
7. Only after a reviewed libre build exists, prepare an executable F-Droid build-metadata YAML with the actual versionCode/commit/variant. Do not ship a placeholder that looks installable or claim an F-Droid badge.

## IzzyOnDroid: separate policy obstacle

The current official policy rejects vibe-coded apps and says apps with code partly or fully generated using generative AI are likely rejected. Hisab includes AI-assisted generated code. Do not conceal this origin or claim eligibility. The policy allows generated README/changelog text, not generated app code. This is independent of licence and Google dependencies. If the owner later requests an eligibility inquiry, disclose the development method accurately, without credentials, private conversations or internal development records.

Other Izzy criteria include libre licensing, accessible source and signed APK release assets, Fastlane descriptions/icon/screenshots, a stable package ID and no debug/testOnly flags except limited stated testing exceptions. Passing some criteria does not override the AI-code restriction or guarantee acceptance.

## Preliminary anti-feature review, not a scanner result

| Area | Current source finding | Required final check |
| --- | --- | --- |
| NonFreeDep / NonFreeNet | Optional Google/Drive path uses Play Services and Google APIs. | Remove proprietary libraries from libre variant; reassess any remaining network service flags under current policy. |
| Ads / Tracking | No first-party ad, analytics, referral or tracking feature is implemented. | Scan final dependency graph/APK and inspect network behaviour. Do not label this a completed independent audit. |
| Permissions | INTERNET, READ_CONTACTS, POST_NOTIFICATIONS and RECEIVE_BOOT_COMPLETED support optional features; no SEND_SMS, SMS reading, accessibility or notification listener. | Inspect the final merged variant manifest, consent timing and runtime behaviour. |
| Data | Phone-first storage; optional own-Drive is not application-encrypted backup. CSV/PDF are readable. Phone-only profile/contact/pay-by data is outside backups. | Test these exact boundaries, account changes, revoke/reset and file contents on a phone. |
| Updates | No in-app binary updater is implemented. | Verify final APK and all dependencies do not add one. |

Anti-feature names/decisions belong to store reviewers and the eventual variant. Do not submit a guessed final label set now.

## Current official references

- https://f-droid.org/docs/Inclusion_Policy/
- https://f-droid.org/docs/Reproducible_Builds/
- https://f-droid.org/docs/Build_Metadata_Reference/
- https://izzyondroid.org/docs/general/AppInclusionPolicy/
- https://izzyondroid.org/docs/general/Fastlane/
- https://izzyondroid.org/docs/devpractices/
