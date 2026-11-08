import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;

/** Config/log-text fixtures only: no Minecraft code, textures, sound or account data. */
public final class RecoveryTests {
    private interface Task{void run()throws Exception;}
    private static int passed;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void test(String name,Task task)throws Exception{task.run();passed++;System.out.println("PASS "+name);}
    public static void main(String[] args)throws Exception{
        test("exit code 1 alone never invents a cause",()->check(StartupDiagnostics.analyze("Game exited. Exit code: 1").category().equals("UNKNOWN"),"generic code treated as diagnosis"));
        test("Java runtime mismatch is recognized",()->check(StartupDiagnostics.analyze("Exception in thread main java.lang.UnsupportedClassVersionError: class file version 61.0").category().equals("JAVA_VERSION"),"runtime mismatch missed"));
        test("native-library and graphics failures stay distinct",()->{
            check(StartupDiagnostics.analyze("java.lang.UnsatisfiedLinkError: no lwjgl in java.library.path").category().equals("NATIVE_LIBRARY"),"native error missed");
            check(StartupDiagnostics.analyze("org.lwjgl.LWJGLException: Pixel format not accelerated").category().equals("GRAPHICS"),"graphics error missed");
        });
        test("agent startup is separate from missing game libraries",()->{
            check(StartupDiagnostics.analyze("Error opening zip file or JAR manifest missing : mod.jar").category().equals("AGENT_STARTUP"),"agent startup missed");
            check(StartupDiagnostics.analyze("Exception in thread main java.lang.NoClassDefFoundError: net/minecraft/launchwrapper/Launch").category().equals("MISSING_CLASS"),"missing class missed");
        });
        test("latest exception block wins over an earlier error",()->{
            String log="java.lang.UnsupportedClassVersionError: old launch\n"+"ordinary line\n".repeat(90)+"Exception in thread main org.lwjgl.LWJGLException: Pixel format not accelerated\n at display.create(Display.java:1)\nExit code 1";
            StartupDiagnostics.Finding finding=StartupDiagnostics.analyze(log);
            check(finding.category().equals("GRAPHICS"),"older launch classified instead");check(finding.evidence().stream().noneMatch(s->s.contains("old launch")),"old exception exported as current block");
        });
        test("launcher credentials and full argument lines are omitted",()->{
            for(String line:List.of("--accessToken FAKE_SECRET_ONE", "access_token: FAKE_SECRET_TWO", "Authorization: Bearer FAKE_SECRET_THREE", "--session FAKE_SECRET_FOUR", "--password FAKE_SECRET_FIVE", "Setting user: FAKE_USER", "JVM arguments: -Dexample=PRIVATE", "java_command: PRIVATE POSITIONAL_ARGUMENT", "jvm_args: -Dprivate=PRIVATE")){
                String redacted=StartupDiagnostics.redact(line);check(!redacted.contains("FAKE_")&&!redacted.contains("PRIVATE"),"sensitive text survived");
            }
        });
        test("home paths, email and long identifiers are masked",()->{
            String input=System.getProperty("user.home")+"/test user@example.invalid 12345678-1234-1234-1234-123456789abc aabbccddeeff00112233445566778899";
            String redacted=StartupDiagnostics.redact(input);
            check(!redacted.contains(System.getProperty("user.home"))&&!redacted.contains("user@example")&&!redacted.contains("12345678-1234")&&!redacted.contains("aabbccdd"),"identifier survived redaction");
        });
        test("log reading is bounded and includes the recent tail",()->{
            Path file=Files.createTempFile("alpha-log-tail-", ".txt");try{
                Files.writeString(file,"OLD_PREFIX_NOT_FOR_EXPORT\n"+"ordinary line\n".repeat(40000)+"java.lang.VerifyError: recent failure\n");
                String tail=StartupDiagnostics.readTail(file);check(tail.length()<=StartupDiagnostics.MAX_TAIL&&!tail.contains("OLD_PREFIX_NOT_FOR_EXPORT")&&tail.contains("recent failure"),"unbounded/stale log content");
            }finally{Files.delete(file);}
        });
        test("UTF16 launcher log text is supported",()->{
            Path file=Files.createTempFile("alpha-log-utf16-", ".txt");try{Files.write(file,("\ufeffjava.lang.VerifyError: fixture\n").getBytes(StandardCharsets.UTF_16LE));check(StartupDiagnostics.readTail(file).contains("VerifyError"),"UTF16 not decoded");}finally{Files.delete(file);}
        });
        test("collector does not read account files and redacts saved excerpts",()->{
            Path root=Files.createTempDirectory("alpha-diagnostic-");try{
                Path mc=root.resolve("minecraft"),data=root.resolve("app");Files.createDirectories(mc);Files.createDirectories(data);
                Files.writeString(mc.resolve("accounts.json"),"PRIVATE_ACCOUNT_SENTINEL");
                Files.writeString(mc.resolve("launcher_log.txt"),"Exception in thread main java.lang.VerifyError: fixture\n--accessToken FAKE_NEVER_EXPORT\n at code.run(Code.java:1)\n");
                StartupDiagnostics.Report report=StartupDiagnostics.collect(mc,data);Path exported=StartupDiagnostics.save(report,data.resolve("diagnostics"));String text=Files.readString(exported);
                check(report.finding().category().equals("CLASS_COMPATIBILITY"),"failure not collected");
                check(!text.contains("FAKE_NEVER_EXPORT")&&!text.contains("PRIVATE_ACCOUNT_SENTINEL")&&!text.contains("accounts.json"),"private content exported");
                check(text.contains("No account files")&&text.contains("Nothing is uploaded"),"report provenance missing");
            }finally{delete(root);}
        });
        test("collector picks newest concrete error source and timestamps it",()->{
            Path root=Files.createTempDirectory("alpha-log-recency-");try{
                Path mc=root.resolve("minecraft");Files.createDirectories(mc);Path old=mc.resolve("launcher_log.txt"),recent=mc.resolve("launcher_log_microsoft_store.txt");
                Files.writeString(old,"java.lang.VerifyError: old\n");Files.writeString(recent,"java.lang.UnsatisfiedLinkError: newest\n");
                Files.setLastModifiedTime(old,FileTime.fromMillis(1000));Files.setLastModifiedTime(recent,FileTime.fromMillis(2000));
                StartupDiagnostics.Report report=StartupDiagnostics.collect(mc,root.resolve("app"));check(report.finding().category().equals("NATIVE_LIBRARY")&&report.text().contains("Modified:"),"source recency missing");
            }finally{delete(root);}
        });
        test("recovery profile resets only its own agents and preserves existing worlds/options",()->{
            Path root=Files.createTempDirectory("alpha-clean-profile-");try{
                Path mc=root.resolve("minecraft");Files.createDirectories(mc);Path profiles=mc.resolve("launcher_profiles.json");
                String original="{\"profiles\":{\"other\":{\"javaArgs\":\"-javaagent:keep.jar\",\"gameDir\":\"unchanged\"},\"alpha-found-recovery\":{\"javaArgs\":\"-javaagent:bad.jar\",\"javaDir\":\"broken\"}},\"selectedProfile\":\"other\"}";Files.writeString(profiles,original);
                Path game=root.resolve("recovery");Files.createDirectories(game.resolve("saves"));Files.writeString(game.resolve("saves/keep.txt"),"SAVE_UNCHANGED");Files.writeString(game.resolve("options.txt"),"USER_OPTIONS_UNCHANGED");
                OriginalAlphaInstallation.Client client=new OriginalAlphaInstallation.Client(mc,mc.resolve("unused.jar"),"a1.0.16");
                OriginalAlphaInstallation.Prepared prepared=OriginalAlphaInstallation.prepareRecovery(client,game,p->null);
                Map<String,Object> output=LauncherJson.object(Files.readString(profiles));Map<?,?> all=(Map<?,?>)output.get("profiles"),own=(Map<?,?>)all.get("alpha-found-recovery");
                check(own.get("javaArgs").equals("-Xmx1G")&&!own.containsKey("javaDir"),"recovery retained injection/broken runtime");
                check(((Map<?,?>)all.get("other")).get("javaArgs").equals("-javaagent:keep.jar")&&output.get("selectedProfile").equals("other"),"other installation changed");
                check(Files.readString(prepared.backups().get(0)).equals(original),"backup not exact");
                check(Files.readString(game.resolve("saves/keep.txt")).equals("SAVE_UNCHANGED")&&Files.readString(game.resolve("options.txt")).equals("USER_OPTIONS_UNCHANGED"),"world/options changed");
                check(!prepared.ambient(),"vanilla baseline mislabeled");
            }finally{delete(root);}
        });
        test("invalid profile refuses recovery before touching runtime or profile",()->{
            Path root=Files.createTempDirectory("alpha-recovery-invalid-");try{
                Path profile=root.resolve("launcher_profiles.json");Files.writeString(profile,"invalid JSON");int[] calls={0};
                try{OriginalAlphaInstallation.prepareRecovery(new OriginalAlphaInstallation.Client(root,root.resolve("unused.jar"),"a1.0.16"),root.resolve("game"),p->{calls[0]++;return null;});throw new AssertionError("invalid metadata accepted");}catch(IOException expected){}
                check(calls[0]==0&&Files.readString(profile).equals("invalid JSON"),"recovery changed files before validation");
            }finally{delete(root);}
        });
        test("custom Minecraft directory is inferred without requiring sound assets",()->{
            Path root=Files.createTempDirectory("alpha-root-detection-");try{
                Path jar=root.resolve("versions/a1.0.16/a1.0.16.jar");check(StartupRecovery.inferMinecraft(jar).equals(root.toAbsolutePath()),"custom launcher directory missed");
            }finally{delete(root);}
        });
        System.out.println(passed+" recovery/diagnostics tests passed.");
    }
    private static void delete(Path root)throws IOException{try(var paths=Files.walk(root)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
}
