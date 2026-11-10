import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class DirectLaunchTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("direct-alpha-tests-");
        try{
            check(DirectAlphaLaunch.allowed(null,"windows"));
            check(!DirectAlphaLaunch.allowed(List.of(Map.of("action","allow","os",Map.of("name","linux"))),"windows"));
            check(DirectAlphaLaunch.allowed(List.of(Map.of("action","allow"),Map.of("action","disallow","os",Map.of("name","osx"))),"windows"));
            Path libraries=root.resolve("libraries");Files.createDirectories(libraries);Files.writeString(libraries.resolve("test.jar"),"fixture");
            check(DirectAlphaLaunch.localArtifact(libraries,Map.of("path","test.jar")).startsWith(libraries));
            try{DirectAlphaLaunch.localArtifact(libraries,Map.of("path","../outside.jar"));throw new AssertionError();}catch(java.io.IOException expected){}
            try{DirectAlphaLaunch.localArtifact(libraries,Map.of("path","test.jar","sha1","bad"));throw new AssertionError();}catch(java.io.IOException expected){}
            Path jar=root.resolve("natives.jar");
            try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(jar))){out.putNextEntry(new ZipEntry("../escape.dll"));out.write(1);out.closeEntry();}
            Path natives=root.resolve("natives");Files.createDirectories(natives);
            try{DirectAlphaLaunch.extractNatives(jar,natives);throw new AssertionError();}catch(java.io.IOException expected){}
            try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(jar))){out.putNextEntry(new ZipEntry("lwjgl.dll"));out.write(new byte[]{1,2});out.closeEntry();out.putNextEntry(new ZipEntry("META-INF/readme.txt"));out.write(1);out.closeEntry();}
            DirectAlphaLaunch.extractNatives(jar,natives);check(Files.size(natives.resolve("lwjgl.dll"))==2);check(!Files.exists(natives.resolve("META-INF/readme.txt")));
            List<String> command=DirectAlphaLaunch.command(root.resolve("java with spaces.exe"),root.resolve("patched client.jar"),List.of(libraries.resolve("test.jar")),root.resolve("game with spaces"));
            check(command.contains("-Djava.util.Arrays.useLegacyMergeSort=true"));
            check(command.stream().noneMatch(s->s.contains("javaagent")||s.contains("launchwrapper")||s.contains("accessToken")));check(command.get(command.size()-2).equals("Player"));
            Path resources=root.resolve("legacy");Files.createDirectories(resources.resolve("newsound/step"));Files.write(resources.resolve("newsound/step/stone1.ogg"),new byte[]{1});
            check(OriginalResourceCopy.copyLegacyFolder(resources,root.resolve("resources"))==1);check(Files.exists(root.resolve("resources/newsound/step/stone1.ogg")));
            Path data=root.resolve("data");Files.createDirectories(data.resolve("recovery-engine/a1.0.16"));Files.writeString(data.resolve("recovery-engine/a1.0.16/direct-launch.log"),"OLD 0.5.2 java.lang.RuntimeException");
            try{LaunchSession.report(data);throw new AssertionError();}catch(java.io.IOException expected){}
            LaunchSession first=LaunchSession.begin(data);Files.writeString(first.directory.resolve("game.log"),"FIRST SESSION");first.finished(0);
            LaunchSession latest=LaunchSession.begin(data);Files.writeString(latest.directory.resolve("game.log"),"LATEST OUTPUT even without exception");latest.finished(-805306369);
            String report=LaunchSession.report(data);check(report.contains("LATEST OUTPUT"));check(report.contains("-805306369"));check(report.contains("state: EXITED"));check(!report.contains("FIRST SESSION")&&!report.contains("OLD 0.5.2"));
            check(command.contains("-Dalpha.scheduledSounds=false"));
            System.out.println("PASS direct launch rules, local artifacts/checksums, native extraction, no-agent command and legacy sound import");
        }finally{try(var files=Files.walk(root)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
