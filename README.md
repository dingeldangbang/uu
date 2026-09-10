# Agent Mobile

Offline-first Android agent app for Android 11–16.

## Scope

- `minSdk 30` (Android 11), `targetSdk 36` (Android 16)
- Compose UI with an explicit edge-to-edge layout
- Optional HTTPS cloud adapter (`GET /health`, `POST /api/v1/runFlow`)
- Android Keystore-backed AES-GCM storage for the endpoint and bearer token
- Room history/document storage with a safe LIKE fallback for local context
- Optional LiteRT-LM `.litertlm` model import; models are deliberately not stored in Git or bundled into the APK
- Cloud failures fall back to the local path and never log the bearer token

The cloud service is intentionally adapter-based because no production endpoint was supplied. Configure it from the Settings screen after installing the APK.

## Build requirements

- JDK 17
- Gradle 8.13 via the checked-in wrapper
- Android SDK Platform 36 and Build Tools 35.0.0

```bash
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Release signing

A release keystore is never committed. To create a local release keystore:

```bash
./scripts/create-release-keystore.sh
./gradlew :app:assembleRelease
```

The script stores the keystore and its properties below `.secrets/`, which is ignored by Git. Back up the keystore and password before publishing updates. Verify a release APK with:

```bash
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
```

## Local model

The app accepts a compatible LiteRT-LM `.litertlm` model through Settings. Model files are large and model licenses vary, so no model is downloaded or committed automatically. Use a model from a source whose license permits your intended distribution.

## Cloud contract

The current adapter expects:

```http
GET /health
Authorization: Bearer <token>   # optional, depending on the service

POST /api/v1/runFlow
Authorization: Bearer <token>
Content-Type: application/json

{"request":"...","context":["..."]}
```

Expected response:

```json
{"result":"..."}
```

`output` is also accepted as a compatibility alias. The real backend must be tested separately once its HTTPS URL and contract are available.

## Compatibility notes

Android 15+ imposes execution limits on `dataSync` foreground services. This project does not use a persistent data-sync foreground service for agent requests; the request is scoped to the visible app and the model engine is explicitly closed when replaced. Long-running background synchronization should be added as a WorkManager job only after a concrete sync contract exists.
