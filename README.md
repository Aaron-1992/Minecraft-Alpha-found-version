# Alpha Desktop 0.12.0

Native launcher for a locally patched copy of the user's installed Minecraft Alpha a1.0.16. No browser, generated assets, redistributed Minecraft client or replacement login system.

## One way to play

Install the full Windows EXE or extract the Windows portable distribution, then open AlphaDesktop. The launcher has only:

- **Play latest** — patches your installed original client locally and launches the latest build directly with bundled Java 8.
- **Save latest session report** — exports only the newest process session from this launcher. Review before sharing; nothing is uploaded.
- **Close**.

No official-launcher setup menu, Recovery-profile creation menu, old compatibility installer or custom-sandbox entry point is exposed. Existing old worlds/profiles are not deleted. Internal legacy classes remain for regression tests but old play CLI options are retired.

Requires your installed `versions/a1.0.16/a1.0.16.jar`, matching JSON and previously installed libraries/natives. No automatic dependency downloads. Accepts only inspected client SHA-256 `fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234`. Your original JAR is never overwritten. Local single-player only; no account/token reading or authenticated multiplayer.

Four new encounter modes join the reactive director; this is now nine encounter modes plus fourteen ambient variations. These are original folklore-inspired scenarios, not literal recreations of named characters or other mods. All figures still reuse the original Alpha humanoid model with black tint. No new skins, generated assets or borrowed mod recordings.

## New encounters

- **Treeline / Herobrine-style sighting:** placement requires an actual nearby log/trunk on suitable already-loaded terrain. Direct observation for eight ticks makes the figure vanish rather than advance. Up to two repositioned returns are possible. No leaf removal, tree damage or new tree generation; treeless terrain can skip the scene.
- **Null-inspired lurker:** a farther-off-angle black figure disappears under sustained direct observation. It never advances and does not return during that encounter. This is not a screen blackout or damage effect.
- **Lost footsteps:** original footstep sounds are emitted from recorded positions along your recent route, rather than following the camera. The route buffer is in memory and capped at 40 active ticks. Moving produces cues; a sufficiently sharp turn after the opening delay can trigger a separate distant sighting. If you ignore it, a bounded timeout ends/progresses it. Ground under the player selects an existing stone/grass/wood/sand/gravel sound family.
- **Repetition / Far Lands-style unease:** four identical anchored cues at fixed intervals, accompanied by temporary small HUD-position and fog-distance drift. No entity is spawned. This is a repetition illusion, not real Far Lands generation, broken geometry or file corruption.

## Controls and behavior

F6 previews the nine director modes first: Stalk → Lure → Interception → Pass-by → Turn-around → Treeline → Null lurk → Lost footsteps → Repeater. The fourteen older variations follow. Release between presses, then wait for the encounter to develop. Move during Lost footsteps; the treeline encounter needs trees. F7 cancels active/pending scenes, as before.

The nine director modes share the existing 65% natural director-selection weight, rather than stacking more independent timers. Sheltered locations favor lure or repetition; movement-dependent natural selections are redirected when stationary. Existing escalation/cooldowns, minimum-distance checks, sampled gaze/occlusion, grounded movement, original-sound loading, 60 FPS cap and latest-session logs remain.

No attacks, jumpscares, chat hints, control lockout, leaf stripping, player teleportation, save damage, desktop effects or deliberate crashes. New effects are transient; previously implemented procedural terrain edits persist as before.

## Verification and limits

39 mod/director/adapter tests include new rules for bounded treeline returns, Null's non-advancing behavior, movement/turn reaction, exactly four repetition cues without a figure, actual tree-required placement, and recorded-route sound origins. Test fixtures are authored stand-ins, not Minecraft source. Java 8 and Windows packaging checks are retained. Automated and bytecode checks do not prove original-game audiovisual quality, watcher visibility, resource availability or graphics-driver stability. If an AMD timeout returns, stop retrying and export the latest session report.


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

## 0.12.0 — persistence and visibility foundations

This is the first foundation batch for the requested 40+ anomaly design, **not a completed 40-event implementation**. The existing 23-event preview palette remains.

- Persist `CorruptionLevel` (0–100), `AlphaActiveTicks`, and stable `AlphaLast_<event>` timestamps inside the vanilla `level.dat` Data compound. No separate save-file writer; vanilla retains its normal save/backup transaction.
- Four active-play tiers: day 1–4, 5–10, 11–18, and 19 onward. Days mean 24,000 active ticks, not wall-clock dates. Pausing/menu time does not advance corruption. Existing worlds start counting from this update.
- Natural events use tier-gated weighted selection. Each event has a 24,000-active-tick (20 minute) repeat lock that survives normal save/reload. Exhaustion produces silence. F6 intentionally bypasses eligibility without recording a natural occurrence. Cooldowns are per world; progression is monotonic.
- Conservative visibility guard: protect an oversized view cone around the camera, checking model extremities. Spawn only outside it, defer removal while inside it. This deliberately protects through walls too; it is **not yet the actual renderer frustum**. F7 cancels cues immediately but a visible actor remains until safely removable. Inventory and pause screens freeze the scene; world exit cleans up actors in the no-longer-rendered world.
- Replace sampled observation rays with bounded voxel traversal; unloaded reads remain forbidden. This is block-level traversal, not exact Alpha block-shape collision.
- Disable legacy terrain mutations and smoke-particle pops pending a proper visibility-aware mutation transaction. No chest/sign/inventory tampering, destructive leaf carving, chunk scars, teleport loops or base replication added.
- 42 authored automated tests, including persistence round-trip, cooldown boundary, world isolation, tier exhaustion, and visibility-aware F7 behavior. Packaging and bytecode verification do not establish gameplay/GPU stability.
