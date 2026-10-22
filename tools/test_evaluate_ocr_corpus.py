"""Synthetic unit test data only; never contributes to the real-photo corpus."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from evaluate_ocr_corpus import evaluate


class EvaluationTests(unittest.TestCase):
    def fixture(self, folder):
        root = Path(folder)
        photos = root / "photos"
        photos.mkdir()
        image = photos / "sample.jpg"
        image.write_bytes(b"synthetic test only, NOT a real photo")
        truth = root / "truth.jsonl"
        predictions = root / "predictions.jsonl"
        truth.write_text(json.dumps({
            "id": "test", "image": image.name,
            "sha256": hashlib.sha256(image.read_bytes()).hexdigest(),
            "real_photo": True, "source_description": "synthetic test declaration only",
            "readable": True, "price_rub": "129.99", "measure": "ml",
            "quantity": "900"
        }) + "\n")
        predictions.write_text(json.dumps({
            "id": "test", "price_rub": "129,99", "measure": "ml", "quantity": "900"
        }) + "\n")
        return truth, predictions, photos

    def test_exact_decimal_normalization(self):
        with tempfile.TemporaryDirectory() as tmp:
            t, p, photos = self.fixture(tmp)
            self.assertTrue(evaluate(t, p, photos, minimum=1)["accepted"])

    def test_minimum_corpus_gate(self):
        with tempfile.TemporaryDirectory() as tmp:
            t, p, photos = self.fixture(tmp)
            with self.assertRaisesRegex(ValueError, "Insufficient"):
                evaluate(t, p, photos)

    def test_changed_image_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            t, p, photos = self.fixture(tmp)
            (photos / "sample.jpg").write_bytes(b"changed")
            with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
                evaluate(t, p, photos, minimum=1)

    def test_ambiguous_extraction_fails_both_metrics(self):
        with tempfile.TemporaryDirectory() as tmp:
            t, p, photos = self.fixture(tmp)
            p.write_text(json.dumps({"id": "test", "ambiguous": True}) + "\n")
            result = evaluate(t, p, photos, minimum=1)
            self.assertEqual(result["ambiguous"], 1)
            self.assertFalse(result["accepted"])
            self.assertEqual(result["quantity_applicable"], 1)

    def test_missing_file_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            t, p, photos = self.fixture(tmp)
            (photos / "sample.jpg").unlink()
            with self.assertRaisesRegex(ValueError, "missing image"):
                evaluate(t, p, photos, minimum=1)


if __name__ == "__main__":
    unittest.main()
