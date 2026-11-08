# Alpha Found — original engine setup + native sandbox

## Recommended: use the real Alpha engine

The custom software renderer cannot reproduce all of Minecraft Alpha's behavior just by reusing its textures. This release adds **Original Alpha mode**, which prepares an isolated installation in your **existing official Minecraft Launcher**. The launcher then runs your installed, unmodified Alpha client, including its real renderer, world generation, menus, lighting, audio and gameplay. No game code is decompiled, extracted into this project, patched, or redistributed.

**This mode is an original-game installation helper, not a modified Alpha client.** It does not add the custom sandbox's fog drift or new horror effects, and does not launch the game directly from our EXE. Performance and appearance are those of your original Alpha client, subject to its compatibility with your hardware/launcher. There are no benchmarked speedup claims for the original game.

### Download and play on Windows

1. Download **`AlphaDesktop-0.4.0.exe`** from [GitHub Releases](https://github.com/Aaron-1992/Minecraft-Alpha-found-version/releases), install it and open AlphaDesktop. Java for the setup/custom sandbox is bundled.
2. Choose **Original Alpha (recommended)**.
3. **Close Minecraft Launcher**, then confirm setup. Select your installed Alpha JAR if asked, such as `.minecraft/versions/a1.0.16/a1.0.16.jar`.
4. Open your usual Minecraft Launcher → Java Edition → Installations. Select **Alpha Found - Original Alpha**, then **Play**.

The normal launcher manages your account, original-game libraries and its Java runtime. This tool never asks for credentials, changes login settings, downloads a game JAR, or bypasses ownership checks. You must have an installed `old_alpha` version with its matching version JSON; launch that version normally once before using this mode.

**Unsigned preview:** the Windows installer is unsigned; Windows may show an unknown-publisher/SmartScreen warning. Verify the source and release checksums before deciding whether to run it. For a portable version, extract the whole `AlphaDesktop-0.4.0-windows-x64-portable.zip`, keeping `AlphaDesktop.exe`, `app` and `runtime` together.

### What original-engine setup changes

- Adds/updates only the `alpha-found-original-engine` installation entry in existing `launcher_profiles.json` and/or `launcher_profiles_microsoft_store.json` files. Other installations, selected profile, unknown properties and account-related metadata are preserved.
- Makes an exact backup next to each changed profile file, using `.alpha-backup-<unique-id>`. Backups remain local. Profile writes are atomic where supported, and a detected concurrent change aborts the write. Keep the launcher closed during setup.
- Uses `%APPDATA%\AlphaDesktop\original-engine\<version>` for isolated original-game options/worlds on Windows, or `~/.alpha-desktop/original-engine/<version>` elsewhere. It never converts or overwrites your custom-sandbox worlds or normal Minecraft worlds.
- Creates conservative Alpha graphics options **only if the isolated options file does not already exist**. Existing options and user tuning in this tool's launcher entry are preserved.
- Copies only **existing original** legacy recordings from local resources/asset indexes into the isolated game's resource folder, without changing them. Hashed asset objects are checked against their recorded SHA-1. No missing recordings, textures or sounds are generated or substituted. The normal launcher/client still determines resource compatibility; this helper cannot create resources absent from your installation.

The original client handles fullscreen (normally F11), render distance and graphics settings. We do not override its renderer, Java selection, or memory settings.

### Undo

With the launcher closed, remove the **Alpha Found - Original Alpha** installation through the launcher or restore the corresponding `.alpha-backup-*` file. The separate `original-engine/<version>` folder can be removed if you no longer want its worlds. **Do not delete it if you want to retain those saves.** This helper does not remove it automatically.

## Custom sandbox (fallback)

Choose **Custom sandbox** at startup, or run the standalone JAR with `--prototype`. This remains an independent, bounded Java/Java2D voxel prototype, not the original Minecraft engine or an exact Alpha port. No browser, WebView or HTTP server is involved.

Original Minecraft assets are required: a legacy JAR containing `terrain.png`, GUI/icons and `default.png` or `font/default.png`, plus original legacy `step/grass*.ogg`, `step/stone*.ogg`, `step/wood*.ogg` and `step/sand*.ogg` recordings. Native dialogs select and remember these paths. Missing assets are reported, never replaced by generated media.

Original title textures (`title/mclogo.png`, `gui/logo.png`, `logo.png`) and cloud masks (`environment/clouds.png`, `clouds.png`) are loaded when present. If absent, clouds are omitted and the title uses the original bitmap font. All block surfaces, font, hotbar, crosshair and recordings come from original assets supplied locally; no generated replacement textures, fonts, logos or synthesized effects are bundled.

### Fallback performance changes in 0.4.0

- Precomputed normalized camera rays instead of per-pixel square roots every frame.
- Original cloud pixels cached outside the render loop.
- Transparent texels continue the same DDA grid walk rather than allocating another hit object and marching in approximate 0.08-unit steps. This also avoids skipping whole foliage volumes behind a transparent texel.
- Paused/menu/inventory screens reuse the last rendered world frame; buffer restoration does not rerun the world renderer.

These remove identifiable overhead. End-to-end FPS improvements have **not** been measured on a user's original assets/hardware, so no percentage or FPS promise is made.

### Fallback controls

| Input | Action |
|---|---|
| W A S D / arrows | Move |
| Mouse | Look |
| Space | Jump / swim upward |
| Shift | Slow movement with ledge protection |
| Left / right click | Break / place selected block |
| 1–9 / mouse wheel | Select block |
| E | Inventory |
| Escape | Pause / resume |
| F11 or Options | Borderless fullscreen, restore previous window |

Fallback worlds use `%APPDATA%\AlphaDesktop\saves\World1.dat` through `World5.dat` (or `~/.alpha-desktop/saves/`). They save every 15 seconds, on pause and on quit. Saves are a custom format, **not** original Minecraft saves. The original-engine mode has its own separate directory and real Minecraft save format.

The fallback has 128 × 128 × 48 worlds, static water, trees, nine selectable blocks, collision, gravity, mining/building, inventory and options. It lacks infinite terrain, survival, crafting, mobs, dynamic light propagation and multiplayer. Rendering remains a 480 × 270 native CPU raycaster, with aspect-correct scaling/letterboxing; it is not the original GPU renderer. The foreground block uses original terrain faces, not a fabricated skin.

### Local asset paths for the fallback

Legacy sounds are looked up through local `assets/indexes`/`assets/objects`, `.minecraft/resources/sound` or `newsound`, and `assets/virtual/legacy/sound`, `sounds` or `newsound`. An explicit sound folder can also be selected. Modern recordings are not silently mapped to legacy names.

Setup stores selected asset paths in the per-user `assets.properties`; original-engine setup remembers its client location separately in `original-engine.properties`. No account credentials are stored by the app.

## Build / CLI

Source builds require **JDK 17+ and Maven 3.8+**:

```sh
mvn clean package
java -jar target/alpha-desktop-0.4.0.jar
```

`build.bat` / `build.sh` also build the shaded JAR with its Ogg decoder. Cross-platform JAR downloads require Java 17+; the Windows EXE bundles Java for this app. The official launcher separately manages the runtime for the original game.

```sh
# Prepare the real Alpha installation (native dialogs):
java -jar target/alpha-desktop-0.4.0.jar --original

# Custom sandbox, with native asset dialogs:
java -jar target/alpha-desktop-0.4.0.jar --prototype

# Custom sandbox with explicit local asset paths:
java -jar target/alpha-desktop-0.4.0.jar \
  --client-jar "/path/to/alpha.jar" \
  --minecraft-dir "/path/to/.minecraft" \
  --sounds "/path/to/original/sound"
```

`--setup` changes fallback asset selections. `--saves` overrides fallback save location. `--check-assets` validates fallback images/Ogg decoding/audio output, and `--check-runtime` checks the bundled decoder without loading game assets. `run.bat` / `run.sh` work with extracted JAR distributions too.

## Tests and release verification

```sh
./test.sh
# Windows: test.bat
```

There are **25 headless tests**: 14 engine/asset-path tests and 11 launcher/optimization tests. They check original-version metadata layout, safe launcher JSON round-trips, preservation of other profiles/settings, exact backups, immutable client files, rejection of malformed files, resource-path safety and precomputed-ray equivalence. Fixtures contain configuration/metadata only, not proprietary game code, fabricated texture pixels or recordings.

CI also runs **three real AWT fullscreen tests** under Xvfb, builds the executable, verifies Ogg SPI packaging and creates the Windows EXE/installer. It smoke-tests the generated Windows launcher and bundled Java before publishing.

**Not verified here:** launching a real original Alpha client through a user's official launcher, original-asset visual parity, audible playback, full gameplay, installation UI and physical Windows multi-monitor behavior. Those need a real installed client, launcher, original resources, audio output and desktop. A metadata-only inspection is not proof that a particular original client will run successfully.

## Rights / scope

Unofficial fan project; not affiliated with Mojang or Microsoft. Minecraft code, assets and marks belong to their respective owners. No Minecraft JAR, source code, textures or recordings are distributed with the release. Using a local installation does not grant redistribution rights. The real-engine mode runs the user's unmodified client through their launcher; it does not transplant copyrighted classes into the custom engine.

The custom sandbox retains restrained fog variation and solitude without jumpscares, entities or chat events. **Original-engine mode is unmodified vanilla Alpha, not a new horror mod.** VorbisSPI and its dependencies have their own upstream licenses; their original distributions and available sources accompany the packaged app.
