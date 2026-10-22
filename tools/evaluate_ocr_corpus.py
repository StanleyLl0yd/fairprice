#!/usr/bin/env python3
"""Offline evaluation of curated Russian shelf-label photo OCR results.

Uses a separate annotated JSONL ground-truth manifest, a prediction JSONL
export and local image files. Checking hashes/attestation does NOT prove
photographs are genuine: a human must curate that independently.
"""
import argparse
from decimal import Decimal, InvalidOperation
import hashlib
import json
from pathlib import Path
import sys


def amount(value):
    if value is None:
        return None
    if not isinstance(value, str):
        raise ValueError("Numbers must be strings for exact decimal comparison")
    try:
        number = Decimal(value.replace(",", "."))
    except InvalidOperation as exc:
        raise ValueError(f"Invalid decimal: {value!r}") from exc
    if not number.is_finite() or number <= 0:
        raise ValueError("Expected positive finite decimal")
    return number


def records(path):
    result = {}
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        try:
            obj = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError(f"{path}:{line_number}: invalid JSON") from exc
        key = obj.get("id") if isinstance(obj, dict) else None
        if not isinstance(key, str) or not key or key in result:
            raise ValueError(f"{path}:{line_number}: invalid or duplicate id")
        result[key] = obj
    return result


def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def evaluate(manifest, predictions, photos, minimum=100):
    truth = records(manifest)
    predicted = records(predictions)
    if set(truth) != set(predicted):
        raise ValueError("Ground truth and prediction IDs do not match")
    if len(truth) < minimum:
        raise ValueError(f"Insufficient real-photo corpus: {len(truth)} < {minimum}")
    hashes = set()
    readable = qty_total = price_ok = qty_ok = ambiguous = 0
    failures = set()
    for key, row in truth.items():
        if row.get("real_photo") is not True or not row.get("source_description"):
            raise ValueError(f"{key}: missing real-photo provenance declaration")
        if type(row.get("readable")) is not bool:
            raise ValueError(f"{key}: explicit readable boolean required")
        rel = row.get("image")
        if not isinstance(rel, str) or not rel or Path(rel).is_absolute() or ".." in Path(rel).parts:
            raise ValueError(f"{key}: image must be a relative path")
        path = (photos / rel).resolve()
        if not path.is_relative_to(photos.resolve()) or not path.is_file():
            raise ValueError(f"{key}: missing image/outside corpus")
        digest = sha256(path)
        if digest != row.get("sha256"):
            raise ValueError(f"{key}: image SHA-256 mismatch")
        if digest in hashes:
            raise ValueError(f"{key}: duplicate image bytes")
        hashes.add(digest)
        if not row["readable"]:
            continue
        readable += 1
        expected_price = amount(row.get("price_rub"))
        if expected_price is None:
            raise ValueError(f"{key}: missing annotated price")
        expected_unit = row.get("measure")
        if expected_unit not in (None, "g", "ml", "piece"):
            raise ValueError(f"{key}: invalid unit")
        expected_qty = amount(row.get("quantity")) if expected_unit else None
        if expected_unit:
            if expected_qty is None:
                raise ValueError(f"{key}: missing annotated quantity")
            qty_total += 1
        observed = predicted[key]
        if observed.get("ambiguous") is True:
            ambiguous += 1
            failures.add(key)
            continue
        if amount(observed.get("price_rub")) == expected_price:
            price_ok += 1
        else:
            failures.add(key)
        if expected_unit:
            if (observed.get("measure") == expected_unit and
                    amount(observed.get("quantity")) == expected_qty):
                qty_ok += 1
            else:
                failures.add(key)
    if not readable or not qty_total:
        raise ValueError("No readable images with price and quantity annotations")
    price_accuracy = price_ok / readable
    quantity_accuracy = qty_ok / qty_total
    return {
        "total": len(truth), "readable": readable,
        "quantity_applicable": qty_total, "price_correct": price_ok,
        "quantity_correct": qty_ok, "ambiguous": ambiguous,
        "price_accuracy": price_accuracy, "quantity_accuracy": quantity_accuracy,
        "price_target_met": price_accuracy >= .98,
        "quantity_target_met": quantity_accuracy >= .95,
        "accepted": price_accuracy >= .98 and quantity_accuracy >= .95,
        "failed_sample_ids": sorted(failures)
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--manifest", required=True, type=Path)
    p.add_argument("--predictions", required=True, type=Path)
    p.add_argument("--photos", required=True, type=Path)
    p.add_argument("--minimum", default=100, type=int)
    args = p.parse_args()
    try:
        result = evaluate(args.manifest, args.predictions, args.photos, args.minimum)
    except (ValueError, OSError) as exc:
        print(f"INVALID CORPUS: {exc}", file=sys.stderr)
        return 2
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["accepted"] else 1


if __name__ == "__main__":
    sys.exit(main())
