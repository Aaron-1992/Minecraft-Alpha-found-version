package alpha.ambient;

import java.util.*;

/** Per-world, active-tick clock. Serialized only through vanilla's level.dat transaction. */
public final class AmbientHooksState {
    static final long COOLDOWN=20L*60*20;
    private static final Map<Object,AmbientHooksState> WORLDS=new WeakHashMap<Object,AmbientHooksState>();
    long ticks;
    int corruption;
    final Map<String,Long> history=new HashMap<String,Long>();
    static synchronized AmbientHooksState of(Object world){
        AmbientHooksState s=WORLDS.get(world);if(s==null){s=new AmbientHooksState();WORLDS.put(world,s);}return s;
    }
    void tick(){if(ticks<Long.MAX_VALUE-1)ticks++;corruption=Math.max(corruption,level(ticks));}
    static int level(long ticks){
        long days=Math.max(0,ticks)/24000;
        return days<4?(int)(days*25/4):days<10?26+(int)((days-4)*24/6):days<18?51+(int)((days-10)*24/8):Math.min(100,76+(int)Math.min(24,days-18));
    }
    int tier(){return corruption<=25?1:corruption<=50?2:corruption<=75?3:4;}
    boolean eligible(String id){Long last=history.get(id);return last==null||ticks>=last&&ticks-last>=COOLDOWN;}
    int choose(String[] ids,int[] tiers,int[] weights,Random random){
        int total=0;for(int i=0;i<ids.length;i++)if(tiers[i]<=tier()&&eligible(ids[i]))total+=weights[i];
        if(total==0)return -1;
        int roll=random.nextInt(total);for(int i=0;i<ids.length;i++)if(tiers[i]<=tier()&&eligible(ids[i])){roll-=weights[i];if(roll<0)return i;}return -1;
    }
    void mark(String id){history.put(id,ticks);}
    public static void load(Object world,Object nbt){
        AmbientHooksState s=new AmbientHooksState();
        try{
            Class<?> c=nbt.getClass();java.lang.reflect.Method get=c.getMethod("f",String.class);
            s.ticks=Math.max(0,(Long)get.invoke(nbt,"AlphaActiveTicks"));
            s.corruption=Math.max(level(s.ticks),Math.max(0,Math.min(100,(Integer)c.getMethod("e",String.class).invoke(nbt,"CorruptionLevel"))));
            for(String id:AmbientHooks.eventNames()){
                String key="AlphaLast_"+id;
                if((Boolean)c.getMethod("b",String.class).invoke(nbt,key))s.history.put(id,Math.max(0,Math.min(s.ticks,(Long)get.invoke(nbt,key))));
            }
            synchronized(AmbientHooksState.class){WORLDS.put(world,s);}
        }catch(ReflectiveOperationException e){System.err.println("[Alpha state] load unavailable: "+e.getClass().getSimpleName());}
    }
    public static void save(Object world,Object nbt){
        AmbientHooksState s=of(world);
        try{
            Class<?> c=nbt.getClass();java.lang.reflect.Method put=c.getMethod("a",String.class,long.class);
            put.invoke(nbt,"AlphaActiveTicks",s.ticks);c.getMethod("a",String.class,int.class).invoke(nbt,"CorruptionLevel",s.corruption);
            for(Map.Entry<String,Long> e:s.history.entrySet())put.invoke(nbt,"AlphaLast_"+e.getKey(),e.getValue());
        }catch(ReflectiveOperationException e){System.err.println("[Alpha state] save unavailable: "+e.getClass().getSimpleName());}
    }
}
