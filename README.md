# Alpha Desktop 0.8.0

Native launcher for a locally patched copy of the user's installed Minecraft Alpha a1.0.16. No browser, generated assets, redistributed Minecraft client or replacement login system.

## One way to play

Install the full Windows EXE or extract the Windows portable distribution, then open AlphaDesktop. The launcher has only:

- **Play latest** — patches your installed original client locally and launches the latest build directly with bundled Java 8.
- **Save latest session report** — exports only the newest process session from this launcher. Review before sharing; nothing is uploaded.
- **Close**.

No official-launcher setup menu, Recovery-profile creation menu, old compatibility installer or custom-sandbox entry point is exposed. Existing old worlds/profiles are not deleted. Internal legacy classes remain for regression tests but old play CLI options are retired.

Requires your installed `versions/a1.0.16/a1.0.16.jar`, matching JSON and previously installed libraries/natives. No automatic dependency downloads. Accepts only inspected client SHA-256 `fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234`. Your original JAR is never overwritten. Local single-player only; no account/token reading or authenticated multiplayer.

## New scenes

- A transient **black humanoid watcher**, reusing Alpha's original zombie model/texture under black tint, not a generated skin. It attempts to appear on suitable ground in an already-loaded chunk, 27 blocks away (20 for previews). No nearby/in-your-face spawning, new terrain generation or block edits for placement.
- The watcher has no AI tick, attacks, collision, damage response or persistence. It expires after about 3.25 seconds naturally (8 seconds in preview), disappears on approach within 12 blocks, or after being seen and then looking away. It is removed on menu/world changes. It can be hidden by terrain; a request is not guaranteed visibility.
- A crossing variation briefly moves the watcher sideways before it disappears. This is a short scene, not general stalking/pathfinding AI.
- Renderer black tint is scoped to the watcher model draw. Original models/textures are used; normal mobs retain their normal renderer path. GL color/lighting state is saved and restored, including when the model draw throws.

## Sounds and apparent deterioration

- Footstep-echo event schedules three quieter original stone steps after the player stops moving. Unexplained original click/pop/wood/grass/gravel cues and smoke scenes remain. Normal sound controls apply; missing recordings remain unavailable.
- Temporary smooth fog-distance drift and cosmetic 1–2-pixel text-position drift suggest instability without rewriting text, game settings or saves. Opening a menu clears text drift. This is not texture/mesh corruption or a simulated system crash.
- Existing escalation: 4–9-minute active-play intervals initially, 2–4 minutes after 20 minutes, 45–120 seconds after 40 minutes. Frequency is capped; volume does not ramp up. Timers reset on process restart.

## F6: preview the actual event types

Each release-and-press advances one event: **Watcher → Crossing → Footstep echo → Unexplained click → Smoke → Fog drift → HUD drift**, then repeats. Some keyboards require Fn+F6. Watcher placement still respects loaded terrain/ground checks; it does not teleport the player or force unsafe placement. Scene spawn/skip/removal and event names are recorded in the latest session log, not in chat. No more all-effects-at-once preview.

## Retained safeguards and verification limits

60 FPS cap, Java 8 sort compatibility, latest-session-only export and original-sound loading fix remain. Leaf stripping stays removed. Prior procedural corridor/sand/gravel edits remain and persist in saves; new watcher/HUD effects do not persist. No generated assets, loud stingers, jumpscares, fake system errors, control lockout or deliberate crashes. Original Minecraft clients/code/media are not distributed.

Added generated-class fixtures for non-collision/non-persistence and render-state cleanup, plus event-cycle/interval tests. The privately supplied client was patched and bytecode-checked; the watcher was constructed against the actual client in a headless JVM and its no-collision/no-serialization overrides exercised. **These are not GPU rendering, original-game gameplay, watcher visibility or audio verification.** Earlier AMD timeout cause remains unknown. If a driver reset returns, stop retrying and export the latest session report.


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
