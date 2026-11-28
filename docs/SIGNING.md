# Signing Hisab Studio 2

Release builds are signed with one key. The key is not in this repo and must never be committed. Do not make a second key: an app signed with a different key cannot update the first one.

## The key
- Type: PKCS12 keystore, alias `hisab`.
- Certificate SHA-1 of the current key: `ab9d4f4acf51c29f3d179c374d3c160733c2dba4`.

## Make a key (only for a brand new project)
```
keytool -genkeypair -v -keystore hisab-release.jks -storetype PKCS12 -alias hisab \
  -keyalg RSA -keysize 4096 -validity 10000
```
Pick a long password. Write it down in a password manager.

## Back it up in two places
1. An encrypted copy in your own cloud drive (private, not shared).
2. An offline copy on a USB stick or another computer, kept apart from the first.
Keep the password with each copy, but not in the same file. Check each copy by running
`keytool -list -keystore <copy> -alias hisab` and confirming the SHA-1 above.
If the key and all copies are lost, nobody can update installed apps. Users must back up, uninstall and reinstall.

## Give the key to Gradle (not committed)
Either set environment variables:
```
export HISAB_STOREFILE=/path/to/hisab-release.jks
export HISAB_STOREPASSWORD='...'
```
or add to `local.properties` (already in .gitignore):
```
hisab.storeFile=/path/to/hisab-release.jks
hisab.storePassword=...
```
Optional: `HISAB_KEYALIAS` or `hisab.keyAlias` (default `hisab`), `HISAB_KEYPASSWORD` or `hisab.keyPassword` (default same as the store password).

Then run `./gradlew :studio:assembleRelease`. If the key is missing, the release build stops with the message
"Release signing key not found" and points here. Debug builds and tests never need the key.

## Check the result
```
apksigner verify --print-certs studio/build/outputs/apk/release/*.apk
```
The SHA-1 must match the one above.

## What happens to existing users when the key changes
Android refuses to install an update signed with a different key ("signed with a different certificate"). The user must make a Hisab backup, uninstall once, install the new build and restore. This happened once, when builds moved from the debug key to this release key (1.0.37). Changing the key again would repeat it, so do not.
