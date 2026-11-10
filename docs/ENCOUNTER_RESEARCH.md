# Encounter design research and implementation boundaries

This pass inspected public documentation and selected control-flow source files, not downloaded/running mod packs. No source, models, textures, sounds or datapack functions from these projects are included in this repository.

## References inspected

- [From The Fog](https://github.com/LunarEclipseStudios/From-The-Fog): its README presents sightings and environmental folklore; the [vanish event](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/player_spotted/spotted/vanish.mcfunction) reacts to a spotted figure and supports further activity/slipping behind; [stalking positioning](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spread_position/stalking/set_position.mcfunction) chooses distant terrain-relative positions. The repository states CC BY-NC-SA licensing and a non-AI-fork policy. This is **not a fork**, and no implementation or assets were copied.
- [Man From The Fog Reimagined](https://github.com/zenolth/man-from-the-fog-reimagined): README describes differing creature behaviors. The inspected [StalkState](https://github.com/zenolth/man-from-the-fog-reimagined/blob/main/src/main/java/dev/zenolth/the_fog/common/state_machine/states/the_man/StalkState.java), [StareState](https://github.com/zenolth/man-from-the-fog-reimagined/blob/main/src/main/java/dev/zenolth/the_fog/common/state_machine/states/the_man/StareState.java), and [FleeState](https://github.com/zenolth/man-from-the-fog-reimagined/blob/main/src/main/java/dev/zenolth/the_fog/common/state_machine/states/the_man/FleeState.java) distinguish observation, pursuit and retreat. Its README says the project moved to Codeberg; that external host was not accessed. No code/assets were reused.

## Conclusions used, written independently for Alpha

The key lesson is responsive encounters rather than an ever-longer random list. Our own pure-Java director has CUE, OBSERVE, ADVANCE, RETREAT, AFTERMATH and COOLDOWN phases, with separate fixture tests. Its adapter uses inspected Alpha members, loaded-chunk guards, bounded placement searches, sampled occlusion and yaw/pitch view tests. A quiet sound remains at a fixed world position so a player can investigate it. The figure advances only while not directly observed, stops under observation, retreats and ends rather than damaging or forcing the player.

No damage/chase/lunge/jumpscare mechanics from the references were adopted. No attack, door breaking, control lockout, destructive world edits, fake crash or custom media. This is not a claim of behavioral parity with either modern mod. The figure uses the user's original Alpha humanoid model/texture under scoped black tint.

## Tests versus gameplay

Pure director transitions are tested without Minecraft. The real scene adapter is additionally exercised against small authored API stand-ins: fixed sound anchors, movement while unseen, freezing when observed, wall occlusion, obstacle withdrawal, missing/unloaded chunks and F7 cleanup. Fixtures are not decompiled Minecraft code. The privately supplied client is used separately for structural/bytecode checks. No headless test proves actual graphics-driver stability or in-game audiovisual quality.
