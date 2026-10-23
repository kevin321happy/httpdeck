# HttpDeck — Flutter Quickstart

HttpDeck's inspector is a native Android panel. A Flutter app can feed its
Dio traffic into that panel through a small MethodChannel; iOS is not
supported in v0.1.

## Requirements

- Flutter 3.x with an Android host
- Dio 4.x
- Android `httpdeck-android` debug artifact + `noop` release artifact

## 1. Flutter dependency

```yaml
dependencies:
  httpdeck_flutter:
    git:
      url: https://github.com/kevin321happy/httpdeck.git
      ref: v0.1.0-alpha.3
      path: httpdeck-flutter
```

```bash
flutter pub get
```

## 2. Install the Dio adapter

```dart
import 'package:httpdeck_flutter/httpdeck_flutter.dart';

await HttpDeckFlutter.install(dio: dio);
```

Add this before the first request. Debug builds capture by default; release
builds are disabled (`kDebugMode`).

## 3. Android host

`android/app/build.gradle`:

```groovy
dependencies {
    debugImplementation 'io.github.kevin321happy:httpdeck-android:0.1.0-alpha.3'
    releaseImplementation 'io.github.kevin321happy:httpdeck-android-noop:0.1.0-alpha.3'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

`MainActivity.kt`:

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

The empty `OkHttpClient.Builder` is intentional for a pure Flutter app: the
same panel/store is used, but no native traffic is captured.

## 4. Open the panel

There is no native toolbar in a pure Flutter app. Add a debug-only hidden
control channel:

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

Flutter:

```dart
GestureDetector(
  onLongPress: () => const MethodChannel(
    'io.github.kevin321happy.httpdeck/control',
  ).invokeMethod('show'),
  child: const Text('version'),
)
```

## 5. Verify

1. Run a debug build.
2. Trigger a Dio request.
3. Long-press the hidden entry.
4. The panel should show `source=flutter`.

## Limits

- Android only in v0.1.
- Release builds do not capture.
- `Stream` / `FormData` bodies are not read.
- Re-request is supported for native records only; Flutter re-request is
  roadmap.
