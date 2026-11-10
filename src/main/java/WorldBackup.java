import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** A local, one-time pre-upgrade copy of the mod's saves; never follows links or scans other games. */
final class WorldBackup {
    static Path beforeUpgrade(Path game,String version)throws IOException {
        if(!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IOException("Invalid backup version");
        Path saves=game.resolve("saves");
        if(!Files.exists(saves,LinkOption.NOFOLLOW_LINKS))return null;
        if(Files.isSymbolicLink(saves)||!Files.isDirectory(saves,LinkOption.NOFOLLOW_LINKS))throw new IOException("Refusing a linked or invalid saves folder");
        List<Path> files;
        try(var walk=Files.walk(saves)){files=walk.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).sorted().toList();}
        if(files.isEmpty())return null;
        Path folder=game.resolve("world-backups");
        if(Files.isSymbolicLink(folder))throw new IOException("Refusing a linked backup folder");
        Files.createDirectories(folder);Path target=folder.resolve("before-"+version+".zip");
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)){
            if(Files.isSymbolicLink(target))throw new IOException("Refusing a linked backup file");
            try(ZipFile check=new ZipFile(target.toFile())){if(check.size()==0)throw new IOException("Empty upgrade backup");}
            return target;
        }
        Path temp=Files.createTempFile(folder,".world-backup-",".tmp");
        try{
            try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(temp))){
                for(Path file:files){
                    if(Files.isSymbolicLink(file))throw new IOException("Save changed to a link during backup");
                    zip.putNextEntry(new ZipEntry("saves/"+saves.relativize(file).toString().replace('\\','/')));
                    Files.copy(file,zip);zip.closeEntry();
                }
            }
            Files.move(temp,target);return target;
        }finally{Files.deleteIfExists(temp);}
    }
    private WorldBackup(){}
}
