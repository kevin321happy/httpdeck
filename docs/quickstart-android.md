# HttpDeck — Android Quickstart

This guide integrates HttpDeck into a native Android app that uses OkHttp or Retrofit.

Current alpha: `v0.1.0-alpha.1`. The API may change before `v0.1.0`.

## Requirements

- Android `minSdk 23+`
- OkHttp 4.x
- JDK 17 for building
- Gradle 8.x / AGP 8.x

## 1. Add the GitHub-hosted Maven repository

Groovy `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://kevin321happy.github.io/httpdeck/maven' }
    }
}
```

Kotlin `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://kevin321happy.github.io/httpdeck/maven") }
    }
}
```

## 2. Add the dependency

```groovy
dependencies {
    debugImplementation 'io.github.kevin321happy:httpdeck-android:0.1.0-alpha.1'
    releaseImplementation 'io.github.kevin321happy:httpdeck-android-noop:0.1.0-alpha.1'
}
```

- `httpdeck-android` contains the interceptor and inspector UI.
- `httpdeck-android-noop` has the same public API but no code, so release builds do not link the inspector.
- Include exactly one of them per variant.

## 3. Install before building OkHttp

```kotlin
val builder = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)

HttpDeck.install(
    application = application,
    builder = builder
)

val client = builder.build()
```

`install` adds an interceptor and stores a replay client. It must run before `builder.build()`.

Retrofit:

```kotlin
Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

If you use Hilt/Koin/Manual DI, call `HttpDeck.install` in the module/provider that creates the `OkHttpClient`.

## 4. Add an entry

```kotlin
HttpDeck.show(activity)
```

Recommended: hide it behind a long-press on the version label or a debug settings row. The alpha inspector opens a full-screen list; the floating traffic-pet entry is on the roadmap.

## 5. Verify

1. Build and install the debug APK.
2. Trigger one API call.
3. Open `HttpDeck.show(activity)`.
4. The list should show method, path, status and duration.
5. Tap a row for headers/body; use Share, cURL or Re-request.

## 6. Release opt-in (controlled builds only)

Replace the no-op artifact with the real one in the controlled variant:

```groovy
// releaseImplementation 'io.github.kevin321happy:httpdeck-android:0.1.0-alpha.1'
```

Pass the opt-in flag:

```kotlin
HttpDeck.install(
    application = application,
    builder = builder,
    config = HttpDeckConfig(
        enableInRelease = true,
        maxRecords = 300
    )
)
```

Then add a hidden entry (long-press version, secret gesture, etc.). Do **not** ship this mode in a public store build without a remote allowlist/kill switch.

## API

| API | Description |
|---|---|
| `HttpDeck.install(application, builder, config)` | Install the interceptor before building OkHttp |
| `HttpDeck.show(activity)` | Open the inspector |
| `HttpDeck.records()` | Current records snapshot |
| `HttpDeck.addListener { records -> }` | Observe record updates (called on the emitting thread) |
| `HttpDeck.removeListener(listener)` | Remove an observer |
| `HttpDeck.clear()` | Clear in-memory records |
| `HttpDeck.replay(activity, record, callback)` | Re-issue a captured GET/POST through the same client |
| `HttpDeck.curl(record)` | Redacted cURL string |
| `HttpDeck.shareText(record)` | Redacted share text |

`HttpDeckConfig`:

| Field | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch |
| `enableInRelease` | `false` | Allow release builds (controlled builds only) |
| `maxRecords` | `300` | Ring-buffer size |
| `maxBodyBytes` | `65536` | Per-body cap |
| `maxTotalBytes` | `12MB` | Total in-memory cap |

## Troubleshooting

**No records**

- Confirm `HttpDeck.install` runs before `builder.build()`.
- Confirm the request uses the same OkHttp client (Retrofit, Coil, etc. may create their own).
- Release build? The no-op artifact or `enableInRelease=false` intentionally disables capture.
- Body is binary or larger than `maxBodyBytes`; it is stored as a placeholder.

**No entry in release**

- Use the real artifact, not `-noop`.
- Set `enableInRelease = true`.
- Add your own hidden entry; the alpha does not auto-attach a floating button.

**Re-request returns 401/signature error**

- The captured body is reused; body-level timestamps/trace IDs are not regenerated.
- The same OkHttp client must contain your auth/signature interceptors.
- Auth endpoints are blocked by design.

**Screenshots are black in release**

- Expected: release capture windows set `FLAG_SECURE`.

## Limitations

- OkHttp only. WebView, third-party SDKs with their own clients, raw TCP/TLS and Flutter's Dart HTTP stack are not captured.
- No disk persistence or upload.
- Re-request supports GET/POST only; auth endpoints are blocked.
- Alpha UI is manual-entry; floating pet, Compose UI, Flutter and hybrid bridge are upcoming.

## Security

Read [SECURITY.md](../SECURITY.md) before enabling release capture. A local hidden switch is not a security boundary.