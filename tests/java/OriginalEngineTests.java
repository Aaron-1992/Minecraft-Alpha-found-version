import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Tests local launcher metadata and math. No original game code/media fixtures. */
public final class OriginalEngineTests {
    private interface Task{void run()throws Exception;}
    private static int passed;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void test(String name,Task task)throws Exception{task.run();passed++;System.out.println("PASS "+name);}
    public static void main(String[] args)throws Exception{
        test("launcher JSON preserves unknown nested properties",()->{
            String source="{\"profiles\":{},\"other\":{\"unicode\":\"hello \\u00e4\",\"path\":\"C:\\\\Games\",\"values\":[true,false,null,1.25e5,9007199254740993]}}";
            Map<String,Object> json=LauncherJson.object(source);check(LauncherJson.object(LauncherJson.write(json)).equals(json),"JSON data lost");
        });
        test("malformed and duplicate-key JSON is rejected",()->{
            for(String source:List.of("{", "{\"a\":1,}", "{\"a\":1,\"a\":2}", "{\"x\":01}", "{} garbage", "[]", "{\"x\":\"bad\\q\"}")){
                try{LauncherJson.object(source);throw new AssertionError("accepted invalid JSON: "+source);}catch(IOException expected){}
            }
        });
        test("original profile merge preserves other installations and settings",()->{
            String source="{\"profiles\":{\"existing\":{\"name\":\"Keep me\",\"lastVersionId\":\"other\"}},\"selectedProfile\":\"existing\",\"unknown\":{\"flag\":true}}";
            Map<String,Object> before=LauncherJson.object(source),after=LauncherJson.object(OriginalAlphaInstallation.mergeProfile(source,"a1.0.16",Path.of("isolated-worlds")));
            check(before.get("selectedProfile").equals(after.get("selectedProfile")),"default selection changed");check(before.get("unknown").equals(after.get("unknown")),"other settings changed");
            Map<?,?> old=(Map<?,?>)before.get("profiles"),profiles=(Map<?,?>)after.get("profiles");
            check(old.get("existing").equals(profiles.get("existing")),"existing installation changed");check(profiles.size()==2,"installation not added");
            Map<?,?> profile=(Map<?,?>)profiles.get(OriginalAlphaInstallation.PROFILE_ID);check(profile.get("lastVersionId").equals("a1.0.16"),"not using actual installed version");
            check(!profile.containsKey("javaDir")&&!profile.containsKey("javaArgs"),"launcher runtime overridden");
        });
        test("updating own profile keeps user tuning",()->{
            String source="{\"profiles\":{\""+OriginalAlphaInstallation.PROFILE_ID+"\":{\"javaArgs\":\"-Xmx1G\",\"customFlag\":true}}}";
            Map<?,?> profiles=(Map<?,?>)LauncherJson.object(OriginalAlphaInstallation.mergeProfile(source,"a1.0.16",Path.of("worlds"))).get("profiles");
            Map<?,?> own=(Map<?,?>)profiles.get(OriginalAlphaInstallation.PROFILE_ID);check(own.get("javaArgs").equals("-Xmx1G")&&own.get("customFlag").equals(true),"user tuning replaced");
        });
        test("installed Alpha metadata and entry-point layout are recognized",()->{
            Path root=Files.createTempDirectory("original-alpha-layout-");
            try{Path jar=installed(root,"old_alpha");OriginalAlphaInstallation.Client client=OriginalAlphaInstallation.inspect(jar);check(client.minecraft().equals(root.toRealPath())&&client.version().equals("a1.0.16"),"incorrect installed client layout");}finally{delete(root);}
        });
        test("modern client metadata is rejected without changes",()->{
            Path root=Files.createTempDirectory("original-alpha-modern-");
            try{Path jar=installed(root,"release");try{OriginalAlphaInstallation.inspect(jar);throw new AssertionError("modern client accepted");}catch(IOException expected){check(expected.getMessage().contains("old_alpha"),"unclear version error");}}finally{delete(root);}
        });
        test("preparation makes exact backup and never changes original JAR or options",()->{
            Path root=Files.createTempDirectory("original-alpha-prepare-");
            try{
                Path jar=installed(root,"old_alpha");byte[] jarBefore=Files.readAllBytes(jar);
                Path profile=root.resolve("launcher_profiles.json");String original="{\"profiles\":{},\"unchanged\":true}";Files.writeString(profile,original);
                Path game=root.resolve("separate-game");Files.createDirectories(game);Files.writeString(game.resolve("options.txt"),"original-user-options");
                OriginalAlphaInstallation.Prepared ready=OriginalAlphaInstallation.prepare(OriginalAlphaInstallation.inspect(jar),game);
                check(ready.backups().size()==1&&Files.readString(ready.backups().get(0)).equals(original),"backup not exact");
                check(Arrays.equals(jarBefore,Files.readAllBytes(jar)),"original JAR changed");check(Files.readString(game.resolve("options.txt")).equals("original-user-options"),"existing options changed");
                check(ready.recordings()==0,"invented recordings");check(!Files.exists(root.resolve("saves")),"original save directory changed");
            }finally{delete(root);}
        });
        test("invalid profile prevents all profile writes",()->{
            Path root=Files.createTempDirectory("original-alpha-invalid-");
            try{
                Path jar=installed(root,"old_alpha");Path first=root.resolve("launcher_profiles.json"),second=root.resolve("launcher_profiles_microsoft_store.json");
                Files.writeString(first,"{\"profiles\":{}}");Files.writeString(second,"not JSON");
                try{OriginalAlphaInstallation.prepare(OriginalAlphaInstallation.inspect(jar),root.resolve("game"));throw new AssertionError("invalid metadata accepted");}catch(IOException expected){}
                check(Files.readString(first).equals("{\"profiles\":{}}")&&Files.readString(second).equals("not JSON"),"profile changed before validation");
            }finally{delete(root);}
        });
        test("resource paths cannot escape and modern recordings are not substituted",()->{
            for(String path:List.of("../escape.ogg","sound/../../escape.ogg","/sound/step/a.ogg","sound\\step\\a.ogg","minecraft/sounds/block/grass/step1.ogg","newsound/step/file.txt"))
                check(OriginalResourceCopy.logicalPath(path)==null,"unsafe or incompatible resource accepted: "+path);
            check(OriginalResourceCopy.logicalPath("newsound/step/grass1.ogg").equals(Path.of("newsound/step/grass1.ogg")),"legacy recording rejected");
        });
        test("precomputed camera rays are normalized and centered",()->{
            CameraRays rays=new CameraRays(101,61,70);int center=30*101+50;
            check(Math.abs(rays.right[center])+Math.abs(rays.up[center])<1e-12&&rays.forward[center]==1,"center projection wrong");
            for(int i=0;i<rays.right.length;i++)check(Math.abs(rays.right[i]*rays.right[i]+rays.up[i]*rays.up[i]+rays.forward[i]*rays.forward[i]-1)<1e-12,"ray not normalized");
        });
        test("precomputed projection matches former per-pixel normalization",()->{
            int width=120,height=68;CameraRays rays=new CameraRays(width,height,70);double tangent=Math.tan(Math.toRadians(70)/2);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++){
                double right=(2*(x+.5)/width-1)*width/(double)height*tangent,up=(1-2*(y+.5)/height)*tangent,length=Math.sqrt(1+right*right+up*up);int i=y*width+x;
                check(Math.abs(rays.right[i]-right/length)<1e-12&&Math.abs(rays.up[i]-up/length)<1e-12,"optimized ray changed projection");
            }
        });
        test("runtime mod inspection recognizes structural layout without executing client code",()->{
            try(InputStream in=OriginalEngineTests.class.getResourceAsStream("/OriginalEngineTests$SignatureChunk.class")){
                check(AlphaAgentSupport.isChunkShape(in),"valid structural signature missed");
            }
            try(InputStream in=OriginalEngineTests.class.getResourceAsStream("/OriginalEngineTests.class")){
                check(!AlphaAgentSupport.isChunkShape(in),"unrelated class accepted");
            }
        });
        test("agent arguments preserve other JVM options and agents",()->{
            String arguments=AlphaAgentSupport.arguments("-Xmx1G -javaagent:\"C:/other tools/other.jar\"",Path.of("C:/Alpha Desktop/mods/alpha-ambient-agent-0.5.0.jar"),"obf/Chunk");
            check(arguments.contains("-Xmx1G")&&arguments.contains("other tools/other.jar"),"user options changed");
            check(arguments.endsWith("=obf/Chunk\""),"mod path not quoted");
        });
        test("agent updates replace only this project's old argument",()->{
            String old="-Xmx2G -javaagent:\"C:/old path/alpha-ambient-agent-0.1.0.jar\"=old/Chunk";
            String updated=AlphaAgentSupport.arguments(old,Path.of("new/alpha-ambient-agent-0.5.0.jar"),"new/Chunk");
            check(!updated.contains("0.1.0")&&updated.contains("-Xmx2G")&&updated.endsWith("=new/Chunk\""),"stale hook not replaced");
            check(updated.indexOf("-javaagent:")==updated.lastIndexOf("-javaagent:"),"hook duplicated");
        });
        test("complete agent argument stays a single token with spaces and Unicode",()->{
            String arguments=AlphaAgentSupport.arguments(null,Path.of("C:/Users/Some Name/mods/alpha-ambient-agent-0.5.1.jar"),"a/Chunk");
            check(AlphaAgentSupport.rawArguments(arguments).size()==2,"agent argument split at spaces");
            check(AlphaAgentSupport.rawArguments("\"-javaagent:C:/Name \u00fc/path.jar=Chunk\"").size()==1,"Unicode quoted token split");
            check(AlphaAgentSupport.rawArguments(arguments).get(1).startsWith("\"-javaagent:"),"whole argument not quoted");
        });
        test("repair removes both historical and complete-quote agent arguments",()->{
            for(String own:List.of("-javaagent:\"C:/a b/alpha-ambient-agent-0.5.0.jar\"=old/Chunk","\"-javaagent:C:/a b/alpha-ambient-agent-0.5.1.jar=new/Chunk\"")){
                String kept=AlphaAgentSupport.removeOwnArguments("-Xmx1G "+own+" -javaagent:other.jar -Dkeep=true");
                check(kept.equals("-Xmx1G -javaagent:other.jar -Dkeep=true"),"repair removed another setting: "+kept);
            }
        });
        test("one-step disabling only edits this project's profiles and makes backups",()->{
            Path root=Files.createTempDirectory("alpha-disable-");
            try{
                String own="-Xmx1G -javaagent:alpha-ambient-agent-0.5.0.jar=Chunk";
                Map<String,Object> ours=new LinkedHashMap<>();ours.put("javaArgs",own);
                Map<String,Object> other=new LinkedHashMap<>();other.put("javaArgs",own);
                Map<String,Object> profiles=new LinkedHashMap<>();profiles.put("alpha-found-ambient",ours);profiles.put("other-installation",other);
                Path file=root.resolve("launcher_profiles.json");String before=LauncherJson.write(Map.of("profiles",profiles,"unchanged",true));Files.writeString(file,before);
                check(OriginalAlphaInstallation.disableAmbient(root)==1,"wrong repair count");
                Map<?,?> result=(Map<?,?>)LauncherJson.object(Files.readString(file)).get("profiles");
                check(((Map<?,?>)result.get("alpha-found-ambient")).get("javaArgs").equals("-Xmx1G"),"own agent remained");
                check(((Map<?,?>)result.get("other-installation")).get("javaArgs").equals(own),"unrelated installation changed");
                try(var backups=Files.list(root)){Path backup=backups.filter(p->p.getFileName().toString().contains(".alpha-backup-")).findFirst().orElseThrow();check(Files.readString(backup).equals(before),"backup not exact");}
            }finally{delete(root);}
        });
        System.out.println(passed+" original-engine/optimization tests passed.");
    }
    public static class SignatureChunk {
        public byte[] blocks;
        public SignatureChunk(Object world,byte[] blocks,int x,int z){this.blocks=blocks;}
        public void initialize(){}
    }
    private static Path installed(Path root,String type)throws IOException{
        Path folder=root.resolve("versions/a1.0.16");Files.createDirectories(folder);
        Files.writeString(folder.resolve("a1.0.16.json"),"{\"id\":\"a1.0.16\",\"type\":\""+type+"\"}");
        Path jar=folder.resolve("a1.0.16.jar");
        // Metadata-only entry: no proprietary game bytecode is copied or generated.
        try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(jar))){zip.putNextEntry(new ZipEntry("net/minecraft/client/MinecraftApplet.class"));zip.closeEntry();}
        return jar;
    }
    private static void delete(Path root)throws IOException{try(var paths=Files.walk(root)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
}
