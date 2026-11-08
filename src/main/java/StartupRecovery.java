import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Native recovery UI. Local diagnosis + a clearly separate vanilla test profile. */
final class StartupRecovery {
    static void open()throws Exception{
        if(GraphicsEnvironment.isHeadless())throw new IOException("Startup recovery needs a desktop display");
        Path data=DesktopSetup.dataDirectory(),jar=savedJar(data),minecraft=inferMinecraft(jar);
        while(true){
            StartupDiagnostics.Report report=StartupDiagnostics.collect(minecraft,data);
            int[] choice={3};
            SwingUtilities.invokeAndWait(()->{
                JTextArea text=new JTextArea(report.text(),24,88);text.setEditable(false);text.setCaretPosition(0);text.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
                JPanel panel=new JPanel(new BorderLayout(0,10));
                panel.add(new JLabel("<html><b>Possible cause: "+report.finding().category()+"</b><br>Local log scan only—not proof that this is the current launch's failure. Nothing is uploaded.</html>"),BorderLayout.NORTH);
                panel.add(new JScrollPane(text),BorderLayout.CENTER);
                panel.add(new JLabel("Clean test installation = vanilla Alpha, no agents or horror effects. Existing profiles/worlds stay unchanged."),BorderLayout.SOUTH);
                choice[0]=JOptionPane.showOptionDialog(null,panel,"Alpha startup recovery",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,
                        new String[]{"Create clean test installation","Save diagnostic report","Refresh","Close"},"Create clean test installation");
            });
            if(choice[0]<0||choice[0]==3)return;
            if(choice[0]==2)continue;
            if(choice[0]==1){
                Path file=StartupDiagnostics.save(report,data.resolve("diagnostics"));
                SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,"Saved a redacted diagnostic report:\n"+file+"\n\nReview it before sharing; redaction is best-effort. Original logs were not copied and nothing was uploaded.","Report saved",JOptionPane.INFORMATION_MESSAGE));
                if(Desktop.isDesktopSupported())try{Desktop.getDesktop().open(file.toFile());}catch(IOException|UnsupportedOperationException ignored){}
                continue;
            }
            int[] consent={JOptionPane.CANCEL_OPTION};
            SwingUtilities.invokeAndWait(()->consent[0]=JOptionPane.showConfirmDialog(null,"Close Minecraft Launcher first.\n\nCreate/reset only the dedicated Alpha Found - Recovery profile?\nIt uses Java 8 on Windows, no agents, and a separate game directory.\nIt is NOT the horror mod. Existing installations and worlds are preserved.\nExisting launcher profile files are backed up before writing.","Create clean startup baseline",JOptionPane.OK_CANCEL_OPTION));
            if(consent[0]!=JOptionPane.OK_OPTION)continue;
            if(!Files.isRegularFile(jar)){jar=chooseJar(jar.getParent());if(jar==null)return;}
            try{
                OriginalAlphaInstallation.Client client=OriginalAlphaInstallation.inspect(jar);
                Path game=data.resolve("recovery-engine").resolve(client.version());
                OriginalAlphaInstallation.Prepared prepared=OriginalAlphaInstallation.prepareRecovery(client,game);
                String message="Clean test installation ready: "+prepared.profileName()+"\n\nOpen your normal Minecraft Launcher → Java Edition → Installations.\nSelect Alpha Found - Recovery, NOT an earlier Alpha Found entry, then Play.\n\nNo agents were installed. Existing profiles/worlds were not changed.\nThis is a vanilla diagnostic baseline, not a claim that the game now launches.\n\nIf it still fails, run Recover startup again and use Save diagnostic report.\nThat collects the available error automatically; you do not need to find log files.";
                SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,message,"Recovery installation ready",JOptionPane.INFORMATION_MESSAGE));return;
            }catch(IOException e){
                String message=e.getMessage();SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,message,"Recovery setup could not finish",JOptionPane.ERROR_MESSAGE));
                jar=chooseJar(jar.getParent());if(jar==null)return;minecraft=inferMinecraft(jar);
            }
        }
    }
    static Path savedJar(Path data)throws IOException{
        for(String filename:java.util.List.of("original-engine.properties","assets.properties")){
            Path file=data.resolve(filename);if(!Files.isRegularFile(file))continue;
            try{
                Properties values=new Properties();try(InputStream input=Files.newInputStream(file)){values.load(input);}
                String client=values.getProperty("client");if(client!=null&&!client.isBlank())return Path.of(client);
            }catch(IOException|IllegalArgumentException ignored){/* Bad old settings must not prevent recovery. */}
        }
        return AlphaDesktop.defaultMinecraft().resolve("versions/a1.0.16/a1.0.16.jar");
    }
    static Path inferMinecraft(Path jar){
        Path version=jar.toAbsolutePath().normalize().getParent(),versions=version==null?null:version.getParent();
        return versions!=null&&versions.getFileName().toString().equalsIgnoreCase("versions")?versions.getParent():AlphaDesktop.defaultMinecraft();
    }
    private static Path chooseJar(Path start)throws Exception{
        Path[] result={null};SwingUtilities.invokeAndWait(()->{
            JFileChooser picker=new JFileChooser();picker.setDialogTitle("Select your installed Alpha client JAR");picker.setFileFilter(new FileNameExtensionFilter("Minecraft client JAR","jar"));
            if(start!=null&&Files.isDirectory(start))picker.setCurrentDirectory(start.toFile());
            if(picker.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)result[0]=picker.getSelectedFile().toPath();
        });return result[0];
    }
    static void verifyPackagedRecovery()throws IOException{
        Path root=Files.createTempDirectory("Alpha recovery test ");
        try{
            Path mc=root.resolve("minecraft");Files.createDirectories(mc);Files.writeString(mc.resolve("launcher_profiles.json"),"{\"profiles\":{}}");
            OriginalAlphaInstallation.Client fixture=new OriginalAlphaInstallation.Client(mc,mc.resolve("unused-test-client.jar"),"a1.0.16");
            OriginalAlphaInstallation.prepareRecovery(fixture,root.resolve("clean game"));
            Map<String,Object> output=LauncherJson.object(Files.readString(mc.resolve("launcher_profiles.json")));
            Map<?,?> profile=(Map<?,?>)((Map<?,?>)output.get("profiles")).get("alpha-found-recovery");
            if(!"-Xmx1G".equals(profile.get("javaArgs")))throw new IOException("Recovery profile included unexpected JVM arguments");
            if(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")&&!profile.containsKey("javaDir"))throw new IOException("Recovery profile did not select the bundled Java 8 runtime");
            System.out.println("Packaged recovery configuration check passed. No original game was launched.");
        }finally{try(var files=Files.walk(root)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
