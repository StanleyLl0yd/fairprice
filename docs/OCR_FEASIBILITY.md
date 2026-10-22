# OCR feasibility - 2026-10-22

Status: Stage 0 OPEN, not a release candidate.

Completed:
- Kotlin parser and desktop Tesseract: locally 18/18 synthetic smoke/integration cases PASS.
- Native Android Debug APK assembled, unit tests and Android Lint succeeded in GitHub Actions at PR #1.
- Russian model provided with SHA-256 verified build-time download, bundled into APK.
- Offline/permissions baseline: source manifest has no INTERNET permission.

Not verified:
- Actual OCR on arm64 Android, JNI operation, bounding-box accuracy, performance or memory.
- Full PP-OCRv5 ncnn Android pipeline, its model provenance and ABI compatibility.
- >=100 legally sourced genuine Russian price-tag photographs and precision/recall metrics.
- Final APK/AAB release acceptance.

A synthetic on-device smoke test was added but has not been run on physical Android hardware.
Any assertion about production OCR quality would therefore be premature.

Upstream reference: https://github.com/Tenshi18/DroidOCR
