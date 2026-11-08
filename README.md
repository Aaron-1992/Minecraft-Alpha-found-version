# Alpha — native Java desktop

A **desktop Java game**, not a website, WebView, browser game, or Java HTTP wrapper. The browser implementation has been removed.

**No generated assets:** no synthesized audio, AI images, replacement textures, custom texture atlas, generated logo, or substitute font. Surface pixels, menus, font, crosshair and hotbar are read from **original Minecraft files supplied by you**. Audio is playback of original Minecraft Ogg recordings, decoded with VorbisSPI. If a required asset is missing, startup fails with its name rather than inventing a replacement.

Procedural world generation and rendering are game logic, not replacement media assets.

## Requirements

- A desktop computer running Windows, Linux or macOS, with a display, mouse, keyboard and audio output.
- **JDK 17+** and **Maven 3.8+** to build.
- Your own legacy Minecraft Alpha client JAR with `terrain.png`, `gui/gui.png`, `gui/icons.png`, and `font/default.png`.
- Original legacy Minecraft sounds, including `step/grass*.ogg`, `step/stone*.ogg`, `step/wood*.ogg`, and `step/sand*.ogg`.

No Minecraft client, recordings or textures are bundled in this repository. The earlier modern texture extracts have also been removed. Assets are read locally and are not copied, rewritten, generated or downloaded by this application.

The visual target is early Alpha, associated with the original Herobrine screenshot. **This is still an independent, bounded prototype, not an exact copy of Alpha's engine or an authentic lost release.** Use your own Alpha assets for the historical look; modern Minecraft JARs are not supported.

## Download a built release

Use the [GitHub Releases page](https://github.com/Aaron-1992/Minecraft-Alpha-found-version/releases) and download the desktop ZIP. Extract it, install **Java 17+**, and use `run.bat` or `./run.sh` with your original asset paths. The executable JAR and audio decoder are already built; **Maven and a JDK are only needed if you build from source**.

The release also provides a standalone executable JAR and SHA-256 checksums. Original Minecraft assets and a Java runtime are not included. GitHub Actions runs the headless tests, compiles and packages the executable, verifies its main class and Vorbis service registration, then publishes a prerelease targeting the exact build commit.

## Build and launch

```sh
mvn clean package
java -jar target/alpha-desktop-0.2.0.jar \
  --client-jar "/path/to/your/alpha-client.jar" \
  --minecraft-dir "/path/to/.minecraft" \
  --sounds "/path/to/original/sound"
```

Windows PowerShell: enter the `java -jar ...` command on one line. `build.bat` and `run.bat` are also included. On macOS/Linux, use `./build.sh` and `./run.sh` with the same arguments.

The executable JAR includes the Ogg decoder and runs without Maven after building. It does **not** include Minecraft assets. Do not run the individual classes or an unshaded JAR if you want Ogg decoding; use the packaged executable JAR.

### Finding local assets

If installed by your launcher, the client may be at:

```text
.minecraft/versions/a1.0.16/a1.0.16.jar
```

That path is detected automatically under the standard Minecraft directory. You can always select a different legacy client with `--client-jar`. Launchers may use other version names or directories. Installing an old client does not necessarily install its old sound resources.

Original sound files can be read from:

- `--sounds /path/to/sound`, containing `step/grass1.ogg`, etc.
- `.minecraft/resources/sound/`
- `.minecraft/assets/virtual/legacy/sound/` or `sounds/`
- `.minecraft/assets/objects/`, resolved through the launcher's local asset indexes for legacy `sound/step/...` or `sounds/step/...` names.

Use original legacy recordings, not a newly generated sound pack. The app does not silently map modern sounds to old ones. If your installation has no legacy recordings, supply them from your own existing backup or installation. No proprietary asset downloader or account bypass is included.

To check original texture loading, Ogg decoding and desktop audio availability without opening the game:

```sh
java -jar target/alpha-desktop-0.2.0.jar --client-jar "/path/to/alpha.jar" --sounds "/path/to/sound" --check-assets
```

This prints the exact local source paths. It validates file structure and decoding, **not historical authenticity or ownership**.

## Playing

Select **Singleplayer**, then one of five world slots. An empty slot asks for a seed; leave it blank for a new world. Existing slots load from disk.

| Input | Action |
|---|---|
| W A S D / arrows | Move |
| Mouse | Look |
| Space | Jump / swim upward |
| Shift | Move faster |
| Left click | Break targeted block |
| Right click | Place selected block |
| 1–9 / mouse wheel | Select block |
| E | Open/close block inventory |
| Escape | Pause/resume, release mouse |

Worlds are stored in `saves/World1.dat` through `World5.dat`, compressed and saved every 15 seconds, on pause, and on quit. `--saves /path/to/folder` changes the location. Saves use a custom format, not original Minecraft save compatibility. Writes use an atomic replacement where supported.

## Scope and atmosphere

The game includes seeded 128 × 128 × 48 worlds, trees, layered terrain, beaches, water, collision, gravity, jumping, swimming, block breaking/placing, nine selectable blocks, inventory, options and persistent worlds.

The restrained atmosphere uses solitude, limited visibility and barely perceptible continuous fog variation. No jumpscares, horror-labelled menus, entities, chat messages, generated sound effects or scripted encounters.

Rendering is a **native Java CPU voxel raycaster** presented through AWT/Java2D at a nearest-neighbor-scaled 320 × 180 internal resolution. It is not the original Minecraft renderer. Water is static; original animated water/lava are not reproduced. Transparent surfaces are approximate. There is no infinite terrain, crafting, survival, mob simulation, dynamic lighting or multiplayer. Desktop performance varies by CPU. No hand or character skin is fabricated.

## Tests and verification

```sh
./test.sh
# Windows:
test.bat
```

Nine headless tests cover seeds, terrain/water/trees, collision and edits, compressed save round-trips, corrupt-save rejection, DDA targeting and placement normals, water targeting, original atlas coordinates and explicit rejection of missing assets. No fake texture or sound fixtures are generated.

The Java sources were compiled and all nine tests passed in the development sandbox. The native desktop window and audio output could not be exercised here: this sandbox has no desktop display, audio device or user-owned Alpha client/resources. Executable Maven packaging and decoder verification run in GitHub Actions before a release is published; inspect the linked release workflow for the outcome. Interactive desktop/audio testing still requires a real desktop and original Minecraft assets.

## Rights

Unofficial fan prototype; not affiliated with Mojang or Microsoft. Minecraft names, textures, fonts, recordings and other assets belong to their respective owners. Supplying local assets does not grant redistribution rights. The repository contains no Minecraft asset license or game JAR. VorbisSPI and its transitive decoder dependencies have their own upstream licenses; consult their Maven artifacts before redistributing a packaged build.
