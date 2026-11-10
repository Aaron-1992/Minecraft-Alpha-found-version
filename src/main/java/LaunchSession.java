import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** One direct-game process, one log directory. Never scans legacy installations. */
final class LaunchSession {
    static final String VERSION="0.11.0";
    final Path directory;
    private final Properties status=new Properties();
    private LaunchSession(Path directory){this.directory=directory;}
    static LaunchSession begin(Path data)throws IOException{
        Path sessions=data.resolve("sessions");Files.createDirectories(sessions);
        LaunchSession session=new LaunchSession(Files.createTempDirectory(sessions,"launch-"));
        session.status.setProperty("features","sounds=on;fog=on;smoke=on;watcher=on;eventPalette=23;directorModes=9;gazeAndOcclusion=on;groundCheckedMovement=on;abort=F7;returnScenes=on;echoes=on;hudDrift=on;leafStripping=off;fpsCap=60;preview=F6");session.status.setProperty("build",VERSION);session.status.setProperty("started",Instant.now().toString());session.status.setProperty("state","STARTING");session.store();
        Properties latest=new Properties();latest.setProperty("session",session.directory.getFileName().toString());
        writeProperties(data.resolve("latest-session.properties"),latest);
        return session;
    }
    void finished(int exit)throws IOException{status.setProperty("state","EXITED");status.setProperty("exitCode",Integer.toString(exit));status.setProperty("finished",Instant.now().toString());store();}
    void failed(Exception e)throws IOException{if(!status.containsKey("exitCode"))status.setProperty("state","LAUNCH_FAILED");status.setProperty("finished",Instant.now().toString());Files.writeString(directory.resolve("launch-error.txt"),StartupDiagnostics.redact(e.toString()));store();}
    private void store()throws IOException{writeProperties(directory.resolve("session.properties"),status);}
    private static void writeProperties(Path file,Properties values)throws IOException{
        Path temp=Files.createTempFile(file.getParent(),".session-",".tmp");
        try{try(OutputStream out=Files.newOutputStream(temp)){values.store(out,"Local launch status; no account data");}Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
    }
    static Path latest(Path data)throws IOException{
        Path pointer=data.resolve("latest-session.properties");if(!Files.isRegularFile(pointer))throw new IOException("No session from the new launcher yet. Old installation logs are deliberately excluded.");
        Properties p=new Properties();try(InputStream in=Files.newInputStream(pointer)){p.load(in);}
        String id=p.getProperty("session","");if(!id.matches("launch-[A-Za-z0-9_-]+"))throw new IOException("Invalid latest-session marker");
        Path root=data.resolve("sessions").toRealPath(),session=root.resolve(id);
        if(Files.isSymbolicLink(session)||!Files.isDirectory(session)||!session.toRealPath().startsWith(root))throw new IOException("Latest session folder is missing or unsafe");return session;
    }
    static String report(Path data)throws IOException{
        Path session=latest(data);Properties p=new Properties();try(InputStream in=Files.newInputStream(session.resolve("session.properties"))){p.load(in);}
        StringBuilder report=new StringBuilder("Alpha Desktop "+VERSION+" - latest direct-play session ONLY\n");
        for(String key:List.of("build","features","started","state","exitCode","finished"))report.append(key).append(": ").append(p.getProperty(key,"not recorded")).append('\n');
        report.append("No legacy launcher, compatibility or recovery logs are included.\nAn unfinished session can indicate a still-running game or an interrupted launcher.\nA driver reset may leave no Java error; UNKNOWN is not a clean bill of health.\nNothing is uploaded. Redaction is best-effort; review before sharing.\n\n");
        try(var files=Files.list(session)){
            for(Path file:files.sorted().toList()){
                String name=file.getFileName().toString();
                if(!name.equals("game.log")&&!name.equals("launch-error.txt")&&!name.matches("hs_err_pid[0-9]+\\.log"))continue;
                if(Files.isSymbolicLink(file)||!Files.isRegularFile(file))continue;
                String tail=StartupDiagnostics.readTail(file);StartupDiagnostics.Finding finding=StartupDiagnostics.analyze(tail);
                report.append("Source: ").append(name).append("\nModified: ").append(Files.getLastModifiedTime(file)).append("\nPossible category: ").append(finding.category()).append('\n');
                // Include bounded recent output even when no Java exception was printed.
                String[] lines=tail.split("\\R");for(int i=Math.max(0,lines.length-120);i<lines.length;i++)report.append(StartupDiagnostics.redact(lines[i])).append('\n');report.append('\n');
            }
        }return report.toString();
    }
}
