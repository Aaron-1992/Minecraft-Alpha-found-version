# Alpha Desktop 0.11.0 — folklore encounters with different rules

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
