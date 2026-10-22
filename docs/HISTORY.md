# Local history, beta implementation

Confirmed comparable offers may be saved to a private SQLite database with a
50-record bound. Entries contain only two RUB prices, two quantities, the unit
dimension and the timestamp; **no photos, OCR text, usernames or telemetry**.
Users can delete individual entries and restore an entry for a new comparison.
Restoring clears both confirmation flags, requiring fresh user review.

History is not automatically backed up or synchronized. Source photos selected
through Photo Picker are never copied into the history database. No INTERNET
permission is needed. SQLite schema version 1 explicitly refuses destructive
fallback upgrades.

This is an engineering preview, not evidence of OCR accuracy on real photos.
