# Alpha Desktop 0.8.0 — watcher scenes and a seven-event preview cycle

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
