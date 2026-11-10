# 0.13 anomaly catalogue and exact-client adaptations

These are original implementations using a1.0.16's original resources. No other mod's code/recordings/skins are included. Forty new rule handlers join the previous 23 preview selections (63 selections total). A selection is an **armed opportunity**, not a guarantee: missing context or visibility protection can skip it. Failed opportunities still consume their natural event's repeat lock, so the scheduler cannot hammer an unsuitable location.

## Common contract

- Weighted per-world tier selection. Four active-play tiers: days 1–4, 5–10, 11–18, and 19 onward; one day = 24,000 unpaused ticks. `CorruptionLevel`, active clock, stable event timestamps and the compact base memory are saved via vanilla's `level.dat` transaction. No system-clock tricks.
- Each natural event ID has a saved 24,000-active-tick / 20-minute repeat lock. Internal multi-cue sequences are one event. F6/F8 bypass tier/repeat selection for testing, **not** loaded-ground/context/visibility safeguards. Exhaustion means silence.
- Renderer `ka` supplies a copied six-plane frustum. Missing, malformed, stale (>250 ms) or moved (>2 blocks on either horizontal axis) camera snapshots block changes. A generously oversized current-facing cone protects against camera/input changes since the snapshot. A bounded voxel preflight checks loaded chunks before the exact client's `cj.a(ah,ah)` block-shape raycast. Unknown/unloaded paths fail closed. Even occluded objects inside the frustum remain protected: this intentionally over-protects rather than trusting partial occlusion.
- Mod-induced block/tile edits, painting/boat changes, actor spawns/removals, torch drops and delayed item reveal use these guards. Ordinary vanilla mining drops and other native game behavior are not suppressed globally. A protected actor may remain until looked away from; F7 never forcibly pops it out of view.
- Terrain transactions preflight every cell and adjacent liquid/unloaded hazards before writing. They reject occupied edit bounds, manufactured replacement targets, have a 2,048-cell ceiling, and attempt rollback on a rejected write. Normal block-neighbor updates still apply. This is not a general world-history/undo system.
- Maximum 128 inspected tile/entity candidates per bounded scan where applicable, a 301-tick motion ring, an eight-cue queue, one shadow, one delayed drop and one horizon marker. No competing wall-clock event threads. No shaders, full-screen overlays, attacks, chat text, added loud recordings or intentional crashes.
- New cues are at most 0.16 source gain before ordinary distance attenuation and game settings. Silence fades 2 seconds each way and never restores above the captured master volume. Music uses the actual `BgMusic` source, not modified files.

## All forty rules

`F8 #` counts presses from a fresh process. Release between presses; wait and provide the required context. F6 retains its previous first 23 selections, then uses this same new-rule order. Replacing a rule interrupts its pending opportunity.

