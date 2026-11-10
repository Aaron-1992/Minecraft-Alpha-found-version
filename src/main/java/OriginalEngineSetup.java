import java.awt.GraphicsEnvironment;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Native setup for using the user's real Alpha engine through their existing launcher. */
final class OriginalEngineSetup {
    static boolean chooseOriginal()throws Exception{
        int[] selected={4};
        SwingUtilities.invokeAndWait(()->selected[0]=JOptionPane.showOptionDialog(null,
                "Play patched Alpha starts your local client directly, without the official launcher.\n"
                +"It can also create a clean, separate original-client test installation with no agents.\n\n"
                +"Original Alpha setup and the independent custom sandbox remain available.",
                "Alpha - startup",JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,null,
                new String[]{"Play patched Alpha","Recover startup","Original Alpha setup","Custom sandbox","Cancel"},"Play patched Alpha"));
        if(selected[0]==3)return false;
        if(selected[0]==0)DirectAlphaLaunch.open();
        else if(selected[0]==1)StartupRecovery.open();
        else if(selected[0]==2)open();
        return true;
    }
    static void open()throws Exception{
        if(GraphicsEnvironment.isHeadless())throw new IOException("Original-engine setup requires a desktop display");
        Path config=DesktopSetup.dataDirectory().resolve("original-engine.properties");
        Properties saved=new Properties();
        if(Files.isRegularFile(config))try(InputStream in=Files.newInputStream(config)){saved.load(in);}
        if(!saved.containsKey("client")){
            Path old=DesktopSetup.dataDirectory().resolve("assets.properties");
            if(Files.isRegularFile(old))try(InputStream in=Files.newInputStream(old)){saved.load(in);}
        }
        Path candidate=Path.of(saved.getProperty("client",AlphaDesktop.defaultMinecraft().resolve("versions/a1.0.16/a1.0.16.jar").toString()));
        final int[] confirm={-1};
        SwingUtilities.invokeAndWait(()->confirm[0]=JOptionPane.showOptionDialog(null,
                "Close Minecraft Launcher before continuing.\n\n"
                +"Compatibility rebuild: isolated agent libraries, startup preflight and bundled Java 8 on Windows.\n"
                +"No replacement assets, jump scares, chat events or loud sound effects.\n"
                +"They use a NEW separate installation/game folder; use a new world.\n"
                +"Terrain changes saved there persist even if the mod is later disabled.\n\n"
                +"This is experimental: structural checks cannot prove compatibility with every Alpha JAR.\n"
                +"Original JARs and other installations are not modified; profile files are backed up.\n\n"
                +"Choose the optional mod or keep vanilla Alpha:",
                "Prepare Alpha installation",JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,null,
                new String[]{"Install compatibility build","Vanilla Alpha","Disable installed mod","Cancel"},"Vanilla Alpha"));
        if(confirm[0]<0||confirm[0]>2)return;
        boolean ambient=confirm[0]==0;
        while(true){
            if(!Files.isRegularFile(candidate)){
                candidate=chooseJar(candidate.getParent());if(candidate==null)return;
            }
            try{
                OriginalAlphaInstallation.Client client=OriginalAlphaInstallation.inspect(candidate);
                if(confirm[0]==2){
                    int changes=OriginalAlphaInstallation.disableAmbient(client.minecraft());
                    SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,"Removed this project's agent arguments from "+changes+" installation entries.\nOther JVM options and saved worlds were preserved. Launcher profile backups were made.","Mod disabled",JOptionPane.INFORMATION_MESSAGE));return;
                }
                Path game=DesktopSetup.dataDirectory().resolve(ambient?"compatibility-engine":"original-engine").resolve(client.version());
                OriginalAlphaInstallation.Prepared prepared=OriginalAlphaInstallation.prepare(client,game,ambient);
                saved.setProperty("client",candidate.toAbsolutePath().toString());
                Files.createDirectories(config.getParent());
                try(OutputStream output=Files.newOutputStream(config)){saved.store(output,"Original Alpha installation location only");}
                String report="Installation ready: "+prepared.profileName()+"\n\n"
                        +"1. Open your usual Minecraft Launcher and select Java Edition.\n"
                        +"2. Under Installations, select '"+prepared.profileName()+"'.\n"
                        +"3. Click Play. The launcher uses your normal account and the real Alpha engine.\n\n"
                        +"Version: "+prepared.version()+"\n"
                        +"Separate game directory: "+prepared.gameDirectory()+"\n"
                        +"Existing original recordings copied: "+prepared.recordings()+"\n"
                        +"Launcher profile backups are next to the original profile files.\n\n"
                        +"F11 and graphics settings are handled by the original client, not the prototype.\n"
                        +(ambient?"Agent startup preflight passed. Original-client launch is still not verified.\nUse the NEW Alpha Found - Compatibility installation, not the old Alpha Found entry.\nUse a new world. Local startup details: mods/agent-preflight.log":"Vanilla installation; optional hooks are not enabled.");
                SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,report,"Original Alpha ready",JOptionPane.INFORMATION_MESSAGE));
                return;
            }catch(IOException e){
                String text=e.getMessage();final int[] retry={JOptionPane.CANCEL_OPTION};
                SwingUtilities.invokeAndWait(()->retry[0]=JOptionPane.showConfirmDialog(null,text+"\n\nSelect a different installed Alpha client?",
                        "Original Alpha setup",JOptionPane.OK_CANCEL_OPTION,JOptionPane.ERROR_MESSAGE));
                if(retry[0]!=JOptionPane.OK_OPTION)return;
                candidate=chooseJar(candidate.getParent());if(candidate==null)return;
            }
        }
    }
    private static Path chooseJar(Path start)throws Exception{
        Path[] chosen={null};SwingUtilities.invokeAndWait(()->{
            JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Select installed Alpha JAR inside .minecraft/versions");
            chooser.setFileFilter(new FileNameExtensionFilter("Installed Minecraft Alpha client (*.jar)","jar"));
            if(start!=null&&Files.isDirectory(start))chooser.setCurrentDirectory(start.toFile());
            if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)chosen[0]=chooser.getSelectedFile().toPath();
        });return chosen[0];
    }
}
