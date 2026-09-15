# E430

**English** | [简体中文](README-ZH-simplified.md)

E430 is a lightweight native Android client for e621 and e926, built with Kotlin and Jetpack Compose. The project is currently at `0.1.0-beta` and implements the main workflows for browsing, searching, account interactions, pool reading, and media viewing.

> E430 is a third-party project and is not affiliated with, authorized by, or partnered with e621 or e926.

## Implemented features

### Browsing and search

- Five primary destinations: Home, Latest, Popular, Favorites, and Pools.
- Home, Latest, and Popular use a two-column masonry grid showing score, favorite count, comment count, rating, and format badges for non-static media.
- Popular uses the site's monthly ranking endpoint and only shows content from the current calendar month. Latest is always ordered from newest to oldest.
- The top search field accepts e621 metatags directly. Submitting a search from any primary page opens Home and scrolls to the top without changing the Latest or Popular queries.
- The search dropdown provides mutually exclusive ratings, rating exclusion, sorting by date/favorites/score/comments, a separate ascending switch, and a system date range picker. Controls stay synchronized with manually entered `rating:`, `order:`, and `date:` fields.
- All media lists support pull-to-refresh and distinguish initial loading, empty results, refresh failures, and pagination failures.
- A signed-in account's blacklist is applied automatically and can be viewed or edited from the account page.

### Presets

- Presets are stored separately for each signed-in account, with an independent configuration for anonymous use.
- Preset files are stored in the shared `Documents/E430/<username>.json` path for backup and import. The app requests access to the Documents directory through the Android system picker on first use.
- Presets can be imported from the search dropdown or selected directly as the Home query.
- When Home has no usable preset, or the selected preset is empty, it falls back to `date:1_month_ago.. order:score`.

### Accounts and interactions

- Sign in with an e621/e926 username and API key, with automatic session restoration on later launches.
- Credentials are encrypted with a key managed by Android Keystore and stored in private app storage. Signing out clears credentials, session state, and account-specific caches.
- Sign-in and automatic restoration failures are reported to the user.
- Signed-in users can browse Favorites, vote, add or remove favorites, create comments, edit their own comments, and hide their own comments.
- Account-only actions do not send write requests while anonymous and instead show a sign-in requirement.

### Post details and media

- The detail page shows media, score, favorite count, full rating, file information, source links, description, tags, and comments.
- Images and GIFs support full-screen pan and zoom. Video controls include playback, seeking, speed, looping, mute, and full-screen mode.
- HTTP and HTTPS source links open in the system's default browser.
- Downloads use Android DownloadManager and default to `Download/E430`; users can choose a quality level and see an estimated size. Sharing uses the Android system share sheet.
- Tags are collapsible by default and open a matching search when selected. Parent and child posts appear as clickable preview cards.
- Horizontal swipes follow the order of the source list: right-to-left opens the next post and left-to-right opens the previous post. Returning to the list centers the last viewed preview.
- Detail data uses an in-memory LRU cache capped at 10 posts. On Wi-Fi, the app preloads the next 10 list previews and the details, tags, comments, and images for two posts on each side of the current detail. Preloading on metered networks is disabled by default.

### Pools and navigation

- Pools are listed newest-first with a cover, title, and post count.
- A pool detail page displays media in directory order in one column and keeps only the current and next post in its loading window.
- Images and GIFs open the shared post detail implementation directly; videos provide a separate detail button.
- Pool sections on post details provide First, Previous, Next, and Last navigation while preserving pool swipe order.
- Pool, post, tag, parent, and child navigation share a bounded navigation state. Once the depth reaches six levels, Back returns to the original second-level page.
- The drawer, secondary pages, and detail overlays support system Back and directionally consistent transitions. Exiting from a primary page requires two Back actions within two seconds.

### Settings and languages

- Settings cover e621/e926 selection, dark theme, separate image quality defaults by network type, video autoplay, default mute, looping, default tag collapse, metered-network preloading, and the download directory.
- English and Simplified Chinese are supported, with resources stored separately in `Language/en` and `Language/zh-s`.
- On first launch, the app selects the first supported locale from the system language list and falls back to English when none match. After a manual selection, the saved user preference is always used.
- The drawer footer shows the app version, third-party disclaimer, and project repository link.

## Network and privacy

- API requests use the `E430/0.1 (by FredWd on e621)` User-Agent and sustained requests are limited to at most one per second.
- Account API calls use HTTP Basic Authentication. Credentials are only sent to the selected e621/e926 API host.
- Media files, thumbnails, and external source links never receive account credentials.
- The project contains no analytics, advertising SDK, or background full-site synchronization.

API behavior is defined by [E621_API.md](E621_API.md) and [e621_openapi.yaml](e621_openapi.yaml).

## Technology and project structure

- Single-Activity architecture with Jetpack Compose and Material 3.
- AndroidX ViewModel, StateFlow, Coroutines, and unidirectional data flow.
- Retrofit, OkHttp, and kotlinx.serialization.
- Coil for images and GIFs; Media3 ExoPlayer for video.
- DataStore for ordinary settings and Android Keystore for account credentials.
- Code is organized by business area under packages such as `account`, `posts`, `pools`, `presets`, `search`, and `settings`, with shared infrastructure under `core`.

## Build environment

| Component | Current configuration |
| --- | --- |
| App version | `0.1.0-beta` |
| Gradle Wrapper | 9.3.1 |
| Android Gradle Plugin | 9.1.1 |
| Kotlin / Compose compiler plugin | 2.2.10 |
| Gradle daemon JDK | 21 |
| Java target | 11 |
| compileSdk | 37.0 |
| targetSdk | 36 |
| minSdk | 28 (Android 9) |

Use an Android Studio version that supports AGP 9.1.1 and install Android SDK Platform 37.0. Dependency versions are centralized in `gradle/libs.versions.toml`; the local SDK path belongs in `local.properties`.

## Build and verification

Open the repository root in Android Studio and run the `app` configuration, or use Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

Unit tests cover query-field replacement, search ordering, blacklist matching, locale selection, preset serialization, network DTOs, and credential boundaries. Device UI automation still needs a connected emulator or physical device:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Current limitations

- The Popular monthly endpoint does not provide continuous pagination.
- The comments section currently reads the first 100 comments returned by the API.
- Media with a missing URL, failed load, or unsupported preview format is hidden from lists.
- The project is still in beta and does not yet include release signing, store distribution, or complete on-device UI automation coverage.

## Project link

[https://github.com/FredWhitedragon/E430](https://github.com/FredWhitedragon/E430)

## Just talking nonsense...

I love codex...
Take this as a practice of mine to familiarize myself with Codex and mobile development; I handled the requirements and architecture, while Codex took care of the actual implementation. Of course, it’s mainly for my own personal use—mostly because accessing the site directly or using other third-party apps just felt... lacking somehow? So, this current version mainly implements the basics and some presets, though I’ll probably add more features later... maybe...?
