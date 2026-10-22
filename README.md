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

## Actual Android device OCR smoke test

Connect an Android arm64-v8a device with USB debugging enabled, then execute:

```sh
./gradlew :app:connectedDebugAndroidTest
```

The instrumented test generates a deliberately synthetic Cyrillic price label in memory, checks local Russian OCR and bounding boxes, and verifies there is no INTERNET permission. This test has **not yet been executed on physical hardware**. A successful Android build alone does not prove JNI runtime or on-device performance.

## Stage 0 verification snapshot

- Parser hardening: PR #3 merged with Kotlin regression coverage.
- Image capture input: Photo Picker decodes bounded bitmaps and honors EXIF
  rotation/reflection (PR #7, instrumentation compiled but not executed).
- Build: Linux/macOS model hash verification (PR #8); Android debug unit
  tests and Lint PASS on GitHub Actions.
- Real-price-tag evaluation: `tools/evaluate_ocr_corpus.py`, documented
  in `docs/OCR_CORPUS.md`. No 100-photo corpus has been scored.
- ARM64 Android emulator test on GitHub's hosted macOS Apple Silicon
  runner failed **before OCR** because Hypervisor.framework reported
  `HV_UNSUPPORTED`; PR #4 closed unmerged. The blocker is in issue #5.

See `docs/OCR_FEASIBILITY.md` for precise evidence and incomplete gates.
No Android-on-device OCR benchmark or release approval is implied.

## Release blockers

Stage 0 requires verified on-device OCR, a fair comparison against PaddleOCR, third-party licensing and ABI checks, and quality evaluation against at least 100 **real** Russian store labels. Target extraction accuracy is >=98% for readable prices and >=95% for quantities. Do not silently accept ambiguous prices.

Stage 1-4 (math, UI comparison, camera, real-photo release testing) are not accepted. Project license has not been selected. RuStore publication requires separate authorization.
