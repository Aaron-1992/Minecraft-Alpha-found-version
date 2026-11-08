import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** First-run native asset chooser. Keeps settings/saves out of Program Files. */
final class DesktopSetup {
    record Loaded(MinecraftAssets assets, Path saves) {}
    static Path dataDirectory() {
        String appData=System.getenv("APPDATA");
        return appData!=null&&!appData.isBlank()?Path.of(appData,"AlphaDesktop"):
                Path.of(System.getProperty("user.home"),".alpha-desktop");
    }
    static Loaded open() throws Exception {
        Path config=dataDirectory().resolve("assets.properties");
        Properties props=new Properties();
        if(Files.isRegularFile(config))try(InputStream in=Files.newInputStream(config)){props.load(in);}
        Path mc=path(props,"minecraft",AlphaDesktop.defaultMinecraft());
        Path jar=path(props,"client",mc.resolve("versions/a1.0.16/a1.0.16.jar"));
        Path sounds=path(props,"sounds",null);
        if(!Files.isRegularFile(jar)){
            message("Choose your original Minecraft Alpha client JAR.\nNext, choose the original sound folder if it is not found automatically.\nMinecraft assets are not included or generated.");
            jar=choose("Select original Alpha client JAR",false,mc);
            if(jar==null)return null;
        }
        while(true){
            try{
                MinecraftAssets assets=new MinecraftAssets(jar,mc,sounds);
                props.setProperty("minecraft",mc.toAbsolutePath().toString());
                props.setProperty("client",jar.toAbsolutePath().toString());
                if(sounds!=null)props.setProperty("sounds",sounds.toAbsolutePath().toString());
                Files.createDirectories(config.getParent());
                try(OutputStream out=Files.newOutputStream(config)){props.store(out,"Original Minecraft asset locations; no media copied");}
                catch(IOException e){assets.close();throw e;}
                return new Loaded(assets,dataDirectory().resolve("saves"));
            }catch(MinecraftAssets.MissingSoundsException e){
                message(e.getMessage()+"\n\nChoose the original sound folder containing the step subfolder.");
                sounds=choose("Select original Minecraft sound folder",true,sounds==null?mc:sounds);
                if(sounds==null)return null;
            }catch(IOException e){
                message("Could not open the original client assets:\n"+e.getMessage()+"\n\nSelect a legacy Alpha client JAR, or cancel to exit.");
                jar=choose("Select original Alpha client JAR",false,jar.getParent());
                if(jar==null)return null;
            }
        }
    }
    private static Path path(Properties props,String key,Path fallback){String value=props.getProperty(key);return value==null?fallback:Path.of(value);}
    private static void message(String text)throws Exception{
        SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,text,"Alpha setup",JOptionPane.INFORMATION_MESSAGE));
    }
    private static Path choose(String title,boolean directory,Path start)throws Exception{
        Path[] result={null};
        SwingUtilities.invokeAndWait(()->{
            JFileChooser picker=new JFileChooser();picker.setDialogTitle(title);
            picker.setFileSelectionMode(directory?JFileChooser.DIRECTORIES_ONLY:JFileChooser.FILES_ONLY);
            if(start!=null&&Files.isDirectory(start))picker.setCurrentDirectory(start.toFile());
            if(!directory)picker.setFileFilter(new FileNameExtensionFilter("Minecraft client JAR (*.jar)","jar"));
            if(picker.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)result[0]=picker.getSelectedFile().toPath();
        });return result[0];
    }
}
