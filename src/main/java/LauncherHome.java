import java.nio.file.*;
import javax.swing.*;

/** The only end-user launch path. Legacy installation tools are not exposed here. */
final class LauncherHome {
    static void open()throws Exception{
        while(true){
            int[] choice={2};SwingUtilities.invokeAndWait(()->choice[0]=JOptionPane.showOptionDialog(null,
                    "Alpha Desktop "+LaunchSession.VERSION+"\n\nPlay the latest locally patched Alpha client directly.\nNo official launcher, old compatibility profiles or custom sandbox.\n\nAMD driver-timeout cause remains unresolved.\n60 FPS cap retained. Reactive encounters, original sounds and cosmetic effects enabled.\nF6 previews 19 events (new encounters first). F7 cancels. Leaf stripping stays off.\nDo not repeatedly retry if the driver resets again.",
                    "Alpha Desktop",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,
                    new String[]{"Play latest","Save latest session report","Close"},"Play latest"));
            if(choice[0]<0||choice[0]==2)return;
            try{
                if(choice[0]==0)DirectAlphaLaunch.open();else saveReport();
            }catch(Exception e){String message=e.getMessage()==null?e.toString():e.getMessage();SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,message,"Alpha Desktop",JOptionPane.ERROR_MESSAGE));}
        }
    }
    static void saveReport()throws Exception{
        String text=LaunchSession.report(DesktopSetup.dataDirectory());Path[] target={null};
        SwingUtilities.invokeAndWait(()->{JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("alpha-latest-session.txt"));if(chooser.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)target[0]=chooser.getSelectedFile().toPath();});
        if(target[0]==null)return;
        if(Files.exists(target[0])){
            int[] overwrite={-1};SwingUtilities.invokeAndWait(()->overwrite[0]=JOptionPane.showConfirmDialog(null,"Replace the selected report file?","Save report",JOptionPane.YES_NO_OPTION));if(overwrite[0]!=JOptionPane.YES_OPTION)return;
        }
        Files.writeString(target[0],text);SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,"Report saved. Review it before sharing; nothing was uploaded.","Latest session report",JOptionPane.INFORMATION_MESSAGE));
    }
}
