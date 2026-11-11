# 0.2.1-dev candidate status

11 November 2026. Candidate build passes local packaging/signature checks and bounded framework regression. It is NOT a real-phone or live Google/Drive pass. Intended publication is an untested prerelease under the user's latest instruction, not a production-ready claim.

README.md contains the exact feature scope and privacy/export boundaries. All original code/mark/wordmark/icons remain original. Only the four requested fonts are bundled third-party assets, with SIL OFL licences and source references in app assets.

Important limits:
- Google login/own-Drive sync is configured and enabled, with only simulated account/sync tests. Google remains External Testing. No billing, trial, card, public verification or consent publication changes.
- Automatic SMS is experimental, local-only, off by default and explicit per payment. SEND_SMS is hard-restricted; installer allowlisting and runtime permission are needed. No real SMS was sent. Google-ledger automatic SMS is unavailable. WhatsApp always requires the final user Send tap.
- Pay-by notes/notification grants, contact links and private profile data are on-device only. Backup customization includes fonts, theme, catalogs/defaults, launcher and daily reminder choices. CSV/PDF are readable exports, not settings-restore formats. Auto-message grants and OS permissions never silently restore.
- PDF is an original raster statement. It is readable and printable, not searchable text; large statements are capped at 150 pages or 16 MiB and should use CSV instead.
- Calendar view, full shared person-to-person ledgers, referenced-item archiving and some name/currency conflict repair flows remain later work. No server was added.
- Android may delay all local alarms/background jobs. Real performance, insets/keyboard, upgrade, launcher, Contacts/photo and notification delivery checks remain.

Inspected pixels: updated Glass Home and scrolled bottom with dock clearance; round profile control/page; fonts chooser; month/day Activity and English/Malayalam charts; mixed-debt ledger; original selection-pill frame sequence; actual exported single/multipage PDF including Malayalam and final page. PDF entry blocks start on a fresh page when needed.

Full native UI suite runs exhausted host memory. No failed run is counted as a pass. All 38 UI methods then passed in five bounded groups. The PDF native-document path was replaced with a deterministic Canvas-to-JPEG PDF writer so the actual export could be generated, parsed and visually inspected here. This is still not device export-picker verification.
