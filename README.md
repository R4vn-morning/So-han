# R4Vn Music

Offline music player for Android with a liquid glass look.
Kotlin, Jetpack Compose and Media3.

## Features
- Plays songs stored on the phone, with album art
- Keeps playing in the background, with notification and lock screen controls
- **Home**: greeting, search, featured card, recently played, mixes made for you
- **Artists**: browse songs grouped by artist (expandable/collapsible lists)
- **Library**: all songs, play and shuffle, search
- **Settings**: rescan library
- Mini player that expands into the full player (tap or drag up, drag down to close)
- Swipe between tabs, springy press animations everywhere
- Backdrop follows the cover of the current song

## Structure
- `MusicRepository.kt` reads songs from the phone
- `PlaybackService.kt` is the background player (ExoPlayer and MediaSession)
- `PlayerViewModel.kt` holds playback state
- `ui/` has the screens and glass components
- `ArtistsScreen.kt` groups songs by artist with expand/collapse

## Build
1. Clone the repo
2. Open in Android Studio (Ladybug 2024.2.1 or newer)
3. Sync Gradle
4. Run on device or emulator (Android 7.0+ / API 24+)

## Requirements
- Android Studio Ladybug 2024.2.1+
- Gradle 8.7+
- Kotlin 2.0+
- compileSdk 34
- minSdk 24

## Permissions
- `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` (older)
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `POST_NOTIFICATIONS` (Android 13+)

## License
MIT
