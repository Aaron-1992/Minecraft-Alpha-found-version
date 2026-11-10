# Alpha Desktop 0.9.0

Native launcher for a locally patched copy of the user's installed Minecraft Alpha a1.0.16. No browser, generated assets, redistributed Minecraft client or replacement login system.

## One way to play

Install the full Windows EXE or extract the Windows portable distribution, then open AlphaDesktop. The launcher has only:

- **Play latest** — patches your installed original client locally and launches the latest build directly with bundled Java 8.
- **Save latest session report** — exports only the newest process session from this launcher. Review before sharing; nothing is uploaded.
- **Close**.

No official-launcher setup menu, Recovery-profile creation menu, old compatibility installer or custom-sandbox entry point is exposed. Existing old worlds/profiles are not deleted. Internal legacy classes remain for regression tests but old play CLI options are retired.

Requires your installed `versions/a1.0.16/a1.0.16.jar`, matching JSON and previously installed libraries/natives. No automatic dependency downloads. Accepts only inspected client SHA-256 `fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234`. Your original JAR is never overwritten. Local single-player only; no account/token reading or authenticated multiplayer.

## Event palette and pacing

The event palette expands from seven to fourteen types/variations. F6 previews them in order, one release-and-press per event:

1. Watcher
2. Crossing
3. Footstep echo
4. Unexplained click
5. Smoke
6. Fog drift
7. HUD drift
8. **Second look** — up to two delayed reappearances, alternating sides, using fresh safe placement checks.
9. **Orbit steps** — a short stopped-player footstep sequence with changing sound direction.
10. **Distant knocks** — quiet original wood sounds from farther away; not a new recording.
11. **False digging** — a short original gravel-sound sequence without block damage.
12. **Long watch** — a stationary sighting lasting up to eight seconds naturally, unless approach/look-away removes it earlier.
13. **Behind you** — an attempted distant sighting behind the initial view direction, not an in-your-face spawn.
14. **Afterimage** — brief watcher/particle/fog combination with one delayed return. Not an actual framebuffer afterimage or new shader.

## More pressure, still bounded

Initial natural event gaps are now 2–4 minutes. After 15 active minutes they become 60–150 seconds; after 35 minutes, 30–90 seconds. Menus/pausing do not advance the timer. Volume does not ramp up. Sound sequences contain only a few cues with 10–26-tick gaps and wait while the player is moving. The sound positions and event/sequence requests are recorded in the latest log.

Returning watchers remain distant: natural placement 27 blocks away, previews 20. No appearances within 12 blocks; approaching removes the figure and cancels that return opportunity. Reappearance can follow timeout or looking away. Ground and already-loaded-chunk checks can skip a scene; nothing teleports the player or edits terrain to force placement. Press F6 and wait if previewing a sequence; immediately advancing again replaces its pending sequence.

All watcher geometry/textures and recordings remain original Minecraft assets. Black tint is scoped/restored. No attacks, collision, saved watcher entities, new skins/audio, leaf stripping, flashing overlays, actual save corruption, fake system errors or deliberate crashes. Existing procedural terrain anomalies still persist in saves; cosmetic scenes do not. 60 FPS cap, Java 8 sorting compatibility and latest-session export remain.

Tests cover the complete F6 palette, bounded interval ranges, original sound-key whitelist, watcher non-persistence/non-collision and render-state cleanup. Automated tests and packaging checks do not verify GPU stability, scene visibility, sound availability or gameplay. If a driver timeout returns, stop retrying and export the latest session report.


## Current-session diagnostics, not stale 0.5.2 logs

The previous scanner had a hard-coded 0.5.2 heading and combined multiple installations. The new export does neither. Every game launch creates `%APPDATA%\AlphaDesktop\sessions\launch-...` containing:

- `session.properties`: build, enabled features, start/end timestamps, state and exit code;
- `game.log`: stdout/stderr for that game process only;
- `hs_err_pid*.log`, if the JVM emits a native crash report;
- `launch-error.txt`, if launching/waiting reports an error.

`latest-session.properties` points to the newest session. The exporter follows only that session; it does not fall back to old launcher/Recovery logs. It includes bounded recent output even when no Java exception exists. An interrupted launcher may leave an unfinished state; a driver reset may leave no Java error at all. Reports use best-effort redaction; inspect before sharing. No report is automatically uploaded. Sessions are retained locally; no game files are bundled in reports.

Game worlds/resources remain in `%APPDATA%\AlphaDesktop\patched-engine\a1.0.16`. Setup can import locally available legacy recordings; missing originals are not generated or downloaded.

## Build and verification

`./test.sh` (or `test.bat`) runs engine, installation, diagnostics and direct-launch/session tests. Java 17 JDK required for the main app. `mvn -f agent/pom.xml clean package` builds/tests the Java 8 patch tools, then `mvn package` builds the launcher. GitHub Actions packages and smoke-tests the Windows distribution with bundled Java 8 and launcher runtime.

No Minecraft source/assets/client are committed or distributed. Unit tests, exact-client patch/bytecode checks and Windows EXE smoke tests do not constitute gameplay, AMD-driver or sound playback verification. Legacy instrumentation tests are retained as regressions, not an alternate exposed play mode.

Bundled Java 8 uses Temurin; upstream runtime sources/notices: https://github.com/adoptium/jdk8u and https://github.com/adoptium/temurin8-binaries. Runtime release metadata and notices accompany the Windows package. Third-party dependency distributions and available source archives accompany release packaging.
