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
        System.out.println(passed+" original-engine/optimization tests passed.");
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
