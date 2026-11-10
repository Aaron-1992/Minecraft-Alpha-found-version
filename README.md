# Alpha Desktop 0.10.0

Native launcher for a locally patched copy of the user's installed Minecraft Alpha a1.0.16. No browser, generated assets, redistributed Minecraft client or replacement login system.

## One way to play

Install the full Windows EXE or extract the Windows portable distribution, then open AlphaDesktop. The launcher has only:

- **Play latest** — patches your installed original client locally and launches the latest build directly with bundled Java 8.
- **Save latest session report** — exports only the newest process session from this launcher. Review before sharing; nothing is uploaded.
- **Close**.

No official-launcher setup menu, Recovery-profile creation menu, old compatibility installer or custom-sandbox entry point is exposed. Existing old worlds/profiles are not deleted. Internal legacy classes remain for regression tests but old play CLI options are retired.

Requires your installed `versions/a1.0.16/a1.0.16.jar`, matching JSON and previously installed libraries/natives. No automatic dependency downloads. Accepts only inspected client SHA-256 `fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234`. Your original JAR is never overwritten. Local single-player only; no account/token reading or authenticated multiplayer.

This pass inspected From The Fog and Man From The Fog Reimagined documentation/selected encounter logic. The new implementation is original; none of their code or assets is included. See `docs/ENCOUNTER_RESEARCH.md` in the source for references and boundaries.

## Five encounter modes with actual state transitions

**Cue → Observe → Advance → Retreat → Aftermath → Cooldown.** Episodes respond to player position, yaw/pitch, sampled wall occlusion and investigation distance. They end after a bounded duration even if ignored.

- **Stalker encounter:** a quiet cue precedes a distant figure. It can advance in ground-checked steps while you are not directly observing it. Looking at it stops the advance; sustained observation makes it retreat. One delayed repositioned return is possible.
- **Sound lure:** cues originate at a fixed world location, not attached to your camera. Approaching that location can trigger a separate distant sighting. If ignored, a timeout still progresses/ends the encounter.
- **Interception:** an ahead-of-view encounter selected naturally while moving. The figure withdraws rather than attacking or blocking the player.
- **Pass-by:** a brief sideways retreat scene instead of a stationary timer-only apparition.
- **Turn around:** an initially behind-view encounter with the same observation/retreat behavior and one possible return.

New encounter modes have a 65% natural selection weight; the existing 14 ambient variations remain. Sheltered positions favor the lure; stationary players do not naturally select interception. Ordinary gaps still escalate from 2–4 minutes, to 60–150 seconds after 15 active minutes, to 30–90 seconds after 35 minutes. A director episode prevents another natural event from interrupting it; cooldown/maximum duration keep it bounded.

## Controls and stronger diagnostics

- **F6** now previews the five new encounters first, then the previous 14 events. Press once, release, and wait: most previews begin with cues and show a figure after roughly 1.3 seconds. Lure previews react to investigation or can take about eight seconds. Repeated presses replace the current episode, rather than showing all its stages.
- **F7** cancels the scene, pending director cues/extra echoes and transient HUD/fog effects. It leaves at least 20 seconds before the next naturally scheduled event; it is not a permanent off switch.
- Logs record selected mode, phase transitions, anchored sound requests, actual spawn/skip/removal, blocked movement and cancellation. Session metadata records the enabled director and controls.

## Ground and sight checks

Placement searches multiple candidate positions, checks footprint/head clearance and refuses unloaded chunks. Movement samples ground and clearance; walls/ledges terminate the movement instead of being crossed or edited. A sampled visibility test plus yaw AND pitch replaces the old yaw-only "seen" test. No intended approach within 14 blocks. This is a bounded encounter controller, **not full Minecraft pathfinding** or combat AI.

Black tint remains scoped to the original model and render-state cleanup. All audio uses existing original recordings and normal volume/mute; fixed-position cues use a modest fixed gain, not escalating loudness. Missing recordings still cannot play. No new assets, leaf stripping, flashing, chat hints, save corruption, player teleportation or deliberate crashes. Existing terrain anomalies persist as before; watcher/director effects are transient. FPS cap, sort compatibility and latest-session-only reports remain.

## Verification

Added seven pure-director tests and five adapter integration tests using authored API stand-ins. These exercise observation freeze, advance/retreat, lure investigation/timeout, approach removal, missing figures, termination/cooldown, fixed sound positions, wall occlusion, obstacles, unloaded-chunk refusal and F7 cleanup. Existing watcher rendering-cleanup/serialization fixtures and launcher tests remain.

Automated/bytecode/Windows package checks are not actual gameplay, sound availability, watcher visibility or AMD-driver verification. If a driver reset returns, stop retrying and export the latest session report.


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
