# FairPrice signed Android build

The project's Android identity is `com.sl.fairprice`. The original
`v0.0.1-ocr-preview.1` is **debug-signed**. Do not replace or mutate it.

## Securely configure your existing key

On a trusted Windows machine with PowerShell 7 and `gh` authenticated as the
repository administrator, run `Set-FairPriceSigningSecrets.ps1` from the
conversation download. Do **not** upload your keystore into chat, GitHub source
history, GitHub Issues, pull requests, or a build artifact.

The script creates environment `android-signing` restricted to protected
branches and sets **environment secrets** (not repository-wide secrets):

- `FP_SIGNING_KEYSTORE_B64` - base64 binary .jks/.keystore/.p12
- `FP_SIGNING_STORE_PASSWORD` - keystore password
- `FP_SIGNING_KEY_ALIAS` - existing signing key alias
- `FP_SIGNING_KEY_PASSWORD` - private key password

GitHub CLI encrypts values locally and stores only encrypted secrets. Neither
the build nor the script writes the unencrypted password to persistent storage.
The decrypted keystore exists only in a private directory on an ephemeral
GitHub-hosted Actions runner and is deleted in a finally-style workflow step.
There is **no automatic runtime secret retrieval** inside the app.

Keep the original keystore and its passwords in an independent encrypted
backup. Losing them prevents reproducing the same signature. Prefer a dedicated
key/alias for FairPrice; sharing Android signatures across apps increases
cross-project blast radius.

## Release pathway

Workflow: `.github/workflows/ocr-signed-preview.yml`.
Release tag: `v0.0.1-ocr-preview.2` (immutable). The CI-tested protected main
commit is checked before signing. The runner builds
`./gradlew -Pfairprice.signedPreview=true :app:assembleRelease :app:bundleRelease`.

The signed release job **fails closed** on missing/invalid signing input,
a moved main branch, failed Gradle, invalid APK signature, missing ARM64
Tesseract library, mismatched Russian model or unexpected INTERNET permission.

It can be triggered by a successful main CI workflow or manually via
`gh workflow run ocr-signed-preview.yml --repo StanleyLl0yd/fairprice --ref main`.
The required `android-signing` environment secrets remain unavailable to
normal PR jobs.

The first signing-key preview uses versionCode **2**, APK for manual install,
and AAB for later distribution preparation. A debug-signed preview.1 APK
cannot be upgraded in place to a key-signed preview.2 APK; uninstall the
old preview first. Future **key-signed** releases with increasing versionCode
can update in place, as long as they use this same key.

The signature is not proof of OCR quality. The Stage 0 Android/device and
100-real-price-tag accuracy acceptance criteria remain open.

## Follow-up signed comparison preview

Release `v0.0.2-comparison-preview.3` increments `versionCode` from 2 to 3. The prerelease workflow `comparison-signed-preview.yml` runs only after exact-main CI success (or explicit dispatch with the same guard), uses existing protected `android-signing` secrets and rejects any certificate SHA-256 different from `v0.0.1-ocr-preview.2`. The previous prerelease tags and signatures remain immutable. No further keystore transfer is needed.

## Signed Beta 1

Workflow `beta-signed-release.yml` uses the already configured `android-signing` environment. `versionCode 4` upgrades signed preview.3 (`versionCode 3`) in place; CI rejects a signing certificate mismatch against `v0.0.2-comparison-preview.3`. The original keystore remains in the user's custody; no secret upload is required for Beta 1.

## Beta signing job does not create a deployment

Because beta APK/AAB packaging is not an actual environment deployment, the `beta-signed-release.yml` job uses `environment: { name: android-signing, deployment: false }`. GitHub's environment access and protection checks still apply, and the key continues to live only in environment secrets. This avoids creating a pending deployment object for a package-signing operation. It does not bypass reviewers or wait timers if these are configured. The workflow still requires successful exact protected-main CI and verifies APK signing certificate continuity.
