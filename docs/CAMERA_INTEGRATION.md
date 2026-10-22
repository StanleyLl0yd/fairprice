# Beta camera integration

There are now two explicit ways to supply images for each offer:
Android Photo Picker or CameraX photo capture. The camera permission is requested
only on first tap of the Camera button. Its capture activity is private, and it
returns only a canonical path under `cacheDir/ocr-camera`. The host validates
that the returned file is a JPEG directly under that path, decodes with existing
bounded EXIF-aware loader, and deletes it after OCR finishes or fails.

All camera OCR suggestions follow the same conservative policy as Photo Picker:
the entry is unconfirmed, loyalty-only/reference unit prices are not promoted
silently, raw recognized text is visible, and the user must confirm numbers.
History never retains captured photos.

Physical ARM64 camera compatibility and real Russian price-label OCR accuracy
are **not proven** by compiling or running synthetic emulator tests.
