# Hisab Studio 2, 1.0.32 test build

Fixes found from a screen recording on a phone running 1.0.31.

- Settings: opening a section (Look, Hisab AI, Your data, ...) used to replace the Settings list, so Back went straight to Home. Sections now open on top of the list; Back returns to Settings, and the second Back goes Home. Changes made inside a section (switches, pickers, Drive backup options) refresh the section in place.
- Home mic: the old icon path drew as a blob. New clean microphone, a lit accent disc with a soft gradient, two pulse rings and live sound bars while listening (static when animations are reduced).
- Checked by tests that tap the real screens (open Settings, tap a row, flip a switch, press Back) and render the Home mic. Not run on a phone.
