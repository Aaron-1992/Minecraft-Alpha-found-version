## 0.5.2 — startup recovery and automatic local diagnosis

**The repeated exit-code-1 cause is still unknown. This is a recovery/diagnostic build, not another claim that the original-client crash is fixed.**

### What to do

1. Install **`AlphaDesktop-0.5.2.exe`** and open it.
2. Choose **Recover startup**. It automatically checks known local launcher/game logs and displays the available error explanation/excerpt. You do not need to locate log files manually.
3. Close Minecraft Launcher and choose **Create clean test installation**.
4. Open your normal launcher → Java Edition → Installations → **Alpha Found - Recovery** → Play.

**This clean test is vanilla Alpha with NO agent or horror effects.** It pins a verified private Java 8 runtime on Windows, resets only its own test profile's JVM arguments and uses a separate game folder. The previous Alpha Found/Compatibility profiles, original client and worlds remain untouched. It is meant to distinguish an injection/settings problem from a client/native-library/driver failure, not pretend the horror mod is working.

If it still exits, open **Recover startup** again and click **Save diagnostic report**. A redacted text report is saved locally and opened in your default text editor. You can attach that one file instead of digging through logs or sending another generic exit-code popup. Review the report before sharing: redaction is best-effort. Nothing is uploaded automatically.

### What changed

- Read-only scan of known launcher log names, isolated game logs, crash reports and agent preflight logs. Reads are bounded; logs are not copied wholesale.
- Recognizes Java-version, JVM-option, agent-startup, native-library, graphics, missing-class and class-linkage failure signatures. An unknown/generic exit code stays UNKNOWN; a matching signature is a possible cause, not a confirmed current diagnosis.
- Reports source timestamps and warns that a shared launcher log can describe an older/different launch.
- Omits sensitive/account/argument lines, masks common tokens/identifiers/home paths and exports only redacted error excerpts. It does not read account files or send network reports.
- Separate **Alpha Found - Recovery** profile with zero agent options, a conservative 1 GB heap, verified Java 8 on Windows, conservative initial graphics options and profile backups. Existing worlds/options are not overwritten.
- Windows packaging now tests creation of the clean profile and verifies that its private runtime actually reports Java 8, in addition to the previous agent/runtime checks.

Original Minecraft files are still required and are not redistributed. The actual user-owned client launch, game libraries/natives, graphics driver and gameplay remain unverified in CI. The clean recovery baseline does not fix missing libraries, ownership/login issues or graphics drivers automatically.

Downloads include the unsigned Windows installer, whole-folder portable ZIP, cross-platform JAR distribution and checksums. The full Windows release includes private Java runtimes; the standalone JAR does not contain Windows Java 8. The previous optional mod remains available under **Original Alpha setup**, but recovery is the recommended next step for the reported crash.
