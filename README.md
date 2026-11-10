# Alpha Desktop 0.13.0

Native launcher for a locally patched copy of the user's installed Minecraft Alpha a1.0.16. No browser, generated assets, redistributed Minecraft client or replacement login system.

## One way to play

Install the full Windows EXE or extract the Windows portable distribution, then open AlphaDesktop. The launcher has only:

- **Play latest** — patches your installed original client locally and launches the latest build directly with bundled Java 8.
- **Save latest session report** — exports only the newest process session from this launcher. Review before sharing; nothing is uploaded.
- **Close**.

No official-launcher setup menu, Recovery-profile creation menu, old compatibility installer or custom-sandbox entry point is exposed. Existing old worlds/profiles are not deleted. Internal legacy classes remain for regression tests but old play CLI options are retired.

Requires your installed `versions/a1.0.16/a1.0.16.jar`, matching JSON and previously installed libraries/natives. No automatic dependency downloads. Accepts only inspected client SHA-256 `fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234`. Your original JAR is never overwritten. Local single-player only; no account/token reading or authenticated multiplayer.

## 0.13: forty new anomaly rules

The saved four-tier corruption scheduler now selects from **40 new context-dependent rules plus the previous 23 selections**. This includes inventory/durability drift, chest/furnace/sign changes, contextual audio, crops/paintings/boats, coordinated animal behavior, stone structures, a compact base echo/mirror room, original-biped sightings and a 15-second route replay, visual sky/time effects, music detuning, silence, bounded cave loops and one-chunk terrain scars.

**This update can permanently change mod worlds.** The native launcher makes a one-time local backup of existing saves at `patched-engine/a1.0.16/world-backups/before-0.13.0.zip` before this upgrade launches. It never overwrites that backup. Use a new/test world or a copy of an important world. New worlds created afterward need their own later backups. Original/Recovery worlds remain separate.

### Controls

- **F6:** previous nine director encounters, fourteen older variations, then forty new rules (63 selections total).
- **F8:** cycle just the forty new rules, starting with durability drift, chest shuffling, and the stop-triggered phantom step. Release between presses and provide the appropriate context. See the numbered catalogue below.
- **F7:** cancel transient effects and request safe actor/marker retirement. It is **not undo** for terrain, signs or inventory changes. A visible actor is retained until safely removable.

Previews bypass corruption tiers and repeat selection, not context, loaded terrain or visibility safeguards. Many rules need an actual chest, ore target, night, cave, tree, boat or remembered compact base. Repeatedly pressing a preview key replaces the current opportunity before it can develop.

### Progression, variety and visibility

Per-world `CorruptionLevel` (0–100), active ticks, 20-minute per-event repeat history and a sanitized compact base memory are stored through vanilla's `level.dat` save transaction. Tiers begin on active-play days 1, 5, 11 and 19. Paused/menu time does not count. A weighted unified pool replaces the old fixed 65% director split; exhausted eligibility stays quiet.

Mutation protection uses copied **actual renderer clipping planes**, a conservative current-facing cone, loaded-chunk voxel preflight and the original client's block-shape raycast. Missing/stale information blocks changes. Edits/spawns/removals in the frustum stay protected even through walls. Candidate searches and transaction sizes are bounded, and no new chunks are generated for an anomaly. Strict guards can skip scenes rather than force a visible pop.

No chat output, added loud audio, charging/attacking mod actors, instant-kill traps, full-screen jumpscare overlays, system tricks or deliberate crashes. No generated assets or redistributed client. The legacy global leaf-edit path remains disabled; new local canopy cuts and natural chunk scars are explicit bounded terrain rules.

### Exact scope, not an unlimited recreation

Read **[the full forty-rule catalogue, numbered F8 controls, adaptations and recovery details](docs/ANOMALIES.md)**. Important adaptations: base echoes are capped partial 9×6×9 captures on already-loaded terrain; cave loops stop after at most three repeats; boats rotate instead of teleporting; water is used instead of a suspended lava hazard; no native crouch or later-version items are invented; distant torchlight is a temporary original torch rather than a new lighting engine. The replay and fog watcher use original `char.png` and the original player biped, with no skin downloads.

78 authored JUnit tests cover controller, adapter, rule, history, visibility and cleanup behavior. Original-client patch/NBT/linkage checks and Windows EXE tests are separate. **These are not proof of gameplay pacing, camera-edge perfection, real audio playback or AMD/GPU stability.** If a driver timeout returns, stop retrying and save the latest session report.

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
