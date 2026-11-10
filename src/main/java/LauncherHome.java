import java.nio.file.*;
import javax.swing.*;

/** The only end-user launch path. Legacy installation tools are not exposed here. */
final class LauncherHome {
    static void open()throws Exception{
        while(true){
            int[] choice={2};SwingUtilities.invokeAndWait(()->choice[0]=JOptionPane.showOptionDialog(null,
                    welcome(),
                    "Minecraft Alpha Launcher",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,
                    new String[]{"Play Alpha","Save session report","Close"},"Play Alpha"));
            if(choice[0]<0||choice[0]==2)return;
            try{
                if(choice[0]==0)DirectAlphaLaunch.open();else saveReport();
            }catch(Exception e){String message=e.getMessage()==null?e.toString():e.getMessage();SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,message,"Minecraft Alpha Launcher",JOptionPane.ERROR_MESSAGE));}
        }
    }
    static String welcome(){
        return "Minecraft Alpha Launcher\n\nMinecraft Alpha a1.0.16\nUnofficial Windows launcher\n\nUses your locally installed client and original resources.\nBundled Java 8. Local single-player. No account sign-in.\n\nSeparate saves and automatic pre-upgrade backups.\nCommunity gameplay adjustments are included; worlds may differ from unmodified Alpha.\n\nIf a graphics-driver reset occurs, stop and save the session report.";
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
