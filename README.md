# A4 Scanner

An Android app that scans multiple pages using the phone camera and produces a
single, multi-page A4 PDF.

Built with **Kotlin** and **Jetpack Compose**.

## Download

A signed release APK is committed in this repository:

- [`releases/a4scanner-v1.0.0.apk`](releases/a4scanner-v1.0.0.apk)

```
SHA-256 (a4scanner-v1.0.0.apk):
6ea5188e83b61b8ea81a79b096fd7b5ff7eee8fb0e2f81de24af43c10e41a915
```

Enable "Install unknown apps" for your browser/file manager, then open the APK to install.

## Features

- **Camera capture** — capture one page at a time using CameraX, with a flash toggle.
- **Manual crop** — drag the 4 corner handles to fit the A4 sheet; 4-point perspective
  correction straightens the page.
- **Rotation** — rotate a page in 90° steps.
- **Filters** — Original, Grayscale, High contrast, and Black & white (Bradley adaptive
  threshold) for clean, readable scans.
- **Page management** — reorder, re-crop/re-filter or delete pages before exporting.
- **Export** — generates a single A4 PDF (all pages at ~200+ DPI, fit-to-page) and opens
  the system share sheet.

No third-party PDF or computer-vision libraries are used: PDF generation relies on
`android.graphics.pdf.PdfDocument` and image processing uses the platform
`android.graphics` APIs.

## Requirements

- Android 7.0 (API 24) or newer
- A device with a camera

## Build

The project uses the Gradle wrapper.

```bash
# Debug build
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Release build (needs signing config, see below)
./gradlew assembleRelease
```

You need a JDK 17 and the Android SDK (compileSdk 34). Point the build at your SDK via
the `ANDROID_HOME` environment variable or a `local.properties` file:

```
sdk.dir=/path/to/Android/Sdk
```

### Release signing

Create `keystore.properties` in the project root (this file is git-ignored):

```
storeFile=/absolute/path/to/release.jks
storePassword=yourStorePassword
keyAlias=yourKeyAlias
keyPassword=yourKeyPassword
```

If the file is missing the release build produces an unsigned APK; the debug build is
unaffected.

## Install

```bash
./gradlew installDebug
```

## Project structure

```
app/src/main/java/com/example/a4scanner/
  MainActivity.kt          # Compose host + navigation + permission handling
  ui/                      # CameraScreen, CropScreen, PagesScreen, theme
  vm/ScanViewModel.kt      # scan state, background image processing, PDF export
  image/                   # BitmapLoader (EXIF/rotation), Perspective, Filters
  pdf/PdfBuilder.kt        # A4 multi-page PDF writer (PdfDocument)
  data/ScanPage.kt         # page model, corner model, filters
```

## How it works

1. A photo is captured to the app cache directory.
2. Corners (normalized 0–1) define the page quad. `Matrix.setPolyToPoly` warps the quad
   onto a rectangle, producing a rectified, full-resolution page.
3. The selected filter is applied.
4. Each processed page is drawn fit-to-an-A4-page (595 × 842 pt) and written with
   `PdfDocument`.

## License

MIT
