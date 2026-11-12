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
- Yandex Mobile Ads SDK: sticky banner at the bottom and a preloaded interstitial between completed levels.

The Android implementation is fully native and does not embed a WebView. Sudoku remains playable offline; ad requests require an internet connection.

## Yandex ads

The app currently uses Yandex demo placements for testing: `demo-banner-yandex` and `demo-interstitial-yandex`. Before publishing, replace them with placement IDs from your Yandex Advertising Network account. Set these Gradle properties in your local `~/.gradle/gradle.properties` (or pass them with `-P`):

```properties
YANDEX_BANNER_AD_UNIT_ID=your-banner-placement-id
YANDEX_INTERSTITIAL_AD_UNIT_ID=your-interstitial-placement-id
```

The interstitial is shown only after a puzzle is solved and the player selects **Next level**. If no interstitial is ready, the next level opens immediately.
