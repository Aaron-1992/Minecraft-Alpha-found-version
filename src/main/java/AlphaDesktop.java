import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;

/** Desktop-only Java game. No browser, HTTP server, WebView, or generated media. */
public final class AlphaDesktop extends Canvas implements Runnable {
    private static final long serialVersionUID=1L;
    private final MinecraftAssets assets;
    private final OriginalAudio audio;
    private final VoxelRenderer renderer;
    private final JFrame window;
    private final WindowMode windowMode;
    private final AlphaPresentation presentation;
    private volatile long suppressFocusUntil;
    private final Robot robot;
    private final Path saveDir;
    private final Set<Integer> keys=ConcurrentHashMap.newKeySet();
    private final Queue<Runnable> actions=new ConcurrentLinkedQueue<>();
    private final AtomicInteger lookX=new AtomicInteger(),lookY=new AtomicInteger();
    private final List<Button> buttons=new ArrayList<>();
    private final Cursor invisible;
    private volatile boolean running=true,captured=false;
    private volatile int mouseX,mouseY;
    private String screen="title",previous="title",notice="";
    private VoxelWorld world;
    private Path worldFile;
    private double velocity=0,steps=0,autosave=0,time=0,sensitivity=.0024;
    private boolean grounded=false;
    private final VoxelRenderer.Hit hit=new VoxelRenderer.Hit();
    private boolean targeted=false,renderDirty=true;
    private record Button(int x,int y,int w,int h,String label,Runnable action) {}

