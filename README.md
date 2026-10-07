# Galaxy Flow

A local-first workflow rehearsal prototype with a browser interface and a native Kotlin Android app. It models how a workflow moves through preview, execution, pause, resume, recovery, and undo while adapting its visual stage to foldable-device postures.

## Engineering highlights

- Pure JavaScript domain state machine with Node's built-in test runner.
- Native Kotlin lifecycle model and Android interface.
- Responsive browser UI and native posture preview renderer.
- Explicit action boundaries, simulated execution, and recoverable failure states.

The browser and native apps are prototypes. Device execution, Samsung DeX, and ecosystem integrations are simulated rather than connected to vendor services.

## Run the browser app

Requires Python 3. No dependency install is needed.

```sh
python3 -m http.server 4174
```

Open `http://localhost:4174`.

## Run tests

Requires Node.js with its built-in test runner.

```sh
npm test
```

Tests exercise flow creation, posture selection, preview completion, live capsules, pause/resume, undo, and failure handling.

## Build Android

Install JDK 17 and Android SDK 36, and set `JAVA_HOME` and `ANDROID_HOME` for your system.

```sh
./gradlew :app:assembleDebug
```

The APK is generated under `app/build/outputs/apk/debug/`. Release distribution requires your own signing configuration.

## Code map

| Component | Location |
| --- | --- |
| Browser domain model | `src/flow-engine.js` |
| Browser UI | `src/app.js`, `styles.css` |
| Domain tests | `test/flow-engine.test.js` |
| Native interface | `app/src/main/java/com/galaxyflow/app/MainActivity.kt` |
| Native lifecycle | `app/src/main/java/com/galaxyflow/app/FlowEngine.kt` |
| Posture renderer | `app/src/main/java/com/galaxyflow/app/FoldStageView.kt` |

## Status

Future work includes actual fold posture adapters, persistence, and vendor integrations. This snapshot does not claim validated hardware automation. Original project code has no blanket license grant in this snapshot.
