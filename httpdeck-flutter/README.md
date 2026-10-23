# httpdeck_flutter

Flutter/Dio adapter for [HttpDeck](https://github.com/kevin321happy/httpdeck).

```yaml
dependencies:
  httpdeck_flutter:
    git:
      url: https://github.com/kevin321happy/httpdeck.git
      ref: v0.1.0-alpha.3
      path: httpdeck-flutter
```

```dart
await HttpDeckFlutter.install(dio: dio);
```

This sends Dio events to the Android host channel
`io.github.kevin321happy.httpdeck/network`; the host calls
`HttpDeck.recordFlutterEvent(...)`.

Debug builds capture by default. Release builds are disabled (`kDebugMode`).
Bodies are capped at 64KB; Stream/FormData bodies are not read.
