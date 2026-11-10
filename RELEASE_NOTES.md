# Alpha Desktop 0.6.3 — one launcher, current-session reports

- Replaced the entry point with **Play latest / Save latest session report / Close**. No old compatibility, official-launcher setup, Recovery-profile creation or custom-sandbox play modes are exposed. Existing installations/worlds are left alone.
- Fixed misleading diagnostics: removed the hard-coded 0.5.2 heading from the legacy scanner and replaced the public report path with newest-session-only exports. Every game process has its own log, build/start/end/state/exit metadata and native JVM error-file destination. Old launcher/Recovery logs are not mixed in or used as fallback.
- Report exports bounded recent output even if the driver reset produced no Java exception. Best-effort redaction; review before sharing. Nothing is uploaded.
- Added a 60 FPS limiter before Minecraft display updates. Disabled the additional scheduled sound events to remove that variable during investigation. Normal original game sounds and the local sound registration fix remain. No new visual effects; leaf stripping and fog changes remain disabled.

**Repeated AMD driver timeouts remain unresolved.** This is not a verified driver fix. Do not repeatedly retry after a reset. No Windows registry, driver or timeout settings are altered. No claim is made that the new audio caused the timeout or that limiting FPS resolves it.

Requires the inspected installed original Alpha client, matching JSON and local libraries/natives; full Windows package bundles Java 8. Original client and launcher profiles are not modified. Local single-player only. No proprietary client/code/media is redistributed.

Regression tests cover latest-session isolation, exit metadata, missing new session without stale fallback, explicit extra-audio disable flag and frame-cap patch ordering, alongside existing suites. Packaged checks are not original-client gameplay or AMD-driver verification.
