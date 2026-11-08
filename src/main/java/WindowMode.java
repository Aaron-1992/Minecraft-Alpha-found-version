import java.awt.*;
import java.awt.image.BufferStrategy;
import javax.swing.*;

/** Borderless fullscreen on the current monitor; no display-mode changes. */
final class WindowMode {
    private final JFrame window;
    private final Canvas canvas;
    private Rectangle windowedBounds;
    private int windowedState;
    private boolean fullscreen;
    WindowMode(JFrame window,Canvas canvas){this.window=window;this.canvas=canvas;}
    boolean isFullscreen(){return fullscreen;}
    void toggle() throws Exception {
        if(SwingUtilities.isEventDispatchThread())switchOnEdt();
        else SwingUtilities.invokeAndWait(this::switchOnEdt);
    }
    private void switchOnEdt(){
        if(!fullscreen){windowedBounds=window.getBounds();windowedState=window.getExtendedState();}
        Rectangle monitor=window.getGraphicsConfiguration().getBounds();
        BufferStrategy old=canvas.getBufferStrategy();if(old!=null)old.dispose();
        window.dispose();
        window.setExtendedState(JFrame.NORMAL);
        window.setUndecorated(!fullscreen);
        if(!fullscreen)window.setBounds(monitor);
        else window.setBounds(windowedBounds);
        window.setVisible(true);
        if(fullscreen)window.setExtendedState(windowedState);
        fullscreen=!fullscreen;
        canvas.createBufferStrategy(2);
        window.toFront();canvas.requestFocusInWindow();
    }
}