    private AlphaDesktop(MinecraftAssets assets,OriginalAudio audio,Path saveDir) throws AWTException {
        this.assets=assets;this.audio=audio;this.saveDir=saveDir;
        renderer=new VoxelRenderer(assets.terrain);renderer.setClouds(assets.clouds);
        presentation=new AlphaPresentation(assets.font,assets.terrain);
        robot=new Robot();
        invisible=Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),new Point(0,0),"hidden");
        window=new JFrame("Minecraft Alpha — desktop recreation");
        windowMode=new WindowMode(window,this);
        setPreferredSize(new Dimension(960,540));setIgnoreRepaint(true);setFocusable(true);
        window.add(this);window.pack();window.setMinimumSize(new Dimension(640,360));window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){actions.add(()->shutdown());}});
        addFocusListener(new FocusAdapter(){@Override public void focusLost(FocusEvent e){actions.add(()->{if(System.nanoTime()>suppressFocusUntil&&!window.isFocused()&&world!=null&&screen.equals("game"))pause();});}});
        addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){boolean fresh=keys.add(e.getKeyCode());if(fresh)actions.add(()->key(e.getKeyCode()));}
            @Override public void keyReleased(KeyEvent e){keys.remove(e.getKeyCode());}
        });
        MouseAdapter mouse=new MouseAdapter(){
            @Override public void mouseMoved(MouseEvent e){motion(e);}
            @Override public void mouseDragged(MouseEvent e){motion(e);}
            @Override public void mousePressed(MouseEvent e){
                int x=uiX(e.getX()),y=uiY(e.getY());
                actions.add(()->{
                    if(screen.equals("game")){if(!captured)capture();else interact(e.getButton()==MouseEvent.BUTTON3);}
                    else for(Button b:List.copyOf(buttons)) if(x>=b.x&&x<b.x+b.w&&y>=b.y&&y<b.y+b.h){b.action.run();break;}
                });requestFocusInWindow();
            }
            @Override public void mouseWheelMoved(MouseWheelEvent e){actions.add(()->{if(world!=null&&screen.equals("game"))world.selected=Math.floorMod(world.selected+e.getWheelRotation(),9);});}
        };
        addMouseListener(mouse);addMouseMotionListener(mouse);addMouseWheelListener(mouse);
        window.setVisible(true);createBufferStrategy(2);requestFocusInWindow();
    }
    private double uiScale(){return Math.max(.1,Math.min(getWidth()/480.0,getHeight()/270.0));}
    private int uiX(int x){return (int)Math.floor((x-(getWidth()-480*uiScale())/2)/uiScale());}
    private int uiY(int y){return (int)Math.floor((y-(getHeight()-270*uiScale())/2)/uiScale());}
    private void toggleFullscreen(){
        boolean resume=screen.equals("game")&&captured;
        release();suppressFocusUntil=System.nanoTime()+1_000_000_000L;
        try{windowMode.toggle();if(resume)capture();}
        catch(Exception e){error("Could not change fullscreen mode: "+e.getMessage());if(world!=null)pause();}
    }
    private void motion(MouseEvent e){
        mouseX=uiX(e.getX());mouseY=uiY(e.getY());
        if(captured){int dx=e.getX()-getWidth()/2,dy=e.getY()-getHeight()/2;if(dx!=0||dy!=0){lookX.addAndGet(dx);lookY.addAndGet(dy);recenter();}}
    }
    private void recenter(){try{Point p=getLocationOnScreen();robot.mouseMove(p.x+getWidth()/2,p.y+getHeight()/2);}catch(IllegalComponentStateException ignored){}}
    private void capture(){screen="game";lookX.set(0);lookY.set(0);keys.clear();captured=true;setCursor(invisible);recenter();}
    private void release(){captured=false;setCursor(Cursor.getDefaultCursor());keys.clear();lookX.set(0);lookY.set(0);}
    private void pause(){screen="pause";release();save();}
    private void key(int key){
        if(key==KeyEvent.VK_F11){toggleFullscreen();return;}
        if(key==KeyEvent.VK_ESCAPE){
            if(screen.equals("game"))pause();
            else if(world!=null&&(screen.equals("pause")||screen.equals("inventory")))capture();
            else if(screen.equals("options"))screen=previous;
            else if(screen.equals("worlds"))screen="title";
        }
        if(world!=null&&screen.equals("game")){
            if(key>=KeyEvent.VK_1&&key<=KeyEvent.VK_9)world.selected=key-KeyEvent.VK_1;
            if(key==KeyEvent.VK_E){screen="inventory";release();}
        }else if(world!=null&&screen.equals("inventory")&&key==KeyEvent.VK_E)capture();
    }
    private void loadSlot(int slot){
        Path path=saveDir.resolve("World"+slot+".dat");
        try{
            if(Files.exists(path))world=VoxelWorld.load(path);
            else{
                // OS-native text entry is only configuration, not a generated game asset.
                final String[] value={null};
                SwingUtilities.invokeAndWait(()->value[0]=JOptionPane.showInputDialog(window,"Seed (leave blank for a new world)","Create World "+slot,JOptionPane.PLAIN_MESSAGE));
                if(value[0]==null)return;
                long seed;
                try{seed=value[0].isBlank()?new Random().nextLong():Long.parseLong(value[0]);}
                catch(NumberFormatException e){seed=value[0].hashCode();}
                world=new VoxelWorld(seed);
            }
            worldFile=path;renderDirty=true;velocity=0;autosave=0;steps=0;save();capture();
        }catch(Exception e){notice="Could not load world. See console.";e.printStackTrace();world=null;screen="worlds";}
    }
    private void save(){if(world==null||worldFile==null)return;try{world.save(worldFile);}catch(IOException e){System.err.println("Save failed: "+e);notice="Save failed. Check disk space and permissions.";}}
    private void shutdown(){save();release();running=false;}
    private void interact(boolean place){
        if(!targeted)return;int x=hit.x,y=hit.y,z=hit.z;
        int sound=hit.block;
        if(place){
            if(hit.axis==0)x+=hit.normal;if(hit.axis==1)y+=hit.normal;if(hit.axis==2)z+=hit.normal;
            if(x+1>world.x-.28&&x<world.x+.28&&z+1>world.z-.28&&z<world.z+.28&&y+1>world.y-1.62&&y<world.y+.16)return;
            sound=world.selected+1;
        }
        if(x<1||z<1||x>=VoxelWorld.SIZE-1||z>=VoxelWorld.SIZE-1||y<1||y>=VoxelWorld.HEIGHT)return;
        world.set(x,y,z,place?world.selected+1:0);audio.play(sound);
    }
    private void update(double dt){
        if(world==null||!screen.equals("game"))return;
        world.yaw-=lookX.getAndSet(0)*sensitivity;world.pitch=Math.max(-1.53,Math.min(1.53,world.pitch-lookY.getAndSet(0)*sensitivity));
        double f=(down(KeyEvent.VK_W)||down(KeyEvent.VK_UP)?1:0)-(down(KeyEvent.VK_S)||down(KeyEvent.VK_DOWN)?1:0);
        double s=(down(KeyEvent.VK_D)||down(KeyEvent.VK_RIGHT)?1:0)-(down(KeyEvent.VK_A)||down(KeyEvent.VK_LEFT)?1:0);
        boolean wet=world.get((int)world.x,(int)(world.y-1),(int)world.z)==10;
        double speed=wet?2.6:down(KeyEvent.VK_SHIFT)?1.3:4.3,length=Math.max(1,Math.hypot(f,s));
        double dx=(s*Math.cos(world.yaw)-f*Math.sin(world.yaw))*speed*dt/length;
        double dz=(-f*Math.cos(world.yaw)-s*Math.sin(world.yaw))*speed*dt/length;
        double nx=Math.max(1,Math.min(VoxelWorld.SIZE-1.01,world.x+dx));
        if(!world.collides(nx,world.y,world.z)&&(!down(KeyEvent.VK_SHIFT)||!grounded||world.collides(nx,world.y-.5,world.z)))world.x=nx;
        double nz=Math.max(1,Math.min(VoxelWorld.SIZE-1.01,world.z+dz));
        if(!world.collides(world.x,world.y,nz)&&(!down(KeyEvent.VK_SHIFT)||!grounded||world.collides(world.x,world.y-.5,nz)))world.z=nz;
        if(down(KeyEvent.VK_SPACE)&&(grounded||wet)){velocity=wet?3.5:7;grounded=false;}
        velocity-=dt*(wet?7:21);
        double ny=world.y+velocity*dt;
        if(world.collides(world.x,ny,world.z)){grounded=velocity<0;velocity=0;}else{world.y=ny;grounded=false;}
        if(world.y<0)world.y=VoxelWorld.HEIGHT+2;
        if((f!=0||s!=0)&&grounded){steps+=dt;if(steps>.45){steps=0;audio.play(world.get((int)world.x,(int)(world.y-1.7),(int)world.z));}}
        targeted=VoxelRenderer.trace(world,world.x,world.y,world.z,-Math.sin(world.yaw)*Math.cos(world.pitch),Math.sin(world.pitch),-Math.cos(world.yaw)*Math.cos(world.pitch),5.5,true,hit);
        autosave+=dt;if(autosave>15){save();autosave=0;}
    }
    private boolean down(int code){return keys.contains(code);}
    private void text(Graphics2D g,String text,int x,int y){presentation.text(g,text,x,y);}
    private void centered(Graphics2D g,String s,int y){text(g,s,240-presentation.width(s)/2,y);}
    private void button(Graphics2D g,int x,int y,int width,String label,Runnable action){
        int sy=mouseX>=x&&mouseX<x+width&&mouseY>=y&&mouseY<y+20?86:66;
        int half=width/2;
        g.drawImage(assets.gui,x,y,x+half,y+20,0,sy,half,sy+20,null);
        g.drawImage(assets.gui,x+half,y,x+width,y+20,200-(width-half),sy,200,sy+20,null);
        text(g,label,x+(width-presentation.width(label))/2,y+6);buttons.add(new Button(x,y,width,20,label,action));
    }
    private void tile(Graphics2D g,int block,int x,int y,int size){int t=VoxelRenderer.tile(block,2,1);g.drawImage(assets.terrain,x,y,x+size,y+size,t%16*16,t/16*16,t%16*16+16,t/16*16+16,null);}
    private void background(Graphics2D g){
        for(int x=0;x<480;x+=32)for(int y=0;y<270;y+=32)tile(g,2,x,y,32);
        g.setColor(new Color(0,0,0,155));g.fillRect(0,0,480,270);
    }
    private void draw(){
        // Paused menus/inventory reuse the last world frame rather than raycasting
        // another 129,600 pixels on every menu repaint/buffer restoration.
        if(world!=null&&(screen.equals("game")||renderDirty)){renderer.render(world,time);renderDirty=false;}
        BufferStrategy buffer=getBufferStrategy();
        do{
            do{
                Graphics2D output=(Graphics2D)buffer.getDrawGraphics();
                try{
                    output.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    output.setColor(Color.BLACK);output.fillRect(0,0,getWidth(),getHeight());
                    double scale=uiScale();
                    output.translate((getWidth()-480*scale)/2,(getHeight()-270*scale)/2);
                    output.scale(scale,scale);output.clipRect(0,0,480,270);
                    buttons.clear();
                    if(world!=null){output.drawImage(renderer.frame,0,0,480,270,null);}
                    else background(output);
                    if(screen.equals("game"))hud(output);
                    else{
                        if(world!=null){output.setColor(new Color(0,0,0,150));output.fillRect(0,0,480,270);}
                        menu(output);
                    }
                }finally{output.dispose();}
            }while(buffer.contentsRestored());
            buffer.show();Toolkit.getDefaultToolkit().sync();
        }while(buffer.contentsLost());
    }
    private void hud(Graphics2D g){
        if(targeted)selection(g);
        presentation.block(g,world.selected+1,370,220,90);
        // Original icons.png crosshair; original gui.png hotbar and selection frame.
        g.drawImage(assets.icons,232,127,248,143,0,0,16,16,null);
        g.drawImage(assets.gui,149,246,331,268,0,0,182,22,null);
        g.drawImage(assets.gui,148+world.selected*20,245,172+world.selected*20,267,0,22,24,44,null);
        for(int i=0;i<9;i++)presentation.block(g,i+1,152+i*20,258,16);
        text(g,"Minecraft Alpha",3,3);
    }
    private void selection(Graphics2D g){
        double[][] corners=new double[8][];
        double sy=Math.sin(world.yaw),cy=Math.cos(world.yaw),sp=Math.sin(world.pitch),cp=Math.cos(world.pitch);
        double focal=135/Math.tan(Math.toRadians(70)/2);
        for(int i=0;i<8;i++){
            double x=hit.x+(i&1)-world.x,y=hit.y+((i>>1)&1)-world.y,z=hit.z+((i>>2)&1)-world.z;
            double depth=-x*sy*cp+y*sp-z*cy*cp;
            if(depth>.05)corners[i]=new double[]{240+(x*cy-z*sy)*focal/depth,135-(x*sy*sp+y*cp+z*cy*sp)*focal/depth};
        }
        g.setColor(new Color(0,0,0,130));g.setStroke(new BasicStroke(.7f));
        for(int i=0;i<8;i++)for(int bit=1;bit<=4;bit*=2)if((i&bit)==0&&corners[i]!=null&&corners[i|bit]!=null)
            g.draw(new java.awt.geom.Line2D.Double(corners[i][0],corners[i][1],corners[i|bit][0],corners[i|bit][1]));
        g.setStroke(new BasicStroke(1));
    }
    private void menu(Graphics2D g){
        switch(screen){
            case "title" -> {
                presentation.title(g,assets.logo);
                button(g,140,110,200,"Singleplayer",()->{notice="";screen="worlds";});
                button(g,140,138,200,"Options...",()->{previous="title";screen="options";});
                button(g,140,166,200,"Quit game",this::shutdown);
                text(g,"Alpha desktop recreation",4,258);
            }
            case "worlds" -> {
                centered(g,"Select world",20);
                for(int i=1;i<=5;i++){final int slot=i;String suffix=Files.exists(saveDir.resolve("World"+i+".dat"))?"":" (empty)";button(g,140,34+i*28,200,"World "+i+suffix,()->loadSlot(slot));}
                button(g,140,220,200,"Back",()->screen="title");
                if(!notice.isEmpty())centered(g,notice,250);
            }
            case "pause" -> {
                centered(g,"Game menu",45);
                button(g,140,90,200,"Back to game",this::capture);
                button(g,140,120,200,"Options...",()->{previous="pause";screen="options";});
                button(g,140,150,200,"Save and quit to title",()->{save();world=null;worldFile=null;screen="title";});
                if(!notice.isEmpty())centered(g,notice,205);
            }
            case "options" -> {
                centered(g,"Options",30);
                button(g,140,70,200,"Sound: "+(audio.enabled?"ON":"OFF"),()->audio.enabled=!audio.enabled);
                button(g,140,100,200,"Render distance: "+renderer.distance,()->{renderer.distance=renderer.distance==52?76:renderer.distance==76?32:52;renderDirty=true;});
                button(g,140,130,200,"Sensitivity: "+(sensitivity==.0024?"Normal":sensitivity==.0012?"Low":"High"),()->sensitivity=sensitivity==.0024?.0012:sensitivity==.0012?.0036:.0024);
                button(g,140,160,200,"Fullscreen: "+(windowMode.isFullscreen()?"ON":"OFF")+" (F11)",this::toggleFullscreen);
                button(g,140,200,200,"Done",()->screen=previous);
            }
            case "inventory" -> {
                centered(g,"Select block",45);
                for(int i=0;i<9;i++){
                    final int b=i;int x=126+i%3*78,y=75+i/3*40;
                    button(g,x,y,72,"",()->{world.selected=b;capture();});presentation.block(g,i+1,x+28,y+10,16);
                }
                centered(g,VoxelWorld.NAMES[world.selected+1],210);
                button(g,140,232,200,"Done",this::capture);
            }
            default -> throw new IllegalStateException("Unknown screen: "+screen);
        }
    }
    @Override public void run(){
        long last=System.nanoTime();
        try{
            while(running){
                long start=System.nanoTime();double dt=Math.min(.05,(start-last)/1e9);last=start;time+=dt;
                Runnable action;while((action=actions.poll())!=null)action.run();
                if(!running)break;
                update(dt);draw();
                long sleep=16_666_667-(System.nanoTime()-start);if(sleep>0)Thread.sleep(sleep/1_000_000,(int)(sleep%1_000_000));
            }
        }catch(Exception e){e.printStackTrace();error("Desktop game stopped: "+e.getMessage());}
        finally{save();release();audio.close();try{assets.close();}catch(IOException ignored){}SwingUtilities.invokeLater(window::dispose);}
    }
    static Path defaultMinecraft(){
        String os=System.getProperty("os.name").toLowerCase(Locale.ROOT),home=System.getProperty("user.home");
        if(os.contains("win")){String app=System.getenv("APPDATA");return Path.of(app==null?home:app,".minecraft");}
        if(os.contains("mac"))return Path.of(home,"Library","Application Support","minecraft");
        return Path.of(home,".minecraft");
    }
    private static void error(String text){System.err.println(text);if(!GraphicsEnvironment.isHeadless())JOptionPane.showMessageDialog(null,text,"Alpha setup",JOptionPane.ERROR_MESSAGE);}
    public static void main(String[] args){
        try{
            if(args.length==0){LauncherHome.open();return;}
            if(args.length!=1)throw new IOException("Use AlphaDesktop without arguments for the latest launcher.");
            switch(args[0]){
                case "--play-direct" -> LauncherHome.open();
                case "--recover" -> LauncherHome.saveReport();
                case "--check-recovery" -> StartupRecovery.verifyPackagedRecovery();
                case "--check-agent-preflight" -> AlphaAgentSupport.verifyPackagedAgent();
                case "--check-runtime" -> {
                    boolean decoder=java.util.ServiceLoader.load(javax.sound.sampled.spi.AudioFileReader.class).stream()
                            .anyMatch(provider->provider.type().getName().toLowerCase(Locale.ROOT).contains("vorbis"));
                    if(!decoder)throw new IOException("Bundled original Ogg decoder is missing");
                    System.out.println("Desktop runtime and original Ogg decoder are available.");
                }
                case "--help" -> System.out.println("Alpha Desktop "+LaunchSession.VERSION+"\nOpen without arguments: Play latest / Save latest session report / Close.\nLegacy play modes are retired. Installed original a1.0.16 client and dependencies required.");
                default -> throw new IOException("Unknown or retired option. Open AlphaDesktop without arguments for Play latest.");
            }
        }catch(Exception e){error(e.getMessage()==null?e.toString():e.getMessage());System.exit(1);}
    }
}
