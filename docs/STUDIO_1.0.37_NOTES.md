# Hisab Studio 2 1.0.37 (test build)

- The ledger database is encrypted with SQLCipher. The key is random (256-bit) and wrapped by the Android Keystore. On first launch an old plain file is copied into an encrypted file, the row counts of all tables are compared, the old file is kept aside until the encrypted file has opened once, then overwritten and deleted.
- Signed with a new release key. Existing installs signed with the debug key cannot be updated in place: back up, uninstall, install, restore.
- APK is about 10 MB (SQLCipher library 5.8 MB).
- Not tested on a phone. Tests use plain SQLite on a computer.
