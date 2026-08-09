# DaycareLog — Android

Kotlin + Jetpack Compose (Material 3) Android client for DaycareLog, a daycare management system for tracking children, attendance, health records, and reports. Talks to the same [Spring Boot backend](../backend) REST API as the [web client](../web), so data is shared across both.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3; manual light/dark mode toggle in Settings — no Android system dynamic/wallpaper-based theming)
- **Navigation Compose** — in-app routing
- **Retrofit** + **Gson** — REST API client
- **Jetpack DataStore (Preferences)** — persisted auth token/user session
- **Coroutines** — async work

## Requirements

- Android Studio (or the Gradle wrapper + Android SDK on the command line)
- The Gradle toolchain is deliberately pinned to versions compatible with **Android Studio Iguana (2023.2.1)** — AGP 8.3.2, Gradle 8.4, Kotlin 1.9.22, Compose Compiler 1.5.10 — rather than the latest available versions.

## Run

Open `mobile/` in Android Studio and run the `app` configuration, or from the command line:

```bash
cd mobile
./gradlew assembleDebug
```

The app points at the production backend URL by default — see `RetrofitClient.kt` to point it at a local backend instead.

### Release build

```bash
cd mobile
./gradlew assembleRelease
```

produces a **signed, shrunk** release APK at
`app/build/outputs/apk/release/app-release.apk` — as opposed to
`assembleDebug`'s output at `app/build/outputs/apk/debug/app-debug.apk`,
which is unminified, debuggable, and signed with the auto-generated debug
keystore (fine for local development, not for handing out).

**One-time setup** (already done in this checkout — `keystore/` and
`keystore.properties` exist locally and are git-ignored, never committed):

```bash
cd mobile
mkdir -p keystore
keytool -genkeypair -v -keystore keystore/daycarelog-release.jks -alias daycarelog \
  -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
```

Then create `mobile/keystore.properties` (git-ignored):

```properties
storeFile=keystore/daycarelog-release.jks
storePassword=<the password you set above>
keyAlias=daycarelog
keyPassword=<same as storePassword - PKCS12 keystores require both to match>
```

**Back this keystore up somewhere safe outside the repo.** It's what proves
future release builds come from the same source — lose it, and no later
build can ever install as an "update" over an already-installed release; the
only option would be uninstalling first.

**CI / no local `keystore.properties`**: the build also reads
`RELEASE_STORE_FILE` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` /
`RELEASE_KEY_PASSWORD` environment variables as a fallback (see
`app/build.gradle.kts`), so a CI pipeline can decode a base64-encoded
keystore secret to a file and export those instead of checking anything in.

**Versioning**: bump both `versionCode` (must strictly increase release to
release — Android uses it to decide if an install is an update) and
`versionName` (the human-readable string, e.g. `1.0.0` → `1.0.1`) in
`defaultConfig` in `app/build.gradle.kts` before cutting a new release.

**After building**: verify the APK is actually signed with the release key
(not the debug one) and test it on a device before handing it out:

```bash
$ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
adb install app/build/outputs/apk/release/app-release.apk
```

Since this release build enables R8 minification/resource shrinking
(`isMinifyEnabled = true`, `isShrinkResources = true` — the debug build type
is untouched and stays unminified), `app/proguard-rules.pro` keeps every
class in `data/model/` unobfuscated: those are all plain Kotlin data classes
deserialized by Gson via field-name reflection through Retrofit's
converter-gson, and R8 has no way to know that on its own — without the
keep rule it would rename/strip fields and every API response would fail to
parse at runtime despite the build itself succeeding. If you add a new
model class or a reflection-based library, re-check that file.

## Project structure

```
app/src/main/java/com/daycarelog/app/
├── data/
│   ├── api/           Retrofit service interface, client, auth token holder
│   ├── model/          Request/response DTOs
│   └── preferences/     DataStore-backed token/user persistence
├── navigation/          Top-level nav graph (Login/Register/Main)
├── ui/
│   ├── auth/            Login, Register, shared AuthViewModel
│   ├── main/             MainScreen — the Navigation Drawer shell wrapping every other screen
│   ├── dashboard/        Stat grid, weekly attendance chart, quick actions
│   ├── children/         Children list + add/edit form
│   ├── attendance/       Daily attendance (weekdays only)
│   ├── health/           Health records list + add/delete form
│   ├── guardians/        Guardians directory (portal accounts) + per-child GuardiansSection
│   ├── parent/           Read-only Parent screens — Dashboard, Attendance, Health Records (`/mine` endpoints)
│   ├── reports/          Monthly report
│   ├── users/            Admin-only user management
│   ├── settings/         Profile (editable name fields), dark mode toggle, sign out
│   ├── common/           Shared composables (date picker field)
│   └── theme/            Material 3 color scheme (light + dark), ScreenPalette theming helper, typography
└── util/TextUtils.kt     Shared name-capitalization logic
```

## Notable behavior

- **Navigation**: `MainScreen.kt` wraps the app in a `ModalNavigationDrawer` grouped into Main / Management / Admin sections for Admin/Staff/Teacher, or a read-only Parent section for the `parent` role, matching the [web sidebar](../web)'s design. Every top-level screen has a menu icon to reopen the drawer.
- **Theming**: a manual light/dark mode toggle (`ThemeState.kt`, DataStore-persisted, survives sign-out) drives a `rememberScreenPalette()` helper threaded through every screen's cards/rows, mirroring the web app's two-tier light/dark surface design.
- **Guardians & Parent portal**: Admin/Staff manage guardians per child (`GuardiansSection`) or via a standalone directory (`GuardiansScreen`), optionally creating a `parent`-role portal login tied to one or more children. Parent users only see the read-only Dashboard/Attendance/Health screens under `ui/parent/`, sourced from `/mine` endpoints resolved server-side from the JWT.
- **Attendance**: restricted to weekdays (Monday–Friday) — the date picker defaults to the nearest weekday and rejects weekend dates before submission.
- **Name fields**: First/Last/Middle Name inputs on Registration and the Settings profile form auto-capitalize each word as you type (`util/TextUtils.kt`, shared by both screens), via `KeyboardCapitalization.Words` plus a `capitalizeWords()` pass before saving — without overriding intentional casing like "McDonald" or disturbing cursor position mid-edit.
- **Auth**: JWT + user JSON persisted in DataStore; `NavGraph.kt` checks for a saved token on launch to decide between the Login screen and the main app.
- **Gotcha**: `painterResource()` cannot load `<adaptive-icon>` mipmap resources (e.g. `R.mipmap.ic_launcher_round`) — throws at runtime, not compile time. Use a plain vector `drawable` instead (see `R.drawable.ic_child`, used for the drawer logo and Login screen icon).
