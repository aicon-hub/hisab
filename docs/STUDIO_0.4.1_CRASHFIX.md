# Hisab Studio 2: 0.4.1 crash-fix test

This is a test pre-release, not a production release. No store submission.

## What crashed

Opening a keyboard while a form or settings page was open could exit the app.
The window-insets callback rebuilt the whole shell and attached retained sheet
holders without first removing them from their old parent. Android raised
`IllegalStateException: The specified child already has a parent` in
`MainActivity.build`.

A regression test reproduced that exception against 0.4.0 before the fix.

## Changes

- System-bar and keyboard inset changes now update geometry in place. They do
  not rebuild the shell or replace focused fields. Typed text, selection and
  focus survive keyboard open/close events.
- Explicit theme rebuilds detach retained layers before moving them.
- Sheets scroll when the available space is small, including above a keyboard.
- The contact picker supplies the phone-contact MIME type. Missing picker apps,
  missing handlers for SMS/browser/export, and incomplete contact values are
  handled without an uncaught exception. Real device/OEM picker testing is still
  needed.
- Avatars handle blank names and emoji without slicing a surrogate pair.
- Configuration changes retain an open form's text.

## Installation and data

Application ID: `in.aicon.hisab.studio2`. Launcher label: **Hisab Studio 2**.
Version code 8. A fresh debug key signs this build. It installs beside 0.4.0
(`in.aicon.hisab.studio`), rather than requiring an uninstall. Existing apps and
their local data are not modified. Data is not migrated between these two apps.
Do not uninstall the old test app if it contains entries you want to retain.

This build starts with sample data. Google sign-in, Drive sync, backup/restore
and Malayalam are still absent. Data remains on this device. CSV export is not
a restorable backup.

## Verification

- Regression reproduced before the fix and passes after it.
- Automated Android framework tests cover keyboard inset changes, text/focus/
  selection preservation, explicit rebuilds, profile save, theme/font/currency
  changes, account/budget/goal/bill forms, all entry types, split bill, contact
  cancellation, missing external apps, empty names and configuration changes.
- Native-rendered light/dark screenshots inspected, including the name form
  before/after simulated keyboard insets and after saving.
- APK build and Android lint run. Lint has no errors; warnings remain.
- **Not run on a real phone or Android emulator.** Framework tests and renders
  do not prove device/OEM keyboard, contacts or permission behavior.
