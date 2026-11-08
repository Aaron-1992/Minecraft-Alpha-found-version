## 0.4.0 — real Alpha engine option + fallback renderer cleanup

**Download `AlphaDesktop-0.4.0.exe` for Windows x64.** Java for this application is bundled. The installer is unsigned; check the release source/checksums before deciding whether to run it.

### Use the actual Alpha game

At startup, choose **Original Alpha (recommended)**. Close Minecraft Launcher when prompted. Setup uses your installed, unmodified `old_alpha` client to add **Alpha Found - Original Alpha** to your existing launcher installations.

Then **open your usual Minecraft Launcher → Java Edition → Installations → Alpha Found - Original Alpha → Play**. The game uses its actual renderer, terrain, lighting, menus, graphics settings and gameplay instead of the custom raycaster.

- Your official launcher handles its normal account, libraries and original-game runtime.
- Client code is not extracted, decompiled, patched or redistributed.
- Other launcher installations and account-related settings are preserved; changed profile files receive exact local backups.
- Original-game worlds/options use a separate per-version directory. Existing Minecraft/custom-sandbox worlds are not converted or overwritten.
- Existing original legacy recordings can be copied unmodified from local resources/indexed objects; missing media is not fabricated.

**This is a launcher installation helper, not a direct original-game executable or new horror mod.** You still click Play in your existing launcher. Original-engine mode is vanilla Alpha: the custom sandbox's atmosphere effects are not injected into it. Compatibility depends on your installed Alpha client and launcher; a real original-client launch could not be tested in CI. This mode does not claim a performance improvement over vanilla Alpha—it uses vanilla Alpha.

### Custom sandbox remains available

Choose **Custom sandbox** to use the previous independent prototype, including F11 fullscreen and original asset loading. Identified overhead was removed:

- Camera rays are normalized once, outside the frame loop.
- Cloud pixels are cached.
- Transparent texture holes continue the existing DDA traversal instead of allocating more hit objects and tiny-step marching/skipping foliage.
- Paused menus/inventory reuse the last world frame instead of rerendering the world.

No end-to-end FPS percentage is claimed without testing original assets on real hardware. No generated replacement media or synthesized sound effects are included.

### Downloads / verification

- `AlphaDesktop-0.4.0.exe`: Windows installer with private Java runtime for the setup/custom app.
- `AlphaDesktop-0.4.0-windows-x64-portable.zip`: extract the whole ZIP and run `AlphaDesktop/AlphaDesktop.exe`; keep `app` and `runtime` alongside it.
- `alpha-desktop-0.4.0.jar` / `.zip`: cross-platform app; requires Java 17+.
- SHA-256 checksum files accompany the downloads.

CI runs **25 headless tests + 3 AWT fullscreen tests**, verifies the executable/decoder packaging and smoke-tests the generated Windows launcher and bundled runtime. Those tests do not verify the user's original client launch, visual parity, audible playback or complete gameplay.

Original Minecraft assets and client code are **not bundled**. See README for setup details, profile backups/undo, separate save locations, controls and known limits.
