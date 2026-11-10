# Alpha Desktop 0.6.3

Native launcher for a locally patched copy of the user's installed Minecraft Alpha a1.0.16. No browser, generated assets, redistributed Minecraft client or replacement login system.

## One way to play

Install the full Windows EXE or extract the Windows portable distribution, then open AlphaDesktop. The launcher has only:

- **Play latest** — patches your installed original client locally and launches the latest build directly with bundled Java 8.
- **Save latest session report** — exports only the newest process session from this launcher. Review before sharing; nothing is uploaded.
- **Close**.

No official-launcher setup menu, Recovery-profile creation menu, old compatibility installer or custom-sandbox entry point is exposed. Existing old worlds/profiles are not deleted. Internal legacy classes remain for regression tests but old play CLI options are retired.

Requires your installed `versions/a1.0.16/a1.0.16.jar`, matching JSON and previously installed libraries/natives. No automatic dependency downloads. Accepts only inspected client SHA-256 `fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234`. Your original JAR is never overwritten. Local single-player only; no account/token reading or authenticated multiplayer.

## Driver-timeout status — unresolved

The user reported repeated AMD driver timeouts, including with fog disabled. **This release is not a confirmed driver fix.** It adds a 60 FPS limiter using LWJGL's `Display.sync(60)` before Minecraft's display updates to avoid unbounded frame submission. Scheduled extra sound events are disabled to remove that variable while investigating the reported roughly seven-minute failure. Original ordinary gameplay audio and local sound registration fix remain.

No Windows registry, driver or timeout settings are changed. Do not repeatedly relaunch if the driver resets. No new visual effects are added. Frame limiting and disabling optional audio events are mitigations, not proof of the root cause or GPU compatibility.

The TimSort legacy sorting setting remains. Leaf stripping and fog changes remain disabled. Existing rare corridor/sand/gravel effects remain; terrain edits persist in saves. Saved missing leaves are not automatically repaired.

## Current-session diagnostics, not stale 0.5.2 logs

The previous scanner had a hard-coded 0.5.2 heading and combined multiple installations. The new export does neither. Every game launch creates `%APPDATA%\AlphaDesktop\sessions\launch-...` containing:

- `session.properties`: build, start/end timestamps, state and exit code;
- `game.log`: stdout/stderr for that game process only;
- `hs_err_pid*.log`, if the JVM emits a native crash report;
- `launch-error.txt`, if launching/waiting reports an error.

`latest-session.properties` points to the newest session. The exporter follows only that session; it does not fall back to old launcher/Recovery logs. It includes bounded recent output even when no Java exception exists. An interrupted launcher may leave an unfinished state; a driver reset may leave no Java error at all. Reports use best-effort redaction; inspect before sharing. No report is automatically uploaded. Sessions are retained locally; no game files are bundled in reports.

Game worlds/resources remain in `%APPDATA%\AlphaDesktop\patched-engine\a1.0.16`. Setup can import locally available legacy recordings; missing originals are not generated or downloaded.

## Build and verification

`./test.sh` (or `test.bat`) runs engine, installation, diagnostics and direct-launch/session tests. Java 17 JDK required for the main app. `mvn -f agent/pom.xml clean package` builds/tests the Java 8 patch tools, then `mvn package` builds the launcher. GitHub Actions packages and smoke-tests the Windows distribution with bundled Java 8 and launcher runtime.

No Minecraft source/assets/client are committed or distributed. Unit tests, exact-client patch/bytecode checks and Windows EXE smoke tests do not constitute gameplay, AMD-driver or sound playback verification. Legacy instrumentation tests are retained as regressions, not an alternate exposed play mode.

Bundled Java 8 uses Temurin; upstream runtime sources/notices: https://github.com/adoptium/jdk8u and https://github.com/adoptium/temurin8-binaries. Runtime release metadata and notices accompany the Windows package. Third-party dependency distributions and available source archives accompany release packaging.
