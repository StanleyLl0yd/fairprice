# How to test signed comparison preview 3

Download the `FairPrice-0.0.2-comparison-preview.3-signed-arm64.apk` from
[GitHub Releases](https://github.com/StanleyLl0yd/fairprice/releases/tag/v0.0.2-comparison-preview.3)
when the automated, gated signed workflow has completed.

If signed preview.2 is installed, install this new APK over it without
uninstalling. Both signatures must match or CI refuses publication.
If only debug-signed preview.1 is installed, uninstall it first.

Manual two-offer workflow:
1. Enter a RUB price and quantity for each product.
2. Select grams, millilitres or pieces consistently for both.
3. Confirm each offer after checking the values.
4. Compare normalized RUB/kg, RUB/litre or RUB/piece and savings.
5. Optionally choose a picture for either offer to receive **unconfirmed**
   OCR suggestions. The app will not trust card-only or reference-unit
   prices without a user's explicit manual correction/confirmation.
6. Run the synthetic Cyrillic OCR self-test and copy its diagnostics.

All comparison math is fully offline. Price-tag OCR remains a technical
preview, not yet accepted against >=100 genuine Russian price-tag photos.
The APK is published to GitHub Releases for testing, not to RuStore.
