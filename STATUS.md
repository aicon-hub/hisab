# 0.2.2-dev candidate status

11 November 2026. Development preview, not production-ready. Local build/signature and bounded framework regression pass; real-device installation, Google/Drive round trips, permission pickers, background/notification/reboot delivery, upgrade and performance remain unverified.

Automatic SMS is completely removed, including SEND_SMS, sender, callback and UI. Legacy automatic grant metadata is scrubbed from on-device due plans. Reviewed SMS and WhatsApp composers remain, with final Send in the messaging app. Local pay-by and daily notifications remain. The final packaged manifest has no SMS, accessibility or notification-listener permission/service. Permission minimization does not guarantee that Play Protect clears a classification.

Google remains External Testing; scopes are drive.appdata, openid, userinfo.email and userinfo.profile only. Public homepage/privacy pages in docs/ are informational and do not change OAuth publication or submit verification. No billing, card, trial or server is added.

Privacy boundaries: profile photo/name, chosen contact links and pay-by notes stay on the phone, outside Drive/backup. Encrypted backup includes supported customization and records. CSV/PDF are readable, not restore backups. PDF is printable raster, not searchable text. Calendar and person-to-person shared ledgers are not implemented.

125 distinct framework methods across 18 classes pass in bounded groups, including all 39 UI methods. A broad regression run hit the host time limit and is not counted. Exact passing methods are in VERIFICATION.md. Visual review covers the no-automatic-SMS menu and the mobile homepage/privacy layout. Previous UI/PDF inspection still applies to unchanged features. All reports remain simulation evidence, not phone certification.
