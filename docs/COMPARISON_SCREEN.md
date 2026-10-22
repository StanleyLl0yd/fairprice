# Stage 1 parallel manual comparison preview

A two-offer screen is available independently of any camera or OCR engine.

1. Enter the price in rubles and the package quantity in grams, millilitres
   or pieces for each product. A comma decimal separator is accepted.
2. Alternatively choose an existing price-tag image for each product. The
   offline OCR pipeline suggests at most one normal price and one quantity.
   Loyalty-only or reference-unit prices are **not** silently used as package
   prices. Where multiple candidates exist the fields remain blank for manual
   correction, with original OCR text visible.
3. Explicitly confirm **each** product's values after reviewing them.
   Changing either field or physical measure revokes confirmation.
4. When both confirmed products have matching physical dimensions, compare
   their per-kg / per-litre / per-piece prices. Winner comparisons use
   unrounded BigDecimal cross-products.

The synthetic on-device OCR diagnostic remains available on the same screen.

**Not yet accepted:** device-specific OCR quality, >=100 real photos, image
capture via CameraX, persisted history, advanced loyalty-card handling,
production typography/accessibility QA, final release.
This is a parallel Stage 1 workstream and does not close Stage 0.

The published signed prerelease preview.2 is immutable; later signed builds
must increment versionCode and remain signed with the already-configured
GitHub Environment key.
