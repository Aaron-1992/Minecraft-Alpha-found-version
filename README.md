# Alpha Found — original engine setup + native sandbox

## Still getting exit code 1? Start here

**0.6.0 adds experimental direct single-player launch and install-time patches for the inspected a1.0.16 client.** The user confirmed the separate vanilla Recovery installation runs. Modded gameplay and sound playback on the user's PC are not yet verified.

### Play without the official launcher

Use the full Windows installer or portable distribution (includes Java 8), open AlphaDesktop and select **Play patched Alpha**. `--play-direct` bypasses the startup menu. Select your installed `versions/a1.0.16/a1.0.16.jar` if prompted. Its matching JSON and already downloaded libraries/natives must be present. No official launcher is started during direct play; if dependencies are missing, install that version once using an existing compatible launcher. No downloads or authentication are performed by this tool.

The installer patches a **local copy**, not your original JAR. It accepts only SHA-256 `fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234`, the inspected uploaded client. Unsupported clients fail closed. The resulting game uses no Java agent, ASM, launchwrapper or instrumentation runtime. Our small Java 8 hooks are included in the patched copy. Fog is patched at game call sites, not inside LWJGL. Subtle tunnel/canopy/sand/gravel changes remain optional by choosing this separate modded mode; saved terrain edits persist. No generated media, added sounds, chat events or jump scares.

The same install patches Alpha's resource thread to register existing local recordings **without waiting for the obsolete HTTP resource list**. It copies compatible unmodified legacy recordings from local resources or asset indexes. If none are found, setup offers a legacy resources-folder picker or an explicit soundless test; it does not silently claim audio is fixed. Some recordings may still be absent even if others exist. There is no substitute/generated audio and no sound-pack download.

Direct play is **local single-player**, with a clearly labelled local Player session, not Microsoft authentication. No account files/tokens are read and authenticated multiplayer is not supported. Installed game files are required; this tool does not distribute Minecraft. Data/logs live in `%APPDATA%\AlphaDesktop\patched-engine\a1.0.16`; Recovery, existing worlds and launcher profiles are untouched. **Recover startup → Save diagnostic report** also scans direct-launch logs.

Development inspection used the privately supplied original JAR. Neither that JAR, decompiled Minecraft source nor a prepatched Minecraft binary is committed or released. Installation applies our bytecode changes locally; no decompiler/compiler is needed on the user's PC.

1. Install the Windows EXE and choose **Recover startup**. The native window automatically reads bounded tails of known local launcher/game logs and displays possible failure categories plus redacted excerpts.
2. Close Minecraft Launcher, then choose **Create clean test installation**.
3. In your normal launcher, play the new **Alpha Found - Recovery** installation—not a previous Alpha Found/Compatibility entry.

The test installation runs **vanilla Alpha with no agent/horror effects**, uses a separate `recovery-engine/<version>` game folder, and selects a private Java 8 runtime on Windows only after checking `java -version`. Its JVM arguments are reset to a conservative `-Xmx1G`. Existing profiles and worlds stay untouched; launcher profile files are backed up. Existing recovery-world/options files are preserved too. This is a baseline to distinguish mod/startup settings from underlying original-client, native-library or driver trouble, not a replacement horror implementation.

If it still fails, reopen Recover startup → **Save diagnostic report**. The app saves a text file under its per-user `diagnostics` folder and opens it in the default editor when possible. Attach that report instead of manually finding logs. **Review before sharing; redaction is best-effort.** Nothing is uploaded automatically.

The collector never reads account files. It examines only known launcher log names and limited game/crash/preflight log locations, caps each tail at 256 KiB and exports at most five short failure excerpts. It omits sensitive/token/session/argument lines and masks common identifiers, emails and home paths. A source timestamp is included because a shared launcher log may describe an older/different launch. A recognized signature is a possible cause, not proof of this launch's root cause. A generic exit code stays UNKNOWN rather than inventing a repair.

`--recover` opens the same tool directly. `--check-recovery` is the packaging/configuration smoke test; it does not launch Minecraft. Windows CI checks the actual packaged Java 8 copy and creates a no-agent fixture profile, in addition to the existing instrumentation checks.

## Recommended: use the real Alpha engine

The custom software renderer cannot reproduce all of Minecraft Alpha's behavior just by reusing its textures. This release adds **Original Alpha mode**, which prepares an isolated installation in your **existing official Minecraft Launcher**. The launcher then runs your installed, unmodified Alpha client, including its real renderer, world generation, menus, lighting, audio and gameplay. No game code is decompiled, copied into this project or redistributed; an optional bundled runtime agent can apply in-memory hooks without rewriting the original JAR.

