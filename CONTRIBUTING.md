# Contributing

Thanks for helping HttpDeck.

## Scope

The alpha focuses on the Android/OkHttp capture core and inspector. Flutter and hybrid support are planned separately. Keep PRs focused and avoid unrelated refactors.

## Development

Requirements:

- JDK 17
- Android SDK 34
- Gradle wrapper included (`./gradlew`)

Commands:

```bash
./gradlew :httpdeck-core:test
./gradlew :sample-android:assembleDebug
```

If your environment cannot reach Google Maven, use a local mirror in your own Gradle init script; do not commit machine-specific paths or mirrors.

## Pull requests

- One behavior change per PR.
- Add or update tests for core logic.
- Keep public API changes documented in `README.md` and `docs/quickstart-android.md`.
- No company-specific endpoints, credentials, package names or customer data.
- No automatic release capture in the default release artifact.
- Include a short "what/why/how tested" description.

## Code style

- Kotlin official style; 4-space indent.
- Prefer small, focused files.
- Public API needs KDoc.
- No secrets in logs, tests or sample data.

## Security

Read [SECURITY.md](SECURITY.md). Do not add disk persistence, upload, or release bypass without an explicit design review.