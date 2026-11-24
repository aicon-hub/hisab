# Hisab Studio 2 - 0.9.1-pro: bring your old app with you

Everything here runs on the phone. No server, no upload.

## Import from screenshots or PDF (new)

Settings > Your data > Import from screenshots or PDF.

- Pick screenshots of another expense app's entry list, or a PDF export.
- Text is read on the phone with ML Kit on-device text recognition. Nothing leaves the phone.
- PDFs are rendered on the phone (PdfRenderer) and read with the same on-device OCR, so scanned PDFs work the same way.
- Tuned to the Money Tracker home list: day header with "Expenses: N" total, rows with negative amounts. The review screen checks each parsed day against its header total and flags mismatches.
- Statement-style PDF tables (date + description + amount on a line) are picked up by a fallback parser.
- Review screen before saving: per-day totals, per-entry category (tap to change or leave out), nothing saved until confirmed. Entries that already exist are skipped.

## Import entries from CSV (now generic)

- Hisab's own export shape imports exactly as before (accounts, categories, people created when missing).
- Other apps' CSVs: the importer finds the date, amount, title, category, debit/credit and type columns by name, reads ISO, dd/mm/yyyy, dd-MMM-yyyy and month-name dates, rupee signs, thousands commas, parentheses and DR suffixes, and opens the same review screen. Income rows are left out.

## Migrating from another app

Export a CSV from the old app, keep it in your own Drive, and use the generic CSV import above. The one-tap preload from 0.9.1/0.9.2 was removed in 0.9.3 so no personal data ships inside the app.

## Notes and limits

- OCR quality depends on the screenshots: cropped, blurry or dark-mode shots read worse. The review screen is the safety net.
- Titles truncated with "..." in the old app stay truncated; the data was never in the screenshots.
- Categories from screenshots are guessed from the title (the old app's icons do not read as text), fix them in the review screen.
- PDF text-layer extraction is not used; every PDF goes through on-device OCR for one uniform, offline pipeline.
- Starting balances do not travel with CSV or screenshot imports, by design.
- Unit tests: OcrTest (7) and ImportTest (6) cover the Money Tracker layout, totals checks, year inference, noise lines, column detection, date and amount formats, and statement tables. Real-device OCR output is untested here.
