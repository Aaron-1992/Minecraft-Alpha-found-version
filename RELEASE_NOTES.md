# Alpha Desktop 0.7.0 — restored atmosphere, escalation and F6 preview

The user reports the capped 0.6.3 build no longer crashes. The 60 FPS cap, latest-session reporting and Java 8 legacy sort compatibility remain. This is not proof that the earlier AMD-driver timeout is fixed for every configuration.

- Restored extra original sounds and subtle fog drift. Leaf stripping stays removed, as requested.
- Added original wood footsteps and quiet pop alongside grass/stone/gravel. Normal volume/mute applies; missing original recordings cannot play. No generated audio or loud stingers.
- Added brief clusters of three original smoke particles near the edge of view on every second scheduled event. No custom assets, actual figure, stalking AI, flashing overlays, fake system errors or effects outside the game.
- Escalating event frequency during active gameplay: 4–9 minutes initially, 2–4 minutes after 20 minutes, 45–120 seconds after 40 minutes. Frequency is bounded; volume does not ramp up. Menus/paused ticks do not advance it. Timing resets on process restart.
- **F6** skips the ambience wait: one original sound request, smoke cue and a six-second stronger fog preview. Release before pressing again. Some keyboards use Fn+F6. No chat messages. F6 does not teleport to terrain structures or force unsafe terrain edits; those remain procedural.
- Each event request is logged with active tick, event number, sound name and preview status. Requests are not proof that a missing recording played or that a particle was visible. Session metadata now records enabled features.

Select **Play latest**. No extra launcher modes. Original JAR, old worlds/profiles and Recovery installation are untouched. Modded terrain changes persist in saves. No Minecraft client, patched game binary, proprietary source or media is redistributed.

Restoring several effects in one build is not a controlled isolation test. If a driver reset returns, stop retrying and export the latest session report. No claim is made that recurrence uniquely identifies a particular mod effect. Automated tests/bytecode and Windows packaging checks are not gameplay or GPU verification.
