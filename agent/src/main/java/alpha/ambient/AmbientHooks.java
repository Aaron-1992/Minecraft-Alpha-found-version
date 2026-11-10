package alpha.ambient;

import java.util.Map;
import java.util.WeakHashMap;

/** Java 8 ambience hooks. Original game model/audio only; no network or file access. */
public final class AmbientHooks {
    private static final Map<Object,Pending> pending=new WeakHashMap<Object,Pending>();
    private static final java.util.Random atmosphere=new java.util.Random();
    private static int quietTicks=20*(120+atmosphere.nextInt(121));
    private static long activeTicks;
    private static boolean keyHeld,forceEvent,particle;
    private static int events,previewCursor,echoes,echoDelay;
    private static String sequenceSound="step.stone";
    private static double audioAngle=135,audioRadius=6;
    private static int sceneReturns,sceneLifetime=65;
    private static boolean orbitSound;
    private static boolean sceneRequest,sceneCrossing,scenePreview;
    private static long driftUntil,textUntil;
    private static String lastEvent="NONE";
    private static final String[] EVENT_NAMES={"WATCHER","CROSSING","FOOTSTEP_ECHO","FALSE_BLOCK","SMOKE","FOG_DRIFT","HUD_DRIFT","SECOND_LOOK","ORBIT_STEPS","DISTANT_KNOCKS","FALSE_DIGGING","LONG_WATCH","BEHIND_YOU","AFTERIMAGE"};
    private static long previewFogUntil;
    public static synchronized void previewKey(boolean down){
        if(down&&!keyHeld){forceEvent=true;}
        keyHeld=down;
    }
    static int intervalSeconds(long ticks){
        if(ticks>=20L*60*35)return 30+atmosphere.nextInt(61);
        if(ticks>=20L*60*15)return 60+atmosphere.nextInt(91);
        return 120+atmosphere.nextInt(121);
    }
    /** Active game ticks only; F6 skips the wait, not terrain placement checks. */
    public static synchronized String nextSound(boolean active){
        if(!active){keyHeld=false;textUntil=0;return null;}
        activeTicks++;
        if(!Boolean.parseBoolean(System.getProperty("alpha.scheduledSounds","true")))return null;
        if(!forceEvent&&echoes>0&&!Scene.moving&&--echoDelay<=0){
            echoes--;echoDelay=10+atmosphere.nextInt(17);
            if(orbitSound)audioAngle+=65;
            System.out.println("[Alpha ambience] sequence="+lastEvent+" remaining="+echoes+" soundRequest="+sequenceSound);return sequenceSound;
        }
        if(!forceEvent&&--quietTicks>0)return null;
        boolean preview=forceEvent;forceEvent=false;quietTicks=20*intervalSeconds(activeTicks);
        echoes=0;
        int kind=preview?(previewCursor++%EVENT_NAMES.length):atmosphere.nextInt(EVENT_NAMES.length);
        lastEvent=EVENT_NAMES[kind];
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
        String sound=sounds[kind];
        System.out.println("[Alpha ambience] tick="+activeTicks+" event="+events+" type="+EVENT_NAMES[kind]+" preview="+preview+" soundRequest="+sound);
        return sound;
    }
    static int eventCount(){return EVENT_NAMES.length;}
    static String lastEventName(){return lastEvent;}
    public static int textOffset(int coordinate){return activeTicks<textUntil?coordinate+((activeTicks/40)%2==0?1:2):coordinate;}
    public static synchronized boolean takeParticle(){boolean result=particle;particle=false;return result;}
    public static double soundX(float yaw){return -Math.sin(Math.toRadians(yaw+audioAngle))*audioRadius;}
    public static double soundZ(float yaw){return Math.cos(Math.toRadians(yaw+audioAngle))*audioRadius;}
    public static double visualX(float yaw){return -Math.sin(Math.toRadians(yaw+32))*9;}
    public static double visualZ(float yaw){return Math.cos(Math.toRadians(yaw+32))*9;}
    /** Runtime-only scene controller; reflection is restricted to inspected a1.0.16 members. */
    public static void scene(Object world,Object player){Scene.tick(world,player);}
    public static boolean isWatcher(Object value){return value!=null&&value.getClass().getName().equals("AlphaWatcher");}
    private static final class Scene {
        static Object world,player,figure;
        static java.lang.reflect.Method block,loaded,spawn,position,dead;
        static java.lang.reflect.Field x,y,z,yaw;
        static boolean disabled,seen,moving;
        static int age,life,returns,returnDelay,placement;
        static double fx,fy,fz,previousX,previousZ;
        static void tick(Object nextWorld,Object nextPlayer){
            if(disabled)return;
            try{
                if(nextWorld==null||nextPlayer==null){returns=0;returnDelay=0;remove();world=null;player=null;return;}
                if(world!=nextWorld){returns=0;returnDelay=0;remove();world=nextWorld;player=nextPlayer;
                    ClassLoader loader=world.getClass().getClassLoader();Class<?> wc=Class.forName("cj",false,loader),entity=Class.forName("jx",false,loader);
                    block=wc.getMethod("a",int.class,int.class,int.class);loaded=wc.getDeclaredMethod("h",int.class,int.class);loaded.setAccessible(true);spawn=wc.getMethod("a",entity);
                    position=entity.getMethod("b",double.class,double.class,double.class,float.class,float.class);dead=entity.getMethod("D");
                    x=entity.getField("ah");y=entity.getField("ai");z=entity.getField("aj");yaw=entity.getField("an");previousX=x.getDouble(player);previousZ=z.getDouble(player);
                }
                player=nextPlayer;double px=x.getDouble(player),py=y.getDouble(player),pz=z.getDouble(player);float facing=yaw.getFloat(player);
                moving=Math.hypot(px-previousX,pz-previousZ)>.025;previousX=px;previousZ=pz;
                if(figure!=null){
                    age++;double dx=fx-px,dz=fz-pz,distance=Math.hypot(dx,dz);
                    double dot=distance==0?1:(-Math.sin(Math.toRadians(facing))*dx+Math.cos(Math.toRadians(facing))*dz)/distance;
                    if(dot>.94)seen=true;
                    if(age>life||distance<12||(seen&&age>20&&dot<.25)){
                        boolean canReturn=distance>=12&&returns>0;remove();
                        if(canReturn){returns--;returnDelay=45+atmosphere.nextInt(36);}
                    }
                    else if(sceneCrossing&&age<45){fx+=.075;position.invoke(figure,fx,fy,fz,(float)(facing+180),0f);}
                }
                if(returnDelay>0&&--returnDelay==0)spawnFigure(px,py,pz,facing);
                if(sceneRequest){sceneRequest=false;returns=sceneReturns;returnDelay=0;placement=0;spawnFigure(px,py,pz,facing);}
            }catch(Throwable error){
                if(error instanceof ThreadDeath)throw (ThreadDeath)error;if(error instanceof VirtualMachineError)throw (VirtualMachineError)error;
                try{remove();}catch(Exception ignored){}disabled=true;
                System.err.println("[Alpha scene] disabled after "+error.getClass().getSimpleName()+": "+error.getMessage());
            }
        }
        static void remove()throws Exception{if(figure!=null){dead.invoke(figure);figure=null;System.out.println("[Alpha scene] watcher removed");}}
        static void spawnFigure(double px,double py,double pz,float facing)throws Exception{
            remove();double angle=Math.toRadians(facing+(lastEvent.equals("BEHIND_YOU")?155:(placement++%2==0?28:-38))),distance=scenePreview?20:27;
            int bx=(int)Math.floor(px-Math.sin(angle)*distance),bz=(int)Math.floor(pz+Math.cos(angle)*distance);
            if(!((Boolean)loaded.invoke(world,bx>>4,bz>>4))){System.out.println("[Alpha scene] skipped: target chunk not loaded");return;}
            for(int by=Math.min(124,(int)py+8);by>=Math.max(2,(int)py-14);by--){
                int base=(Integer)block.invoke(world,bx,by,bz);
                if(base!=1&&base!=2&&base!=3&&base!=12&&base!=13&&base!=80)continue;
                if((Integer)block.invoke(world,bx,by+1,bz)!=0||(Integer)block.invoke(world,bx,by+2,bz)!=0)continue;
                Object candidate=Class.forName("AlphaWatcher",true,world.getClass().getClassLoader()).getConstructor(Class.forName("cj",false,world.getClass().getClassLoader())).newInstance(world);
                fx=bx+.5;fy=by+1;fz=bz+.5;position.invoke(candidate,fx,fy,fz,(float)(facing+180),0f);
                if((Boolean)spawn.invoke(world,candidate)){figure=candidate;age=0;seen=false;life=scenePreview?Math.max(160,sceneLifetime):sceneLifetime;System.out.println("[Alpha scene] watcher spawned; crossing="+sceneCrossing+" preview="+scenePreview);}
                return;
            }
            System.out.println("[Alpha scene] skipped: no suitable ground (no blocks edited)");
        }
    }
    private static final class Pending {
        final byte[] blocks;final int x,z,kind;int attempts;
        Pending(byte[] blocks,int x,int z,int kind){this.blocks=blocks;this.x=x;this.z=z;this.kind=kind;}
    }
    private AmbientHooks(){}
    public static float fog(int parameter,float original){
        if(System.nanoTime()<previewFogUntil&&(parameter==0x0B63||parameter==0x0B64))return original*.88f;
        float base=fogAt(parameter,original,activeTicks/20.0);
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
        Pending p=pending.get(chunk);if(p==null)return;
        if(++p.attempts>8){pending.remove(chunk);return;}
        int natural=0;
        for(int i=0;i<p.blocks.length;i++)if((p.blocks[i]&255)==1)natural++;
        if(natural<128)return; // Constructors can run before terrain generation fills the array.
        pending.remove(chunk); // At most one attempt on this chunk object; no tick-time world editing.
        apply(p.blocks,p.x,p.z,p.kind);
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
