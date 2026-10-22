# OCR feasibility - status 2026-10-22

**Stage 0 OPEN - no accepted real-photo or physical Android OCR benchmark.**

## Evidence completed

- Android Gradle `assembleDebug`, `assembleDebugAndroidTest`, unit tests
  and Android Lint passed on GitHub Actions for commits through PR #8.
- PR #3 merged: parser no longer drops a package mass on a line that also
  prints reference unit price; supports reverse multipacks, guarded bare
  prices and loyalty qualifiers. Independent local Kotlin probes: 22/22 PASS.
  Android JUnit regression tests compile and execute in CI.
- PR #6 merged: reproducible offline JSONL photo corpus evaluator with
  checked photo SHA-256, sample ID matching, duplicate detection, strict
  quantity/price denominators and explicit abstention penalties.
  Evaluator's five synthetic *unit tests* PASS; they are not real-photo
  OCR measurements. No actual photo corpus has been collected/scored.
- PR #7 merged: Android Photo Picker image loader now normalizes JPEG EXIF
  rotations/reflections and caps bitmap decode size. Three instrumentation
  tests are **compiled**, not run on Android.
- PR #8 merged: build-time traineddata checksum verification works with
  sha256sum on Linux and shasum on macOS, without changing Android runtime
  permissions.
- App manifest has no INTERNET permission. Native Tesseract model downloaded
  **at build time** with a pinned SHA-256 and included as offline APK asset.

## Native arm64 CI experiment - not a model failure

PR #4, run 39766635509 on GitHub-hosted macos-26 (Apple Silicon):
- JDK 25, Android SDK and offline OCR model setup completed.
- QEMU Android arm64-v8a emulator failed to start:
  `HVF error: HV_UNSUPPORTED`;
  `qemu-system-aarch64-headless: failed to initialize HVF`.
- 10-minute emulator boot deadline expired; no app instrumentation or OCR
  inference was executed. PR #4 closed *unmerged*, its independent portable
  model hash improvement was retained in PR #8.
- Do **not** rerun the same hosted ARM macOS configuration without a
  specific infrastructure change. See issue #5.

## Remaining blockers (must not infer success)

1. Real arm64-v8a device or reliable hardware-backed Android environment:
   execute `:app:connectedDebugAndroidTest`, verify JNI, bounding boxes,
   Cyrilic text, runtime, memory, ABI and absence of network use.
2. Production candidate comparison: PP-OCRv5 East-Slavic recognition **and**
   compatible detector/decoder on Android (ncnn/DroidOCR is one reference);
   native conversion, dictionary, checksums and licensing not accepted.
3. At least 100 legally obtained, genuine photographed Russian retail
   price labels from multiple shops, independently annotated. Score actual
   OCR + parser output using `tools/evaluate_ocr_corpus.py`.
   Target readable-photo price >=98%, quantity >=95%; ambiguous results must
   not be silently treated as successes.
4. Release APK/AAB verification, image handling on devices, performance
   and third-party NOTICE acceptance.

The Android build is a **technical prototype only**. Stage 1 and publication
are not authorized by these checks.

Upstream model reference: https://huggingface.co/PaddlePaddle/eslav_PP-OCRv5_mobile_rec
Android ncnn reference: https://github.com/Tenshi18/DroidOCR
Blocker tracker: https://github.com/StanleyLl0yd/fairprice/issues/5
