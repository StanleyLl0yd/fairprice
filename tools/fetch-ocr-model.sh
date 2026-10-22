#!/usr/bin/env bash
# Build-time download ONLY. Android runtime has no network permission.
set -euo pipefail
cd "$(dirname "$0")/.."
output="app/src/main/assets/ocr/rus.traineddata"
sha="e16e5e036cce1d9ec2b00063cf8b54472625b9e14d893a169e2b0dedeb4df225"
mkdir -p "$(dirname "$output")"
if [[ -f "$output" ]] && printf '%s  %s\n' "$sha" "$output" | sha256sum -c --status; then
  echo "OCR model already validated."
  exit 0
fi
tmp="$output.part"
trap 'rm -f "$tmp"' EXIT
curl -fsSL --retry 3 --connect-timeout 20 --max-time 180 \
  https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/rus.traineddata \
  -o "$tmp"
printf '%s  %s\n' "$sha" "$tmp" | sha256sum -c
mv "$tmp" "$output"
echo "OCR model downloaded and validated."
