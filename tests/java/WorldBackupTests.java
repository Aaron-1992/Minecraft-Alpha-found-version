import java.nio.file.*;
import java.util.zip.*;

public class WorldBackupTests {
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("alpha-backup-test");
        try{
            if(WorldBackup.beforeUpgrade(root,"0.15.0")!=null)throw new AssertionError();
            Path save=root.resolve("saves/World1/level.dat");Files.createDirectories(save.getParent());Files.writeString(save,"original save");
            Path backup=WorldBackup.beforeUpgrade(root,"0.15.0");
            try(ZipFile zip=new ZipFile(backup.toFile())){
                if(!new String(zip.getInputStream(zip.getEntry("saves/World1/level.dat")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).equals("original save"))throw new AssertionError();
            }
            Files.writeString(save,"later save");WorldBackup.beforeUpgrade(root,"0.15.0");
            try(ZipFile zip=new ZipFile(backup.toFile())){if(!new String(zip.getInputStream(zip.getEntry("saves/World1/level.dat")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).equals("original save"))throw new AssertionError("Backup was overwritten");}
            try{WorldBackup.beforeUpgrade(root,"../other");throw new AssertionError();}catch(java.io.IOException expected){}
            Files.writeString(root.resolve("world-backups/before-0.15.0.zip"),"bad backup");
            try{WorldBackup.beforeUpgrade(root,"0.15.0");throw new AssertionError();}catch(java.io.IOException expected){}
            System.out.println("PASS one-time pre-upgrade world backup, original preservation, invalid paths and corrupt backup rejection");
        }finally{try(var walk=Files.walk(root)){for(Path p:walk.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
