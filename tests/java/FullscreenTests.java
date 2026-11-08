import java.awt.*;
import javax.swing.*;

/** Real AWT window/buffer lifecycle test. No Minecraft asset or media fixtures. */
public final class FullscreenTests {
    public static void main(String[] args)throws Exception{
        if(GraphicsEnvironment.isHeadless())throw new IllegalStateException("Run with a desktop or xvfb-run");
        SwingUtilities.invokeAndWait(()->{
            JFrame frame=new JFrame("Window mode test");Canvas canvas=new Canvas();
            try{
                canvas.setPreferredSize(new Dimension(640,360));frame.add(canvas);frame.pack();frame.setLocation(30,40);frame.setVisible(true);
                canvas.createBufferStrategy(2);
                Rectangle bounds=frame.getBounds();Rectangle monitor=frame.getGraphicsConfiguration().getBounds();
                WindowMode mode=new WindowMode(frame,canvas);
                try{
                    mode.toggle();
                    check(mode.isFullscreen()&&frame.isUndecorated(),"fullscreen must be borderless");
                    check(frame.getBounds().equals(monitor),"fullscreen must fill the current monitor");
                    check(canvas.getBufferStrategy()!=null,"fullscreen drawing buffer missing");
                    System.out.println("PASS enter borderless fullscreen and recreate buffer");
                    mode.toggle();
                    check(!mode.isFullscreen()&&!frame.isUndecorated(),"window decorations not restored");
                    check(frame.getBounds().equals(bounds),"window position/size not restored");
                    check(canvas.getBufferStrategy()!=null,"windowed drawing buffer missing");
                    System.out.println("PASS restore window bounds and decorations");
                    for(int i=0;i<4;i++)mode.toggle();
                    check(!mode.isFullscreen()&&frame.getBounds().equals(bounds),"repeated toggles changed window bounds");
                    System.out.println("PASS repeated fullscreen toggles");
                }catch(Exception e){throw new RuntimeException(e);}
            }finally{
                if(canvas.getBufferStrategy()!=null)canvas.getBufferStrategy().dispose();frame.dispose();
            }
        });
        System.out.println("3 desktop fullscreen tests passed.");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
