## 0.5.1 — compatibility rebuild

**Download `AlphaDesktop-0.5.1.exe`, install it, then run setup again.** This is a compatibility rebuild, not a claim that the reported exit-code-1 cause has been confirmed without a crash log.

### What to do

1. Close Minecraft Launcher.
2. Open AlphaDesktop → **Original Alpha** → **Install compatibility build**.
3. Select the installed Alpha JAR if asked. Wait for the packaged agent's startup preflight to complete.
4. Open Minecraft Launcher and select the **NEW `Alpha Found - Compatibility`** installation, not the old `Alpha Found` entry.
5. Create a new world in this separate installation.

There is also a **Disable installed mod** setup option. It removes only this project's agent arguments from this project's profiles, keeps other options/worlds and backs up profile files. No manual JVM-argument editing is required.

### Reworked startup

- **ASM is private/relocated.** It is no longer exported into the bootstrap classloader where it could collide with LaunchWrapper's own instrumentation libraries. Only a tiny helper JAR containing this project's hooks is bootstrap-visible.
- **The Windows distribution now carries Java 8 for original Alpha**, separately from Java 17 used by our setup app. Setup copies it to the isolated compatibility folder and pins only the new compatibility profile to it. Existing installations and their Java settings are untouched.
- The entire `-javaagent:...` option is quoted as one JVM argument. Paths containing spaces have regression coverage.
- Before writing/enabling the compatibility profile, setup starts the packaged agent in a separate JVM using that runtime. The probe tests actual transformation through an isolated classloader and rejects bootstrap ASM leakage.
- Failed preflight leaves launcher profiles unmodified and retains local details at `mods/agent-preflight.log`. It does **not** silently report success or ask you to manually collect logs first.
- Startup/transformer initialization errors now disable optional instrumentation for nonfatal failures rather than deliberately aborting startup.

The old installation, original client JAR and worlds are left untouched. The new compatibility profile uses `%APPDATA%\AlphaDesktop\compatibility-engine\<version>`. Disabling the agent still cannot undo terrain already saved in any modded world.

### Verification / limits

CI tests app/profile repair, isolated helper packaging, private ASM relocation, actual Java-agent startup on Java 8 and 17, and the **packaged Windows distribution's Java 8 + extracted-agent preflight**, as well as fullscreen and the native EXE entry point.

**This preflight exercises our own fixture, not the original Alpha game.** Your actual client, graphics drivers, native game libraries, original launcher execution and complete gameplay remain unverified. Another exit-code-1 cause is still possible. The original mod's sparse environmental behavior is otherwise unchanged; no jumpscares, chat events, generated media or new sound effects were added.

The EXE includes OpenJDK runtimes and their accompanying license/notice files, not Minecraft assets or client code. Runtime build metadata is retained in `release`; upstream source is available from Adoptium/OpenJDK (https://github.com/adoptium/jdk8u and https://github.com/adoptium/temurin8-binaries). Cross-platform JAR downloads do not carry the Windows Java 8 runtime; use the complete Windows installer/portable ZIP for the compatibility profile on Windows.
