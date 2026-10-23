# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| `v0.1.0-alpha.x` | Security fixes on a best-effort basis |

## Reporting

Please use GitHub Security Advisories for this repository. Do not open a public issue for a vulnerability that could expose tokens, cookies, customer data or a release bypass.

## Threat model

HttpDeck is a debugging tool that sees plaintext HTTP requests and responses after TLS termination. Treat captured data as sensitive.

### Default mode

- Debug builds only.
- In-memory only; no disk persistence, no upload.
- No `SYSTEM_ALERT_WINDOW` permission; the inspector is an in-app window.
- Sensitive headers, query parameters and JSON fields are redacted for display, share and cURL.

### Release-enabled mode

Release capture is opt-in and intended only for controlled internal/field builds.

- The real library must be included in the release variant.
- `enableInRelease = true` must be set explicitly.
- Inspector windows set `FLAG_SECURE`.
- Disabling the inspector clears in-memory records.

A local hidden switch is **not a security boundary**. Anyone who obtains the APK may be able to find and enable it. Do not ship release capture in a public store build without a server-side allowlist, short TTL and a remote kill switch.

## Data handling

- Do not share captured payloads publicly.
- Do not commit captured logs, HAR files or screenshots containing customer data.
- Redaction is best-effort; unknown business fields may still contain personal data.
- If you add exporters, keep redaction and explicit user action.

## Dependency and build security

- Keep OkHttp, AGP and Kotlin versions current.
- Review third-party PRs and CI changes.
- Do not add telemetry/upload without a design review.