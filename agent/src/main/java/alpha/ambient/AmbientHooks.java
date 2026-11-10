package alpha.ambient;

import java.util.Map;
import java.util.WeakHashMap;

/** Java 8 ambience hooks. Original game model/audio only; no network or direct file access. */
public final class AmbientHooks {
    private static final Map<Object,Pending> pending=new WeakHashMap<Object,Pending>();
    private static final java.util.Random atmosphere=new java.util.Random();
    private static int quietTicks=20*(120+atmosphere.nextInt(121));
    private static long activeTicks;
    private static boolean newKeyHeld;
    private static int newCursor,forcedIndex=-1;
    private static volatile float lastFogEnd=48;
    private static boolean keyHeld,forceEvent,particle,cancelHeld,cancelRequested,replaceScene;
    private static int encounterRequest=-1,anomalyRequest=-1;
    private static boolean encounterPreview,fixedAudio;
    private static double cueX,cueY,cueZ;
    private static float cueGain=.16f,cuePitch=.95f;
    private static final java.util.ArrayDeque<Cue> cues=new java.util.ArrayDeque<Cue>();
    private static final class Cue {final String sound;final double x,y,z;final float gain,pitch;Cue(String s,double x,double y,double z,float gain,float pitch){sound=s;this.x=x;this.y=y;this.z=z;this.gain=gain;this.pitch=pitch;}}
    private static void queueCue(String sound,double x,double y,double z){queueCue(sound,x,y,z,.16f,.95f);}
    static void queueCue(String sound,double x,double y,double z,float gain,float pitch){if(cues.size()<8)cues.addLast(new Cue(sound,x,y,z,gain,pitch));}
    public static void cancelKey(boolean down){if(down&&!cancelHeld)cancelRequested=true;cancelHeld=down;}
    public static float soundVolume(){return fixedAudio?cueGain:.16f;}
    public static float soundPitch(){return fixedAudio?cuePitch:.95f;}
    public static double soundY(double original){return fixedAudio?cueY:original;}
    private static int events,previewCursor,echoes,echoDelay;
    private static String sequenceSound="step.stone";
    private static double audioAngle=135,audioRadius=6;
    private static int sceneReturns,sceneLifetime=65;
    private static boolean orbitSound;
    private static boolean sceneRequest,sceneCrossing,scenePreview;
    private static long driftUntil,textUntil;
    private static String lastEvent="NONE";
    private static final String[] LEGACY_NAMES={"WATCHER","CROSSING","FOOTSTEP_ECHO","FALSE_BLOCK","SMOKE","FOG_DRIFT","HUD_DRIFT","SECOND_LOOK","ORBIT_STEPS","DISTANT_KNOCKS","FALSE_DIGGING","LONG_WATCH","BEHIND_YOU","AFTERIMAGE","STALKER_ENCOUNTER","SOUND_LURE","INTERCEPTION","PASS_BY","TURN_AROUND","TREE_LINE","NULL_LURK","LOST_FOOTSTEPS","REPEATER"};
    private static final String[] EVENT_NAMES=allNames();
    private static String[] allNames(){String[] n=java.util.Arrays.copyOf(LEGACY_NAMES,LEGACY_NAMES.length+AmbientHooksAnomalies.Event.values().length);for(AmbientHooksAnomalies.Event e:AmbientHooksAnomalies.Event.values())n[LEGACY_NAMES.length+e.ordinal()]=e.name();return n;}
    static String[] eventNames(){return EVENT_NAMES.clone();}
    private static final int[] LEGACY_TIERS={2,2,1,1,1,1,1,2,2,1,2,2,2,2,2,2,3,2,3,2,3,2,3};
    private static final int[] LEGACY_WEIGHTS={2,2,5,1,1,4,1,2,3,4,3,2,2,1,3,3,2,3,2,3,2,4,2};
    private static final int[] EVENT_TIERS=allValues(true),EVENT_WEIGHTS=allValues(false);
    private static int[] allValues(boolean tiers){int[] values=java.util.Arrays.copyOf(tiers?LEGACY_TIERS:LEGACY_WEIGHTS,EVENT_NAMES.length);for(AmbientHooksAnomalies.Event e:AmbientHooksAnomalies.Event.values())values[23+e.ordinal()]=tiers?e.tier:e.weight;return values;}
    private static long previewFogUntil;
    public static synchronized void previewKey(boolean down){
        if(down&&!keyHeld){forceEvent=true;forcedIndex=-1;}
        keyHeld=down;
    }
    public static synchronized void anomalyPreviewKey(boolean down){if(down&&!newKeyHeld){forceEvent=true;forcedIndex=23+(newCursor++%AmbientHooksAnomalies.Event.values().length);}newKeyHeld=down;}
    static double outsideFogDistance(){return Math.max(64,Math.min(272,lastFogEnd+16));}
    static double fogEdge(){return Math.max(24,Math.min(96,lastFogEnd*.9));}
    static int intervalSeconds(long ticks){
        if(ticks>=20L*60*35)return 30+atmosphere.nextInt(61);
        if(ticks>=20L*60*15)return 60+atmosphere.nextInt(91);
        return 120+atmosphere.nextInt(121);
    }
    /** Active game ticks only; F6 skips the wait, not terrain placement checks. */
    public static synchronized String nextSound(boolean active){
        fixedAudio=false;
        if(!active){keyHeld=false;newKeyHeld=false;cancelHeld=false;forceEvent=false;forcedIndex=-1;textUntil=0;return null;}
        activeTicks++;
        if(!cues.isEmpty()){
            Cue cue=cues.removeFirst();fixedAudio=true;cueX=cue.x;cueY=cue.y;cueZ=cue.z;cueGain=cue.gain;cuePitch=cue.pitch;
            System.out.println("[Alpha director] anchored soundRequest="+cue.sound);return cue.sound;
        }
        if(!Boolean.parseBoolean(System.getProperty("alpha.scheduledSounds","true")))return null;
        if(!forceEvent&&echoes>0&&!Scene.moving&&--echoDelay<=0){
            echoes--;echoDelay=10+atmosphere.nextInt(17);
            if(orbitSound)audioAngle+=65;
            System.out.println("[Alpha ambience] sequence="+lastEvent+" remaining="+echoes+" soundRequest="+sequenceSound);return sequenceSound;
        }
        if(!forceEvent&&(Scene.director.active()||AmbientHooksAnomalies.active()))return null;
        if(!forceEvent&&--quietTicks>0)return null;
        boolean preview=forceEvent;forceEvent=false;quietTicks=20*intervalSeconds(activeTicks);
        echoes=0;
        int index=0;if(preview){if(forcedIndex>=0){index=forcedIndex;forcedIndex=-1;}else index=previewCursor++%EVENT_NAMES.length;}
        AmbientHooksState state=Scene.world==null?null:AmbientHooksState.of(Scene.world);
        int kind=preview?(index<9?14+index:index<23?index-9:index):state==null?-1:state.choose(EVENT_NAMES,EVENT_TIERS,EVENT_WEIGHTS,atmosphere);
        if(kind<0)return null; // Exhaustion means quiet, never bypass the repeat lock.
        if(!preview)state.mark(EVENT_NAMES[kind]);
        replaceScene=true;
        lastEvent=EVENT_NAMES[kind];
        if(kind>=23){anomalyRequest=kind-23;++events;sceneRequest=false;encounterRequest=-1;System.out.println("[Alpha anomaly] selected="+lastEvent+" preview="+preview);return null;}
        particle=kind==4||kind==13;
        audioAngle=135;audioRadius=6;orbitSound=false;sceneReturns=0;sceneLifetime=65;
        sceneRequest=kind==0||kind==1||kind==7||kind==11||kind==12||kind==13;sceneCrossing=kind==1;scenePreview=preview;
        if(kind==2||kind==8){echoes=4;echoDelay=12;sequenceSound="step.stone";orbitSound=kind==8;}
        if(kind==9){echoes=3;echoDelay=18;sequenceSound="step.wood";audioRadius=10;}
        if(kind==10){echoes=5;echoDelay=14;sequenceSound="step.gravel";audioRadius=8;}
        if(kind==7){sceneReturns=2;sceneLifetime=80;}
        if(kind==11){sceneLifetime=160;audioRadius=12;}
        if(kind==12){audioAngle=170;sceneLifetime=100;}
        if(kind==13){sceneReturns=1;sceneLifetime=45;driftUntil=activeTicks+240;}
        if(kind==6)textUntil=activeTicks+20*8;
        if(kind==5){driftUntil=activeTicks+20*12;if(preview)previewFogUntil=System.nanoTime()+6000000000L;}
        ++events;
        String[] sounds={"step.grass","step.wood","step.stone","random.click","random.pop","step.gravel","step.wood","step.grass","step.stone","step.wood","step.gravel","step.grass","step.gravel","random.pop"};
        if(kind>=14){encounterRequest=kind-14;encounterPreview=preview;}
        String sound=kind>=14?null:sounds[kind];
        System.out.println("[Alpha ambience] tick="+activeTicks+" event="+events+" type="+EVENT_NAMES[kind]+" preview="+preview+" soundRequest="+sound);
        return sound;
    }
    static int eventCount(){return EVENT_NAMES.length;}
    static String lastEventName(){return lastEvent;}
    public static int textOffset(int coordinate){return activeTicks<textUntil?coordinate+((activeTicks/40)%2==0?1:2):coordinate;}
    public static synchronized boolean takeParticle(){particle=false;return false;}
    public static double soundX(float yaw){return fixedAudio?cueX-Scene.px:-Math.sin(Math.toRadians(yaw+audioAngle))*audioRadius;}
    public static double soundZ(float yaw){return fixedAudio?cueZ-Scene.pz:Math.cos(Math.toRadians(yaw+audioAngle))*audioRadius;}
    public static double visualX(float yaw){return -Math.sin(Math.toRadians(yaw+32))*9;}
    public static double visualZ(float yaw){return Math.cos(Math.toRadians(yaw+32))*9;}
    /** Runtime-only adapter for the inspected a1.0.16 client. All world work stays on its tick thread. */
    public static void scene(Object world,Object player){Scene.tick(world,player);}
    public static boolean isWatcher(Object value){return value!=null&&value.getClass().getName().equals("AlphaWatcher");}
    static final class Scene {
        static Object world,player,figure;
        static java.lang.reflect.Method block,loaded,spawn,position,dead,raycast,vector;
        static java.lang.reflect.Field x,y,z,yaw,pitch;
        static boolean disabled,seen,moving,sheltered,pendingRemoval;
        static int age,life,returns,returnDelay,placement,ticks;
        static double fx,fy,fz,previousX,previousZ,px,py,pz;
        static float facing,lookingPitch,lastFacing;
        static int groundMaterial;
        static final AmbientHooksDirector director=new AmbientHooksDirector();
        static final AmbientHooksDirector.Input input=new AmbientHooksDirector.Input();
        static double anchorX,anchorY,anchorZ;
        static final double[][] trail=new double[40][3];static int trailCursor,trailCount;
        static AmbientHooksDirector.Phase loggedPhase=AmbientHooksDirector.Phase.IDLE;
        static boolean rayVisible;
        static void tick(Object nextWorld,Object nextPlayer){
            if(disabled)return;
            try{
                if(nextWorld==null||nextPlayer==null){AmbientHooksAnomalies.cancel(true);forceRemove();cancel();world=null;player=null;return;}
                if(world!=nextWorld){AmbientHooksAnomalies.cancel(true);forceRemove();cancel();world=nextWorld;player=nextPlayer;ticks=0;sheltered=false;moving=false;groundMaterial=0;activeTicks=AmbientHooksState.of(world).ticks;quietTicks=20*intervalSeconds(activeTicks);
                    ClassLoader loader=world.getClass().getClassLoader();Class<?> wc=Class.forName("cj",false,loader),entity=Class.forName("jx",false,loader);
                    block=wc.getMethod("a",int.class,int.class,int.class);loaded=wc.getDeclaredMethod("h",int.class,int.class);loaded.setAccessible(true);spawn=wc.getMethod("a",entity);
                    position=entity.getMethod("b",double.class,double.class,double.class,float.class,float.class);dead=entity.getMethod("D");
                    Class<?> vec=Class.forName("ah",false,loader);vector=vec.getMethod("a",double.class,double.class,double.class);raycast=wc.getMethod("a",vec,vec);
                    x=entity.getField("ah");y=entity.getField("ai");z=entity.getField("aj");yaw=entity.getField("an");pitch=entity.getField("ao");previousX=x.getDouble(player);previousZ=z.getDouble(player);
                }
                player=nextPlayer;px=x.getDouble(player);py=y.getDouble(player);pz=z.getDouble(player);facing=yaw.getFloat(player);lookingPitch=pitch.getFloat(player);ticks++;AmbientHooksState.of(world).tick();
                if(cancelRequested){cancelRequested=false;cancel();forceEvent=false;forcedIndex=-1;echoes=0;particle=false;textUntil=0;driftUntil=0;previewFogUntil=0;quietTicks=Math.max(quietTicks,400);System.out.println("[Alpha director] cancelled by F7");return;}
                if(pendingRemoval){remove();if(pendingRemoval)return;}
                moving=Math.hypot(px-previousX,pz-previousZ)>.025;previousX=px;previousZ=pz;
                trail[trailCursor][0]=px;trail[trailCursor][1]=py;trail[trailCursor][2]=pz;trailCursor=(trailCursor+1)%trail.length;trailCount=Math.min(trail.length,trailCount+1);
                if(ticks%20==0){sheltered=read((int)Math.floor(px),(int)Math.floor(py)+3,(int)Math.floor(pz))!=0;groundMaterial=read((int)Math.floor(px),(int)Math.floor(py)-1,(int)Math.floor(pz));}
                if(replaceScene){int requested=encounterRequest,anomaly=anomalyRequest;boolean legacy=sceneRequest;cancel();encounterRequest=requested;anomalyRequest=anomaly;sceneRequest=legacy;replaceScene=false;}
                if(anomalyRequest>=0){int requested=anomalyRequest;anomalyRequest=-1;AmbientHooksAnomalies.begin(requested,scenePreview);}
                try{AmbientHooksAnomalies.tick();}catch(Exception error){AmbientHooksAnomalies.cancel(false);System.err.println("[Alpha anomaly] safely cancelled: "+error.getClass().getSimpleName()+": "+error.getMessage());}
                if(encounterRequest>=0){
                    int mode=encounterRequest;encounterRequest=-1;cancel();scenePreview=encounterPreview;
                    director.begin(AmbientHooksDirector.Mode.values()[mode],scenePreview);
                    double angle=Math.toRadians(facing+70);anchorX=px-Math.sin(angle)*14;anchorY=py;anchorZ=pz+Math.cos(angle)*14;
                    System.out.println("[Alpha director] started mode="+director.mode()+" preview="+scenePreview);
                }
                if(director.active()){tickDirector();return;}
                if(figure!=null){
                    age++;double distance=Math.hypot(fx-px,fz-pz);
                    boolean observed=observed();if(observed)seen=true;
                    if(age>life||distance<14||(seen&&age>20&&!observed)){
                        boolean canReturn=distance>=14&&returns>0;remove();
                        if(canReturn){returns--;returnDelay=45+atmosphere.nextInt(36);}
                    }else if(sceneCrossing&&age<45&&ticks%4==0)move(.3,0);
                }
                if(returnDelay>0&&--returnDelay==0)spawnFigure(px,py,pz,facing);
                if(sceneRequest){sceneRequest=false;returns=sceneReturns;returnDelay=0;placement=0;spawnFigure(px,py,pz,facing);}
            }catch(Throwable error){
                if(error instanceof ThreadDeath)throw (ThreadDeath)error;if(error instanceof VirtualMachineError)throw (VirtualMachineError)error;
                try{cancel();}catch(Exception ignored){}disabled=true;
                System.err.println("[Alpha scene] disabled after "+error.getClass().getSimpleName()+": "+error.getMessage());
            }
        }
        static void cancel()throws Exception{AmbientHooksAnomalies.cancel(false);anomalyRequest=-1;returns=0;returnDelay=0;sceneRequest=false;encounterRequest=-1;remove();director.reset();trailCount=0;trailCursor=0;loggedPhase=director.phase();cues.clear();}
        static void tickDirector()throws Exception{
            input.figurePresent=figure!=null;input.distance=figure==null?100:Math.hypot(fx-px,fz-pz);
            input.observed=figure!=null&&observed();input.visible=figure!=null&&rayVisible;
            input.nearAnchor=Math.hypot(anchorX-px,anchorZ-pz)<8;input.moving=moving;
            double turn=Math.abs(Math.IEEEremainder(facing-lastFacing,360));lastFacing=facing;input.turned=turn>25;
            AmbientHooksDirector.Action action=director.tick(input);
            if(action.phase!=loggedPhase){System.out.println("[Alpha director] phase="+action.phase+" mode="+director.mode());loggedPhase=action.phase;}
            if(action.remove)remove();
            if(action.cue){
                if(director.mode()==AmbientHooksDirector.Mode.LOST_FOOTSTEPS){
                    double[] foot=trail[(trailCursor-trailCount+trail.length)%trail.length];
                    queueCue(groundMaterial==5||groundMaterial==17?"step.wood":groundMaterial==12?"step.sand":groundMaterial==13?"step.gravel":groundMaterial==2?"step.grass":"step.stone",foot[0],foot[1],foot[2]);
                }else queueCue(action.phase==AmbientHooksDirector.Phase.AFTERMATH?"step.gravel":"step.wood",anchorX,anchorY,anchorZ);
                if(director.mode()==AmbientHooksDirector.Mode.REPEATER){textUntil=activeTicks+35;driftUntil=activeTicks+240;}
            }
            if(action.spawn){
                double angle=director.mode()==AmbientHooksDirector.Mode.INTERCEPT?0:director.mode()==AmbientHooksDirector.Mode.PASS_BY?-42:director.mode()==AmbientHooksDirector.Mode.TURN_AROUND?155:28;
                if(director.mode()==AmbientHooksDirector.Mode.NULL_LURK)angle=55;
                if(director.mode()==AmbientHooksDirector.Mode.LOST_FOOTSTEPS)angle=-32;
                if(action.relocate)angle=placement++%2==0?-55:40;
                spawnAt(angle,scenePreview?21:27,scenePreview?260:400);
            }
            if(action.step&&figure!=null&&ticks%4==0){
                double dx=px-fx,dz=pz-fz,len=Math.hypot(dx,dz);
                if(len>0){double amount=action.speed*4;double mx=dx/len*amount,mz=dz/len*amount;if(action.sideways){double swap=mx;mx=-mz;mz=swap;}
                    if(!move(mx,mz)){remove();System.out.println("[Alpha director] movement blocked; withdrew without editing terrain");}
                }
            }
            if(!director.active())quietTicks=Math.max(quietTicks,300);
        }
        static boolean observed()throws Exception{
            if(ticks%4==0)rayVisible=lineClear(px,py+1.62,pz,fx,fy+1,fz);
            double dx=fx-px,dy=fy+1-(py+1.62),dz=fz-pz,length=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(length==0)return true;
            double yRad=Math.toRadians(facing),pRad=Math.toRadians(lookingPitch);
            double dot=(-Math.sin(yRad)*Math.cos(pRad)*dx-Math.sin(pRad)*dy+Math.cos(yRad)*Math.cos(pRad)*dz)/length;
            return rayVisible&&dot>.965;
        }
        static boolean lineClear(double ax,double ay,double az,double bx,double by,double bz)throws Exception{
            int vx=(int)Math.floor(ax),vy=(int)Math.floor(ay),vz=(int)Math.floor(az);
            int ex=(int)Math.floor(bx),ey=(int)Math.floor(by),ez=(int)Math.floor(bz);
            double dx=bx-ax,dy=by-ay,dz=bz-az;
            int sx=dx>0?1:-1,sy=dy>0?1:-1,sz=dz>0?1:-1;
            double tx=dx==0?Double.POSITIVE_INFINITY:((dx>0?vx+1:vx)-ax)/dx;
            double ty=dy==0?Double.POSITIVE_INFINITY:((dy>0?vy+1:vy)-ay)/dy;
            double tz=dz==0?Double.POSITIVE_INFINITY:((dz>0?vz+1:vz)-az)/dz;
            for(int budget=0;budget<256;budget++){
                if(vx==ex&&vy==ey&&vz==ez){
                    Object start=vector.invoke(null,ax,ay,az),end=vector.invoke(null,bx,by,bz);
                    return raycast.invoke(world,start,end)==null;
                }
                for(int ox:new int[]{-1,1})for(int oz:new int[]{-1,1})if(!((Boolean)loaded.invoke(world,(vx+ox)>>4,(vz+oz)>>4)))return false;
                double t=Math.min(tx,Math.min(ty,tz));
                if(tx==t){vx+=sx;tx+=Math.abs(1/dx);}
                if(ty==t){vy+=sy;ty+=Math.abs(1/dy);}
                if(tz==t){vz+=sz;tz+=Math.abs(1/dz);}
                int id=read(vx,vy,vz);
                if(id!=0&&id!=8&&id!=9&&id!=20&&id!=37&&id!=38&&id!=39&&id!=40&&id!=50&&id!=78)return false;
            }return false; // Unloaded or over-budget paths fail closed.
        }
        static int read(int bx,int by,int bz)throws Exception{
            if(by<0||by>127||!((Boolean)loaded.invoke(world,bx>>4,bz>>4)))return -1;
            return (Integer)block.invoke(world,bx,by,bz);
        }
        static boolean ground(int id){return id==1||id==2||id==3||id==12||id==13||id==80;}
        static double standY(double bx,double bz,double around)throws Exception{
            int ix=(int)Math.floor(bx),iz=(int)Math.floor(bz);
            for(int by=Math.min(124,(int)around+2);by>=Math.max(1,(int)around-3);by--){
                if(!ground(read(ix,by,iz)))continue;
                boolean clear=true;
                for(double ox:new double[]{-.31,.31})for(double oz:new double[]{-.31,.31}){
                    int cx=(int)Math.floor(bx+ox),cz=(int)Math.floor(bz+oz);
                    if(!ground(read(cx,by,cz))||read(cx,by+1,cz)!=0||read(cx,by+2,cz)!=0)clear=false;
                }
                if(clear)return by+1;
            }return Double.NaN;
        }
        static boolean move(double mx,double mz)throws Exception{
            double nx=fx+mx,nz=fz+mz;if(Math.hypot(nx-px,nz-pz)<14)return false;
            double ny=standY(nx,nz,fy-1);if(Double.isNaN(ny)||Math.abs(ny-fy)>1)return false;
            fx=nx;fy=ny;fz=nz;float rotation=(float)Math.toDegrees(Math.atan2(-(px-fx),pz-fz));
            position.invoke(figure,fx,fy,fz,rotation,0f);return true;
        }
        // A deliberately oversized view cone protects the entire normal Alpha frustum,
        // including the model's extremities. Transparent/unknown blocks never hide it.
        static boolean protectedAt(double bx,double by,double bz)throws Exception{
            if(!AmbientHooksVisibility.available(px,py,pz))return true;
            boolean inFrustum=AmbientHooksVisibility.intersects(bx-.5,by,bz-.5,bx+.5,by+2,bz+.5);
            // Every mutation checks the ray as well as the renderer snapshot; conservatively
            // disallow anything in-frustum even when occluded, including transparent glass.
            lineClear(px,py+1.62,pz,bx,by+1,bz);
            if(inFrustum)return true;
            for(double ox:new double[]{-.5,.5})for(double oy:new double[]{0,1,2})for(double oz:new double[]{-.5,.5}){
                double dx=bx+ox-px,dy=by+oy-(py+1.62),dz=bz+oz-pz;
                double len=Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<2)return true;
                double yr=Math.toRadians(facing),pr=Math.toRadians(lookingPitch);
                double dot=(-Math.sin(yr)*Math.cos(pr)*dx-Math.sin(pr)*dy+Math.cos(yr)*Math.cos(pr)*dz)/len;
                // No occlusion exemption yet: conservatively protect through walls too.
                if(dot>-.25)return true;
            }return false;
        }
        static void forceRemove()throws Exception{if(figure!=null){dead.invoke(figure);figure=null;}pendingRemoval=false;rayVisible=false;}
        static void remove()throws Exception{
            if(figure!=null&&protectedAt(fx,fy,fz)){pendingRemoval=true;return;}
            forceRemove();
        }
        static void spawnFigure(double px,double py,double pz,float facing)throws Exception{
            spawnAt(lastEvent.equals("BEHIND_YOU")?155:(placement++%2==0?28:-38),scenePreview?20:27,scenePreview?Math.max(160,sceneLifetime):sceneLifetime);
        }
        static boolean treeNear(double bx,double bz,double by)throws Exception{
            int y=(int)Math.floor(by);
            for(int dx:new int[]{-3,0,3})for(int dz:new int[]{-3,0,3})for(int dy=0;dy<=2;dy++)if(read((int)Math.floor(bx)+dx,y+dy,(int)Math.floor(bz)+dz)==17)return true;
            return false;
        }
        static void spawnAt(double relative,double distance,int lifetime)throws Exception{
            remove();if(figure!=null)return;
            for(int offset:new int[]{0,12,-12,25,-25,40,-40,150,-150,180}){
                double angle=Math.toRadians(facing+relative+offset);double bx=Math.floor(px-Math.sin(angle)*distance)+.5,bz=Math.floor(pz+Math.cos(angle)*distance)+.5;
                if(director.active()&&director.mode()==AmbientHooksDirector.Mode.TREE_LINE&&!treeNear(bx,bz,py))continue;
                double by=Double.NaN;
                for(int band:new int[]{0,4,-4,8,-8}){by=standY(bx,bz,py-1+band);if(!Double.isNaN(by))break;}
                if(Double.isNaN(by)||protectedAt(bx,by,bz))continue;
                Object candidate=Class.forName("AlphaWatcher",true,world.getClass().getClassLoader()).getConstructor(Class.forName("cj",false,world.getClass().getClassLoader())).newInstance(world);
                fx=bx;fy=by;fz=bz;position.invoke(candidate,fx,fy,fz,(float)Math.toDegrees(Math.atan2(-(px-fx),pz-fz)),0f);
                if((Boolean)spawn.invoke(world,candidate)){figure=candidate;age=0;seen=false;life=lifetime;rayVisible=lineClear(px,py+1.62,pz,fx,fy+1,fz);System.out.println("[Alpha scene] watcher spawned; preview="+scenePreview+" director="+director.active());return;}
            }
            System.out.println("[Alpha scene] skipped: bounded placement search found no suitable loaded ground");
        }
    }
    private static final class Pending {
        final byte[] blocks;final int x,z,kind;int attempts;
        Pending(byte[] blocks,int x,int z,int kind){this.blocks=blocks;this.x=x;this.z=z;this.kind=kind;}
    }
    private AmbientHooks(){}
    public static float fog(int parameter,float original){
        if(parameter==0x0B64&&original>0&&!Float.isNaN(original)&&!Float.isInfinite(original))lastFogEnd=original;
        if(System.nanoTime()<previewFogUntil&&(parameter==0x0B63||parameter==0x0B64))return original*.88f;
        float base=fogAt(parameter,original,activeTicks/20.0);
        if(parameter==0x0B63||parameter==0x0B64)base*=AmbientHooksAnomalies.fogFactor();
        if(activeTicks<driftUntil&&(parameter==0x0B63||parameter==0x0B64)){
            double remaining=(driftUntil-activeTicks)/240.0;
            return (float)(base*(1-.10*Math.sin(Math.PI*remaining)));
        }
        return base;
    }
    public static float fogAt(int parameter,float original,double seconds){
        if((parameter!=0x0B63&&parameter!=0x0B64)||original<=0||Float.isNaN(original)||Float.isInfinite(original))return original;
        double envelope=Math.min(1,Math.max(0,seconds/180));
        double drift=.018+.012*Math.sin(seconds*.009)+.006*Math.sin(seconds*.0031);
        return (float)(original*(1-envelope*drift));
    }
    public static synchronized void register(Object chunk,byte[] blocks,int x,int z){
        if(blocks==null||blocks.length!=32768||Math.max(Math.abs((long)x),Math.abs((long)z))<4)return;
        long hash=mix(x,z);
        if((hash&127)!=0)return; // Roughly one eligible chunk in 128, not every visit.
        pending.put(chunk,new Pending(blocks,x,z,(int)((hash>>>9)%4)));
    }
    public static synchronized void ready(Object chunk){
        pending.remove(chunk); // Terrain edits disabled until visibility-aware transaction support.
    }
    static long mix(int x,int z){
        long n=x*341873128712L+z*132897987541L+0x5b7c913dL;
        n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;
        return (n^(n>>>31))&Long.MAX_VALUE;
    }
    private static int index(int x,int y,int z){return (x<<11)|(z<<7)|y;}
    private static int get(byte[] b,int x,int y,int z){return b[index(x,y,z)]&255;}
    private static void set(byte[] b,int x,int y,int z,int value){b[index(x,y,z)]=(byte)value;}
    private static int ground(byte[] b,int x,int z){
        for(int y=124;y>5;y--){int id=get(b,x,y,z);if(id!=0&&id!=18&&id!=17)return y;}return 0;
    }
    /** Only original numeric block IDs. Never touches bedrock or manufactured blocks. */
    public static boolean apply(byte[] blocks,int chunkX,int chunkZ,int kind){
        if(blocks==null||blocks.length!=32768||kind<0||kind>3)return false;
        // Refuse the whole chunk near common player construction, storage or lighting.
        for(byte value:blocks)if(!natural(value&255))return false;
        if(kind==0)return corridor(blocks);
        if(kind==1)return false; // Never strip leaves from existing or newly loaded trees.
        if(kind==2)return sandSteps(blocks);
        return seam(blocks);
    }
    private static boolean natural(int block){
        switch(block){
            case 0:case 1:case 2:case 3:case 7:case 8:case 9:case 10:case 11:
            case 12:case 13:case 14:case 15:case 16:case 17:case 18:
            case 37:case 38:case 39:case 40:case 56:case 73:case 74:
            case 78:case 79:case 80:case 81:case 82:case 83:case 86:
            case 87:case 88:case 89:return true;
            default:return false;
        }
    }
    private static boolean corridor(byte[] b){
        int y=ground(b,8,8)-8;if(y<9||y>112)return false;
        for(int x=2;x<=13;x++)for(int z=7;z<=8;z++)for(int dy=0;dy<2;dy++){
            int id=get(b,x,y+dy,z);if(id!=1&&id!=3)return false;
        }
        for(int x=2;x<=13;x++)for(int z=7;z<=8;z++)for(int dy=0;dy<2;dy++)set(b,x,y+dy,z,0);
        return true;
    }
    private static boolean sandSteps(byte[] b){
        int y=ground(b,8,8);if(y<6||y>119)return false;
        for(int x=6;x<=10;x++)for(int z=6;z<=10;z++){
            if(get(b,x,y,z)!=12)return false;
            for(int dy=1;dy<=3;dy++)if(get(b,x,y+dy,z)!=0)return false;
        }
        for(int layer=0;layer<3;layer++)for(int x=6+layer;x<=10-layer;x++)for(int z=6+layer;z<=10-layer;z++)set(b,x,y+layer+1,z,12);
        return true;
    }
    private static boolean seam(byte[] b){
        int y=ground(b,8,8)-5;if(y<9||y>114)return false;
        for(int x=4;x<=11;x++)if(get(b,x,y,8)!=1||get(b,x,y+1,8)!=1)return false;
        // A narrow, unnaturally level gravel seam, not a symbol or written message.
        for(int x=4;x<=11;x++)set(b,x,y,8,13);return true;
    }
}
