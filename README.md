# Nearfolk

A native Android app for meeting strangers and talking at your own pace. It grew out of a solitude day counter and keeps that counter, but the product is now peer chat: discover people, read their profile, start a conversation, stay safe while doing it.

"Nearfolk" is a working name. It lives in `app/src/main/res/values/strings.xml` (`app_name`) and `feature/onboarding` (`onb_app_name`). The application id is `app.nearfolk`. Pick the final name and id before the first Play Store upload, because the id cannot change afterwards.

| Welcome | Discover | Profile | Chat |
| --- | --- | --- | --- |
| ![](docs/screenshots/welcome_dark.png) | ![](docs/screenshots/discover_light.png) | ![](docs/screenshots/flow_2_profile.png) | ![](docs/screenshots/chat_dark.png) |

| Messages | Chat request | Composer | Journal lock |
| --- | --- | --- | --- |
| ![](docs/screenshots/inbox_dark.png) | ![](docs/screenshots/chat_request_dark.png) | ![](docs/screenshots/composer_long_dark.png) | ![](docs/screenshots/passcode_dark.png) |

## What works today

- **Onboarding**: name, gender, date of birth (18+ only), country, community guidelines, notification permission.
- **Discover**: dense member list with avatar, presence, age, gender, sign, city and a bio snippet. Gender chips, a filter sheet (online only, countries, age range for Plus), pull to refresh, and typo tolerant name search ("amra" finds "Amara").
- **Profiles**: cover image, avatar, exact last seen time, status, day counter, details, voice intro, highlights grid with a full screen viewer, and an endless post stream with likes and comments. Report and block from the overflow menu.
- **Composer**: one container for chat messages and comments, with attachments waiting to send, the text, and a fixed action row (attach on the left, send on the right). The text grows up to a quarter of the space visible above the keyboard (3 to 12 lines), then scrolls inside the box. Send stays on screen, dimmed until there is something to send.
- **Attachments**: up to 10 photos, videos or GIFs per message or comment, from the system photo picker or straight from the keyboard's GIF and sticker panels. Photos are re-encoded without location data; GIFs and stickers keep their animation; videos play in a full screen viewer.
- **Messages**: chats and requests (first messages from people you have not accepted yet; the message bar stays hidden until you accept, block or delete), archived threads, unread counts, typing state, delivery and read marks, day separators, grouped bubbles, long press to copy.
- **Safety**: dismissible safety notice per chat, a cap of three messages until the other person replies, report with reasons, block with confirmation (archives the thread), delete chat with confirmation, blocked members list.
- **You**: profile preview as others see it, edit identity, avatar and cover crop (pan and pinch in a squircle frame), voice intro recording, posts, highlights, and an activity feed for likes and comments.
- **Journal**: private or public entries with a mood, protected by a 6 digit passcode (salted PBKDF2, escalating lockout, relocks when the app goes to the background).
- **Settings**: system, light or dark theme plus a true black option, notification and haptic toggles, Plus plans, support and bug report by email, guidelines, privacy, terms, licenses, deactivate, delete account (three steps, typed confirmation), sign out.

## The network is simulated for now

There is no backend yet. `core:data` ships a local implementation of every repository plus `CommunitySimulator`, which seeds 40 sample members with posts, highlights and journals, moves people online and offline, answers your messages, sends a few first messages to new profiles and reacts to your posts. All of it writes to the same Room tables a real sync layer would, so screens do not know the difference. Settings says plainly that community data is a local sample, and Plus says that store billing is not connected.

Choosing the backend is the main decision for the next cycle. The seams are ready:

- Repository interfaces: `core/data/src/main/kotlin/app/nearfolk/core/data/repository/Repositories.kt`
- Local implementations to replace: `core/data/.../local/`
- Push notifications: `ChatNotifier` in `core/data/.../platform/AppVisibility.kt`, implemented in `app/.../notifications/Notifications.kt`
- Billing: `PlusRepository` (local implementation unlocks Plus on the device for testing)

## Design system

Everything visual lives in `core:designsystem`.

- **Squircles**: `SquircleShape` draws continuous curvature corners. Each corner keeps its usual circle centre, trims the arc at both ends and joins the straight edge with a cubic ramp whose first three control points sit on the edge, so curvature starts at zero instead of jumping. Smoothing defaults to 0.6. The geometry is unit tested in `SquircleGeometryTest`.
- **Where squircles are not used**: switches keep a stadium track, presence dots and the playback waveform stay simple, because those read faster in their usual shape.
- **Palette**: graphite and paper neutrals with one straw accent (hue about 46 degrees). Green is reserved for presence, red for destructive actions. No pink, purple, violet, blue or orange anywhere, including generated artwork. Chat bubbles are monochrome so long conversations stay calm.
- **Type**: Instrument Sans, variable, SIL Open Font License 1.1 (bundled with its license).
- **Motion**: spring presets in `NfMotion`. Buttons and rows shrink slightly on press, the tab indicator and segmented thumbs glide, screens slide in, likes bounce.
- **Icons**: the four navigation icons and the journal icon are drawn for this app (`NfIcons`). Other icons come from Material Symbols (Apache 2.0).
- **Copy**: plain and functional. A Gradle task (`./gradlew verifyCopyPunctuation`) fails the build if an em dash or en dash appears in any Kotlin source or resource file.

## Architecture

Kotlin, Jetpack Compose, Hilt, Room, DataStore, Navigation (type safe routes), Paging 3, Coil. Offline first: screens observe Room through repositories; whatever talks to the network writes into Room.

```
app                     Activity, navigation, notifications, theme selection
core/model              Plain Kotlin domain types
core/common             Fuzzy search, passcode hashing, clock
core/designsystem       Theme, squircle shape, components, icons
core/ui                 Shared composables that know the domain (avatars, rows, posts, sheets)
core/database           Room entities and DAOs (schemas exported to core/database/schemas, auto migrations)
core/datastore          Typed preferences
core/data               Repositories, media storage, voice recording, local community
feature/onboarding      Welcome and profile creation
feature/discover        Member feed, filters, search
feature/profile         Public profile, highlights, posts, comments
feature/messages        Inbox, requests, archive, chat
feature/you             Your hub, editing, crop, voice intro, posts, highlights, activity
feature/journal         Journal, passcode gate, public journals
feature/settings        Settings, Plus, account lifecycle, documents
build-logic             Convention plugins shared by every module
```

## Build and test

Requirements: JDK 17 or newer and the Android SDK with platform 37.

```
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # minified release APK (unsigned)
./gradlew test                        # unit, repository, screenshot and end to end tests
./gradlew :app:lintDebug              # lint across all modules
./gradlew verifyCopyPunctuation       # copy rule check
```

`AppFlowTest` runs the real app on Robolectric: it creates a profile, searches with a typo, opens a profile, sends a message and waits for the read mark. `ScreenshotTest` renders the main screens in both themes; pass `-Proborazzi.test.record=true` to write the images to `app/build/screenshots`.

Toolchain: AGP 9.5, Gradle 9.6, Kotlin 2.4, Compose BOM 2026.09, compile and target SDK 37, min SDK 29.

## Before release

- Replace `SUPPORT_EMAIL` in `app/build.gradle.kts` with a mailbox you own.
- Connect a backend and Play Billing, then remove the local sample notes in Settings and Plus.
- Have the privacy policy and terms reviewed; the in app versions describe this build only.
- Run a trademark search on the final name.
