# FairPrice 0.0.1 offline OCR test prerelease

**Technical test APK only. Not FairPrice 1.0, not RuStore.**

## Install without Android Studio or USB

1. Open [GitHub Releases](https://github.com/StanleyLl0yd/fairprice/releases).
2. Select prerelease `v0.0.1-ocr-preview.1`. Download `FairPrice-0.0.1-ocr-preview.1-arm64.apk`.
3. Install it on Android 8.0+ with an arm64-v8a CPU. If prompted, temporarily allow installation from your browser/files app; do not disable Play Protect.
4. Tap **Самопроверка OCR**. Copy the diagnostics with **Скопировать отчёт** and send that text to the developer. The test uses only a synthetic image generated in memory, not personal photos.
5. Tap **Выбрать фотографию** to select a real Russian price label and inspect recognized text, prices, package quantity and OCR duration. Report mistakes manually.

The APK is **debug-signed by CI** with an ephemeral debug key. Future test APK versions may not install over it. Uninstall the prior test app if Android shows a signing conflict; there is no user data persistence guarantee. The APK contains no INTERNET permission and does not upload photographs.

The SHA-256 file alongside the APK permits an integrity check. The GitHub release notes record the exact built source commit. A self-test PASS proves only the generated example works, not the target 98%/95% quality on >=100 real, independently labelled Russian store photos.

No finished two-product price comparison, manual-input mode, CameraX shooting or app-store release is included yet. This preview is solely for removing the physical-device OCR smoke-test blocker.
