# Stage 0 parser regressions - 2026-10-22

- Retain package weight on a line that also says `цена за 1 кг`.
  Exclude only the reference unit span, not every unit on the line.
- Parse both `4 шт. по 125 г` and `125 г × 4` into one total.
- Exclude decimal amounts with unit suffixes or percentages from the
  fallback search for unmarked prices.
- Support Russian thousands separators and trailing `по карте` qualifiers.
- Keep distinct card/reference prices marked for user review.
- No speculative value insertion for missing fields.

Verification: 22/22 independent local Kotlin parser probes passed. These
are synthetic TEXT samples, not OCR measurements or real photographic labels.
GitHub Actions runs JUnit regression tests with the Android project.

Stage 0 remains OPEN. A built APK does not establish arm64 OCR runtime,
accuracy, performance or production suitability.
