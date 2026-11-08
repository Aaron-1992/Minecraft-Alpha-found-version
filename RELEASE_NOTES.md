## Native Java desktop preview

**Download `alpha-desktop-0.2.0.zip`, extract it, and use `run.bat` (Windows) or `./run.sh` (Linux/macOS). Requires an installed Java 17+ runtime; no Maven or compilation is needed to play this build.**

Supply your original Minecraft assets when launching, for example:

```text
run.bat --client-jar "C:\path\to\alpha.jar" --sounds "C:\path\to\sound"
```

```sh
./run.sh --client-jar "/path/to/alpha.jar" --sounds "/path/to/sound"
```

The standalone executable JAR is available as a separate download. Run it with `java -jar alpha-desktop-0.2.0.jar` followed by the same asset arguments. `SHA256SUMS.txt` lists download checksums.

### Included
- Native Java desktop game, not a browser application.
- Seeded voxel terrain, walking, jumping, swimming, breaking and placing blocks.
- Block inventory, world slots, options and compressed local saves.
- Original Minecraft texture loading and original Ogg audio playback; the Vorbis decoder is included.
- No generated media, synthesized sound effects, jumpscares or chat messages.

### Important limitations
- **Minecraft textures and recordings are NOT bundled.** Supply an original legacy Alpha client JAR and original legacy sound resources from your installation. Missing assets are reported explicitly, not substituted.
- This is an early independent prototype, not a complete or exact copy of Minecraft Alpha.
- No bundled Java runtime, platform-native installer, survival, infinite terrain, mobs, crafting or multiplayer.
- CI compiles the game, runs nine headless tests, checks the executable entry point, and verifies Ogg decoder packaging. It does not verify interactive graphics or audible playback on your desktop.

See the included README for asset locations, controls and troubleshooting. The ZIP includes the original audio-decoder dependency distributions and available source archives for their licensing information; no Minecraft asset redistribution is included.
