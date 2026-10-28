# Agent Instructions & Guidelines

This repository is one of three in the HeCoExp stack: `hecoexp` (infrastructure), `hecoexp-server` (NestJS API), and `HealthConnectExport` (this one).

**The rules are canonical in the main repository and apply here unchanged:**

→ [grubfx-dev/hecoexp/blob/development/docs/development.md](https://github.com/grubfx-dev/hecoexp/blob/development/docs/development.md)

That document defines the definition of done, the documentation rules and the conventions. Read it before making a change.

**Scope:** this repository is the Android app only. The system-level documentation — how the components fit together, the API contract, the phase plan, the release pipelines — lives in [grubfx-dev/hecoexp](https://github.com/grubfx-dev/hecoexp). Document here only what is specific to the app and not covered there.

**Commits** follow Conventional Commits. The release pipeline derives the SemVer version from them, so the commit message is what determines the next release.
