# Hisab Studio 2, 1.0.35 test build

Motion across the app. All of it uses only move, scale and fade (cheap on weak phones) and switches off when Android animations are off.

- Morph: a screen opened from a tile or row grows out of that tile (position, size, rounded corners and fade together) and shrinks back into it on Back. Screens opened without a tile slide in as before.
- Tiles and rows rise and fade in one after another when a screen or tab opens (first 9 only, so long lists stay quick).
- Tab switches fade and rise slightly.
- Tap press, sheets, dock and mic animation are unchanged.
- Checked by tests that the morph and stagger run and screens still open and close. Not seen on a phone; the motion itself is untested visually.
