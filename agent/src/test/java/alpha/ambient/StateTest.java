package alpha.ambient;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class StateTest {
    public static class Nbt {
        final Map<String,Number> data=new HashMap<String,Number>();
        public void a(String k,long v){data.put(k,v);}public void a(String k,int v){data.put(k,v);}
        public long f(String k){return data.containsKey(k)?data.get(k).longValue():0;}
        public int e(String k){return data.containsKey(k)?data.get(k).intValue():0;}
        public boolean b(String k){return data.containsKey(k);}
    }
    @Test public void cooldownSurvivesSaveReloadAndWorldsAreIsolated(){
        Object w=new Object();AmbientHooksState s=AmbientHooksState.of(w);s.ticks=100;s.mark("WATCHER");
        Nbt n=new Nbt();AmbientHooksState.save(w,n);Object reopened=new Object();AmbientHooksState.load(reopened,n);
        AmbientHooksState r=AmbientHooksState.of(reopened);assertFalse(r.eligible("WATCHER"));
        r.ticks+=AmbientHooksState.COOLDOWN-1;assertFalse(r.eligible("WATCHER"));r.tick();assertTrue(r.eligible("WATCHER"));
        assertTrue(AmbientHooksState.of(new Object()).eligible("WATCHER"));
    }
    @Test public void exhaustionStaysQuietAndPreviewDoesNotMarkHistory(){
        AmbientHooksState s=new AmbientHooksState();String[] ids={"a","b"};int[] tiers={1,2},weights={1,100};
        assertEquals(0,s.choose(ids,tiers,weights,new Random(1)));s.mark("a");assertEquals(-1,s.choose(ids,tiers,weights,new Random(1)));
        s.corruption=26;assertEquals(1,s.choose(ids,tiers,weights,new Random(1)));
    }
    @Test public void dayBoundariesAndMalformedValuesAreClamped(){
        assertEquals(0,AmbientHooksState.level(0));assertEquals(26,AmbientHooksState.level(4*24000));
        assertEquals(51,AmbientHooksState.level(10*24000));assertEquals(76,AmbientHooksState.level(18*24000));
        assertEquals(100,AmbientHooksState.level(Long.MAX_VALUE));
        Nbt n=new Nbt();n.a("AlphaActiveTicks",-1L);n.a("CorruptionLevel",500);Object w=new Object();AmbientHooksState.load(w,n);
        assertEquals(0,AmbientHooksState.of(w).ticks);assertEquals(100,AmbientHooksState.of(w).corruption);
    }
}
