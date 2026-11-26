# Hisab Studio 2 - 1.0.22 test build

Test build on the `studio-redesign` branch. Same app ID `in.aicon.hisab.studio2` and same signing certificate as every earlier Studio 2 build, so it installs in place over them. Version code 45, version name `1.0.22-pro`. Keep an encrypted backup before updating (Settings > Your data > Encrypted backup).

Everything stays on the phone. No server, no account, no ads, no tracking, GPL-3.0. Opt-in permissions only.

## What is new

**Optional Malayalam voice (Indic Tiny TTS)**
- Settings > Language and privacy > Spoken feedback. Optional one-time download of about 91 MB from huggingface.co, checksum-verified, deletable.
- Runs offline on the phone with ONNX Runtime. Numbers are spoken as Malayalam words; English words inside a sentence are skipped. Falls back to the phone's voice on any failure.
- Credits: Indic Tiny TTS by sulabhkatiyar (CC BY 4.0), Rasa dataset by AI4Bharat, Vocos vocoder (MIT).
- App size grows by about 18 MB (ONNX Runtime, arm64).

## Tests

152 checks green. Test data is fully synthetic. The Java voice pipeline matches the Python reference waveform to 0.00003 on a computer.

## Not verified

These changes were checked on rendered screenshots only; this build has not run on a real phone; on-device behaviour remains the true test. While the consent screen is in Google's Testing mode, only the registered test accounts can sign in to Drive backup. Debug-signed pre-release, no store submission. arm64 phones only.
