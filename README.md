# FairPrice

Offline Android application for comparing real unit prices from Russian retail price labels. Kotlin, Jetpack Compose Material 3, fully offline Cyrillic OCR.

**Current stage: OCR feasibility prototype (not release-ready).** The intended device workflow is capture/select an image, detect Russian text and prices, verify recognized values, and compare per kg/litre/item. No accounts, backend, telemetry or INTERNET permission are planned for v1.

The initial source import and Android CI validation are in progress. The OCR engine is not considered accepted until actual Android arm64 tests and real price-tag benchmarks are completed.

Application ID: `com.sl.fairprice`. Min Android API 26. RuStore first; publication requires separate authorization.

Third-party OCR assets will retain their own license notices. Project license not chosen yet.
