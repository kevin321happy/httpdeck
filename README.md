# HttpDeck

> In-app HTTP workbench for Android, Flutter and hybrid apps.

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

**Status: `v0.1.0-alpha.2` — Android capture core + basic inspector UI.** Flutter and hybrid support are on the roadmap; APIs may change before `v0.1.0`.

![Android alpha](docs/images/android-alpha.png)

## Why HttpDeck

- **Debug-only by default** — production builds are no-ops unless you explicitly opt in.
- **No root, VPN or MITM certificate** — capture happens inside the app's HTTP client.
- **One product for native + Flutter (planned)** — a hybrid app should not need two separate inspectors.
- **Readable, redacted data** — tokens, cookies, phone numbers and signing fields are masked before display/share.
- **Re-request from the panel** — resend through the same client, with fresh auth/signature headers.

## What works in the alpha

- OkHttp interceptor: request/response, status, duration, headers and text bodies.
- In-memory ring buffer (default 300 records / 12 MB), no disk persistence.
- Basic Android inspector: list, search, detail, share text, copy cURL, re-request.
- Debug by default; release opt-in requires both the real artifact and `enableInRelease = true`.
- Unit tests for redaction, store, interceptor and re-request request building.

Not in this alpha (roadmap): Compose UI, floating traffic-pet entry, Flutter package, hybrid bridge, HAR export, WebSocket, mock/compare.

## Install (Android)

### 1. Add the repository

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://kevin321happy.github.io/httpdeck/maven' }
    }
}
```

### 2. Add the dependency

```groovy
dependencies {
    debugImplementation 'io.github.kevin321happy:httpdeck-android:0.1.0-alpha.2'
    releaseImplementation 'io.github.kevin321happy:httpdeck-android-noop:0.1.0-alpha.2'
}
```

The `-noop` artifact has the same API but does nothing, so release builds do not link the inspector.

### 3. Install into your OkHttp client

```kotlin
val builder = OkHttpClient.Builder()
HttpDeck.install(application, builder)

val client = builder.build()
```

With Retrofit, pass the same client:

```kotlin
Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(client)
    // converters/call adapters...
    .build()
```

`install` must run **before** `builder.build()`. If you use DI, call it in the module that creates the client.

### 4. Open the inspector

```kotlin
HttpDeck.show(activity)
```

Put this behind your own hidden gesture/settings entry — for example, long-press a version label.

### Release opt-in

Only for controlled internal/field builds:

```groovy
// releaseImplementation 'io.github.kevin321happy:httpdeck-android:0.1.0-alpha.2'
```

```kotlin
HttpDeck.install(
    application = application,
    builder = builder,
    config = HttpDeckConfig(enableInRelease = true)
)
```

A local hidden switch is **not a security boundary**. Anyone who obtains the APK may be able to enable it. Do not ship this mode in a public store build without a remote allowlist/kill switch.

More detail: [docs/quickstart-android.md](docs/quickstart-android.md).

## Build from source

Requirements: JDK 17, Android SDK 34, Gradle wrapper included.

```bash
./gradlew :httpdeck-core:test
./gradlew :sample-android:assembleDebug
```

Open the sample app, tap **GET**, then **Open HttpDeck**.

## Repository layout

```text
httpdeck-core/          Kotlin/JVM: record model, store, redactor, OkHttp interceptor, replay factory
httpdeck-android/       Android library: HttpDeck facade, inspector activity
httpdeck-android-noop/  Release no-op artifact
sample-android/         Runnable native sample
docs/                   Quickstart, design spec, screenshots
```

## Capture scope

HttpDeck only sees traffic that goes through the OkHttp client you install it into. It does **not** capture WebView traffic, third-party SDKs with their own clients, raw TCP/TLS packets, or Flutter's Dart HTTP stack (Flutter support is planned separately).

## Security

- Default: debug-only, in-memory, no upload, no disk.
- Sensitive headers/query/body fields are redacted for display, share and cURL.
- Release-enabled windows use `FLAG_SECURE`; disabling the inspector clears records.
- Do not publish captured data. Do not enable release capture in public builds without an explicit security review.

See [SECURITY.md](SECURITY.md).

## Roadmap

- `v0.1.0`: Flutter package + hybrid bridge, Compose UI, floating traffic-pet entry, HAR/JSON export.
- `v0.2`: WebSocket, mock/compare, Dio 4 adapter, Ktor/http adapters.
- `v1.0`: API stability, Maven Central, pub.dev.

## Contributing

Issues and PRs are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and keep changes focused.

## License

Apache-2.0. See [LICENSE](LICENSE).