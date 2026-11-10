# Alpha Desktop 0.6.0 — direct play and local patches

- **Play patched Alpha**: direct local single-player launch, without opening the official Minecraft Launcher. Full Windows distribution includes Java 8.
- Applies install-time bytecode patches to a separate copy of the user's exact inspected a1.0.16 JAR. No runtime Java agent, ASM dependency or LaunchWrapper in the game process.
- Includes subtle environmental tunnel/canopy/sand/gravel and fog hooks. No jump scares, chat events, generated assets or added sounds. Use a new world; terrain edits persist.
- **Sound-loader fix included immediately**: registers original local recordings without the obsolete HTTP resource-list request. Copies compatible existing legacy recordings; offers folder import or an explicitly soundless test when none are found. Does not fabricate/download missing audio. A nonzero recording count does not guarantee the entire original sound collection is present.
- Preserves original JAR, Recovery worlds, other worlds and all launcher profiles. Modded data is separate under `%APPDATA%\AlphaDesktop\patched-engine\a1.0.16`.
- Requires the matching installed version JSON and previously downloaded libraries/natives. Missing dependencies are reported; no downloads occur. Only the inspected original JAR fingerprint is supported. Use an existing compatible launcher once to install dependencies if needed, not every time you play.
- Local single-player Player session only. No account-token reading, replacement sign-in system or authenticated multiplayer support.
- Automatic diagnostic reports include direct-launch/patch-install logs. Nothing is uploaded; review best-effort redaction before sharing.

## Verification and limits

The original vanilla Recovery installation is user-confirmed working. Locally patched the supplied exact client, confirmed its original fingerprint was unchanged and verified stack/local types for all 120 methods in modified/added classes. Automated tests cover local-library rules, checksum/path safety, native extraction, sound import, patch structure and fail-closed unsupported clients, alongside previous engine/recovery/agent tests and Windows packaging checks.

**These checks do not prove modded gameplay, native graphics or sound playback works on the user's PC.** Original-client gameplay was not run in this headless environment. No original client, decompiled proprietary source, patched game binary or Minecraft media is included in the release.
