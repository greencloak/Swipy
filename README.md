# Swipy

A free, open-source, ad-free vertical media feed for local photos and
videos — swipe up/down through your own media, like a short-form video
app, but nothing leaves your device.

- No ads, no analytics, no network access
- No account or login
- 100% local: browses photos/videos already on your phone via `MediaStore`
- Filter by folder, sort by date/name, or shuffle
- GPLv3 licensed, F-Droid friendly (no proprietary/Google-Play-only deps)

## Features

- Vertical swipe feed (photos + videos) using Jetpack Compose `VerticalPager`
- Folder picker — choose which folders to include
- Sort: newest, oldest, name (A-Z), or shuffle
- "Shuffle now" button to instantly re-randomize the feed
- Video playback via AndroidX Media3 (ExoPlayer), auto-plays only the
  currently visible item to save battery

## Building

Requirements: Android Studio (Koala or newer) or command-line Gradle with
Android SDK 34 installed.

```bash
# From the project root
./gradlew assembleDebug
# APK will be at app/build/outputs/apk/debug/app-debug.apk

# Or a release build (unsigned — sign it yourself before distributing)
./gradlew assembleRelease
```

If you don't have the Gradle wrapper jar yet, generate it once with a
local Gradle install:

```bash
gradle wrapper --gradle-version 8.7
```

Then open the project in Android Studio, let it sync, and press Run —
or use the command above to get a debug APK directly.

## Permissions

- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 and below)

That's it — no internet permission is requested at all, since the app
never needs network access.

## Project structure

```
app/src/main/java/org/fdroid/swipy/
  MainActivity.kt          # permission handling + app state
  data/
    MediaItem.kt            # data model + sort enum
    MediaRepository.kt       # MediaStore queries (folders, photos, videos)
  ui/
    FeedScreen.kt            # VerticalPager + top controls (sort/folder/shuffle)
    ImagePage.kt              # full-screen image page (Coil)
    VideoPage.kt               # full-screen video page (Media3 ExoPlayer)
    FolderPickerScreen.kt       # multi-select folder dialog
```

## Publishing to F-Droid

To get this into F-Droid's repo you'd submit a build metadata file to
[fdroiddata](https://gitlab.com/fdroid/fdroiddata) pointing at your own
public git repo tag/release — F-Droid builds from source itself, it
doesn't accept prebuilt APKs. Push this project to a public git host
first (GitHub/GitLab/Codeberg), tag a release, then follow F-Droid's
["Submitting to F-Droid Quick Start Guide"](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/).

## License

GPLv3 — see [LICENSE](LICENSE).
