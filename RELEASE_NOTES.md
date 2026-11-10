# Alpha Desktop 0.9.0 — returning sightings and longer sound sequences

The event palette expands from seven to fourteen types/variations. F6 previews them in order, one release-and-press per event:

1. Watcher
2. Crossing
3. Footstep echo
4. Unexplained click
5. Smoke
6. Fog drift
7. HUD drift
8. **Second look** — up to two delayed reappearances, alternating sides, using fresh safe placement checks.
9. **Orbit steps** — a short stopped-player footstep sequence with changing sound direction.
10. **Distant knocks** — quiet original wood sounds from farther away; not a new recording.
11. **False digging** — a short original gravel-sound sequence without block damage.
12. **Long watch** — a stationary sighting lasting up to eight seconds naturally, unless approach/look-away removes it earlier.
13. **Behind you** — an attempted distant sighting behind the initial view direction, not an in-your-face spawn.
14. **Afterimage** — brief watcher/particle/fog combination with one delayed return. Not an actual framebuffer afterimage or new shader.

## More pressure, still bounded

Initial natural event gaps are now 2–4 minutes. After 15 active minutes they become 60–150 seconds; after 35 minutes, 30–90 seconds. Menus/pausing do not advance the timer. Volume does not ramp up. Sound sequences contain only a few cues with 10–26-tick gaps and wait while the player is moving. The sound positions and event/sequence requests are recorded in the latest log.

Returning watchers remain distant: natural placement 27 blocks away, previews 20. No appearances within 12 blocks; approaching removes the figure and cancels that return opportunity. Reappearance can follow timeout or looking away. Ground and already-loaded-chunk checks can skip a scene; nothing teleports the player or edits terrain to force placement. Press F6 and wait if previewing a sequence; immediately advancing again replaces its pending sequence.

All watcher geometry/textures and recordings remain original Minecraft assets. Black tint is scoped/restored. No attacks, collision, saved watcher entities, new skins/audio, leaf stripping, flashing overlays, actual save corruption, fake system errors or deliberate crashes. Existing procedural terrain anomalies still persist in saves; cosmetic scenes do not. 60 FPS cap, Java 8 sorting compatibility and latest-session export remain.

Tests cover the complete F6 palette, bounded interval ranges, original sound-key whitelist, watcher non-persistence/non-collision and render-state cleanup. Automated tests and packaging checks do not verify GPU stability, scene visibility, sound availability or gameplay. If a driver timeout returns, stop retrying and export the latest session report.
