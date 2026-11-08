import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Prepares an official-launcher profile. Never extracts or redistributes game code. */
final class OriginalAlphaInstallation {
    static final String PROFILE_ID="alpha-found-original-engine";
    static final String PROFILE_NAME="Alpha Found - Original Alpha";
    record Client(Path minecraft,Path jar,String version){}
    record Prepared(String version,Path gameDirectory,List<Path> profileFiles,List<Path> backups,int recordings,String profileName,boolean ambient){}

    static Client inspect(Path jar)throws IOException{
        jar=jar.toRealPath();Path versionFolder=jar.getParent(),versions=versionFolder==null?null:versionFolder.getParent();
        if(versions==null||!versions.getFileName().toString().equalsIgnoreCase("versions"))
            throw new IOException("Select the installed client under .minecraft/versions/<version>/<version>.jar. The official launcher also needs its matching version JSON and libraries.");
        String version=versionFolder.getFileName().toString();
        if(!version.matches("[A-Za-z0-9._-]+")||!jar.getFileName().toString().equals(version+".jar"))throw new IOException("The installed version folder and client JAR names do not match");
        Path metadata=versionFolder.resolve(version+".json");
        if(!Files.isRegularFile(metadata))throw new IOException("Matching launcher metadata missing: "+metadata);
        Map<String,Object> data=LauncherJson.object(Files.readString(metadata));
        if(!version.equals(data.get("id"))||!"old_alpha".equals(data.get("type")))
            throw new IOException("Select an installed old_alpha client. This mode does not rewrite modern Minecraft or another version's metadata.");
        try(ZipFile archive=new ZipFile(jar.toFile())){
            if(archive.getEntry("net/minecraft/client/MinecraftApplet.class")==null&&archive.getEntry("net/minecraft/client/Minecraft.class")==null)
                throw new IOException("Original Minecraft client entry point missing from selected JAR");
        }
        return new Client(versions.getParent(),jar,version);
    }
    static Prepared prepare(Client client,Path gameDirectory)throws IOException{
        return prepare(client,gameDirectory,false);
    }
    static Prepared prepare(Client client,Path gameDirectory,boolean ambient)throws IOException{
        gameDirectory=gameDirectory.toAbsolutePath().normalize();
        String chunkClass=ambient?AlphaAgentSupport.findChunkClass(client.jar):null;
        String profileId=ambient?"alpha-found-ambient":PROFILE_ID;
        String profileName=ambient?"Alpha Found":PROFILE_NAME;
        List<Path> profileFiles=new ArrayList<>();
        for(String name:List.of("launcher_profiles.json","launcher_profiles_microsoft_store.json")){
            Path path=client.minecraft.resolve(name);if(Files.isRegularFile(path))profileFiles.add(path);
        }
        if(profileFiles.isEmpty())throw new IOException("No Minecraft Launcher profiles were found in "+client.minecraft+". Open the official launcher's Java Edition once, then close it and try again. No launcher account or profile file was invented.");
        // Validate every file before writing any of them. Account/other settings are preserved.
        Map<Path,String> originals=new LinkedHashMap<>(),updated=new LinkedHashMap<>();
        for(Path file:profileFiles){
            String input=Files.readString(file);originals.put(file,input);
            updated.put(file,mergeProfile(input,client.version,gameDirectory,profileId,profileName));
        }
        Files.createDirectories(gameDirectory);
        if(ambient){
            Path agent=AlphaAgentSupport.install(gameDirectory.resolve("mods"));
            for(Path file:profileFiles){
                Map<String,Object> root=LauncherJson.object(updated.get(file));
                @SuppressWarnings("unchecked") Map<String,Object> profiles=(Map<String,Object>)root.get("profiles");
                @SuppressWarnings("unchecked") Map<String,Object> profile=(Map<String,Object>)profiles.get(profileId);
                Object oldArgs=profile.get("javaArgs");
                if(oldArgs!=null&&!(oldArgs instanceof String))throw new IOException("Unexpected JVM arguments in the selected profile");
                profile.put("javaArgs",AlphaAgentSupport.arguments((String)oldArgs,agent,chunkClass));
                updated.put(file,LauncherJson.write(root));
            }
        }
        Path options=gameDirectory.resolve("options.txt");
        if(!Files.exists(options))Files.writeString(options,"renderDistance:1\nfancyGraphics:true\nanaglyph3d:false\nbobView:true\nsound:1.0\nmusic:0.5\n",StandardOpenOption.CREATE_NEW);
        int recordings=OriginalResourceCopy.copy(client.minecraft,gameDirectory.resolve("resources"));
        List<Path> backups=new ArrayList<>();
        for(Path file:profileFiles){
            String original=originals.get(file);
            if(!Files.readString(file).equals(original))throw new IOException("Launcher profiles changed while preparing the installation. Close the launcher and retry; this file was not overwritten: "+file);
            Path backup=file.resolveSibling(file.getFileName()+".alpha-backup-"+UUID.randomUUID());
            Files.copy(file,backup,StandardCopyOption.COPY_ATTRIBUTES);backups.add(backup);
            Path temporary=Files.createTempFile(file.getParent(),".alpha-profile-",".tmp");
            try{
                Files.writeString(temporary,updated.get(file));
                if(!Files.readString(file).equals(original))throw new IOException("Launcher profiles changed before writing. Close the launcher and retry.");
                try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException e){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
            }finally{Files.deleteIfExists(temporary);}
        }
        return new Prepared(client.version,gameDirectory,List.copyOf(profileFiles),List.copyOf(backups),recordings,profileName,ambient);
    }
    static String mergeProfile(String input,String version,Path gameDirectory)throws IOException{
        return mergeProfile(input,version,gameDirectory,PROFILE_ID,PROFILE_NAME);
    }
    @SuppressWarnings("unchecked")
    private static String mergeProfile(String input,String version,Path gameDirectory,String profileId,String profileName)throws IOException{
        Map<String,Object> root=LauncherJson.object(input);Object existing=root.get("profiles");
        if(existing!=null&&!(existing instanceof Map))throw new IOException("Unexpected launcher profiles format; nothing was overwritten");
        Map<String,Object> profiles=existing==null?new LinkedHashMap<>():new LinkedHashMap<>((Map<String,Object>)existing);
        Object old=profiles.get(profileId);
        Map<String,Object> profile=old instanceof Map?new LinkedHashMap<>((Map<String,Object>)old):new LinkedHashMap<>();
        profile.put("name",profileName);profile.put("type","custom");profile.put("lastVersionId",version);
        profile.put("gameDir",gameDirectory.toAbsolutePath().normalize().toString());
        profile.putIfAbsent("resolution",Map.of("width",1280,"height",720));
        profiles.put(profileId,profile);root.put("profiles",profiles);
        return LauncherJson.write(root);
    }
}
