# Galaxy Flow

A local Android workspace with saved notes, priorities, app-level focus, observed window/fold information, and an approval-gated Calendar draft handoff. A companion browser app rehearses illustrative workflows without accessing device services.

## Engineering highlights

- Pure Kotlin execution model with pause, explicit resume, approval, skip, recovery, and undo.
- Private local persistence; interrupted runs pause on restoration. Saved notes survive undo.
- Lifecycle-aware execution stops in the background and refreshes controls on return.
- AndroidX WindowManager reports the current window's fold features; manual posture tabs are clearly labeled previews.
- Standard Android Calendar insertion intent opens a generic draft only after approval. The user decides whether to save it in Calendar. The app records the handoff, not a saved event.
- No network, location, Calendar access, or notification policy permissions. Cloud backup and device transfer exclude local workspace data.
- Separate JavaScript rehearsal state machine and responsive browser interface.

App focus hides Galaxy Flow's planning rail; it does not alter system notifications. Undo restores the prior local workspace/focus flags and keeps notes. A reminder saved in Calendar must be managed in Calendar. Samsung DeX, SmartThings, Watch, Buds, and vendor automation are not integrated. External display detection does not establish DeX.

## Run the browser rehearsal

Requires Python 3. No dependency install is needed.

```sh
python3 -m http.server 4174
```

Open `http://localhost:4174`. Device visuals, office context, and service connections in the browser are mock data.

## Build and test Android

Install JDK 17 and Android SDK 36; set `JAVA_HOME` and `ANDROID_HOME` for your system.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

The connected tests require a disposable emulator or test device: they clear this app's local workspace state. They cover persistence, interrupted execution, restored approval, corrupted storage, Calendar intent boundaries, Activity recreation, and undo. The debug APK is under `app/build/outputs/apk/debug/`. Release distribution requires your own signing configuration.

## Browser tests

Requires Node.js with its built-in test runner.

```sh
npm test
```

## Code map

| Component | Location |
| --- | --- |
| Browser rehearsal model | `src/flow-engine.js` |
| Browser UI | `src/app.js`, `styles.css` |
| Native interface and window adapter | `app/src/main/java/com/galaxyflow/app/MainActivity.kt` |
| Native execution model | `app/src/main/java/com/galaxyflow/app/FlowEngine.kt` |
| Private persistence | `app/src/main/java/com/galaxyflow/app/FlowStore.kt` |
| Calendar handoff adapter | `app/src/main/java/com/galaxyflow/app/CalendarDraft.kt` |
| Native unit tests | `app/src/test/java/com/galaxyflow/app/FlowEngineTest.kt` |
| Android device tests | `app/src/androidTest/java/com/galaxyflow/app/WorkspaceDeviceTest.kt` |

See [validation](VALIDATION.md) for observed results and remaining hardware scope. Original project code has no blanket license grant in this snapshot.


## Publication guard

Requires Python 3. Install the pre-push guard separately in each clone; Git does not clone local hooks.

```sh
python3 tools/install_publication_guard.py
python3 tools/test_public_export.py
python3 tools/check_public_export.py
```

The guard checks every reachable commit's author and committer against matching GitHub noreply identities, scans historical file contents and commit messages for common disclosure patterns, and rejects generated/credential files, symlinks, submodules, and unreviewed binaries. Errors identify Git objects without printing suspicious values. The retained Gradle wrapper is allowed only at its reviewed SHA-256; provenance is recorded in `tools/public-export-policy.json`. Gradle distribution downloads also have pinned checksums.

The installer preserves existing hooks and custom hook settings. CI runs the same checks against full history. These checks complement manual review; they are not an exhaustive privacy guarantee. CI checks occur after a push, so use the local hook to catch mistakes before upload. Intentional code contributions should use the contributor's public GitHub handle and corresponding noreply email.
