# FairPrice 0.1.0-beta.1: readiness and device test plan

## Included

- Offline Tesseract Russian Cyrillic OCR from Android Photo Picker and
  on-demand CameraX capture. Bounded EXIF-aware image decoding.
- Manual price and quantity edits for two offers, explicit confirmation.
- Exact BigDecimal pricing per 1 kg, 1 l and 1 piece, with savings.
- Private SQLite comparison history (up to 50 entries); no stored photos.
- Offline, no INTERNET permission, accounts, telemetry or backend.
- Android API 26+, arm64-v8a. Signed by the existing private key as preview.3.
- GitHub prerelease APK/AAB only, no app store publication.

## Test on a physical Android arm64 handset

1. Update in place over signed preview.3.
2. Execute built-in Cyrillic OCR self-test and copy the report.
3. Try CameraX capture, choose photo, and manually verify all suggested values.
4. Refuse camera permission and verify manual and picker options still work.
5. Compare 500 g vs 1 kg, 900 ml vs 1 l, and multipacks.
6. Save and restore comparison history, restart app, delete entries.

## Not yet accepted (beta limitations)

- Physical native ARM64 camera/OCR performance has not been measured.
- Independent corpus of >=100 readable genuine Russian retail price-tag photos,
  with targets >=98% package-price and >=95% quantity extraction, is missing.
- PaddleOCR PP-OCRv5 East-Slavic benchmark and runtime integration are pending.
- This beta is for feedback. No authorization for production or RuStore.

No user photos, OCR texts or private credentials are included in diagnostics or
release artifacts. Beta accuracy requires real-device reports from testers.
