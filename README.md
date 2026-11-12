# Little Logic Sudoku — Android

The repository contains a native Android version of the attached Sudoku game, implemented with Kotlin and Jetpack Compose. The browser version remains available in `index.html`.

## Open and run

1. Open this repository’s root folder in Android Studio (Ladybug or newer).
2. Allow Gradle sync to complete, then choose an emulator or connected Android device.
3. Run the `app` configuration.

The app targets Android 7.0 (API 24) and newer. The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, and Compose Material 3. Android Studio will download the required build components during sync.

## Included in the native app

- 9 × 9 Sudoku board with generated puzzles that have a unique solution.
- Easy, medium, and hard difficulties.
- Russian and English interface.
- Number pad, pencil notes, erase, undo, and up to three hints per puzzle.
- Timer, progress, level progression, and a friendly completion screen.
- Automatic local save and restore of the current puzzle.
- Responsive Compose layout that adapts to phone screen sizes.

The Android implementation is fully native: it does not embed a WebView or need a network connection while playing. Platform-specific web integrations from the browser version are not included.
