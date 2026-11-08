# Alpha Found — original engine setup + native sandbox

## Recommended: use the real Alpha engine

The custom software renderer cannot reproduce all of Minecraft Alpha's behavior just by reusing its textures. This release adds **Original Alpha mode**, which prepares an isolated installation in your **existing official Minecraft Launcher**. The launcher then runs your installed, unmodified Alpha client, including its real renderer, world generation, menus, lighting, audio and gameplay. No game code is decompiled, copied into this project or redistributed; an optional bundled runtime agent can apply in-memory hooks without rewriting the original JAR.

**This mode uses your existing launcher, not direct game execution from our EXE.** Choose vanilla setup or explicitly install experimental environmental changes through a Java runtime agent. Performance and appearance are those of your original Alpha client, subject to its compatibility with your hardware/launcher. There are no benchmarked speedup claims for the original game.

### Download and play on Windows

1. Download **`AlphaDesktop-0.5.0.exe`** from [GitHub Releases](https://github.com/Aaron-1992/Minecraft-Alpha-found-version/releases), install it and open AlphaDesktop. Java for the setup/custom app is bundled.
2. Choose **Original Alpha (recommended)**, close Minecraft Launcher, then choose **Install environmental changes**. Select your installed Alpha JAR if asked.
3. Setup inspects its compiled class signatures without decompiling or executing it. If a compatible chunk layout cannot be uniquely identified, modded installation is refused; vanilla setup remains available.
4. Open your usual Minecraft Launcher → Java Edition → Installations → **Alpha Found** → Play. **Create a new world.**

For an unmodified client, choose **Vanilla Alpha** during setup and use the separate **Alpha Found - Original Alpha** installation instead.

The normal launcher manages your account, original-game libraries and Java 8+ runtime. This tool never asks for credentials, changes login settings, downloads a game JAR, or bypasses ownership checks. You must have an installed `old_alpha` version with its matching version JSON; launch that version normally once before using this mode.

**Unsigned preview:** Windows may show an unknown-publisher/SmartScreen warning. Verify the source and release checksums before deciding whether to run it. Portable users must extract the whole `AlphaDesktop-0.5.0-windows-x64-portable.zip`, keeping `AlphaDesktop.exe`, `app` and `runtime` together.

### Experimental original-engine changes

The optional agent is written by this project, targets Java 8+ and contains **no Minecraft classes or media**. It is copied into the isolated modded game folder and referenced by a quoted `-javaagent:` option in only the new **Alpha Found** profile. ASM supplies bytecode instrumentation; the original JAR remains unchanged. Bootstrap hook visibility supports an isolated game classloader without rewriting LaunchWrapper.

The game keeps its original menus and assets. There are no horror-labelled in-game controls, chat messages, jump scares, fake crashes, loud stingers, custom skins or synthesized sounds. This is an atmospheric blend of old Minecraft folklore, **not a literal collection of every creepypasta**. Some later folklore is not from Alpha's era; no claim of historical/canonical accuracy is made.

<details>
<summary>Implementation details / spoilers</summary>

Roughly one in 128 coordinate-selected chunks is eligible, excluding the central origin area. Depending on natural terrain, an eligible chunk may receive a small two-by-two underground corridor, one stripped canopy, a small stepped sand formation, or a level gravel seam. Eligibility does not guarantee a visible change. No named monster or pursuing entity is implemented.

The hook waits for natural stone to exist in the chunk array, runs at a no-argument chunk lifecycle call and makes at most one mutation attempt per chunk object. It refuses the entire chunk if any block ID is outside a conservative natural-block whitelist; individual patterns also validate their target volume. This is not proof that an apparently natural chunk has never been edited by a player.

Fog endpoints share the same smoothly changing factor, bounded to a few percent and faded in over three minutes. This changes the existing rendering values rather than adding textures, geometry overlays or audio.

</details>

**Use a new world.** Terrain changes saved by Minecraft persist; removing the agent does not undo them. Actual original-client compatibility, chunk lifecycle timing, lighting recalculation and visible results could not be verified without the user's exact client and desktop. Structural inspection and fixture tests are not a substitute for that integration test. If transformation fails, the original class is left unchanged and a diagnostic goes to launcher logs, not chat. That may mean no effects appear.

The custom sandbox is separate and does **not** receive these original-engine hooks.

### What original-engine setup changes

- Adds/updates only this project's `alpha-found-ambient` (modded) or `alpha-found-original-engine` (vanilla) installation entry in existing `launcher_profiles.json` and/or `launcher_profiles_microsoft_store.json` files. Other installations, selected profile, unknown properties and account-related metadata are preserved.
- Makes an exact backup next to each changed profile file, using `.alpha-backup-<unique-id>`. Backups remain local. Profile writes are atomic where supported, and a detected concurrent change aborts the write. Keep the launcher closed during setup.
- Uses `%APPDATA%\AlphaDesktop\ambient-engine\<version>` for the modded profile and `%APPDATA%\AlphaDesktop\original-engine\<version>` for vanilla for isolated original-game options/worlds on Windows, or `~/.alpha-desktop/original-engine/<version>` elsewhere. It never converts or overwrites your custom-sandbox worlds or normal Minecraft worlds.
- Creates conservative Alpha graphics options **only if the isolated options file does not already exist**. Existing options and user tuning in this tool's launcher entry are preserved.
- Copies only **existing original** legacy recordings from local resources/asset indexes into the isolated game's resource folder, without changing them. Hashed asset objects are checked against their recorded SHA-1. No missing recordings, textures or sounds are generated or substituted. The normal launcher/client still determines resource compatibility; this helper cannot create resources absent from your installation.

The original client handles fullscreen (normally F11), render distance and graphics settings. The optional mod changes fog values and chunk arrays in memory, not Java selection or unrelated JVM options.

### Undo

With the game and launcher closed, remove the **Alpha Found** or **Alpha Found - Original Alpha** installation, or restore its corresponding `.alpha-backup-*` profile file. To disable only the mod, remove this project's `-javaagent:` argument under Edit → More Options → JVM arguments. **This does not undo terrain already saved: restore a pre-mod world backup for that.** The separate `original-engine/<version>` folder can be removed if you no longer want its worlds. **Do not delete it if you want to retain those saves.** This helper does not remove it automatically.

## Custom sandbox (fallback)

Choose **Custom sandbox** at startup, or run the standalone JAR with `--prototype`. This remains an independent, bounded Java/Java2D voxel prototype, not the original Minecraft engine or an exact Alpha port. No browser, WebView or HTTP server is involved.

Original Minecraft assets are required: a legacy JAR containing `terrain.png`, GUI/icons and `default.png` or `font/default.png`, plus original legacy `step/grass*.ogg`, `step/stone*.ogg`, `step/wood*.ogg` and `step/sand*.ogg` recordings. Native dialogs select and remember these paths. Missing assets are reported, never replaced by generated media.

Original title textures (`title/mclogo.png`, `gui/logo.png`, `logo.png`) and cloud masks (`environment/clouds.png`, `clouds.png`) are loaded when present. If absent, clouds are omitted and the title uses the original bitmap font. All block surfaces, font, hotbar, crosshair and recordings come from original assets supplied locally; no generated replacement textures, fonts, logos or synthesized effects are bundled.

### Fallback performance changes in 0.5.0

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
mvn -f agent/pom.xml clean package
mvn clean package
java -jar target/alpha-desktop-0.5.0.jar
```

`build.bat` / `build.sh` also build the shaded JAR with its Ogg decoder. Cross-platform JAR downloads require Java 17+; the Windows EXE bundles Java for this app. The official launcher separately manages the runtime for the original game.

```sh
# Prepare the real Alpha installation (native dialogs):
java -jar target/alpha-desktop-0.5.0.jar --original

# Custom sandbox, with native asset dialogs:
java -jar target/alpha-desktop-0.5.0.jar --prototype

# Custom sandbox with explicit local asset paths:
java -jar target/alpha-desktop-0.5.0.jar \
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

There are **28 main-app headless tests** plus **11 agent unit tests** and fresh-JVM live instrumentation smoke tests on Java 8 and 17. Run `mvn -f agent/pom.xml clean package` to execute the agent tests. They check original-version metadata layout, safe launcher JSON round-trips, preservation of other profiles/settings, exact backups, immutable client files, rejection of malformed files, resource-path safety and precomputed-ray equivalence. Fixtures contain configuration/metadata only, not proprietary game code, fabricated texture pixels or recordings.

CI also runs **three real AWT fullscreen tests** under Xvfb, builds the executable, verifies Ogg SPI and embedded Java 8-agent packaging and creates the Windows EXE/installer. It smoke-tests the generated Windows launcher and bundled Java before publishing.

**Not verified here:** launching a real original Alpha client through a user's official launcher, original-asset visual parity, audible playback, full gameplay, installation UI and physical Windows multi-monitor behavior. Those need a real installed client, launcher, original resources, audio output and desktop. A metadata-only inspection is not proof that a particular original client will run successfully.

## Rights / scope

Unofficial fan project; not affiliated with Mojang or Microsoft. Minecraft code, assets and marks belong to their respective owners. No Minecraft JAR, source code, textures or recordings are distributed with the release. Using a local installation does not grant redistribution rights. The real-engine mode runs the user's unmodified client through their launcher; it does not transplant copyrighted classes into the custom engine.

The custom sandbox retains restrained fog variation and solitude without jumpscares, entities or chat events. **Vanilla original-engine setup is unmodified; the separate opt-in Alpha Found profile enables experimental runtime changes.** VorbisSPI and its dependencies have their own upstream licenses; their original distributions and available sources accompany the packaged app.