| Tier | F8 # | Rule | Actual behavior / required context |
|---|---:|---|---|
| 1 | 1 | Tool durability drift | On hotbar-slot change, a held Alpha pickaxe gains 2 damage or loses 3; clamped below breakage; stack count unchanged. |
| 1 | 2 | Chest shuffling | In a hidden loaded chest 8–32 blocks away, move one coal from an earlier slot to empty slot 26. Coal is conserved. |
| 1 | 3 | Phantom step | A movement-to-stop transition emits one quiet gravel step eight blocks behind the current facing. |
| 1 | 4 | Door mimicry | A nearby actual `random.door_open` request is repeated at its original position eight ticks later. No recursive cue chain. |
| 1 | 5 | Crop/sapling reset | Reset nonzero metadata of a single hidden wheat/sapling block to its first stage. Never delete the plant. |
| 1 | 6 | Painting swap | Swap a hidden loaded 16×16 painting to another original 16×16 motive. No hitbox-size change. This operates while out of view, not in unloaded chunks. |
| 1 | 7 | Water ripple | A quiet original splash at a hidden nearby water block, skipped if other entities are nearby. |
| 1 | 8 | Fog breathing | After 15 seconds motionless, a smooth 10% fog-distance contraction/return over eight seconds. No overlay. |
| 1 | 9 | Phantom torch | A quiet wooden placement-like cue nine blocks behind, only with overhead cover. No block is placed. |
| 1 | 28 | Delayed drop | A hidden new cobblestone item is held for 30 ticks, longer if visible at reveal time. It remains a real world-owned/saved item throughout; its tick, pickup and render are held, not its inventory data deleted. Visible native drops are untouched. |
| 2 | 10 | Helpful stranger | Convert one existing raw pork in a hidden furnace to cooked pork, only if output can accept it. No item deletion or fuel requirement. |
| 2 | 11 | Extinguished trail | Remove up to three nearby hidden torches and spawn corresponding real torch item drops. Only one is used if only one qualifies. |
| 2 | 12 | Strip-mine intersection | In loaded underground natural stone/dirt, carve a seven-block 1×2 cross-tunnel with a floor-supported torch at one end. No exit/tunnel network or chunk generation. |
| 2 | 13 | Mimicked mining | Quiet stone strikes 12 blocks behind on an eight-tick rhythm, only while arm-swinging at ore. Stops on the same tick as swinging stops. |
| 2 | 14 | Animal hive-mind | Loaded sheep, pigs and cows within 30 blocks suspend normal ticks and face one elevated point for 400 ticks. Restore orientations/ticks on cancellation. |
| 2 | 15 | Staircase | With a nearby trunk and no overhead cover, carve a 15-step one-wide cobblestone descent through loaded natural material. Skip player-built blocks, fluids or an unsuitable entrance. |
| 2 | 16 | Boat relocation | Hidden, unoccupied boat turns toward a direction with sampled open water. Rotation only: no 15-block relocation or occupied-boat teleport. |
| 2 | 17 | Chest gift | One original red flower in an empty hidden center chest slot. Never overwrite an existing stack. No dead bushes/rotten flesh: those are not a1.0.16 items. |
| 2 | 18 | Sign erasure | Blank one nonempty line of a hidden sign and mark the tile dirty. Other lines remain intact. This is permanent. |
| 2 | 29 | Fog watcher | Original `char.png` and original `cn` player biped, placed near the render fog edge on loaded ground. A watched figure can walk sideways; removal waits until hidden. No attacks/AI. |
| 3 | 19 | Breathing cave | Underground/covered quiet original cow groan, distant and pitched to 0.5 (the legacy sound library's supported floor), not 0.4. |
| 3 | 20 | Doorway blockade | At night under cover, place one hidden dirt block beside a nearby door on suitable natural support. Does not overwrite objects or create a kill trap. |
| 3 | 21 | Inverted sounds | Dry sand movement produces short quiet swimming cues; rising movement can produce a stone step. Original sounds only. |
| 3 | 22 | Empty cave-in | Under cover, three low gravel cues behind the player; no physical gravel/falling-block entity. |
| 3 | 23 | Sun stutter | Freeze the rendered celestial angle for 90 seconds, then interpolate its shortest angular path back over two seconds. World time/spawning are unchanged. |
| 3 | 30 | Base echo | Remember an original-material 9×6×9 envelope around a compact chest/furnace/crafting-table cluster. Try a partial mossy/half-buried copy 200 blocks away, only if destination terrain is already loaded and hidden. No forced exploration, inventories, signs, fluids or unlimited full-house capture. |
| 3 | 31 | False daylight | Thirty seconds of blue renderer sky/fog with the sun quad collapsed. Real world time, block lighting and hostile spawning continue unchanged. No time/save manipulation. |
| 3 | 32 | Impossible water | Place a single stationary Alpha water source in a hidden cave air pocket. Vanilla stationary-water behavior waits for neighbor changes. Water only: no suspended lava hazard or replacement fluid simulation. |
| 3 | 33 | Inventory ghost | When opening an actual chest screen while armed, the hotbar renderer draws a phantom diamond pickaxe in its first empty slot once. No inventory mutation, pickup or click target. |
| 3 | 34 | Leaf geometry | Nine-leaf cross cut in one suitable hidden canopy. Never global leaf stripping; trunks and unrelated canopies remain intact. This local edit is permanent. |
| 4 | 24 | 15-second replay | A bounded 301-sample position/yaw/pitch/arm-and-limb ring drives a second original player biped. Replay stays at least 14 blocks away, needs loaded walkable ground and retires safely. Crouching is not native to this client, so none is invented. No account/custom-skin download. |
| 4 | 25 | Tape warp | Existing `BgMusic` gradually pitches from 1.0 to 0.5 over 30 seconds, pauses briefly, then restores. Does nothing when no music is playing. No OGG rewriting. |
| 4 | 26 | Lockstep footsteps | While moving, captured-position steps beside the player arrive three ticks late. Bounded 20-second sequence. |
| 4 | 27 | Silent world | Fade captured master volume to zero over two seconds, hold 30 seconds, fade back over two seconds. Restore on cancel/exit; preserve ordinary music/sound category settings. |
| 4 | 35 | Hallway loop | Match a recorded dry, walkable cave cross-section ~20 blocks behind along the same heading before relocating. Reject nearby entities/unsafe ground. Maximum three loops, ending on turn or stop—not an unbounded trap. |
| 4 | 36 | Base evacuation | Loaded hostiles inside the remembered base's 40-block neighborhood suspend normal AI and ground-step away for 30 seconds. Never step toward the player. Blocked terrain can stop them; this is not full pathfinding. |
| 4 | 37 | False horizon | An actual temporary original torch ~60 blocks away, on hidden suitable higher ground. Original torchlight/flame provides the illusion; not a new distant-light renderer. Remove offscreen after approach/timeout; never delete a player-replaced block. |
| 4 | 38 | Window watcher | At night, with hidden nearby glass, place a stationary dark biped ~15 blocks away. Movement from the starting position, glass removal or timeout requests retirement, still visibility-deferred. Ground placement is bounded, not a house/window architecture solver. |
| 4 | 39 | Chunk scar | One loaded 16×16 natural chunk beyond the fog edge, clearing up to 2,048 tree/grass/dirt cells in y48–100 down to stone. Reject constructed materials, fluids and the remembered base neighborhood. Can skip entirely if the beyond-fog area is unloaded. Permanent. |
| 4 | 40 | Mirror room | A hidden five-by-five interior cobblestone room in loaded deep natural stone. Chest/furnace/table offsets match the remembered compact base cluster exactly, only if those offsets fit the room. Empty containers, no copied loot. |

## Base memory, permanent changes and recovery

Base memory requires an already loaded chest/furnace pair within four blocks, a nearby crafting table and at least twelve eligible building-material blocks. It is a compact cluster heuristic, **not reliable identification of every player's main house**. The first matching cluster is remembered, with a sanitized 486-byte block envelope and nine layout integers in `level.dat`. Save data cannot turn the allowed template into TNT/lava.

Before 0.13 first launches an existing mod save folder, the native launcher makes `patched-engine/a1.0.16/world-backups/before-0.13.0.zip`. It is local, one-time and never overwritten. A corrupt backup aborts launch instead of being treated as success. Original Minecraft and Recovery saves remain separate. Close the game before restoring a backup; preserve the current folder elsewhere before extracting its `saves/` folder. New worlds created after that one-time backup need their own later backups.

F7 is **cancel**, not undo. It clears pending cues, fog, sky/time visuals, audio changes and suspended AI. It safely retires actors/temporary markers and releases held items; visible removals can wait. It does not reconstruct carved terrain, sign text, transferred inventory or changed durability. A queued delayed reveal may wait until hidden even after cancellation; the item remains world-owned.

## Verification boundaries

78 JUnit tests currently exercise the original controller, real reflective adapter, generated actors and authored API fixtures. These include positive cases for the new mutations/structures/audio/route systems, all forty bounded missing-context paths, tier/history/base-memory round trips, mutation rejections, renderer-plane copying, exact-ray invocation, one-render-only ghosting, F7 during deferred removal, and scoped render-state restoration. Desktop/save-backup suites and Windows Java 8 packaging run separately. Private local client checks validate patch/bytecode/NBT linkage without publishing that client.

None of these tests establish real gameplay pacing, visual accuracy, perceived audio volume, camera-edge perfection or AMD/GPU stability. The geometry/camera rules are intentionally conservative but need in-game testing across camera modes/settings. There is no claim of an exact recreation of another mod or every unlimited detail in the supplied prompt.
