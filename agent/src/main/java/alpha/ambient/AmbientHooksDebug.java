package alpha.ambient;

import javax.swing.*;
import java.awt.*;

/** Hidden local developer selector. UI only submits requests; the tick thread executes them. */
public final class AmbientHooksDebug {
    private static boolean held;
    private static JFrame window;
    public static synchronized void key(boolean down){
        if(down&&!held&&!GraphicsEnvironment.isHeadless())SwingUtilities.invokeLater(new Runnable(){public void run(){open();}});
        held=down;
    }
    private static void open(){
        if(window!=null){window.setVisible(true);window.toFront();return;}
        window=new JFrame("Local debug tools");window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        final JComboBox<String> choices=new JComboBox<String>(AmbientHooks.eventNames());
        final JLabel status=new JLabel("Select a rule, then return to the game.");
        JButton run=new JButton("Queue selected"),stop=new JButton("Cancel transient effects");
        run.addActionListener(e->{AmbientHooks.selectDebug(choices.getSelectedIndex());status.setText("Queued: "+choices.getSelectedItem()+". Return to the game.");window.setVisible(false);});
        stop.addActionListener(e->{AmbientHooks.cancelDebug();status.setText("Cancellation queued. Saved edits are not undone.");window.setVisible(false);});
        JPanel panel=new JPanel(new GridLayout(0,1,6,6));panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        panel.add(new JLabel("Debug previews can permanently alter your world. Use a test save."));
        panel.add(new JLabel("Placement and visibility checks still apply; unsuitable contexts may skip."));
        panel.add(choices);panel.add(run);panel.add(stop);panel.add(status);
        window.setContentPane(panel);window.pack();window.setLocationRelativeTo(null);window.setVisible(true);
    }
    private AmbientHooksDebug(){}
}
