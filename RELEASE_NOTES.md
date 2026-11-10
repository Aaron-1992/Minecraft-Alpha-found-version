# Alpha Desktop 0.10.0 — reactive encounters, not just more random cues

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
