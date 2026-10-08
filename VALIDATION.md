# Portfolio snapshot validation

Checked on October 7, 2026 against this source export.

- Node domain tests: 7 passed, 0 failed.
- Native Android debug build: `:app:assembleDebug` completed successfully. Generated APK verified as a readable archive containing its manifest and DEX code.
- Installation, physical-device behavior, and release signing were not validated in this publication pass.

CI runs the portable tests shown above. Third-party build recipes and Android SDK requirements are separate from these portable checks.
