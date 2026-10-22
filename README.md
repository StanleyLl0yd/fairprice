# FairPrice

Small offline Android utility to compare real unit prices from Russian shelf labels and packaging photographs.

**Status: Stage 0 - Android OCR feasibility / NOT RELEASE READY.** No publication is authorized.

- Package: `com.sl.fairprice`; Kotlin / Compose Material 3 / Android API 26+.
- Gradle 9.5.1; AGP 9.3.1; Kotlin Compose compiler 2.4.20; CI JDK 25; JVM bytecode 17.
- Target SDK 36; compile SDK 37.0.
- Baseline Russian OCR: Tesseract4Android 4.9.0 plus bundled Cyrillic `tessdata_fast` model.
- Alternative OCR under investigation: PP-OCRv5 East-Slavic plus ncnn.
- The app has no INTERNET permission, account, backend, advertising or analytics.
- Photo Picker only. No broad gallery permission.

## Build

Run `bash tools/fetch-ocr-model.sh` once on a networked development machine to obtain the pinned OCR model. The script verifies SHA-256 and fails closed if upstream data differs. This is a **build-time** operation only; the packaged app and OCR work offline.

Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` (Windows: `gradlew.bat`).

The APK is available from the successful GitHub Actions build artifact. It is a technical preview, not an accepted release.

## Verified Android OCR runs (Stage 0, not release acceptance)

- **Android 11, x86_64 emulator:** PR #11 ran 5/5 real instrumentation
  tests on a KVM-backed Android emulator. Local Cyrillic Tesseract JNI,
  text-area bounds, EXIF handling and lack of INTERNET permission PASS.
- **Android 15, ARM64 native bridge:** PR #14 ran 5/5 Android tests using
  the **standard arm64-v8a APK**, with ARM64 library translation on an
  x86_64 host. This confirms execution of the ARM64 native library via
  translation, **not** on physical ARM64 silicon.
- **APK security:** PR #10 verifies the built APK contains only the expected
  arm64-v8a native OCR library, the pinned Russian OCR model SHA-256, and
  no INTERNET permission after manifest merging.
- **Corpus exporter:** PR #13 provides an optional Android instrumentation
  benchmark exporter to offline JSONL, documented in
  `docs/DEVICE_OCR_BENCHMARK.md`. Without explicitly staged real photos,
  that particular test is skipped, not counted as accuracy evidence.
- **OCR corpus acceptance:** `tools/evaluate_ocr_corpus.py`, hardened in
  PR #12, requires 100 **readable genuine** price-label photos and enforces
  price >=98% and quantity >=95% extraction success. No genuine-photo
  benchmark has been scored yet.

For an ARM64 physical device with USB debugging, run:

```sh
bash tools/fetch-ocr-model.sh
./gradlew :app:connectedDebugAndroidTest
```

This remains a pending physical-device gate. See
`docs/OCR_FEASIBILITY.md` and issue #5 for test links, limitations and
PaddleOCR candidate feasibility status.

## Release blockers

Stage 0 requires verified on-device OCR, a fair comparison against PaddleOCR, third-party licensing and ABI checks, and quality evaluation against at least 100 **real** Russian store labels. Target extraction accuracy is >=98% for readable prices and >=95% for quantities. Do not silently accept ambiguous prices.

Stage 1-4 (math, UI comparison, camera, real-photo release testing) are not accepted. Project license has not been selected. RuStore publication requires separate authorization.

## Installable OCR device preview

The independently testable **debug-signed prerelease** is produced through GitHub Actions only after successful exact-main CI. See [test instructions](docs/TEST_RELEASE.md). The APK runs offline and includes a synthetic self-check with copyable diagnostics. **Neither a prerelease nor its self-check completes Stage 0 or constitutes authorization for RuStore publication.**

## Signed offline two-offer comparison preview

Tag `v0.0.2-comparison-preview.3` is generated from an exact-CI-proven protected main commit through GitHub Environment signing, with the public release certificate SHA-256 matched against the existing signed preview.2. It contains manual two-offer comparison, per-product offline image OCR suggestions and explicit confirmation. The existing signed preview.2 remains unchanged and can be upgraded in place. This is **not** OCR Stage 0 acceptance or RuStore publication.

## FairPrice Beta 1

The signed `v0.1.0-beta.1` APK and AAB include offline Photo Picker/CameraX OCR, manual comparison with verified price/quantity, and local history. Built from protected exact-main CI with the existing signing key. This is a **GitHub prerelease** for device testing, not a RuStore/public production release or proof of real-photo OCR accuracy. See [beta readiness](docs/BETA_READINESS.md).
