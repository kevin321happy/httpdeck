# HttpDeck

> **In-app HTTP inspector for Android & Flutter.** Capture OkHttp and Dio traffic without root, VPN or a MITM certificate.
>
> 应用内 HTTP 抓包调试台：原生 Android、纯 Flutter、原生 + Flutter 混合项目共用一套面板。

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Maven](https://img.shields.io/badge/Maven-0.1.0--alpha.3-blue)](https://kevin321happy.github.io/httpdeck/maven)
[![Flutter](https://img.shields.io/badge/Flutter-0.1.0--alpha.1-02569B)](docs/quickstart-flutter.md)
[![Android](https://img.shields.io/badge/Android-minSdk%2023-3DDC84)](docs/quickstart-android.md)
[![Status](https://img.shields.io/badge/status-alpha-orange)](CHANGELOG.md)

![HttpDeck Android inspector](docs/images/android-alpha.png)

## Why HttpDeck

Most in-app inspectors stop at native Android. Most Flutter inspectors only
see Dio. Hybrid apps end up with two tools, two sets of screenshots and two
different data contracts.

HttpDeck puts both sources in one record store and one panel:

- **No root, no VPN, no certificate** — capture inside the app's own clients.
- **Android + Flutter** — OkHttp and Dio records share one list, one detail
  view and one share flow.
- **Debug-only by default** — release builds are no-ops unless you explicitly
  opt in.
- **Redacted by default** — authorization, cookies, tokens, phone numbers and
  signing fields are masked for display/share.
- **Re-request** — replay a captured native request through the same OkHttp
  client, with fresh auth/signature headers.
- **Zero disk persistence** — in-memory ring buffer only in v0.1.

## Three ways to integrate

| Your app | What to add | What you get |
|---|---|---|
| Pure native Android | `httpdeck-android` + `httpdeck-android-noop` | OkHttp requests in the native panel |
| Pure Flutter (Android host) | `httpdeck_flutter` + a tiny Android bridge | Dio requests in the same native panel |
| Native + Flutter hybrid | both of the above | OkHttp + Dio in **one** panel |

> iOS is not supported in v0.1. The inspector UI is Android-native.

## 1. Pure native Android

### Gradle

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

```groovy
// app/build.gradle
dependencies {
    debugImplementation 'io.github.kevin321happy:httpdeck-android:0.1.0-alpha.3'
    releaseImplementation 'io.github.kevin321happy:httpdeck-android-noop:0.1.0-alpha.3'
}
```

### Install

```kotlin
val builder = OkHttpClient.Builder()
HttpDeck.install(application, builder)
val client = builder.build()
```

`install` must run before `builder.build()`. With Retrofit, pass the same
`client`.

### Open

```kotlin
HttpDeck.show(activity)
```

Put it behind your own debug entry (long-press version, debug menu, etc.).
There is no forced floating bubble in the public alpha.

Full guide: [Android quickstart](docs/quickstart-android.md).

## 2. Pure Flutter (Android host)

Flutter traffic is forwarded to the native panel over a MethodChannel; the
panel itself remains native Android.

### Flutter dependency

```yaml
dependencies:
  httpdeck_flutter:
    git:
      url: https://github.com/kevin321happy/httpdeck.git
      ref: v0.1.0-alpha.3
      path: httpdeck-flutter
```

```dart
import 'package:httpdeck_flutter/httpdeck_flutter.dart';

await HttpDeckFlutter.install(dio: dio);
```

Debug builds capture by default; release builds are disabled.

### Android host bridge

```kotlin
class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(engine: FlutterEngine) {
        super.configureFlutterEngine(engine)

        HttpDeck.install(application, OkHttpClient.Builder())

        MethodChannel(
            engine.dartExecutor.binaryMessenger,
            "io.github.kevin321happy.httpdeck/network"
        ).setMethodCallHandler { call, result ->
            if (call.method == "debugNetworkEvent") {
                HttpDeck.recordFlutterEvent(
                    call.arguments as? Map<*, *> ?: emptyMap<Any, Any>()
                )
                result.success(true)
            } else {
                result.notImplemented()
            }
        }
    }
}
```

The empty `OkHttpClient.Builder` is intentional for a Flutter-only app: it
creates the shared record store/panel without capturing native traffic.

Open the panel from a hidden Flutter entry:

```kotlin
MethodChannel(
    engine.dartExecutor.binaryMessenger,
    "io.github.kevin321happy.httpdeck/control"
).setMethodCallHandler { call, result ->
    if (call.method == "show") {
        HttpDeck.show(this)
        result.success(true)
    } else {
        result.notImplemented()
    }
}
```

```dart
GestureDetector(
  onLongPress: () => const MethodChannel(
    'io.github.kevin321happy.httpdeck/control',
  ).invokeMethod('show'),
  child: const Text('version'),
)
```

Full guide: [Flutter quickstart](docs/quickstart-flutter.md).

## 3. Native + Flutter hybrid

1. Install `httpdeck-android` in the native app and add the OkHttp interceptor.
2. Add `httpdeck_flutter` to the Flutter module and install it on Dio.
3. Register **one** `io.github.kevin321happy.httpdeck/network` handler on the
   existing Flutter engine.
4. Open the panel from your existing native debug entry.

Both sources end up in the same list:

```text
source=android  POST /v1/orders        200  120ms
source=flutter  GET  /v1/site/analysis 200   86ms
```

## How it works

```text
Android OkHttp ──HttpDeckInterceptor──┐
                                      ├── MemoryRecordStore ── native panel
Flutter Dio ──HttpDeckFlutter─────────┘
                 │
                 └── MethodChannel: io.github.kevin321happy.httpdeck/network
```

- `httpdeck-core` is a pure Kotlin/JVM module: model, ring buffer, redaction,
  OkHttp interceptor, re-request factory.
- `httpdeck-android` owns the Android panel and exposes
  `HttpDeck.recordFlutterEvent`.
- `httpdeck-flutter` is a small Dio adapter; it does not ship a second UI.

## Comparison

| | Chucker | Flipper | Charles | **HttpDeck** |
|---|---|---|---|---|
| No root / VPN / cert | ✅ | ✅ | ❌ | ✅ |
| Native OkHttp | ✅ | ✅ | ✅ | ✅ |
| Flutter Dio | ❌ | partial | ✅ | ✅ |
| One panel for hybrid | ❌ | partial | ✅ | ✅ |
| In-app share / cURL | partial | ❌ | ❌ | ✅ |
| Re-request native | ✅ | ✅ | ✅ | ✅ |
| HAR / WebSocket / mock | partial | ✅ | ✅ | roadmap |

HttpDeck is alpha. It is intentionally smaller than Charles/Flipper; the goal
is a fast, beautiful in-app loop for mobile teams.

## What gets captured

- OkHttp: method, URL, status, duration, request/response headers, text bodies.
- Dio: same fields through `HttpDeckFlutter`.
- Bodies are capped (64KB default); binary, multipart and `Stream` bodies are
  represented as placeholders rather than consumed.
- URL paths are shown without host in the list; full URL in detail.

## Release builds

Default release is a no-op: use the `-noop` artifact.

For a controlled internal/field build, depend on the real artifact in release
and opt in:

```kotlin
HttpDeck.install(
    application = application,
    builder = builder,
    config = HttpDeckConfig(enableInRelease = true)
)
```

The inspector uses `FLAG_SECURE` in release mode. A local hidden switch is
**not a security boundary**; do not ship release capture in a public store
build without a remote allowlist/kill switch.

## API

| API | Purpose |
|---|---|
| `HttpDeck.install(application, builder, config)` | Add the OkHttp interceptor |
| `HttpDeck.show(activity)` | Open the panel |
| `HttpDeck.records()` | Snapshot |
| `HttpDeck.recordFlutterEvent(arguments)` | Feed a Flutter Dio event |
| `HttpDeck.replay(activity, record, callback)` | Re-request a native GET/POST |
| `HttpDeck.curl(record)` / `shareText(record)` | Redacted export |
| `HttpDeckFlutter.install(dio: dio)` | Add the Dio interceptor |
| `HttpDeckFlutter.captureEnabled` | Debug-only runtime gate |

## Build from source

```bash
./gradlew :httpdeck-core:test
./gradlew :sample-android:assembleDebug
```

Flutter package:

```bash
cd httpdeck-flutter
flutter pub get
```

Repository layout:

```text
httpdeck-core/          Kotlin/JVM capture core
httpdeck-android/       Android panel + OkHttp interceptor
httpdeck-android-noop/  Release no-op
httpdeck-flutter/       Flutter Dio adapter
sample-android/         Native sample
docs/                   Quickstarts, design spec, screenshots
```

## Status

`v0.1.0-alpha.3`:

- Native OkHttp capture, ring buffer, redaction, panel, share, cURL, re-request.
- Flutter Dio adapter + native bridge.
- Release no-op + opt-in `FLAG_SECURE`.
- Unit tests for core.

Roadmap: floating traffic pet / Compose UI, iOS, HAR, WebSocket, mock/compare,
Dio 5 adapter.

## Security

- Debug-only by default; no upload; no disk persistence.
- Sensitive headers/query/JSON fields are redacted for display/share/cURL.
- Read [SECURITY.md](SECURITY.md) before enabling release capture.

## Contributing

Issues and PRs are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache-2.0. See [LICENSE](LICENSE).