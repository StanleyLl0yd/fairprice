# Stage 0 real-photo OCR accuracy corpus

This is **not** an existing benchmark and no real photographed samples have
been scored yet. Use at least 100 **readable**, independently annotated, legally obtained
**genuine Russian store price-label photographs from different shops**.
Additional unreadable images may be included, but never count toward the
100-photo acceptance minimum.
Store the photos and annotations privately/offline; do not commit private photos
to GitHub. Human curation must verify that a photo is real (a filename, a
boolean assertion and SHA-256 cannot prove photographic authenticity).

Input 1, `truth.jsonl`: one JSON object per image:
```json
{"id":"shop1-001","image":"shop1/001.jpg","sha256":"<64 lowercase hex chars>","real_photo":true,"source_description":"Store, date, collection rights","readable":true,"price_rub":"129.99","quantity":"900","measure":"ml"}
```

Input 2, `predictions.jsonl`: one object per same ID, independently exported
from the **actual OCR+parser under test**:
```json
{"id":"shop1-001","price_rub":"129.99","quantity":"900","measure":"ml","ambiguous":false}
```

If OCR cannot choose safely, set `ambiguous:true` and omit the numeric fields.
A missed or ambiguous value counts as extraction failure; it must not be
silently credited as correct. The `ambiguous` boolean is mandatory for every
prediction; a malformed OCR numeric value counts as a failed extraction rather
than invalid ground truth. Units must be normalized to `g`, `ml` or
`piece`. For readable photos with no quantity printed, omit measure and
quantity ground truth; those do not affect the quantity denominator.
`readable:false` items are retained in total count but excluded from
the stated readable-image quality thresholds and do not count toward the
required minimum 100 readable images; disclose this count.

Run offline:
```sh
python3 tools/evaluate_ocr_corpus.py --manifest truth.jsonl --predictions predictions.jsonl --photos /path/to/photo-root
```

Exit 0 = sample size and both quality targets met, 1 = quality below target,
2 = invalid corpus/annotation/hash. Tests: `python3 -m unittest discover -s tools -p 'test_*.py'`.
This tool verifies **reported** predictions only: it does not perform Android
inference, and genuine provenance, variety of stores and source annotation
require manual review. Do not call Stage 0 accepted based on this script alone.
