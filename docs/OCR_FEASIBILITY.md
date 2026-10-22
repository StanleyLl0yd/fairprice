# FairPrice: OCR feasibility checkpoint
Date 2026-10-22. Stage 0: OPEN. NOT a release candidate.

Locally executed before source upload: Kotlin parser/desktop OCR 18/18 PASS,
including 5 synthetic price-label pictures with Tesseract Russian recognition.
These are NOT real store photos, NOT Android benchmarks or accuracy claims.

Not yet verified: Android arm64 compilation, JNI execution, bounding boxes,
CPU performance, ABI/16-KB alignment, app storage, >=100 real Russian price
labels, model comparison vs PaddleOCR PP-OCRv5, and reproducible AAB output.

Targets for readable real labels: price >=98%; quantity >=95%.
Do not silently accept multiple prices or card-only prices.
No telemetry, no backend, no network permission in Android manifest.
