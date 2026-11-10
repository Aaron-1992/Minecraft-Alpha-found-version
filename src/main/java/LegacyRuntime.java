import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Windows release carries a private Java 8 runtime for the isolated Alpha profile. */
final class LegacyRuntime {
    static Path installIfAvailable(Path gameDirectory)throws IOException{
        if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"))return null;
        Path app;
        try{app=Path.of(LegacyRuntime.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();}
        catch(Exception e){throw new IOException("Cannot locate packaged compatibility runtime",e);}
        Path source=app.resolve("legacy-runtime");
        if(!Files.isRegularFile(source.resolve("bin/java.exe")))throw new IOException("Bundled Java 8 runtime missing. Install the complete Windows 0.10.0 release, not its standalone JAR.");
        Path parent=gameDirectory.resolve("runtime");Files.createDirectories(parent);
        Path target=parent.resolve("java8-0.10.0"),marker=target.resolve(".alpha-runtime-complete");
        if(Files.exists(target)){
            if(!Files.isRegularFile(marker)||!Files.isRegularFile(target.resolve("bin/java.exe")))throw new IOException("Incomplete compatibility runtime; left untouched: "+target);
            return target.resolve("bin/java.exe").toAbsolutePath();
        }
        Path temp=Files.createTempDirectory(parent,".java8-install-");
        try{
            try(var paths=Files.walk(source)){
                for(Path file:paths.toList()){
                    Path output=temp.resolve(source.relativize(file));
                    if(Files.isSymbolicLink(file))throw new IOException("Unexpected symbolic link in packaged Windows runtime");
                    if(Files.isDirectory(file))Files.createDirectories(output);
                    else if(Files.isRegularFile(file))Files.copy(file,output,StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
            Files.writeString(temp.resolve(".alpha-runtime-complete"),"AlphaDesktop 0.10.0 Java 8 compatibility runtime\n",StandardOpenOption.CREATE_NEW);
            try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,target);}
        }finally{
            if(Files.exists(temp))try(var paths=Files.walk(temp)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
        }
        return target.resolve("bin/java.exe").toAbsolutePath();
    }
    static String verifyJava8(Path executable)throws IOException{
        Path log=executable.getParent().getParent().resolve("runtime-version-check.log");
        Process process=new ProcessBuilder(executable.toString(),"-version").redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try{
            if(!process.waitFor(15,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();throw new IOException("Java 8 runtime check timed out; no recovery profile was written. Details: "+log);}
            String version=Files.readString(log);
            if(process.exitValue()!=0||!version.contains("version \"1.8."))throw new IOException("The private runtime did not report Java 8; no recovery profile was written. Details: "+log);
            return version;
        }catch(InterruptedException e){process.destroyForcibly();Thread.currentThread().interrupt();throw new IOException("Runtime check interrupted",e);}
    }
    static Path currentJava(){return Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")?"java.exe":"java");}
}
