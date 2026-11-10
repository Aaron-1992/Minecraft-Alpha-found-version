package alpha.ambient;

import java.lang.reflect.*;
import java.util.*;

/** Context-dependent Alpha anomalies. All reflection/world mutations run on the game tick. */
public final class AmbientHooksAnomalies {
    public enum Event {
        TOOL_DRIFT(1,4), CHEST_SHUFFLE(1,3), PHANTOM_STEP(1,5), DOOR_MIMIC(1,4), CROP_RESET(1,2),
        PAINTING_SWAP(1,2), WATER_RIPPLE(1,3), FOG_BREATH(1,5), PHANTOM_TORCH(1,4),
        HELPFUL_FURNACE(2,2), EXTINGUISHED_TRAIL(2,2), CROSS_TUNNEL(2,1), MIMIC_MINING(2,5),
        ANIMAL_STARE(2,2), STAIRCASE(2,1), BOAT_TURN(2,2), CHEST_GIFT(2,2), SIGN_ERASURE(2,1),
        BREATHING_CAVE(3,3), DOOR_BLOCKADE(3,1), INVERTED_STEPS(3,4), EMPTY_CAVEIN(3,3), SUN_STUTTER(3,1),
        SHADOW_REPLAY(4,2), MUSIC_WARP(4,2), SYNC_STEPS(4,4), SILENT_WORLD(4,2),
        DELAYED_DROP(1,2), FOG_WATCHER(2,2), BASE_REPLICA(3,1), FALSE_DAYLIGHT(3,2),
        IMPOSSIBLE_WATER(3,1), INVENTORY_GHOST(3,2), TREE_LEAF_GEOMETRY(3,1),
        INFINITE_LOOP(4,1), BASE_EVACUATION(4,2), FALSE_HORIZON(4,1), WINDOW_WATCHER(4,2), CHUNK_SCAR(4,1), MIRROR_ROOM(4,1);
        public final int tier,weight;
        Event(int tier,int weight){this.tier=tier;this.weight=weight;}
    }
    private static Event event;
    private static float loopFacing;
    private static int effectAge;
    private static int age,stillTicks,lastSlot=-1,doorAge=1000;
    private static boolean previousMoving,started;
    private static double lastY,sourceX,sourceY,sourceZ;
    private static int shadowAge,daylightAge=-1,loops,lastLoopTick,evacuationTicks;
    private static Object delayedDrop;
    private static int dropWait;
    private static boolean ghostArmed;
    private static int[] horizon,windowGlass;
    private static int horizonAge;
    private static final IdentityHashMap<Object,float[]> evacuees=new IdentityHashMap<Object,float[]>();
    private static volatile int fogAge=-1,sunAge=-1;
    private static volatile float heldSun=Float.NaN;
    private static Object soundSystem;
    private static float originalMaster=1;
    private static boolean audioChanged,musicChanged,musicWasPlaying;
    private static final double[][] history=new double[301][12];
    private static int historyCursor,historyCount;
    private static final IdentityHashMap<Object,float[]> animals=new IdentityHashMap<Object,float[]>();
    private static int animalTicks;
    private static Object shadow;
    private static boolean shadowRetiring;
    private static final ArrayDeque<Delayed> delayed=new ArrayDeque<Delayed>();
    private static final class Delayed {final String sound;final double x,y,z;int ticks;Delayed(String s,double x,double y,double z,int ticks){sound=s;this.x=x;this.y=y;this.z=z;this.ticks=ticks;}}
    static void begin(int index,boolean preview){
        cancel(false);event=Event.values()[index];age=0;started=false;doorAge=1000;loops=0;lastLoopTick=-1000;effectAge=0;
        double r=Math.toRadians(AmbientHooks.Scene.facing+180);
        sourceX=AmbientHooks.Scene.px-Math.sin(r)*16;sourceY=AmbientHooks.Scene.py;sourceZ=AmbientHooks.Scene.pz+Math.cos(r)*16;
        AmbientHooksLog.debug("[Alpha runtime] armed="+event+" preview="+preview);
    }
    static boolean active(){return event!=null||shadow!=null||animalTicks>0||fogAge>=0||sunAge>=0||daylightAge>=0||delayedDrop!=null||horizon!=null||evacuationTicks>0;}
    static void cancel(boolean leavingWorld){
        event=null;age=0;fogAge=-1;sunAge=-1;daylightAge=-1;ghostArmed=false;heldSun=Float.NaN;delayed.clear();restoreAudio();
        animalTicks=0;restoreAnimals();evacuationTicks=0;restoreEvacuees();
        if(leavingWorld){releaseDrop();if(horizon!=null)try{if(AmbientHooks.Scene.read(horizon[0],horizon[1],horizon[2])==50)rawEdit(horizon[0],horizon[1],horizon[2],0);}catch(Exception ignored){}horizon=null;}
        if(shadow!=null){shadowRetiring=true;if(leavingWorld)removeShadow();}
        if(leavingWorld){historyCursor=0;historyCount=0;lastSlot=-1;stillTicks=0;previousMoving=false;AmbientHooksVisibility.clear();}
    }
    static void tick()throws Exception{
        boolean moving=AmbientHooks.Scene.moving;
        stillTicks=moving?0:Math.min(10000,stillTicks+1);doorAge=Math.min(1000,doorAge+1);
        for(Iterator<Delayed> it=delayed.iterator();it.hasNext();){Delayed d=it.next();if(--d.ticks<=0){emit(d.sound,d.x,d.y,d.z,.12f,.95f);it.remove();}}
        record();
        if(AmbientHooks.Scene.ticks%200==0)learnBase();
        if(delayedDrop!=null){dropWait++;if(dropWait>=30&&hidden(number(delayedDrop,"ah"),number(delayedDrop,"ai"),number(delayedDrop,"aj")))releaseDrop();}
        if(daylightAge>=0&&++daylightAge>600)daylightAge=-1;
        if(evacuationTicks>0){evacuationTicks--;if(evacuationTicks==0)restoreEvacuees();else evacuate();}
        if(horizon!=null&&(++horizonAge>400||Math.hypot(horizon[0]-AmbientHooks.Scene.px,horizon[2]-AmbientHooks.Scene.pz)<24)&&hidden(horizon[0]+.5,horizon[1],horizon[2]+.5)){
            if(AmbientHooks.Scene.read(horizon[0],horizon[1],horizon[2])==50)rawEdit(horizon[0],horizon[1],horizon[2],0);horizon=null;
        }
        if(shadow!=null)tickShadow();
        if(animalTicks>0){animalTicks--;if(animalTicks==0)restoreAnimals();else faceAnimals();}
        if(fogAge>=0&&++fogAge>160)fogAge=-1;
        if(sunAge>=0&&++sunAge>1840){sunAge=-1;heldSun=Float.NaN;}
        if(event!=null){
            age++;
            boolean done=run(moving);
            if(done||age>1200){AmbientHooksLog.debug("[Alpha runtime] "+(started?"completed=":"skipped:no suitable context=")+event);event=null;restoreAudio();}
        }
        previousMoving=moving;lastY=AmbientHooks.Scene.py;
        Object inv=inventory();if(inv!=null)lastSlot=integer(inv,"d");
    }
    private static boolean run(boolean moving)throws Exception{
        switch(event){
            case PHANTOM_STEP:
                if(previousMoving&&!moving){behind("step.gravel",8,.12f,.95f);started=true;return true;}break;
            case DOOR_MIMIC:
                if(doorAge==8){emit("random.door_open",sourceX,sourceY,sourceZ,.12f,.95f);started=true;return true;}break;
            case FOG_BREATH:
                if(stillTicks>=300){fogAge=0;started=true;return true;}break;
            case TOOL_DRIFT:
                Object inv=inventory();if(inv!=null&&lastSlot>=0&&lastSlot!=integer(inv,"d")){
                    Object stack=call(inv,"e",new Class<?>[0]);if(stack!=null){int id=integer(stack,"c"),damage=integer(stack,"d");
                        if(id==257||id==270||id==274||id==278||id==285){int max=(Integer)call(stack,"d",new Class<?>[0]);int change=(age%2==0?2:-3);
                            set(stack,"d",Math.max(0,Math.min(max-1,damage+change)));started=true;return true;}}
                }break;
            case CHEST_SHUFFLE: case CHEST_GIFT: case HELPFUL_FURNACE: case SIGN_ERASURE:
                if(age%20==0&&tamperTile()){started=true;return true;}break;
            case CROP_RESET:
                if(age%20==0&&resetCrop()){started=true;return true;}break;
            case PAINTING_SWAP:
                if(age%20==0&&painting()){started=true;return true;}break;
            case WATER_RIPPLE:
                if(age%20==0&&water()){started=true;return true;}break;
            case PHANTOM_TORCH:
                if(AmbientHooks.Scene.sheltered){behind("step.wood",9,.10f,.95f);started=true;return true;}break;
            case EXTINGUISHED_TRAIL:
                if(age%20==0&&extinguish()){started=true;return true;}break;
            case CROSS_TUNNEL:case STAIRCASE:
                if(age%40==0&&structure()){started=true;return true;}break;
            case MIMIC_MINING:
                if(swinging()&&miningOre()&&age%8==0){behind("step.stone",12,.12f,.90f);started=true;}
                if(age>400)return true;break;
            case ANIMAL_STARE:
                if(age%20==0&&startAnimals()){started=true;return true;}break;
            case BOAT_TURN:
                if(age%20==0&&boat()){started=true;return true;}break;
            case BREATHING_CAVE:
                if(AmbientHooks.Scene.py<48&&AmbientHooks.Scene.sheltered){behind("mob.cow",18,.09f,.5f);started=true;return true;}break;
            case DOOR_BLOCKADE:
                if(age%20==0&&blockDoor()){started=true;return true;}break;
            case INVERTED_STEPS:
                if(moving&&ground()==12&&age%10==0){emit("random.splash",AmbientHooks.Scene.px,AmbientHooks.Scene.py,AmbientHooks.Scene.pz,.08f,.9f);started=true;}
                else if(AmbientHooks.Scene.py-lastY>.08&&age%5==0){emit("step.stone",AmbientHooks.Scene.px,AmbientHooks.Scene.py,AmbientHooks.Scene.pz,.10f,.95f);started=true;}
                if(age>120)return true;break;
            case EMPTY_CAVEIN:
                if(AmbientHooks.Scene.sheltered){effectAge++;if(effectAge==1||effectAge==8||effectAge==15)behind("step.gravel",6,.10f,.8f);started=true;return effectAge>20;}break;
            case SUN_STUTTER:
                if(!AmbientHooks.Scene.sheltered){sunAge=0;heldSun=Float.NaN;started=true;return true;}break;
            case SHADOW_REPLAY:
                if(historyCount==history.length&&shadow==null&&age%20==0&&spawnShadow()){started=true;return true;}break;
            case MUSIC_WARP:
                if(!bindAudio())return age>40;
                if(age==1){musicWasPlaying=(Boolean)call(soundSystem,"playing",new Class<?>[]{String.class},"BgMusic");if(!musicWasPlaying)return true;}
                musicChanged=true;call(soundSystem,"setPitch",new Class<?>[]{String.class,float.class},"BgMusic",Math.max(.5f,1-age/1200f));
                if(age==600)call(soundSystem,"pause",new Class<?>[]{String.class},"BgMusic");started=true;return age>660;
            case SYNC_STEPS:
                if(moving&&age%10==0&&delayed.size()<8){double r=Math.toRadians(AmbientHooks.Scene.facing+90);delayed.add(new Delayed(step(),AmbientHooks.Scene.px-Math.sin(r)*1.5,AmbientHooks.Scene.py,AmbientHooks.Scene.pz+Math.cos(r)*1.5,3));started=true;}
                return age>400;
            case SILENT_WORLD:
                if(!bindAudio())return age>40;
                if(!audioChanged){originalMaster=(Float)call(soundSystem,"getMasterVolume",new Class<?>[0]);audioChanged=true;}
                float envelope=age<40?1-age/40f:age>640?(age-640)/40f:0;
                call(soundSystem,"setMasterVolume",new Class<?>[]{float.class},Math.max(0,Math.min(originalMaster,originalMaster*envelope)));started=true;return age>=680;
            case DELAYED_DROP:
                if(delayedDrop!=null){started=true;return true;}break;
            case FOG_WATCHER:case WINDOW_WATCHER:
                if(!started&&age%20==0){
                    if(event==Event.WINDOW_WATCHER){if(!night())break;windowGlass=findBlock(20);if(windowGlass==null)break;}
                    AmbientHooks.Scene.spawnAt(180,event==Event.WINDOW_WATCHER?15:AmbientHooks.fogEdge(),600);
                    if(AmbientHooks.Scene.figure!=null){
                        if(event==Event.FOG_WATCHER){set(AmbientHooks.Scene.figure,"alpha$plain",true);set(AmbientHooks.Scene.figure,"u","/mob/char.png");}
                        started=true;sourceX=AmbientHooks.Scene.px;sourceZ=AmbientHooks.Scene.pz;
                    }
                }
                if(started){
                    if(AmbientHooks.Scene.figure==null)return true;
                    if(event==Event.FOG_WATCHER&&AmbientHooks.Scene.observed()&&age%4==0)AmbientHooks.Scene.move(.2,0);
                    if(age>240||event==Event.WINDOW_WATCHER&&(Math.hypot(sourceX-AmbientHooks.Scene.px,sourceZ-AmbientHooks.Scene.pz)>3||windowGlass!=null&&AmbientHooks.Scene.read(windowGlass[0],windowGlass[1],windowGlass[2])!=20)){AmbientHooks.Scene.remove();return AmbientHooks.Scene.figure==null;}
                }break;
            case FALSE_DAYLIGHT:
                if(!AmbientHooks.Scene.sheltered){daylightAge=0;started=true;return true;}break;
            case INVENTORY_GHOST:
                if(ghostArmed){started=true;return true;}break;
            case IMPOSSIBLE_WATER:
                if(age%40==0&&suspendedWater()){started=true;return true;}break;
            case TREE_LEAF_GEOMETRY:
                if(age%40==0&&leafCross()){started=true;return true;}break;
            case BASE_REPLICA:case MIRROR_ROOM:
                if(age%100==0&&replica(event==Event.MIRROR_ROOM)){started=true;return true;}break;
            case INFINITE_LOOP:
                if(loops>0&&Math.abs(Math.IEEEremainder(AmbientHooks.Scene.facing-loopFacing,360))>20)return true;
                if(loop(moving)){started=true;loops++;lastLoopTick=age;loopFacing=AmbientHooks.Scene.facing;}
                if(loops>=3||started&&!moving)return true;break;
            case BASE_EVACUATION:
                if(age%20==0&&startEvacuation()){started=true;return true;}break;
            case FALSE_HORIZON:
                if(age%40==0&&horizonLight()){started=true;return true;}break;
            case CHUNK_SCAR:
                if(age%100==0&&scar()){started=true;return true;}break;
            default:throw new AssertionError(event);
        }
        return age>=600;
    }
    public static void hear(Object world,double x,double y,double z,String sound){
        if(world!=AmbientHooks.Scene.world||event!=Event.DOOR_MIMIC||!"random.door_open".equals(sound))return;
        if(Math.hypot(x-AmbientHooks.Scene.px,z-AmbientHooks.Scene.pz)>6)return;
        sourceX=x;sourceY=y;sourceZ=z;doorAge=0;
    }
    static float fogFactor(){return fogAge<0?1:(float)(1-.10*Math.sin(Math.PI*fogAge/160.0));}
    public static float sunSize(float original){return daylightAge>=0?0:original;}
    public static float sun(float original){
        if(sunAge<0)return original;if(Float.isNaN(heldSun))heldSun=original;
        if(sunAge<1800)return heldSun;
        double delta=Math.IEEEremainder(original-heldSun,1);return (float)(heldSun+delta*Math.min(1,(sunAge-1800)/40.0));
    }
    public static boolean freezeAnimal(Object entity){return animalTicks>0&&animals.containsKey(entity)||evacuationTicks>0&&evacuees.containsKey(entity);}
    private static void emit(String sound,double x,double y,double z,float gain,float pitch){
        AmbientHooks.queueCue(sound,x,y,z,Math.min(.16f,gain),Math.max(.5f,Math.min(1.1f,pitch)));
    }
    private static void behind(String sound,double distance,float gain,float pitch){double r=Math.toRadians(AmbientHooks.Scene.facing+180);emit(sound,AmbientHooks.Scene.px-Math.sin(r)*distance,AmbientHooks.Scene.py,AmbientHooks.Scene.pz+Math.cos(r)*distance,gain,pitch);}
    private static int ground()throws Exception{return AmbientHooks.Scene.read((int)Math.floor(AmbientHooks.Scene.px),(int)Math.floor(AmbientHooks.Scene.py)-1,(int)Math.floor(AmbientHooks.Scene.pz));}
    private static String step()throws Exception{int id=ground();return id==12?"step.sand":id==13?"step.gravel":id==5||id==17?"step.wood":id==2?"step.grass":"step.stone";}
    private static Object inventory()throws Exception{try{return field(AmbientHooks.Scene.player,"b").get(AmbientHooks.Scene.player);}catch(NoSuchFieldException e){return null;}}
    private static boolean miningOre()throws Exception{
        try{Object mc=field(AmbientHooks.Scene.player,"bd").get(AmbientHooks.Scene.player),hit=field(mc,"x").get(mc);if(hit==null||integer(hit,"a")!=0)return false;
            int id=AmbientHooks.Scene.read(integer(hit,"b"),integer(hit,"c"),integer(hit,"d"));return id==14||id==15||id==16||id==21||id==56||id==73||id==74;
        }catch(NoSuchFieldException e){return false;}
    }
    private static boolean swinging()throws Exception{try{return field(AmbientHooks.Scene.player,"g").getBoolean(AmbientHooks.Scene.player);}catch(NoSuchFieldException e){return false;}}
    private static Field field(Object o,String name)throws Exception{
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    static int integer(Object o,String name)throws Exception{return field(o,name).getInt(o);}
    private static double number(Object o,String name)throws Exception{return field(o,name).getDouble(o);}
    private static void set(Object o,String name,Object v)throws Exception{field(o,name).set(o,v);}
    private static Object call(Object o,String name,Class<?>[] types,Object...args)throws Exception{return o.getClass().getMethod(name,types).invoke(o,args);}
    private static Class<?> type(String name)throws Exception{return Class.forName(name,false,AmbientHooks.Scene.world.getClass().getClassLoader());}
    private static Object item(int id,int count,int damage)throws Exception{return type("eo").getConstructor(int.class,int.class,int.class).newInstance(id,count,damage);}
    private static Object getItem(Object tile,int slot)throws Exception{return call(tile,"a_",new Class<?>[]{int.class},slot);}
    private static void putItem(Object tile,int slot,Object item)throws Exception{call(tile,"a",new Class<?>[]{int.class,type("eo")},slot,item);call(tile,"d",new Class<?>[0]);}
    private static List<?> tiles()throws Exception{try{return (List<?>)field(AmbientHooks.Scene.world,"a").get(AmbientHooks.Scene.world);}catch(NoSuchFieldException e){return Collections.emptyList();}}
    private static List<?> entities()throws Exception{try{return (List<?>)field(AmbientHooks.Scene.world,"v").get(AmbientHooks.Scene.world);}catch(NoSuchFieldException e){return Collections.emptyList();}}
    private static boolean hidden(double x,double y,double z)throws Exception{return !AmbientHooks.Scene.protectedAt(x,y,z);}
    private static boolean tileSafe(Object tile)throws Exception{
        int x=integer(tile,"f"),y=integer(tile,"g"),z=integer(tile,"h");
        return Math.hypot(x-AmbientHooks.Scene.px,z-AmbientHooks.Scene.pz)>=8&&Math.hypot(x-AmbientHooks.Scene.px,z-AmbientHooks.Scene.pz)<=32&&AmbientHooks.Scene.read(x,y,z)>=0&&hidden(x+.5,y,z+.5);
    }
    private static boolean tamperTile()throws Exception{
        int budget=0;
        for(Object t:tiles()){
            if(++budget>128)break;String name=t.getClass().getName();
            if(!(name.equals("ew")||name.equals("ju")||name.equals("nn"))||!tileSafe(t))continue;
            if(event==Event.SIGN_ERASURE&&name.equals("nn")){
                String[] lines=(String[])field(t,"a").get(t);for(int i=0;i<lines.length;i++)if(!lines[i].isEmpty()){lines[i]="";call(t,"d",new Class<?>[0]);return true;}
            }
            if(event==Event.HELPFUL_FURNACE&&name.equals("ju")){
                Object raw=getItem(t,0),out=getItem(t,2);if(raw!=null&&integer(raw,"c")==319&&integer(raw,"a")>0&&(out==null||integer(out,"c")==320&&integer(out,"a")<64)){
                    int count=integer(raw,"a");putItem(t,0,count==1?null:item(319,count-1,integer(raw,"d")));putItem(t,2,item(320,out==null?1:integer(out,"a")+1,0));return true;
                }
            }
            if(!name.equals("ew"))continue;
            if(event==Event.CHEST_GIFT&&getItem(t,13)==null){putItem(t,13,item(38,1,0));return true;}
            if(event==Event.CHEST_SHUFFLE){
                for(int i=0;i<26;i++){Object a=getItem(t,i);if(a!=null&&integer(a,"c")==263&&getItem(t,26)==null){int n=integer(a,"a");putItem(t,26,item(263,1,integer(a,"d")));putItem(t,i,n==1?null:item(263,n-1,integer(a,"d")));return true;}}
            }
        }return false;
    }
    private static boolean painting()throws Exception{
        int budget=0;for(Object e:entities()){
            if(++budget>128)break;if(!e.getClass().getName().equals("it"))continue;
            double x=number(e,"ah"),y=number(e,"ai"),z=number(e,"aj");if(Math.hypot(x-AmbientHooks.Scene.px,z-AmbientHooks.Scene.pz)>32||!hidden(x,y,z))continue;
            Object art=field(e,"b").get(e);if(integer(art,"u")!=16||integer(art,"v")!=16)continue;
            Object[] all=(Object[])art.getClass().getMethod("values").invoke(null);for(Object alternative:all)if(alternative!=art&&integer(alternative,"u")==16&&integer(alternative,"v")==16){set(e,"b",alternative);return true;}
        }return false;
    }
    private static int[] findBlock(int...ids)throws Exception{
        // Bounded 13x13x7 search on a two-block grid; no chunk loading.
        int px=(int)Math.floor(AmbientHooks.Scene.px),py=(int)Math.floor(AmbientHooks.Scene.py),pz=(int)Math.floor(AmbientHooks.Scene.pz);
        for(int radius=8;radius<=20;radius+=4)for(int angle=0;angle<360;angle+=30){
            double r=Math.toRadians(angle);int x=px+(int)Math.round(Math.sin(r)*radius),z=pz+(int)Math.round(Math.cos(r)*radius);
            for(int y=Math.max(2,py-3);y<=Math.min(124,py+3);y++){
                int id=AmbientHooks.Scene.read(x,y,z);for(int match:ids)if(id==match&&hidden(x+.5,y,z+.5))return new int[]{x,y,z};
            }
        }return null;
    }
    private static boolean resetCrop()throws Exception{
        int[] p=findBlock(59,6);if(p==null)return false;
        Object w=AmbientHooks.Scene.world;int meta=(Integer)call(w,"e",new Class<?>[]{int.class,int.class,int.class},p[0],p[1],p[2]);if(meta==0)return false;
        call(w,"b",new Class<?>[]{int.class,int.class,int.class,int.class},p[0],p[1],p[2],0);return true;
    }
    private static boolean water()throws Exception{
        int[] p=findBlock(8,9);if(p==null)return false;
        for(Object e:entities())if(Math.hypot(number(e,"ah")-p[0],number(e,"aj")-p[2])<8&&e!=AmbientHooks.Scene.player)return false;
        emit("random.splash",p[0]+.5,p[1]+.5,p[2]+.5,.10f,.9f);return true;
    }
    private static boolean extinguish()throws Exception{
        int[] first=findBlock(50);if(first==null)return false;List<int[]> plan=new ArrayList<int[]>();plan.add(new int[]{first[0],first[1],first[2],0});
        outer:for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=-1;dy<=1;dy++){
            if(dx==0&&dy==0&&dz==0)continue;int x=first[0]+dx,y=first[1]+dy,z=first[2]+dz;
            if(AmbientHooks.Scene.read(x,y,z)==50&&hidden(x+.5,y,z+.5))plan.add(new int[]{x,y,z,0});if(plan.size()==3)break outer;
        }
        List<Object> drops=new ArrayList<Object>();for(int[] p:plan)drops.add(type("dr").getConstructor(type("cj"),double.class,double.class,double.class,type("eo")).newInstance(AmbientHooks.Scene.world,p[0]+.5,p[1]+.1,p[2]+.5,item(50,1,0)));
        if(!edit(plan.toArray(new int[plan.size()][])))return false;
        boolean any=false;for(int i=0;i<drops.size();i++)if((Boolean)AmbientHooks.Scene.spawn.invoke(AmbientHooks.Scene.world,drops.get(i)))any=true;else{int[] p=plan.get(i);rawEdit(p[0],p[1],p[2],50);}
        return any;
    }
    private static boolean boat()throws Exception{
        int budget=0;for(Object e:entities()){
            if(++budget>128)break;if(!e.getClass().getName().equals("cx"))continue;
            double x=number(e,"ah"),y=number(e,"ai"),z=number(e,"aj");if(Math.hypot(x-AmbientHooks.Scene.px,z-AmbientHooks.Scene.pz)>30||!hidden(x,y,z))continue;
            // Do not move an occupied boat or a boat carrying anything.
            if(field(e,"ab").get(e)!=null||field(e,"ac").get(e)!=null)continue;
            for(int direction:new int[]{180,90,270,0}){double angle=Math.toRadians(direction);boolean ocean=true;for(int distance:new int[]{8,12,16}){int id=AmbientHooks.Scene.read((int)Math.floor(x-Math.sin(angle)*distance),(int)Math.floor(y)-1,(int)Math.floor(z+Math.cos(angle)*distance));if(id!=8&&id!=9)ocean=false;}if(ocean){AmbientHooks.Scene.position.invoke(e,x,y,z,(float)direction,0f);return true;}}
        }return false;
    }
    private static boolean night()throws Exception{try{long time=field(AmbientHooks.Scene.world,"b").getLong(AmbientHooks.Scene.world)%24000;return time>=13000&&time<=23000;}catch(NoSuchFieldException e){return false;}}
    private static boolean blockDoor()throws Exception{
        if(!AmbientHooks.Scene.sheltered||!night())return false;
        int[] p=findBlock(64);if(p==null)return false;
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=p[0]+d[0],y=p[1],z=p[2]+d[1];if(AmbientHooks.Scene.read(x,y,z)==0&&AmbientHooks.Scene.ground(AmbientHooks.Scene.read(x,y-1,z))&&edit(new int[][]{{x,y,z,3}}))return true;}
        return false;
    }
    private static boolean structure()throws Exception{
        int x=(int)Math.round(sourceX),y=(int)Math.floor(sourceY)-1,z=(int)Math.round(sourceZ);
        if(event==Event.CROSS_TUNNEL&&y>50)return false;
        List<int[]> plan=new ArrayList<int[]>();
        if(event==Event.CROSS_TUNNEL){for(int dx=-3;dx<=3;dx++)for(int dy=0;dy<2;dy++)plan.add(new int[]{x+dx,y+dy,z,dx==3&&dy==0?50:0});}
        else {if(y<22||AmbientHooks.Scene.sheltered||!AmbientHooks.Scene.treeNear(x,z,AmbientHooks.Scene.py))return false;for(int i=0;i<15;i++){int ix=x+(int)Math.round(Math.sin(Math.toRadians(AmbientHooks.Scene.facing))*i),iz=z-(int)Math.round(Math.cos(Math.toRadians(AmbientHooks.Scene.facing))*i);plan.add(new int[]{ix,y-i-1,iz,4});plan.add(new int[]{ix,y-i,iz,0});plan.add(new int[]{ix,y-i+1,iz,0});}}
        for(int[] p:plan){int old=AmbientHooks.Scene.read(p[0],p[1],p[2]);if(old!=1&&old!=3&&old!=2&&!(event==Event.STAIRCASE&&old==0))return false;}
        return edit(plan.toArray(new int[plan.size()][]));
    }
    /** Preflight whole transaction before touching any block. Never replaces player-made material. */
    static boolean edit(int[][] plan)throws Exception{
        if(plan.length==0||plan.length>2048)return false;
        int[] old=new int[plan.length],metadata=new int[plan.length];
        for(int i=0;i<plan.length;i++){
            int[] p=plan[i];if(p.length!=4||p[1]<2||p[1]>124||Math.hypot(p[0]-AmbientHooks.Scene.px,p[2]-AmbientHooks.Scene.pz)<8||!hidden(p[0]+.5,p[1],p[2]+.5))return false;
            old[i]=AmbientHooks.Scene.read(p[0],p[1],p[2]);if(old[i]<0)return false;
            metadata[i]=(Integer)call(AmbientHooks.Scene.world,"e",new Class<?>[]{int.class,int.class,int.class},p[0],p[1],p[2]);
            for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){int id=AmbientHooks.Scene.read(p[0]+d[0],p[1]+d[1],p[2]+d[2]);if(id<0||id==8||id==9||id==10||id==11)return false;}
            if(old[i]!=0&&old[i]!=1&&old[i]!=2&&old[i]!=3&&old[i]!=13&&old[i]!=50&&old[i]!=17&&old[i]!=18)return false;
        }
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for(int[] p:plan){minX=Math.min(minX,p[0]);minY=Math.min(minY,p[1]);minZ=Math.min(minZ,p[2]);maxX=Math.max(maxX,p[0]);maxY=Math.max(maxY,p[1]);maxZ=Math.max(maxZ,p[2]);}
        List<?> occupants=entities();if(occupants.size()>256)return false;
        for(Object entity:occupants){double x=number(entity,"ah"),y=number(entity,"ai"),z=number(entity,"aj");if(x>=minX-1.5&&x<=maxX+2.5&&y>=minY-2&&y<=maxY+3&&z>=minZ-1.5&&z<=maxZ+2.5)return false;}
        Object w=AmbientHooks.Scene.world;Class<?>[] types={int.class,int.class,int.class,int.class};
        for(int i=0;i<plan.length;i++){
            int[] p=plan[i];if(old[i]==p[3])continue;
            boolean written=false;try{written=(Boolean)call(w,"d",types,p[0],p[1],p[2],p[3]);}catch(ReflectiveOperationException failure){System.err.println("[Alpha runtime] edit rejected: "+failure.getClass().getSimpleName());}
            if(!written){
                for(int j=i;j>=0;j--){int[] q=plan[j];if(old[j]==q[3])continue;try{call(w,"d",types,q[0],q[1],q[2],old[j]);call(w,"b",types,q[0],q[1],q[2],metadata[j]);}catch(ReflectiveOperationException failure){System.err.println("[Alpha runtime] edit rollback incomplete: "+failure.getClass().getSimpleName());}}
                return false;
            }
        }return true;
    }
    private static boolean startAnimals()throws Exception{
        animals.clear();int budget=0;
        for(Object e:entities()){
            if(++budget>128)break;String name=e.getClass().getName();if(!name.equals("bk")&&!name.equals("mj")&&!name.equals("ak"))continue;
            if(Math.hypot(number(e,"ah")-AmbientHooks.Scene.px,number(e,"aj")-AmbientHooks.Scene.pz)<=30)animals.put(e,new float[]{field(e,"an").getFloat(e),field(e,"ao").getFloat(e)});
        }
        if(animals.isEmpty())return false;sourceX=AmbientHooks.Scene.px+8;sourceY=AmbientHooks.Scene.py+25;sourceZ=AmbientHooks.Scene.pz+8;animalTicks=400;faceAnimals();return true;
    }
    private static void faceAnimals()throws Exception{
        for(Object e:animals.keySet()){
            double dx=sourceX-number(e,"ah"),dy=sourceY-number(e,"ai"),dz=sourceZ-number(e,"aj");
            set(e,"ak",0d);set(e,"al",0d);set(e,"am",0d);set(e,"an",(float)Math.toDegrees(Math.atan2(-dx,dz)));set(e,"ao",(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
        }
    }
    private static void restoreAnimals(){for(Map.Entry<Object,float[]> e:animals.entrySet())try{set(e.getKey(),"an",e.getValue()[0]);set(e.getKey(),"ao",e.getValue()[1]);}catch(Exception ignored){}animals.clear();}
    private static void record()throws Exception{
        double[] h=history[historyCursor];h[0]=AmbientHooks.Scene.px;h[1]=AmbientHooks.Scene.py;h[2]=AmbientHooks.Scene.pz;h[3]=AmbientHooks.Scene.facing;h[4]=AmbientHooks.Scene.lookingPitch;
        try{String[] fields={"B","C","P","Q","R","n","o"};for(int i=0;i<fields.length;i++)h[5+i]=field(AmbientHooks.Scene.player,fields[i]).getFloat(AmbientHooks.Scene.player);}catch(NoSuchFieldException ignored){}
        historyCursor=(historyCursor+1)%history.length;historyCount=Math.min(history.length,historyCount+1);
    }
    private static boolean spawnShadow()throws Exception{
        double[] h=history[historyCursor];if(Math.hypot(h[0]-AmbientHooks.Scene.px,h[2]-AmbientHooks.Scene.pz)<14||!hidden(h[0],h[1],h[2]))return false;
        if(Double.isNaN(AmbientHooks.Scene.standY(h[0],h[2],h[1]-1)))return false;
        Object e=type("AlphaWatcher").getConstructor(type("cj")).newInstance(AmbientHooks.Scene.world);
        set(e,"alpha$plain",true);set(e,"u","/mob/char.png");
        AmbientHooks.Scene.position.invoke(e,h[0],h[1],h[2],(float)h[3],(float)h[4]);
        if(!(Boolean)AmbientHooks.Scene.spawn.invoke(AmbientHooks.Scene.world,e))return false;shadow=e;shadowRetiring=false;shadowAge=0;return true;
    }
    private static void tickShadow()throws Exception{
        double[] h=history[historyCursor];double x=number(shadow,"ah"),y=number(shadow,"ai"),z=number(shadow,"aj");
        if(shadowRetiring){if(hidden(x,y,z))removeShadow();return;}
        if(Math.hypot(h[0]-AmbientHooks.Scene.px,h[2]-AmbientHooks.Scene.pz)<14||Double.isNaN(AmbientHooks.Scene.standY(h[0],h[2],h[1]-1))){shadowRetiring=true;return;}
        // A discontinuous recorded teleport is never replayed visibly.
        if(Math.hypot(h[0]-x,h[2]-z)>1&&(!hidden(x,y,z)||!hidden(h[0],h[1],h[2]))){shadowRetiring=true;return;}
        AmbientHooks.Scene.position.invoke(shadow,h[0],h[1],h[2],(float)h[3],(float)h[4]);
        try{String[] fields={"B","C","P","Q","R","n","o"};for(int i=0;i<fields.length;i++)set(shadow,fields[i],(float)h[5+i]);}catch(NoSuchFieldException ignored){}
        if(++shadowAge>600)shadowRetiring=true;
    }
    private static void removeShadow(){try{AmbientHooks.Scene.dead.invoke(shadow);}catch(Exception ignored){}shadow=null;shadowRetiring=false;}
    public static boolean delayDrop(Object world,Object entity){
        if(world!=AmbientHooks.Scene.world||event!=Event.DELAYED_DROP||delayedDrop!=null||!entity.getClass().getName().equals("dr"))return false;
        try{
            Object stack=field(entity,"a").get(entity);
            if(integer(stack,"c")!=4||!hidden(number(entity,"ah"),number(entity,"ai"),number(entity,"aj")))return false;
            delayedDrop=entity;dropWait=0;return false; // Vanilla still owns and saves the actual item entity.
        }catch(Exception e){return false;}
    }
    public static boolean holdDrop(Object entity){return entity!=null&&entity==delayedDrop;}
    private static void releaseDrop(){delayedDrop=null;dropWait=0;}
    public static void screen(Object screen){
        if(screen!=null&&screen.getClass().getName().equals("du")&&event==Event.INVENTORY_GHOST)ghostArmed=true;
    }
    public static Object ghostItem(Object original){
        if(original!=null||!ghostArmed)return original;
        ghostArmed=false;try{return item(278,1,0);}catch(Exception e){return null;}
    }
    public static Object sky(Object vector){
        if(daylightAge>=0)try{set(vector,"a",.5d);set(vector,"b",.7d);set(vector,"c",1d);}catch(Exception ignored){}
        return vector;
    }
    private static void rawEdit(int x,int y,int z,int id)throws Exception{
        call(AmbientHooks.Scene.world,"d",new Class<?>[]{int.class,int.class,int.class,int.class},x,y,z,id);
    }
    private static boolean suspendedWater()throws Exception{
        if(AmbientHooks.Scene.py>45||!AmbientHooks.Scene.sheltered)return false;
        int x=(int)Math.round(sourceX),y=(int)Math.floor(sourceY)+1,z=(int)Math.round(sourceZ);
        if(!hidden(x+.5,y,z+.5)||AmbientHooks.Scene.read(x,y,z)!=0||AmbientHooks.Scene.read(x,y-1,z)!=0||AmbientHooks.Scene.read(x,y+1,z)!=0)return false;
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})if(AmbientHooks.Scene.read(x+d[0],y,z+d[1])!=0)return false;
        // Alpha's stationary water already waits for a neighbor update; no fake fluid engine.
        if(!(Boolean)call(AmbientHooks.Scene.world,"a",new Class<?>[]{int.class,int.class,int.class,int.class},x,y,z,9))return false;
        call(AmbientHooks.Scene.world,"h",new Class<?>[]{int.class,int.class,int.class},x,y,z);return true;
    }
    private static boolean leafCross()throws Exception{
        int[] p=findBlock(18);if(p==null)return false;List<int[]> plan=new ArrayList<int[]>();
        for(int i=-2;i<=2;i++){if(AmbientHooks.Scene.read(p[0]+i,p[1],p[2])!=18||AmbientHooks.Scene.read(p[0],p[1],p[2]+i)!=18)return false;plan.add(new int[]{p[0]+i,p[1],p[2],0});if(i!=0)plan.add(new int[]{p[0],p[1],p[2]+i,0});}
        return edit(plan.toArray(new int[plan.size()][]));
    }
    private static boolean horizonLight()throws Exception{
        for(int angle:new int[]{150,180,210}){
            double a=Math.toRadians(AmbientHooks.Scene.facing+angle);int x=(int)Math.floor(AmbientHooks.Scene.px-Math.sin(a)*60),z=(int)Math.floor(AmbientHooks.Scene.pz+Math.cos(a)*60);
            double y=AmbientHooks.Scene.standY(x+.5,z+.5,AmbientHooks.Scene.py+8);if(Double.isNaN(y))continue;
            if(edit(new int[][]{{x,(int)y,z,50}})){horizon=new int[]{x,(int)y,z};horizonAge=0;return true;}
        }return false;
    }
    private static boolean loop(boolean moving)throws Exception{
        if(!moving||age-lastLoopTick<100||!AmbientHooks.Scene.sheltered||historyCount<100)return false;
        Object p=AmbientHooks.Scene.player;try{if(field(p,"ab").get(p)!=null)return false;}catch(NoSuchFieldException ignored){}
        for(int i=100;i<historyCount;i+=10){double[] h=history[(historyCursor-i+history.length)%history.length];
            if(Math.abs(Math.IEEEremainder(AmbientHooks.Scene.facing-h[3],360))>5||Math.abs(h[1]-AmbientHooks.Scene.py)>.1||Math.abs(Math.hypot(h[0]-AmbientHooks.Scene.px,h[2]-AmbientHooks.Scene.pz)-20)>1)continue;
            if(AmbientHooks.Scene.read((int)Math.floor(h[0]),(int)Math.floor(h[1])-2,(int)Math.floor(h[2]))!=1||Double.isNaN(AmbientHooks.Scene.standY(h[0],h[2],h[1]-1))||!AmbientHooks.Scene.lineClear(AmbientHooks.Scene.px,AmbientHooks.Scene.py+1,AmbientHooks.Scene.pz,h[0],h[1]+1,h[2]))continue;
            // Require an actually matching local cave cross-section, never jump into unknown geometry.
            boolean same=true;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=2;dy++)for(int dz=-1;dz<=1;dz++)if(AmbientHooks.Scene.read((int)Math.floor(h[0])+dx,(int)Math.floor(h[1])+dy,(int)Math.floor(h[2])+dz)!=AmbientHooks.Scene.read((int)Math.floor(AmbientHooks.Scene.px)+dx,(int)Math.floor(AmbientHooks.Scene.py)+dy,(int)Math.floor(AmbientHooks.Scene.pz)+dz))same=false;
            for(Object entity:entities())if(entity!=p&&Math.hypot(number(entity,"ah")-h[0],number(entity,"aj")-h[2])<4)same=false;
            if(same){AmbientHooks.Scene.position.invoke(p,h[0],h[1],h[2],AmbientHooks.Scene.facing,AmbientHooks.Scene.lookingPitch);return true;}
        }return false;
    }
    private static boolean startEvacuation()throws Exception{
        AmbientHooksState state=AmbientHooksState.of(AmbientHooks.Scene.world);if(!state.baseKnown)return false;
        evacuees.clear();int budget=0;
        for(Object e:entities()){
            if(++budget>128)break;String name=e.getClass().getName();if(!name.equals("lr")&&!name.equals("cs")&&!name.equals("at")&&!name.equals("cy"))continue;
            if(Math.hypot(number(e,"ah")-state.baseX,number(e,"aj")-state.baseZ)<=40)evacuees.put(e,new float[]{field(e,"an").getFloat(e),field(e,"ao").getFloat(e)});
        }
        if(evacuees.isEmpty())return false;evacuationTicks=600;evacuate();return true;
    }
    private static void evacuate()throws Exception{
        AmbientHooksState state=AmbientHooksState.of(AmbientHooks.Scene.world);
        for(Object e:evacuees.keySet()){
            double x=number(e,"ah"),y=number(e,"ai"),z=number(e,"aj"),dx=x-state.baseX,dz=z-state.baseZ,len=Math.hypot(dx,dz);if(len<.1||len>42)continue;
            double nx=x+dx/len*.13,nz=z+dz/len*.13,ny=AmbientHooks.Scene.standY(nx,nz,y-1);
            if(Double.isNaN(ny)||Math.abs(ny-y)>1||Math.hypot(nx-AmbientHooks.Scene.px,nz-AmbientHooks.Scene.pz)<Math.hypot(x-AmbientHooks.Scene.px,z-AmbientHooks.Scene.pz))continue;
            AmbientHooks.Scene.position.invoke(e,nx,ny,nz,(float)Math.toDegrees(Math.atan2(-dx,dz)),0f);
        }
    }
    private static void restoreEvacuees(){for(Map.Entry<Object,float[]> e:evacuees.entrySet())try{set(e.getKey(),"an",e.getValue()[0]);set(e.getKey(),"ao",e.getValue()[1]);}catch(Exception ignored){}evacuees.clear();}
    private static void learnBase()throws Exception{
        AmbientHooksState state=AmbientHooksState.of(AmbientHooks.Scene.world);if(state.baseKnown)return;
        Object chest=null,furnace=null;int budget=0;for(Object t:tiles()){if(++budget>128)break;
            String name=t.getClass().getName();if(!name.equals("ew")&&!name.equals("ju"))continue;
            if(Math.hypot(integer(t,"f")-AmbientHooks.Scene.px,integer(t,"h")-AmbientHooks.Scene.pz)>16)continue;
            if(name.equals("ew"))chest=t;else furnace=t;
        }
        if(chest==null||furnace==null)return;
        int x=integer(chest,"f"),y=integer(chest,"g"),z=integer(chest,"h");if(Math.hypot(integer(furnace,"f")-x,integer(furnace,"h")-z)>4)return;
        int[] craft=null;for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(AmbientHooks.Scene.read(x+dx,y,z+dz)==58)craft=new int[]{dx,0,dz};
        if(craft==null)return;
        byte[] blueprint=new byte[9*6*9];int count=0;
        for(int dx=-4;dx<=4;dx++)for(int dy=-1;dy<=4;dy++)for(int dz=-4;dz<=4;dz++){
            int id=AmbientHooks.Scene.read(x+dx,y+dy,z+dz);if(id<0)return;
            // Original building materials only. Never copy inventories, signs, doors or fluids.
            if(id==4||id==5||id==17||id==20||id==48){blueprint[((dx+4)*6+dy+1)*9+dz+4]=(byte)id;count++;}
        }
        if(count<12)return;
        state.baseX=x;state.baseY=y;state.baseZ=z;state.baseKnown=true;state.blueprint=blueprint;
        state.layout=new int[]{0,0,0,integer(furnace,"f")-x,integer(furnace,"g")-y,integer(furnace,"h")-z,craft[0],craft[1],craft[2]};
    }
    private static boolean replica(boolean room)throws Exception{
        AmbientHooksState state=AmbientHooksState.of(AmbientHooks.Scene.world);if(!state.baseKnown)return false;
        if(room){
            if(AmbientHooks.Scene.py>40||state.layout.length!=9)return false;
            int x=(int)Math.round(sourceX),y=(int)Math.floor(sourceY),z=(int)Math.round(sourceZ);List<int[]> plan=new ArrayList<int[]>();
            for(int dx=-3;dx<=3;dx++)for(int dy=-1;dy<=3;dy++)for(int dz=-3;dz<=3;dz++){
                int old=AmbientHooks.Scene.read(x+dx,y+dy,z+dz);if(old!=1&&old!=3)return false;
                plan.add(new int[]{x+dx,y+dy,z+dz,Math.abs(dx)==3||Math.abs(dz)==3||dy==-1||dy==3?4:0});
            }
            for(int i=0;i<9;i+=3){if(Math.abs(state.layout[i])>2||Math.abs(state.layout[i+2])>2||state.layout[i+1]!=0)return false;replacePlan(plan,x+state.layout[i],y,z+state.layout[i+2],i==0?54:i==3?61:58);}
            return edit(plan.toArray(new int[plan.size()][]));
        }
        if(state.blueprint.length!=486)return false;
        for(int angle:new int[]{0,90,180,270}){
            double a=Math.toRadians(angle);int x=state.baseX+(int)(Math.sin(a)*200),z=state.baseZ+(int)(Math.cos(a)*200);double ground=AmbientHooks.Scene.standY(x+.5,z+.5,state.baseY-2);if(Double.isNaN(ground))continue;
            int y=(int)ground-1;List<int[]> plan=new ArrayList<int[]>();boolean clear=true;
            for(int dx=-4;dx<=4;dx++)for(int dy=-1;dy<=4;dy++)for(int dz=-4;dz<=4;dz++){
                int id=state.blueprint[((dx+4)*6+dy+1)*9+dz+4]&255;if(id==0)continue;
                // Deterministically missing roof/wall pieces make the copy incomplete, not a duplicate home with loot.
                if(dy>1&&((dx*31+dz*17+dy)&3)==0)continue;
                int old=AmbientHooks.Scene.read(x+dx,y+dy,z+dz);if(old!=0&&old!=1&&old!=2&&old!=3){clear=false;break;}
                plan.add(new int[]{x+dx,y+dy,z+dz,id==4?48:id});
            }
            if(clear&&edit(plan.toArray(new int[plan.size()][])))return true;
        }return false;
    }
    private static void replacePlan(List<int[]> plan,int x,int y,int z,int id){for(int[] p:plan)if(p[0]==x&&p[1]==y&&p[2]==z){p[3]=id;return;}}
    private static boolean scar()throws Exception{
        // At most one loaded chunk, at least 64 blocks from the player and away from the recorded base.
        AmbientHooksState state=AmbientHooksState.of(AmbientHooks.Scene.world);int cx=((int)Math.round(sourceX)>>4)*16,cz=((int)Math.round(sourceZ)>>4)*16;
        cx+=(int)(Math.sin(Math.toRadians(AmbientHooks.Scene.facing))*AmbientHooks.outsideFogDistance());cz-=(int)(Math.cos(Math.toRadians(AmbientHooks.Scene.facing))*AmbientHooks.outsideFogDistance());cx=(cx>>4)*16;cz=(cz>>4)*16;
        if(state.baseKnown&&Math.hypot(cx-state.baseX,cz-state.baseZ)<88)return false;
        List<int[]> plan=new ArrayList<int[]>();
        for(int x=cx;x<cx+16;x++)for(int z=cz;z<cz+16;z++){
            int bottom=-1;for(int y=100;y>=48;y--){int id=AmbientHooks.Scene.read(x,y,z);if(id<0)return false;if(id==1){bottom=y;break;}if(id!=0&&id!=2&&id!=3&&id!=17&&id!=18)return false;if(id!=0)plan.add(new int[]{x,y,z,0});if(plan.size()>2048)return false;}
            if(bottom<0)return false;
        }
        return edit(plan.toArray(new int[plan.size()][]));
    }
    private static boolean bindAudio(){
        try{Object mc=field(AmbientHooks.Scene.player,"bd").get(AmbientHooks.Scene.player);Object nr=field(mc,"A").get(mc);Object sys=field(nr,"a").get(nr);if(sys==null)return false;soundSystem=sys;return true;}catch(Exception e){return false;}
    }
    private static void restoreAudio(){
        if(soundSystem!=null)try{
            if(audioChanged)call(soundSystem,"setMasterVolume",new Class<?>[]{float.class},originalMaster);
            if(musicChanged){call(soundSystem,"setPitch",new Class<?>[]{String.class,float.class},"BgMusic",1f);if(musicWasPlaying)call(soundSystem,"play",new Class<?>[]{String.class},"BgMusic");}
        }catch(Exception ignored){}
        soundSystem=null;audioChanged=false;musicChanged=false;musicWasPlaying=false;
    }
    private AmbientHooksAnomalies(){}
}
