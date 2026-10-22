# Offline CameraX capture, beta

Uses CameraX **1.6.2 stable** with the back camera, Android permission requested
only when the user deliberately starts capturing a label. The capture activity
is not exported. Images are placed temporarily in the application's private
cache directory (not shared storage), and are returned to the caller via a
private activity result. The caller must remove the temporary JPEG after OCR,
whether recognition succeeds or fails.

No INTERNET permission, third-party cloud OCR, location or broad photo/storage
permission. Devices with no camera can still install and manually compare.
Photo Picker remains an alternative. Camera compatibility still requires actual
ARM64 handset beta testing; emulator/build tests do not prove every camera device.
