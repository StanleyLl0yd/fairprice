# FairPrice OCR feasibility - verified evidence, 2026-10-22

**Stage 0 OPEN.** Android Russian OCR has executed in emulators, including
an ARM64 library via native-bridge translation. There is **no** physical
ARM64 device benchmark, no accepted PaddleOCR comparison and **no** 100-photo
real Russian price-tag accuracy result. Do not start Stage 1 or publish.

## Proven Android evidence

- **Normal APK, ARM64-only, offline:** PR #10, exact-head and post-merge CI
  PASS. Inspect actual packaged APK after manifest merging: no
  `android.permission.INTERNET`, bundled `rus.traineddata` has pinned
  SHA-256 `e16e5e036cce1d9ec2b00063cf8b54472625b9e14d893a169e2b0dedeb4df225`,
  native library `lib/arm64-v8a/libtesseract.so` present; other native
  ABI libraries forbidden. Build, unit tests, Android Lint PASS.
- **Android 11 x86_64 with hardware virtualization:** PR #11, workflow
  39770208099, 5/5 on-device instrumentation tests PASS, including
  local Tesseract Cyrillic text recognition, text boxes, EXIF geometry
  and lack of INTERNET permission. The explicit `-Pfairprice.ocrTestAbi=x86_64`
  property is only for emulator testing; default APK remains ARM64-only.
- **Android 15 API35 native-bridge translation of ARM64:** PR #14,
  workflow 39772717849 exact-head PASS. Guest ABI list is
  `x86_64,arm64-v8a`; the normal Android APK uses default ARM64 native
  Tesseract library, not the x86 override. Instrumentation started
  5 tests on Android 15 and ended `BUILD SUCCESSFUL`. This is evidence
  that ARM64 JNI is usable **through Android translation on an x86_64
  host**, not proof of native physical ARM64 CPU execution.
- **Optional real-photo producer:** PR #13, workflow 39772255477,
  6-test Android 11 x86_64 instrumentation run PASS. This includes an
  optional JSONL prediction exporter that **skips if no real corpus
  is staged**. No real-photo images were supplied or evaluated. See
  `docs/DEVICE_OCR_BENCHMARK.md`.
- **Image handling:** PR #7 added bounded bitmap decode and JPEG EXIF
  rotation/reflection; Android instrumentation geometry tests are now
  actually run, not merely compiled.

## Deterministic parser and acceptance harness

- PR #3: Russian price and quantity regressions: package mass alongside
  per-kg reference price, multi-packs, fractions/percentages, loyalty
  conditions. Kotlin parser regression/CI PASS; 22/22 standalone probes PASS.
- PR #6: offline per-image SHA-256 + JSONL ground-truth evaluator;
  PR #12: 100 **readable** genuine-photo minimum (not relaxed by the
  release CLI), mandatory explicit ambiguity flag, malformed/missing
  predictions count as failed extraction. Eight synthetic evaluator
  unit tests PASS; these do **not** measure actual OCR accuracy.
- A possible source of real shelf photos:
  https://huggingface.co/datasets/D-Shatnev/price-tag-detection
  (declares 2,007 images and CC BY 4.0; bounding boxes only).
  Independent rights/provenance review, store diversity, crop readability,
  and manual price/quantity labels remain required. None downloaded/scored.

## Documented infrastructure limitation

- PR #4 CLOSED UNMERGED: hosted macOS Apple Silicon ARM64 emulator failed
  to initialize hardware virtualization (`HVF error: HV_UNSUPPORTED`),
  workflow 39766635509. No OCR code was executed in that run. This is
  an infrastructure failure, not evidence of model failure. The portable
  build-time SHA-256 check was retained separately in PR #8. Avoid
  rerunning the identical macOS setup.

## Acceptance blockers

1. Actual physical arm64-v8a Android device (or trusted non-translated
   hardware-backed device farm), JNI/word bounds, APK performance/RAM,
   device-specific lifecycle, 16KB compatibility where applicable.
2. Full production PaddleOCR PP-OCRv5 East Slavic detector and recognizer
   Android feasibility plus independent comparison against Tesseract.
   Upstream ncnn implementation reference:
   https://github.com/Tenshi18/DroidOCR pinned commit
   `3e29dfcb183345a8761bb97be67c35dec077e66c`.
   Check dictionary/CTC, model hashes, size, license, NOTICE and native ABI.
   This model is **not integrated in FairPrice**.
3. At least 100 independently verified, readable, genuine photographs of
   Russian store price labels from multiple retailers. Benchmark actual
   on-device OCR/parser JSONL with `tools/evaluate_ocr_corpus.py`:
   price accuracy >=98%, mass/volume >=95% where annotated. Never mark
   ambiguous extractions correct or count synthetic fixtures as real.
4. Release candidate builds APK/AAB, complete license and notice review,
   physical device memory/performance and user photo handling validation.

Issue: https://github.com/StanleyLl0yd/fairprice/issues/5

**Nothing here authorizes Stage 1, FairPrice 1.0 acceptance, or RuStore release.**
