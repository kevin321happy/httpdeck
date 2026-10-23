# Changelog

## v0.1.0-alpha.2

- Capture failures are isolated from the business response path.
- Skip SSE / multipart / octet-stream and oversized response bodies (no `peekBody` on large bodies).
- `install` / listener / facade calls no longer throw into the host.
- Added tests for streaming, multipart, large bodies and listener failures.
- Artifacts published to GitHub Pages Maven (`0.1.0-alpha.2`).

## v0.1.0-alpha.1

- Initial public alpha.
- Kotlin/JVM core: record model, in-memory ring buffer, redactor, OkHttp interceptor, re-request request factory.
- Android library: `HttpDeck.install`, `HttpDeck.show`, list/search/detail UI, share, cURL, re-request.
- Release no-op artifact (`httpdeck-android-noop`).
- Optional release-enabled mode (`enableInRelease = true`) with `FLAG_SECURE`.
- Native sample app and unit tests.
- Design spec for the Flutter/hybrid/pet roadmap.