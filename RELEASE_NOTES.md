## 0.5.0 — optional original-engine environmental mod

**Download `AlphaDesktop-0.5.0.exe` for Windows x64.** Java for the setup/custom app is bundled. The installer is unsigned; verify its source/checksum before deciding whether to run it.

### Enable it

1. Open AlphaDesktop and choose **Original Alpha (recommended)**.
2. Close Minecraft Launcher, then choose **Install environmental changes**.
3. Select your installed Alpha client if asked. Setup checks its chunk class layout and refuses the optional mod if the layout is ambiguous or unsupported.
4. Open your normal launcher → Java Edition → Installations → **Alpha Found** → Play.
5. **Create a new world.** Do not import a valued save into this experimental installation.

The mod uses a **new isolated game directory** under `%APPDATA%\AlphaDesktop\ambient-engine\<version>`. The existing vanilla profile **Alpha Found - Original Alpha**, original JAR and other worlds remain separate.

### The intended feeling

An environmental blend of early Minecraft folklore and lost-version stories—not a literal implementation of every creepypasta or a verified historical account. The game itself has no horror-labelled menu, story exposition, chat messages, jump scares, loud stingers, fake crashes or replacement assets. Changes are deliberately sparse; immediate sightings are not guaranteed.

The agent adds only original-block environmental patterns and a very slow, bounded fog drift. It does not add pursuing entities, custom skins, white-eye textures, gore, new audio, text signs or scripted attacks.

### Technical / safety notes

- A bundled **Java 8-compatible runtime agent** modifies a structurally inspected chunk class and LWJGL fog calls **in memory**. The original client JAR is never rewritten or distributed.
- It uses only original numeric block IDs and the game's existing rendering. No media is generated or bundled.
- Rare chunk candidates are coordinate-deterministic, skip the central origin area and refuse chunks containing non-natural/manufactured block IDs. Each chunk object receives at most one mutation attempt after natural terrain is present. These checks do **not** prove a chunk has never been edited by a player.
- Block changes that the original client saves **persist**. Removing the agent does not revert them. Keep backups and use a new world.
- Structural compatibility is not full runtime compatibility. The actual user-owned Alpha client, its chunk lifecycle, its lighting updates and visible results have **not been integration-tested here**. Optional transformation failures leave the original class unmodified and report to launcher logs, never game chat.
- This is neither universal across all Alpha builds nor a claim of support for every old creepypasta. Vanilla setup remains available separately; the custom sandbox does not receive these hooks.

CI tests the mutation policies, transformed fixture classes, live Java-agent loading on Java 8 and 17, native app/profile safety, fullscreen, embedded-agent packaging and the Windows launcher. Fixtures are this project's own code/terrain arrays—not Minecraft bytecode or generated textures/sounds.

### Disabling

Close Minecraft/Launcher and remove the `-javaagent:...alpha-ambient-agent-0.5.0.jar=...` argument from **Alpha Found → Edit → More Options → JVM arguments**, or use the separate vanilla installation. Restore a pre-mod world backup to undo terrain changes. Profile-file backups remain next to the original launcher profile files.

Downloads include the Windows installer/portable ZIP, cross-platform JAR distribution and SHA-256 checksums. Original Minecraft client/assets are not bundled. See README for spoiler details, build instructions and limitations.
