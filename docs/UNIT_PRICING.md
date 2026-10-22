# Stage 1 independent unit-price foundation

Implementation in `UnitPriceCalculator.kt` and `UnitPriceCalculatorTest.kt`.
The pure Kotlin core is independent of Android, JNI, OCR quality and camera.

- Prices in RUB; quantities in the parser's canonical grams, millilitres and
  piece counts. Comparisons normalize to per 1 kg, 1 litre or 1 piece.
- Display uses two decimal places and HALF_UP rounding to kopecks.
- **Winner determination uses exact BigDecimal cross-products before rounding,**
  so 2 offers whose display values round to the same number are not silently
  misclassified as exactly equivalent.
- Saving percentage is relative to the more expensive standardized offer,
  rather than relative to the package price.
- Mismatched physical dimensions are rejected. Zero, negative, malformed and
  overprecise manual fields fail closed.
- OCR candidates are *not* automatically promoted to confirmed values;
  explicit human review remains mandatory before this calculation is shown.

This is a parallel development workstream, **not Stage 0 acceptance**. Android
runtime signing and 100 genuine Russian-photo OCR accuracy checks remain open.
