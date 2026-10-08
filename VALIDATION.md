# Portfolio export validation

Checked October 7, 2026 against this source export.

| Check | Result |
| --- | --- |
| JavaScript rehearsal domain tests | 7 passed |
| Kotlin native execution model tests | 7 passed |
| Android debug and unsigned release builds | Passed |
| Debug and release lint | Zero errors; 20 warnings in each lane |
| Installed-app emulator tests, Android 36 arm64 | 8 passed |

The Kotlin tests cover preview isolation, approval blocking, distinct skip/handoff evidence, undo preserving user edits, pause/resume, retry after Calendar failure, and bounded inputs. The connected Android suite covers local persistence, restored approval, invalid storage, Calendar intent contents, installed approval controls, Activity recreation, background interruption, and observed fold signals versus manual previews. See the README for reproducible commands; connected tests clear this app's workspace and should run on a disposable test instance.

GitHub Actions runs JavaScript tests plus native unit tests, debug lint, and debug app/test compilation. Connected Android tests are a separate emulator/device lane.

Physical foldable/controller behavior, OEM backup compliance, real Calendar-provider interoperability, vendor integrations, release signing, and long-duration qualification remain unverified. Fold signals in the automated suite are injected through AndroidX WindowManager's testing adapter. A Calendar handoff is not proof that the user saved an event. Samsung ecosystem automation is not connected.

Lint warnings include dependency update notices, presentation/localization recommendations, and existing renderer allocation notices. They are recorded rather than hidden behind a baseline.
