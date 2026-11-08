## Windows EXE preview — 0.2.2

### Fixed in 0.2.2

- Fixed startup rejecting early Alpha clients because the font is at **`default.png`**, not `font/default.png`. Both original layouts are now supported; do not rename or modify your Minecraft JAR.
- Accepted original root-level legacy GUI/icon paths as well as their `gui/` paths.
- Recognized original `newsound/step/...` recordings in legacy resource layouts.
- Added five asset-path regression tests; all **14 tests** must pass before publishing.

### Windows: no Java installation required

**Download `AlphaDesktop-0.2.2.exe` and double-click it to install.** Launch **AlphaDesktop** from the Start menu or desktop shortcut. Java is bundled privately with the application.

Alternatively, download `AlphaDesktop-0.2.2-windows-x64-portable.zip`, extract the **whole ZIP**, then double-click `AlphaDesktop/AlphaDesktop.exe`. Keep the `app` and `runtime` folders next to the EXE; it is not a single-file portable executable.

On first launch, native file-selection dialogs ask for your **original legacy Alpha client JAR** and, if not found automatically, the **original sound folder** containing `step/grass1.ogg`, etc. Your selections are remembered. No command-line arguments are needed.

The app stores configuration and saves in `%APPDATA%\AlphaDesktop`, not the installation directory. It does not require administrator access to save games.

**This preview is unsigned.** Windows may show an unknown-publisher or SmartScreen warning. Verify the publisher/source and the provided SHA-256 checksum before deciding whether to run it.

### Other downloads

- `alpha-desktop-0.2.2.jar`: cross-platform executable; requires an installed Java 17+ runtime.
- `alpha-desktop-0.2.2.zip`: cross-platform JAR, scripts, documentation and decoder dependency distributions.
- `SHA256SUMS.txt` and `SHA256SUMS-windows.txt`: download integrity checksums.

### Included

Native desktop voxel sandbox, seeded terrain, walking/jumping/swimming, block breaking and placing, inventory, options and local world saves. Textures are read from original Minecraft files and original Ogg recordings are played through the bundled decoder. No generated replacement media, synthesized sound effects, jumpscares or chat messages.

### Important limitations

- **Original Minecraft assets are still required and are NOT bundled.** The EXE bundles Java and the decoder, not Minecraft. Missing assets are not substituted.
- Windows packages target **64-bit Windows 10/11**.
- This is an early independent prototype, not an exact copy of Minecraft Alpha. No survival, infinite terrain, mobs, crafting or multiplayer.
- CI runs 14 engine tests, verifies decoder packaging, creates the Windows app image, executes the actual generated `AlphaDesktop.exe --check-runtime`, and verifies the installer is a Windows PE executable. Interactive rendering, file selection, installation and audible playback still require desktop testing; the EXE smoke test does not cover those.

See README for controls, original-asset locations and build instructions. Original audio-decoder dependency distributions and available source archives accompany the app for their upstream licensing information.
