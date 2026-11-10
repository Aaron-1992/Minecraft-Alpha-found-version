# Alpha Desktop 0.6.1 — Alpha renderer sorting compatibility

The user reported a world-creation crash in 0.6.0: `IllegalArgumentException: Comparison method violates its general contract!`, from Java 8 TimSort called by Alpha's renderer (`e.a`, `ig.c`).

Direct play now includes `-Djava.util.Arrays.useLegacyMergeSort=true`, selecting Java 8's legacy sorting implementation instead of TimSort. This targets the reported failure; it does not repair Alpha's comparator or constitute user-confirmed world-creation success. No launcher profiles or Recovery worlds are changed. Launch-command tests assert the setting is present.

The environmental mod is unchanged: rare straight underground corridors, stripped tree crowns, stepped sand, level gravel seams, and subtle fog drift. No Herobrine entity, stalking AI, apparitions, custom audio, messages or jumpscares. This remains a limited environmental implementation, not a complete collection of creepypasta mechanics.

The direct-launch setup retains the local sound registration fix from 0.6.0 and imports existing original recordings. Missing audio is not generated or downloaded. Requires the inspected installed a1.0.16 client, matching JSON and local libraries/natives. Windows distribution bundles Java 8. Local single-player only; no account-token reading or authenticated multiplayer.

Original client, patched game binaries, decompiled source and Minecraft media are not distributed. Automated tests and packaging checks are not original-game gameplay verification.
