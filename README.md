# Minecraft Alpha Launcher

An unofficial native Windows launcher for **Minecraft Alpha a1.0.16**.

Launch Alpha directly with a bundled Java 8 runtime, original local resources and separate saves. No browser interface, official-launcher sign-in, account-token handling or automatic game downloads.

## Download and launch

Get the current **Windows EXE** from [Releases](https://github.com/Aaron-1992/Minecraft-Alpha-found-version/releases), install it, and choose **Play Alpha**. The Windows portable ZIP is also available; extract the complete folder and keep its runtime files beside the executable.

You need a locally installed `versions/a1.0.16/a1.0.16.jar`, matching version JSON, libraries and native dependencies. This launcher supports the inspected client with SHA-256:

`fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234`

Your original client is never overwritten. A separate local copy receives this build's compatibility and gameplay adjustments. This is a community build, not an official Mojang launcher or an unchanged vanilla client. Local single-player only.

Original Alpha textures and locally available recordings are used. Missing original resources are not generated or downloaded. Java 8 is bundled in the complete Windows distributions; the standalone JAR does not include that runtime.

## Saves and backups

Launcher data lives under `%APPDATA%\AlphaDesktop`. Game files and saves are kept in `patched-engine\a1.0.16`, separate from other installations.

**Gameplay adjustments can permanently change saved worlds and inventory.** Use a copy of an important world. Before this version first launches existing saves, a one-time local ZIP is created at:

`patched-engine\a1.0.16\world-backups\before-0.15.0.zip`

That backup is never overwritten. New worlds created afterward need their own later backups. Close the game before restoring saves; preserve the current folder elsewhere before extracting a backup's `saves/` directory. A backup is not a live undo feature.

## Session reports

Choose **Save session report** to export the latest launch's status, process output and any available Java crash report. Nothing is uploaded automatically. Review the report before sharing; redaction is best-effort.

The 60 FPS limit remains enabled. Packaging tests do not establish real-game or GPU stability. If a graphics-driver reset occurs, stop rather than repeatedly retrying, and retain the latest session report.

## Build

The launcher requires JDK 17 and Maven. Its bundled runtime helpers target Java 8.

```sh
./test.sh
mvn -f agent/pom.xml clean package
mvn package
```

Windows users can run `test.bat`. GitHub Actions tests the desktop/runtime components and packages the Windows EXE and portable distribution with their Java runtimes. Only the current downloadable release is retained.

No Minecraft client, source, textures or recordings are distributed by this repository. Dependency notices and available source archives accompany the package. Bundled Temurin Java 8 sources/notices: [OpenJDK 8](https://github.com/adoptium/jdk8u) and [Temurin distributions](https://github.com/adoptium/temurin8-binaries).
