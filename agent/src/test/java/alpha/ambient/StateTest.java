package alpha.ambient;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class StateTest {
    public static class Nbt {
        final Map<String,Object> data=new HashMap<String,Object>();
        public void a(String k,long v){data.put(k,v);}public void a(String k,int v){data.put(k,v);}
        public long f(String k){return data.containsKey(k)?((Number)data.get(k)).longValue():0;}
        public int e(String k){return data.containsKey(k)?((Number)data.get(k)).intValue():0;}
        public void a(String k,byte[] v){data.put(k,v.clone());}
        public byte[] j(String k){return data.containsKey(k)?((byte[])data.get(k)).clone():new byte[0];}
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

    @Test public void baseBlueprintAndEveryNewCooldownSurviveReloadAndUnsafeTemplateIsRejected(){
        Object world=new Object();AmbientHooksState state=AmbientHooksState.of(world);state.baseKnown=true;state.baseX=2;state.baseY=64;state.baseZ=-12;state.blueprint=new byte[486];state.blueprint[0]=5;state.layout=new int[]{0,0,0,1,0,0,-1,0,0};
        for(AmbientHooksAnomalies.Event e:AmbientHooksAnomalies.Event.values())state.mark(e.name());
        Nbt n=new Nbt();AmbientHooksState.save(world,n);Object loaded=new Object();AmbientHooksState.load(loaded,n);AmbientHooksState copy=AmbientHooksState.of(loaded);assertTrue(copy.baseKnown);assertArrayEquals(state.blueprint,copy.blueprint);assertArrayEquals(state.layout,copy.layout);
        for(AmbientHooksAnomalies.Event e:AmbientHooksAnomalies.Event.values())assertFalse(copy.eligible(e.name()));
        byte[] bad=n.j("AlphaBlueprint");bad[0]=46;n.a("AlphaBlueprint",bad);loaded=new Object();AmbientHooksState.load(loaded,n);assertFalse(AmbientHooksState.of(loaded).baseKnown);
    }
}
