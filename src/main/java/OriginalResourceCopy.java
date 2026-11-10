import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Copies only existing, unmodified legacy recordings into the isolated original game directory. */
final class OriginalResourceCopy {
    static Path logicalPath(String name){
        if(name.startsWith("minecraft/"))name=name.substring(10);
        if(name.contains("\\")||name.startsWith("/")||!name.endsWith(".ogg"))return null;
        Path path=Path.of(name).normalize();
        if(path.isAbsolute()||path.startsWith("..")||path.getNameCount()<2)return null;
        if(!Set.of("sound","newsound","music","newmusic").contains(path.getName(0).toString()))return null;
        return path;
    }
    static int copy(Path minecraft,Path destination)throws IOException{
        int count=0;destination=destination.toAbsolutePath().normalize();
        for(Path root:List.of(minecraft.resolve("resources"),minecraft.resolve("assets/virtual/legacy"),minecraft.resolve("assets/virtual/pre-1.6"))){
            if(!Files.isDirectory(root))continue;
            try(var paths=Files.walk(root,8)){
                for(Path source:paths.filter(p->Files.isRegularFile(p)&&!Files.isSymbolicLink(p)).toList()){
                    Path logical=logicalPath(root.relativize(source).toString().replace('\\','/'));
                    if(logical!=null&&copyFile(source,destination.resolve(logical)))count++;
                }
            }
        }
        Path indexes=minecraft.resolve("assets/indexes");
        if(!Files.isDirectory(indexes))return count;
        try(var files=Files.list(indexes)){
            for(Path index:files.filter(p->p.toString().endsWith(".json")).sorted().toList()){
                Map<String,Object> data;
                try{data=LauncherJson.object(Files.readString(index));}catch(IOException e){continue;}
                if(!(data.get("objects") instanceof Map<?,?> objects))continue;
                for(var entry:objects.entrySet()){
                    if(!(entry.getKey() instanceof String key)||!(entry.getValue() instanceof Map<?,?> info))continue;
                    Path logical=logicalPath(key);Object value=info.get("hash");
                    if(logical==null||!(value instanceof String hash)||!hash.matches("[a-f0-9]{40}"))continue;
                    Path target=destination.resolve(logical);if(Files.exists(target))continue;
                    Path source=minecraft.resolve("assets/objects/"+hash.substring(0,2)+"/"+hash);
                    if(!Files.isRegularFile(source)||Files.isSymbolicLink(source))continue;
                    if(!sha1(source).equals(hash))throw new IOException("Local original resource hash mismatch: "+source);
                    if(copyFile(source,target))count++;
                }
            }
        }
        return count;
    }
    static int copyLegacyFolder(Path root,Path destination)throws IOException{
        int count=0;root=root.toRealPath();destination=destination.toAbsolutePath().normalize();
        try(var files=Files.walk(root,8)){
            for(Path source:files.filter(p->Files.isRegularFile(p)&&!Files.isSymbolicLink(p)).toList()){
                Path logical=logicalPath(root.relativize(source).toString().replace('\\','/'));
                if(logical!=null&&source.toRealPath().startsWith(root)&&copyFile(source,destination.resolve(logical)))count++;
            }
        }return count;
    }
    private static boolean copyFile(Path source,Path target)throws IOException{
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))return false;
        Files.createDirectories(target.getParent());Files.copy(source,target);return true;
    }
    private static String sha1(Path source)throws IOException{
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-1");
            try(InputStream input=Files.newInputStream(source)){byte[] buffer=new byte[32768];int read;while((read=input.read(buffer))!=-1)digest.update(buffer,0,read);}
            return HexFormat.of().formatHex(digest.digest());
        }catch(NoSuchAlgorithmException e){throw new IOException(e);}
    }
}
