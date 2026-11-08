import java.awt.GraphicsEnvironment;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Native setup for using the user's real Alpha engine through their existing launcher. */
final class OriginalEngineSetup {
    static boolean chooseOriginal()throws Exception{
        int[] selected={2};
        SwingUtilities.invokeAndWait(()->selected[0]=JOptionPane.showOptionDialog(null,
                "For the authentic Alpha renderer, terrain, menus and gameplay, use the original engine.\n"
                +"This prepares a separate installation in your existing Minecraft Launcher.\n\n"
                +"The custom sandbox remains available, but is not the original Minecraft engine.",
                "Alpha - choose engine",JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,null,
                new String[]{"Original Alpha (recommended)","Custom sandbox","Cancel"},"Original Alpha (recommended)"));
        if(selected[0]==2||selected[0]<0)return true;
        if(selected[0]==0){open();return true;}return false;
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
        final int[] confirm={JOptionPane.CANCEL_OPTION};
        SwingUtilities.invokeAndWait(()->confirm[0]=JOptionPane.showConfirmDialog(null,
                "Close Minecraft Launcher before continuing so it will not overwrite installation settings.\n\n"
                +"This adds '"+OriginalAlphaInstallation.PROFILE_NAME+"' using your installed Alpha client.\n"
                +"Other installations and accounts are preserved; profile files are backed up first.\n"
                +"Original-engine worlds use their own folder, separate from the custom sandbox.\n\n"
                +"This mode is unmodified original Alpha. It does not add the prototype's atmosphere changes.\n\nContinue?",
                "Prepare original Alpha engine",JOptionPane.OK_CANCEL_OPTION,JOptionPane.INFORMATION_MESSAGE));
        if(confirm[0]!=JOptionPane.OK_OPTION)return;
        while(true){
            if(!Files.isRegularFile(candidate)){
                candidate=chooseJar(candidate.getParent());if(candidate==null)return;
            }
            try{
                OriginalAlphaInstallation.Client client=OriginalAlphaInstallation.inspect(candidate);
                Path game=DesktopSetup.dataDirectory().resolve("original-engine").resolve(client.version());
                OriginalAlphaInstallation.Prepared prepared=OriginalAlphaInstallation.prepare(client,game);
                saved.setProperty("client",candidate.toAbsolutePath().toString());
                Files.createDirectories(config.getParent());
                try(OutputStream output=Files.newOutputStream(config)){saved.store(output,"Original Alpha installation location only");}
                String report="Installation ready: "+OriginalAlphaInstallation.PROFILE_NAME+"\n\n"
                        +"1. Open your usual Minecraft Launcher and select Java Edition.\n"
                        +"2. Under Installations, select '"+OriginalAlphaInstallation.PROFILE_NAME+"'.\n"
                        +"3. Click Play. The launcher uses your normal account and the real Alpha engine.\n\n"
                        +"Version: "+prepared.version()+"\n"
                        +"Separate game directory: "+prepared.gameDirectory()+"\n"
                        +"Existing original recordings copied: "+prepared.recordings()+"\n"
                        +"Launcher profile backups are next to the original profile files.\n\n"
                        +"F11 and graphics settings are handled by the original client, not the prototype.\n"
                        +"No game classes were extracted, decompiled, patched or redistributed.";
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
