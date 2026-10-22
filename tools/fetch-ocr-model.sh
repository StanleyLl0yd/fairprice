#!/usr/bin/env bash
# Build-time download ONLY. The packaged Android app needs no network access.
set -euo pipefail
cd "$(dirname "$0")/.."
output="app/src/main/assets/ocr/rus.traineddata"
sha="e16e5e036cce1d9ec2b00063cf8b54472625b9e14d893a169e2b0dedeb4df225"

verify_model() {
  local file="$1" actual
  [[ -f "$file" ]] || return 1
  if command -v sha256sum >/dev/null 2>&1; then
    actual="$(sha256sum "$file" | awk '{print $1}')"
  elif command -v shasum >/dev/null 2>&1; then
    actual="$(shasum -a 256 "$file" | awk '{print $1}')"
  else
    echo "No SHA-256 tool available" >&2
    return 1
  fi
  [[ "$actual" == "$sha" ]]
}

mkdir -p "$(dirname "$output")"
if verify_model "$output"; then
  echo "OCR model already validated."
  exit 0
fi

tmp="$output.part"
trap 'rm -f "$tmp"' EXIT
curl -fsSL --retry 3 --connect-timeout 20 --max-time 180 \
  https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/rus.traineddata \
  -o "$tmp"
verify_model "$tmp" || { echo "Offline OCR model SHA-256 mismatch" >&2; exit 1; }
mv "$tmp" "$output"
echo "OCR model downloaded and validated."