**This mode uses your existing launcher, not direct game execution from our EXE.** Choose vanilla setup or explicitly install experimental environmental changes through a Java runtime agent. Performance and appearance are those of your original Alpha client, subject to its compatibility with your hardware/launcher. There are no benchmarked speedup claims for the original game.

### Windows compatibility rebuild (0.6.0)

The reported exit-code-1 cause is **not known** without its exception. This rebuild addresses startup risks rather than claiming that exact crash has been reproduced.

1. Download **`AlphaDesktop-0.6.0.exe`** from [GitHub Releases](https://github.com/Aaron-1992/Minecraft-Alpha-found-version/releases), install and open AlphaDesktop.
2. **Close Minecraft Launcher**, choose **Original Alpha → Install compatibility build** and select your installed Alpha JAR if asked.
3. Setup copies the bundled Java 8 runtime into the isolated compatibility directory and tests the actual packaged agent in a separate JVM **before** writing its launcher profile. Wait for this check; on failure the profile is not enabled and local details remain in `mods/agent-preflight.log`.
4. Open your usual Minecraft Launcher → Java Edition → Installations → **Alpha Found - Compatibility** → Play. Use this **new** installation, not the previous `Alpha Found` entry. Create a new world.

The compatibility profile has a new key (`alpha-found-ambient-compat`) and game directory (`%APPDATA%\AlphaDesktop\compatibility-engine\<version>`). Your previous modded/vanilla profiles, runtimes and saves are left untouched. The Windows EXE/portable ZIP now includes **both Java 17 for our setup/custom sandbox and Java 8 for original Alpha**. Java 8 is copied to a versioned per-user directory so an application reinstall does not remove a profile's runtime.

Choose **Disable installed mod** to remove only this project's agent options from this project's known profile entries, with backups; other JVM options and worlds are preserved. This does not undo saved terrain. Vanilla setup remains separately available as **Alpha Found - Original Alpha**.

A standalone cross-platform JAR does **not** contain the Windows Java 8 runtime. Use the complete Windows release for this compatibility profile on Windows. On other platforms, the preflight uses the app's Java runtime and the official launcher retains its original-game runtime selection; this is not a bundled cross-platform Java 8 solution.

The official launcher still manages your normal account, game libraries and Play operation. No credentials, ownership bypass or Minecraft client/assets are bundled. The selected original Alpha version must already be installed with matching version metadata. For the reported startup crash, use **Recover startup** first; the experimental mod setup below is not the clean baseline.

**Unsigned preview:** Windows may show an unknown-publisher/SmartScreen warning. Verify the source/checksums before deciding whether to run it. Extract the **whole** portable ZIP if not using the installer.

### Experimental original-engine changes

The optional agent is written by this project, targets Java 8+ and contains **no Minecraft classes or media**. It is copied into the isolated modded game folder and referenced by a completely quoted `-javaagent:` option in only the new **Alpha Found - Compatibility** profile. ASM supplies bytecode instrumentation; the original JAR remains unchanged. Only a tiny helper JAR containing this project's hook classes is appended to bootstrap. ASM is relocated into a private namespace and stays out of bootstrap, avoiding interference with LaunchWrapper's own ASM version. This corrects a concrete isolation risk in 0.5.0, but does not prove it caused the reported crash.

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

- Adds/updates only this project's `alpha-found-ambient-compat` (compatibility) or `alpha-found-original-engine` (vanilla) installation entry in existing `launcher_profiles.json` and/or `launcher_profiles_microsoft_store.json` files. Other installations, selected profile, unknown properties and account-related metadata are preserved.
- Makes an exact backup next to each changed profile file, using `.alpha-backup-<unique-id>`. Backups remain local. Profile writes are atomic where supported, and a detected concurrent change aborts the write. Keep the launcher closed during setup.
- Uses `%APPDATA%\AlphaDesktop\compatibility-engine\<version>` for the new compatibility profile and `%APPDATA%\AlphaDesktop\original-engine\<version>` for vanilla for isolated original-game options/worlds on Windows, or `~/.alpha-desktop/original-engine/<version>` elsewhere. It never converts or overwrites your custom-sandbox worlds or normal Minecraft worlds.
- Creates conservative Alpha graphics options **only if the isolated options file does not already exist**. Existing options and user tuning in this tool's launcher entry are preserved.
- Copies only **existing original** legacy recordings from local resources/asset indexes into the isolated game's resource folder, without changing them. Hashed asset objects are checked against their recorded SHA-1. No missing recordings, textures or sounds are generated or substituted. The normal launcher/client still determines resource compatibility; this helper cannot create resources absent from your installation.

The original client handles fullscreen (normally F11), render distance and graphics settings. The optional mod changes fog values/chunk arrays in memory. The new Windows compatibility profile explicitly uses its bundled Java 8, but other installations' Java selection/options are not changed.

### Undo

With the game and launcher closed, remove the **Alpha Found - Compatibility**, previous **Alpha Found**, or **Alpha Found - Original Alpha** installation, or restore its corresponding `.alpha-backup-*` profile file. To disable only the mod, remove this project's `-javaagent:` argument under Edit → More Options → JVM arguments. **This does not undo terrain already saved: restore a pre-mod world backup for that.** The separate `original-engine/<version>` folder can be removed if you no longer want its worlds. **Do not delete it if you want to retain those saves.** This helper does not remove it automatically.

## Custom sandbox (fallback)

Choose **Custom sandbox** at startup, or run the standalone JAR with `--prototype`. This remains an independent, bounded Java/Java2D voxel prototype, not the original Minecraft engine or an exact Alpha port. No browser, WebView or HTTP server is involved.

Original Minecraft assets are required: a legacy JAR containing `terrain.png`, GUI/icons and `default.png` or `font/default.png`, plus original legacy `step/grass*.ogg`, `step/stone*.ogg`, `step/wood*.ogg` and `step/sand*.ogg` recordings. Native dialogs select and remember these paths. Missing assets are reported, never replaced by generated media.

Original title textures (`title/mclogo.png`, `gui/logo.png`, `logo.png`) and cloud masks (`environment/clouds.png`, `clouds.png`) are loaded when present. If absent, clouds are omitted and the title uses the original bitmap font. All block surfaces, font, hotbar, crosshair and recordings come from original assets supplied locally; no generated replacement textures, fonts, logos or synthesized effects are bundled.

### Fallback performance changes in 0.6.0

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
java -jar target/alpha-desktop-0.6.0.jar
```

`build.bat` / `build.sh` also build the shaded JAR with its Ogg decoder. Cross-platform JAR downloads require Java 17+; the Windows EXE bundles Java for this app. The official launcher separately manages the runtime for the original game.

```sh
# Prepare the real Alpha installation (native dialogs):
java -jar target/alpha-desktop-0.6.0.jar --original

# Custom sandbox, with native asset dialogs:
java -jar target/alpha-desktop-0.6.0.jar --prototype

# Custom sandbox with explicit local asset paths:
java -jar target/alpha-desktop-0.6.0.jar \
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

There are **45 main-app headless tests plus a direct-launch safety suite** and **17 patcher/agent unit tests** and fresh-JVM instrumentation/preflight tests on Java 8 and 17, including an isolated child classloader and a check that neither public nor private ASM is bootstrap-visible. Run `mvn -f agent/pom.xml clean package` to execute the agent tests. They check original-version metadata layout, safe launcher JSON round-trips, preservation of other profiles/settings, exact backups, immutable client files, rejection of malformed files, resource-path safety and precomputed-ray equivalence. Fixtures contain configuration/metadata only, not proprietary game code, fabricated texture pixels or recordings.

CI also runs **three real AWT fullscreen tests** under Xvfb, builds the executable, verifies Ogg SPI and embedded Java 8-agent packaging and creates the Windows EXE/installer. It smoke-tests the generated Windows launcher, then runs the main distribution's extracted agent with the actual bundled Java 8 runtime in a path containing spaces before publishing. The packaging verifier checks the bootstrap helper contains only hook classes and no unrelocated ASM remains.

**Not verified here:** launching a real original Alpha client through a user's official launcher, original-asset visual parity, audible playback, full gameplay, installation UI and physical Windows multi-monitor behavior. Those need a real installed client, launcher, original resources, audio output and desktop. A metadata-only inspection is not proof that a particular original client will run successfully.

## Rights / scope

Unofficial fan project; not affiliated with Mojang or Microsoft. Minecraft code, assets and marks belong to their respective owners. No Minecraft JAR, source code, textures or recordings are distributed with the release. Using a local installation does not grant redistribution rights. The real-engine mode runs the user's unmodified client through their launcher; it does not transplant copyrighted classes into the custom engine.

The custom sandbox retains restrained fog variation and solitude without jumpscares, entities or chat events. **Vanilla original-engine setup is unmodified; the separate opt-in Alpha Found profile enables experimental runtime changes.** VorbisSPI and its dependencies have their own upstream licenses; their original distributions and available sources accompany the packaged app.

### Runtime licensing

The Windows release contains OpenJDK/Temurin 17 and 8 runtime distributions, separate from proprietary Minecraft files. Runtime license/notice files and Java 8 build metadata accompany them. Upstream source and build information are available from [Adoptium jdk8u](https://github.com/adoptium/jdk8u) and [Temurin 8 binaries](https://github.com/adoptium/temurin8-binaries); inspect the bundled `release` file for its build version. No Java runtime is fabricated or replaced with generated media.
