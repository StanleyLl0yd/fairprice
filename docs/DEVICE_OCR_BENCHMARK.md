# On-device Russian OCR benchmark export (Stage 0)

This is a **test-only** utility, not app functionality. It uses the same
`OcrImageLoader`, `TesseractOfflineOcr` and `PriceLabelParser` as the
Stage 0 prototype. It does not access a backend, network or user photo library.

## Preparation

1. Use an arm64-v8a physical Android phone with USB debugging, or a separately
   documented Android test environment. The standard Android app build targets
   arm64-v8a; the Linux x86_64 test variant is **not** ARM64 evidence.
2. Curate an independently verified corpus of at least **100 readable, genuine
   Russian store label photographs** from different retailers, with rights to
   use them. Do not commit user photographs to GitHub. Ground-truth labels
   and image SHA-256 are covered in `docs/OCR_CORPUS.md`.
3. Put the photos and `truth.jsonl` under a local folder such as `corpus/`.
   Paths in the manifest must be relative to that folder, for example
   `photos/shop01/0001.jpg`. The benchmark test reads only `id` and
   `image` from the manifest; the independent evaluator uses the expected
   price, quantity and measurement annotations.

## Example using a physical ARM64 phone

```sh
# Build-time fetch only; runtime application remains offline.
bash tools/fetch-ocr-model.sh
./gradlew :app:installDebug :app:installDebugAndroidTest
adb shell mkdir -p /sdcard/Android/data/com.sl.fairprice/files/ocr-benchmark
adb push corpus/. /sdcard/Android/data/com.sl.fairprice/files/ocr-benchmark/
./gradlew :app:connectedDebugAndroidTest

adb pull /sdcard/Android/data/com.sl.fairprice/files/ocr-benchmark/predictions.jsonl ./predictions.jsonl
python3 tools/evaluate_ocr_corpus.py \
  --manifest corpus/truth.jsonl \
  --predictions predictions.jsonl \
  --photos corpus
```

The JUnit producer `RussianCorpusExportTest` is **SKIPPED** if no
`truth.jsonl` has been explicitly staged. This skip is not a passed OCR
quality evaluation. A staged but invalid/empty corpus fails.

On a valid corpus, the test decodes each image with EXIF correction, runs
native offline OCR, interprets candidates, and writes a new JSONL output
atomically to application-specific external storage. For ambiguous,
missing or loyalty-card-only prices, it writes `ambiguous:true` without
inventing a value. All such abstentions count as failures under the
independent evaluator. It outputs only candidate numbers, elapsed time
and OCR confidence, not raw OCR text or photos.

Remove staged photo files from the device after completing the test.
Neither these commands nor successful synthetic emulator tests alone
satisfy the physical ARM64 + 100-real-photo release gates.
