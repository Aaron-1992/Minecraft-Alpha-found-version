package alpha.ambient;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real anomaly adapter, generated actor, and authored exact-signature stand-ins. No game assets. */
public class AnomalyTest {
    static class Env implements AutoCloseable {
        final SceneIntegrationTest.Loader loader=new SceneIntegrationTest.Loader();
        final Object w,p;final Class<?> wc,pc;String heard;
        Env()throws Exception{SceneIntegrationTest.reset();wc=loader.loadClass("cj");pc=loader.loadClass("jx");w=wc.getConstructor().newInstance();p=pc.getConstructor().newInstance();tick();SceneIntegrationTest.frame(p);}
        void arm(AmbientHooksAnomalies.Event e){SceneIntegrationTest.frame(p);AmbientHooks.scene(w,p);SceneIntegrationTest.frame(p);AmbientHooksAnomalies.begin(e.ordinal(),true);}
        void tick(){SceneIntegrationTest.frame(p);AmbientHooks.scene(w,p);String cue=AmbientHooks.nextSound(true);if(cue!=null)heard=cue;}
        void ticks(int n){for(int i=0;i<n;i++)tick();}
        Object make(String type)throws Exception{return loader.loadClass(type).getConstructor().newInstance();}
        Object item(int id,int count,int damage)throws Exception{return loader.loadClass("eo").getConstructor(int.class,int.class,int.class).newInstance(id,count,damage);}
        void block(int x,int y,int z,int id)throws Exception{wc.getMethod("fixture",int.class,int.class,int.class,int.class).invoke(w,x,y,z,id);}
        int block(int x,int y,int z)throws Exception{return (Integer)wc.getMethod("a",int.class,int.class,int.class).invoke(w,x,y,z);}
        void tile(Object t,int x,int y,int z,int id)throws Exception{set(t,"f",x);set(t,"g",y);set(t,"h",z);((List<Object>)get(w,"a")).add(t);block(x,y,z,id);}
        void entity(Object e,double x,double y,double z)throws Exception{set(e,"ah",x);set(e,"ai",y);set(e,"aj",z);((List<Object>)get(w,"v")).add(e);}
        Object stack(Object tile,int i)throws Exception{return tile.getClass().getMethod("a_",int.class).invoke(tile,i);}
        void stack(Object tile,int i,Object stack)throws Exception{tile.getClass().getMethod("a",int.class,loader.loadClass("eo")).invoke(tile,i,stack);}
        public void close()throws Exception{SceneIntegrationTest.reset();}
    }
    static Object get(Object o,String f)throws Exception{return o.getClass().getField(f).get(o);}
    static void set(Object o,String f,Object value)throws Exception{o.getClass().getField(f).set(o,value);}
    static int n(Object o,String f)throws Exception{return (Integer)get(o,f);}
    @Test public void allFortyRulesHaveDistinctStableIdsAndBoundedMissingContext()throws Exception{
        assertEquals(44,AmbientHooksAnomalies.Event.values().length);assertEquals(67,AmbientHooks.eventCount());
        Set<String> names=new HashSet<String>();
        for(AmbientHooksAnomalies.Event event:AmbientHooksAnomalies.Event.values())try(Env e=new Env()){
            assertTrue(names.add(event.name()));assertTrue(event.tier>=1&&event.tier<=4);assertTrue(event.weight>0);
            e.arm(event);e.ticks(1250);assertFalse("Event exceeded time budget: "+event,privateValue("event")!=null);
            assertFalse("Adapter globally disabled by "+event,AmbientHooks.Scene.disabled);
        }
    }
    @Test public void debugSelectorUsesExactStableIdsAndSurvivesInactiveTicks()throws Exception{
        try(Env e=new Env()){
            for(int i=0;i<AmbientHooks.eventCount();i++){
                AmbientHooks.selectDebug(i);AmbientHooks.nextSound(false);AmbientHooks.nextSound(true);
                assertEquals(AmbientHooks.eventNames()[i],AmbientHooks.lastEventName());
            }
            try{AmbientHooks.selectDebug(-1);fail();}catch(IllegalArgumentException expected){}
            AmbientHooks.selectDebug(0);AmbientHooks.cancelDebug();AmbientHooks.nextSound(true);
        }
    }
    @Test public void newSoundSequencesAreQuietBoundedAndCancellable()throws Exception{
        for(AmbientHooksAnomalies.Event rule:new AmbientHooksAnomalies.Event[]{AmbientHooksAnomalies.Event.ANSWERING_KNOCKS,AmbientHooksAnomalies.Event.APPROACHING_STEPS})try(Env e=new Env()){
            e.arm(rule);e.ticks(180);assertNotNull(e.heard);assertNull(privateValue("event"));
            e.arm(rule);e.ticks(20);AmbientHooksAnomalies.cancel(false);assertNull(privateValue("event"));
        }
    }
    static Object privateValue(String name)throws Exception{Field f=AmbientHooksAnomalies.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    @Test public void toolOnlyDriftsOnSlotChangeAndNeverBreaks()throws Exception{try(Env e=new Env()){
        Object inv=get(e.p,"b");Object[] stacks=(Object[])get(inv,"a");stacks[1]=e.item(270,1,98);e.arm(AmbientHooksAnomalies.Event.TOOL_DRIFT);e.ticks(3);assertEquals(98,n(stacks[1],"d"));set(inv,"d",1);e.tick();int damage=n(stacks[1],"d");assertTrue(damage>=95&&damage<=99);assertEquals(1,n(stacks[1],"a"));
    }}
    @Test public void chestShuffleConservesCoalAndVisibleChestIsUntouched()throws Exception{try(Env e=new Env()){
        Object chest=e.make("ew");e.tile(chest,0,64,12,54);e.stack(chest,0,e.item(263,7,0));e.arm(AmbientHooksAnomalies.Event.CHEST_SHUFFLE);e.ticks(20);assertNull(e.stack(chest,26));assertEquals(7,n(e.stack(chest,0),"a"));
        e.tile(chest,0,64,-12,54);e.ticks(20);assertEquals(6,n(e.stack(chest,0),"a"));assertEquals(1,n(e.stack(chest,26),"a"));
    }}
    @Test public void giftUsesEmptyCenterAndFurnaceConservesPork()throws Exception{try(Env e=new Env()){
        Object chest=e.make("ew");e.tile(chest,0,64,-12,54);e.arm(AmbientHooksAnomalies.Event.CHEST_GIFT);e.ticks(20);assertEquals(38,n(e.stack(chest,13),"c"));
        Object furnace=e.make("ju");e.tile(furnace,3,64,-12,61);e.stack(furnace,0,e.item(319,2,0));e.arm(AmbientHooksAnomalies.Event.HELPFUL_FURNACE);e.ticks(20);assertEquals(1,n(e.stack(furnace,0),"a"));assertEquals(320,n(e.stack(furnace,2),"c"));
    }}
    @Test public void signErasesOnlyOneNonemptyLineAndPaintingKeepsDimensions()throws Exception{try(Env e=new Env()){
        Object sign=e.make("nn");e.tile(sign,0,64,-12,63);e.arm(AmbientHooksAnomalies.Event.SIGN_ERASURE);e.ticks(20);assertArrayEquals(new String[]{"","second","","fourth"},(String[])get(sign,"a"));
        Object painting=e.make("it");e.entity(painting,0,64,-14);Object before=get(painting,"b");e.arm(AmbientHooksAnomalies.Event.PAINTING_SWAP);e.ticks(20);Object after=get(painting,"b");assertNotSame(before,after);assertEquals(16,n(after,"u"));assertEquals(16,n(after,"v"));
    }}
    @Test public void cropStageResetsWithoutDeletingPlant()throws Exception{try(Env e=new Env()){
        e.block(0,64,-8,59);e.wc.getMethod("b",int.class,int.class,int.class,int.class).invoke(e.w,0,64,-8,7);e.arm(AmbientHooksAnomalies.Event.CROP_RESET);e.ticks(20);assertEquals(59,e.block(0,64,-8));assertEquals(0,e.wc.getMethod("e",int.class,int.class,int.class).invoke(e.w,0,64,-8));
    }}
    @Test public void phantomStepRequiresStopAndDoorWaitsEightTicks()throws Exception{try(Env e=new Env()){
        e.arm(AmbientHooksAnomalies.Event.PHANTOM_STEP);set(e.p,"aj",.2d);e.tick();e.heard=null;e.tick();assertEquals("step.gravel",e.heard);
        e.arm(AmbientHooksAnomalies.Event.DOOR_MIMIC);AmbientHooksAnomalies.hear(e.w,0,64,.2,"random.door_open");e.heard=null;e.ticks(7);assertNull(e.heard);e.tick();assertEquals("random.door_open",e.heard);
    }}
    @Test public void miningEchoStopsOnSameTickAsSwingAndSyncStepsAreDelayed()throws Exception{try(Env e=new Env()){
        e.block(0,64,2,16);e.arm(AmbientHooksAnomalies.Event.MIMIC_MINING);set(e.p,"g",true);e.ticks(8);assertEquals("step.stone",e.heard);set(e.p,"g",false);e.heard=null;e.ticks(16);assertNull(e.heard);
        e.arm(AmbientHooksAnomalies.Event.SYNC_STEPS);e.heard=null;for(int i=0;i<10;i++){set(e.p,"aj",i*.15d);e.tick();}assertNull(e.heard);e.ticks(3);assertEquals("step.stone",e.heard);
    }}
    @Test public void fogBreathIsSmoothTenPercentAndCancellationRestoresIt()throws Exception{try(Env e=new Env()){
        e.arm(AmbientHooksAnomalies.Event.FOG_BREATH);e.ticks(300);assertTrue(AmbientHooksAnomalies.fogFactor()>.99f);e.ticks(80);assertEquals(.9f,AmbientHooksAnomalies.fogFactor(),.001f);AmbientHooksAnomalies.cancel(false);assertEquals(1,AmbientHooksAnomalies.fogFactor(),0);
    }}
    @Test public void silenceAndMusicRestoreWithoutExceedingOriginalVolume()throws Exception{try(Env e=new Env()){
        Object sound=get(get(get(e.p,"bd"),"A"),"a");e.arm(AmbientHooksAnomalies.Event.SILENT_WORLD);e.ticks(40);assertEquals(0,(Float)get(sound,"gain"),0);e.ticks(600);assertEquals(0,(Float)get(sound,"gain"),0);e.ticks(40);assertEquals(.75f,(Float)get(sound,"gain"),0);
        e.arm(AmbientHooksAnomalies.Event.MUSIC_WARP);e.ticks(600);assertEquals(.5f,(Float)get(sound,"pitch"),0);assertEquals(false,get(sound,"playing"));AmbientHooksAnomalies.cancel(false);assertEquals(1,(Float)get(sound,"pitch"),0);assertEquals(true,get(sound,"playing"));
    }}
    @Test public void animalStareIsBoundedAndF7RestoresOrientation()throws Exception{try(Env e=new Env()){
        Object pig=e.make("mj");e.entity(pig,10,64,0);set(pig,"an",23f);e.arm(AmbientHooksAnomalies.Event.ANIMAL_STARE);e.ticks(20);assertTrue(AmbientHooksAnomalies.freezeAnimal(pig));assertNotEquals(23f,(Float)get(pig,"an"),.1f);AmbientHooksAnomalies.cancel(false);assertFalse(AmbientHooksAnomalies.freezeAnimal(pig));assertEquals(23f,(Float)get(pig,"an"),0);
    }}
    @Test public void boatRotatesOnlyWhileUnoccupiedAndHidden()throws Exception{try(Env e=new Env()){
        for(int z:new int[]{-24,-28,-32}){e.block(-1,63,z,9);e.block(0,63,z,9);}Object boat=e.make("cx");e.entity(boat,0,64,-16);set(boat,"ab",e.p);e.arm(AmbientHooksAnomalies.Event.BOAT_TURN);e.ticks(20);assertEquals(0,(Float)get(boat,"an"),0);set(boat,"ab",null);e.ticks(20);assertEquals(180,(Float)get(boat,"an"),0);
    }}
    @Test public void transactionRejectsVisibleUnloadedLiquidAndManufacturedTargets()throws Exception{try(Env e=new Env()){
        SceneIntegrationTest.frame(e.p);assertFalse(AmbientHooksAnomalies.edit(new int[][]{{0,64,12,3}}));assertFalse(AmbientHooksAnomalies.edit(new int[][]{{0,64,-200,3}}));
        e.block(0,64,-12,54);assertFalse(AmbientHooksAnomalies.edit(new int[][]{{0,64,-12,0}}));e.block(0,64,-12,1);e.block(1,64,-12,9);assertFalse(AmbientHooksAnomalies.edit(new int[][]{{0,64,-12,0}}));
        e.block(1,64,-12,0);assertTrue(AmbientHooksAnomalies.edit(new int[][]{{0,64,-12,0}}));assertEquals(0,e.block(0,64,-12));
        AmbientHooksVisibility.clear();assertFalse(AmbientHooksAnomalies.edit(new int[][]{{0,64,-14,3}}));
    }}
    @Test public void delayedDropNeverInterceptsVisibleItemsAndFlushesOnWorldExit()throws Exception{try(Env e=new Env()){
        Class<?> drop=e.loader.loadClass("dr");Constructor<?> ctor=drop.getConstructor(e.wc,double.class,double.class,double.class,e.loader.loadClass("eo"));Object stack=e.item(4,1,0);
        e.arm(AmbientHooksAnomalies.Event.DELAYED_DROP);assertFalse(AmbientHooksAnomalies.delayDrop(e.w,ctor.newInstance(e.w,0d,64d,12d,stack)));
        Object entity=ctor.newInstance(e.w,0d,64d,-12d,stack);assertFalse(AmbientHooksAnomalies.delayDrop(e.w,entity));assertTrue(AmbientHooksAnomalies.holdDrop(entity));e.wc.getMethod("a",e.pc).invoke(e.w,entity);int before=n(e.w,"spawned");e.ticks(29);assertTrue(AmbientHooksAnomalies.holdDrop(entity));e.tick();assertFalse(AmbientHooksAnomalies.holdDrop(entity));assertEquals("Original item stays owned by the world",before,n(e.w,"spawned"));
    }}
    @Test public void ghostIsOneRenderOnlyAndNeverAddedToInventory()throws Exception{try(Env e=new Env()){
        e.arm(AmbientHooksAnomalies.Event.INVENTORY_GHOST);AmbientHooksAnomalies.screen(e.make("du"));Object ghost=AmbientHooksAnomalies.ghostItem(null);assertNotNull(ghost);assertEquals(278,n(ghost,"c"));assertNull(AmbientHooksAnomalies.ghostItem(null));for(Object item:(Object[])get(get(e.p,"b"),"a"))assertNull(item);
    }}
    @Test public void sunAndSkyAreVisualOnlyAndCancelCleanly()throws Exception{try(Env e=new Env()){
        e.arm(AmbientHooksAnomalies.Event.SUN_STUTTER);e.tick();assertEquals(.2f,AmbientHooksAnomalies.sun(.2f),0);e.ticks(100);assertEquals(.2f,AmbientHooksAnomalies.sun(.3f),0);AmbientHooksAnomalies.cancel(false);assertEquals(.3f,AmbientHooksAnomalies.sun(.3f),0);
    }}
    @Test public void canopyCrossCutsNineLeavesNotAllTrees()throws Exception{try(Env e=new Env()){
        for(int i=-2;i<=2;i++){e.block(i,64,-12,18);e.block(0,64,-12+i,18);}e.block(5,64,-12,18);e.arm(AmbientHooksAnomalies.Event.TREE_LEAF_GEOMETRY);e.ticks(40);assertEquals(0,e.block(0,64,-12));assertEquals(18,e.block(5,64,-12));
    }}

    @Test public void waterTorchBreathingAndCaveinUseOnlyQuietOriginalSounds()throws Exception{try(Env e=new Env()){
        e.block(0,64,-8,9);e.arm(AmbientHooksAnomalies.Event.WATER_RIPPLE);e.ticks(20);assertEquals("random.splash",e.heard);
        AmbientHooks.Scene.sheltered=true;e.arm(AmbientHooksAnomalies.Event.PHANTOM_TORCH);e.tick();assertEquals("step.wood",e.heard);
        set(e.p,"ai",32d);e.arm(AmbientHooksAnomalies.Event.BREATHING_CAVE);e.ticks(20);assertEquals("mob.cow",e.heard);assertTrue(AmbientHooks.soundVolume()<=.16f);
        e.arm(AmbientHooksAnomalies.Event.EMPTY_CAVEIN);e.tick();assertEquals("step.gravel",e.heard);
    }}
    @Test public void trailDropsAnActualTorchAndDrySandMakesSwimmingSounds()throws Exception{try(Env e=new Env()){
        e.block(0,64,-8,50);e.arm(AmbientHooksAnomalies.Event.EXTINGUISHED_TRAIL);e.ticks(20);assertEquals(0,e.block(0,64,-8));assertEquals(1,n(e.w,"spawned"));assertEquals(50,n(get(get(e.w,"figure"),"a"),"c"));
        e.block(0,63,0,12);e.arm(AmbientHooksAnomalies.Event.INVERTED_STEPS);for(int i=0;i<10;i++){set(e.p,"ah",i*.05d);e.tick();}assertEquals("random.splash",e.heard);
    }}
    @Test public void staircaseAndCrossTunnelActuallyEditBoundedLoadedStone()throws Exception{try(Env e=new Env()){
        e.block(3,64,-16,17);e.arm(AmbientHooksAnomalies.Event.STAIRCASE);e.ticks(40);assertEquals(4,e.block(0,62,-16));assertEquals(4,e.block(0,48,-30));assertEquals(0,e.block(0,63,-16));assertTrue(n(e.w,"writes")<=45);
    }try(Env e=new Env()){
        set(e.p,"ai",32d);e.arm(AmbientHooksAnomalies.Event.CROSS_TUNNEL);e.ticks(40);assertEquals(0,e.block(0,31,-16));assertEquals(50,e.block(3,31,-16));assertEquals(1,e.block(4,31,-16));
    }}
    @Test public void stationaryWaterWaitsInAnAirPocketAndDaylightDoesNotChangeWorldTime()throws Exception{try(Env e=new Env()){
        set(e.p,"ai",32d);for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=2;dy++)for(int dz=-1;dz<=1;dz++)e.block(dx,32+dy,-16+dz,0);
        e.arm(AmbientHooksAnomalies.Event.IMPOSSIBLE_WATER);e.ticks(40);assertEquals(9,e.block(0,33,-16));
        set(e.p,"ai",64d);e.ticks(20);Object sky=new Sky();long time=(Long)get(e.w,"b");e.arm(AmbientHooksAnomalies.Event.FALSE_DAYLIGHT);e.tick();AmbientHooksAnomalies.sky(sky);assertEquals(1d,((Sky)sky).c,0);assertEquals(time,(long)(Long)get(e.w,"b"));
    }}
    public static class Sky {public double a,b,c;}
    @Test public void mirrorRoomUsesRecordedCompactLayoutNotInventories()throws Exception{try(Env e=new Env()){
        set(e.p,"ai",24d);AmbientHooksState state=AmbientHooksState.of(e.w);state.baseKnown=true;state.layout=new int[]{0,0,0,1,0,1,-1,0,0};
        e.arm(AmbientHooksAnomalies.Event.MIRROR_ROOM);e.ticks(100);assertEquals(54,e.block(0,24,-16));assertEquals(61,e.block(1,24,-15));assertEquals(58,e.block(-1,24,-16));assertEquals(4,e.block(3,24,-16));assertEquals(0,e.block(2,24,-16));
    }}
    @Test public void baseMemoryAndDistantPartialReplicaUseOnlyLoadedChunks()throws Exception{try(Env e=new Env()){
        Object chest=e.make("ew"),furnace=e.make("ju");e.tile(chest,0,64,-12,54);e.tile(furnace,1,64,-12,61);e.block(-1,64,-12,58);
        for(int dx:new int[]{-2,2})for(int dy=0;dy<3;dy++)for(int dz=0;dz<2;dz++)e.block(dx,64+dy,-12+dz,5);
        e.ticks(200);AmbientHooksState state=AmbientHooksState.of(e.w);assertTrue(state.baseKnown);assertEquals(486,state.blueprint.length);assertArrayEquals(new int[]{0,0,0,1,0,0,-1,0,0},state.layout);
        set(e.w,"range",20);e.arm(AmbientHooksAnomalies.Event.BASE_REPLICA);e.ticks(100);assertTrue(n(e.w,"writes")>0);assertEquals(5,e.block(-2,63,-212));
    }}
    @Test public void scarRejectsConstructionAndCutsOnlyOneNaturalChunk()throws Exception{try(Env e=new Env()){
        set(e.w,"range",10);for(int x=0;x<16;x++)for(int z=-80;z<-64;z++){e.block(x,64,z,2);e.block(x,63,z,3);}
        e.block(0,64,-80,54);e.arm(AmbientHooksAnomalies.Event.CHUNK_SCAR);e.ticks(100);assertEquals(0,n(e.w,"writes"));e.block(0,64,-80,2);e.ticks(100);assertEquals(0,e.block(0,64,-80));assertEquals(0,e.block(15,63,-65));assertEquals(1,e.block(0,62,-80));assertTrue(n(e.w,"writes")<=512);
    }}
    @Test public void replayFollowsFifteenSecondHistoryAndRetiresOnlyWhenHidden()throws Exception{try(Env e=new Env()){
        set(e.w,"range",10);for(int i=0;i<310;i++){set(e.p,"aj",i*.15d);e.tick();}e.arm(AmbientHooksAnomalies.Event.SHADOW_REPLAY);
        for(int i=310;i<335;i++){set(e.p,"aj",i*.15d);e.tick();}assertTrue(n(e.w,"spawned")>0);Object shadow=get(e.w,"figure");assertTrue((Double)get(e.p,"aj")-(Double)get(shadow,"aj")>40);
        set(e.p,"an",180f);e.tick();AmbientHooksAnomalies.cancel(false);e.tick();assertEquals(false,get(shadow,"removed"));set(e.p,"an",0f);e.tick();assertEquals(true,get(shadow,"removed"));
    }}
    @Test public void evacuationMovesHostilesAwayFromRememberedBaseAndRestoresAi()throws Exception{try(Env e=new Env()){
        AmbientHooksState state=AmbientHooksState.of(e.w);state.baseKnown=true;state.baseX=0;state.baseY=64;state.baseZ=-12;
        Object hostile=e.loader.loadClass("lr").getConstructor(e.wc).newInstance(e.w);e.entity(hostile,4,64,-12);e.arm(AmbientHooksAnomalies.Event.BASE_EVACUATION);e.ticks(20);assertTrue((Double)get(hostile,"ah")>4);assertTrue(AmbientHooksAnomalies.freezeAnimal(hostile));AmbientHooksAnomalies.cancel(false);assertFalse(AmbientHooksAnomalies.freezeAnimal(hostile));
    }}
    @Test public void caveLoopRequiresMatchingDryWalkableGeometry()throws Exception{try(Env e=new Env()){
        set(e.w,"range",10);set(e.p,"ai",32d);for(int x=-1;x<=1;x++)for(int y=32;y<=34;y++)for(int z=-50;z<100;z++)e.block(x,y,z,0);
        for(int i=0;i<310;i++){set(e.p,"aj",i*.15d);e.tick();}double before=(Double)get(e.p,"aj");e.arm(AmbientHooksAnomalies.Event.INFINITE_LOOP);set(e.p,"aj",before+.15);e.tick();assertTrue(before-(Double)get(e.p,"aj")>18);assertEquals(32,(Double)get(e.p,"ai"),0);
    }}
    @Test public void fogWatcherUsesOriginalSkinAndWindowRequiresNightAndGlass()throws Exception{try(Env e=new Env()){
        set(e.w,"range",10);e.arm(AmbientHooksAnomalies.Event.FOG_WATCHER);e.ticks(20);Object watcher=get(e.w,"figure");assertNotNull(watcher);assertEquals(true,get(watcher,"alpha$plain"));assertEquals("/mob/char.png",get(watcher,"u"));
    }try(Env e=new Env()){
        e.block(0,64,-8,20);set(e.w,"b",6000L);e.arm(AmbientHooksAnomalies.Event.WINDOW_WATCHER);e.ticks(20);assertEquals(0,n(e.w,"spawned"));set(e.w,"b",14000L);e.ticks(20);assertTrue(n(e.w,"spawned")>0);
    }}
    @Test public void failedWriteRollsBackEarlierChangesAndMalformedMatricesFailClosed()throws Exception{try(Env e=new Env()){
        set(e.w,"failWrite",1);assertFalse(AmbientHooksAnomalies.edit(new int[][]{{0,63,-12,0},{1,63,-12,0}}));assertEquals(1,e.block(0,63,-12));assertEquals(1,e.block(1,63,-12));
        AmbientHooksVisibility.capturePlanes(new float[6][4],0,64,0);assertTrue(AmbientHooks.Scene.protectedAt(0,64,-20));
    }}

    @Test public void horizonTorchCleansUpOffscreenButNeverDeletesReplacement()throws Exception{try(Env e=new Env()){
        set(e.w,"range",10);e.block(-30,72,-52,1);e.arm(AmbientHooksAnomalies.Event.FALSE_HORIZON);e.ticks(40);assertEquals(50,e.block(-30,73,-52));e.ticks(401);assertEquals(0,e.block(-30,73,-52));
        e.arm(AmbientHooksAnomalies.Event.FALSE_HORIZON);e.ticks(40);assertEquals(50,e.block(-30,73,-52));e.block(-30,73,-52,54);AmbientHooksAnomalies.cancel(true);assertEquals(54,e.block(-30,73,-52));
    }}
    @Test public void doorBlockadeIsSingleHiddenBlockAndF7WorksWithDeferredActor()throws Exception{try(Env e=new Env()){
        e.block(0,64,-12,64);e.block(0,67,0,1);AmbientHooks.Scene.sheltered=true;e.arm(AmbientHooksAnomalies.Event.DOOR_BLOCKADE);AmbientHooks.Scene.sheltered=true;e.ticks(20);assertEquals(3,e.block(1,64,-12));assertEquals(1,n(e.w,"writes"));
        Object actor=e.loader.loadClass("AlphaWatcher").getConstructor(e.wc).newInstance(e.w);AmbientHooks.Scene.figure=actor;AmbientHooks.Scene.fx=0;AmbientHooks.Scene.fy=64;AmbientHooks.Scene.fz=16;AmbientHooks.Scene.pendingRemoval=true;
        e.arm(AmbientHooksAnomalies.Event.SILENT_WORLD);e.ticks(1);AmbientHooks.cancelKey(true);e.tick();AmbientHooks.cancelKey(false);assertNull(privateValue("event"));assertNotNull("Visible actor should be retained",AmbientHooks.Scene.figure);
    }}
    @Test public void exactClientRaycastIsConsultedAndUnloadedPathsNeverReachIt()throws Exception{try(Env e=new Env()){
        assertTrue(AmbientHooks.Scene.lineClear(0,65,0,0,65,-12));int calls=n(e.w,"rayCalls");assertTrue(calls>0);set(e.w,"rayBlocked",true);assertFalse(AmbientHooks.Scene.lineClear(0,65,0,0,65,-12));assertEquals(calls+1,n(e.w,"rayCalls"));
        calls=n(e.w,"rayCalls");assertFalse(AmbientHooks.Scene.lineClear(0,65,0,0,65,-200));assertEquals(calls,n(e.w,"rayCalls"));
    }}
    @Test public void frustumSnapshotCopiesPlanesAndRejectsStaleCameraPositions()throws Exception{
        float[][] planes={{0,0,1,0},{1,0,0,100},{-1,0,0,100},{0,1,0,100},{0,-1,0,100},{0,0,-1,100}};
        AmbientHooksVisibility.capturePlanes(planes,0,64,0);assertTrue(AmbientHooksVisibility.available(0,64,0));assertFalse(AmbientHooksVisibility.intersects(0,64,-20,1,66,-19));planes[0][2]=-1;assertFalse(AmbientHooksVisibility.intersects(0,64,-20,1,66,-19));assertFalse(AmbientHooksVisibility.available(5,64,0));AmbientHooksVisibility.clear();assertTrue(AmbientHooksVisibility.intersects(0,64,-20,1,66,-19));
    }

    @Test public void f8PreviewsNewRulesWithoutWaitingForCorruptionAndIsEdgeTriggered()throws Exception{try(Env e=new Env()){
        SceneIntegrationTest.set("newCursor",0);AmbientHooks.anomalyPreviewKey(false);AmbientHooks.anomalyPreviewKey(true);AmbientHooks.nextSound(true);assertEquals("TOOL_DRIFT",AmbientHooks.lastEventName());
        AmbientHooks.anomalyPreviewKey(true);AmbientHooks.nextSound(true);assertEquals("TOOL_DRIFT",AmbientHooks.lastEventName());AmbientHooks.anomalyPreviewKey(false);AmbientHooks.anomalyPreviewKey(true);AmbientHooks.nextSound(true);assertEquals("CHEST_SHUFFLE",AmbientHooks.lastEventName());AmbientHooks.anomalyPreviewKey(false);
    }}

    @Test public void terrainTransactionDoesNotCarveThroughExistingAnimalsOrItems()throws Exception{try(Env e=new Env()){
        Object pig=e.make("mj");e.entity(pig,0,64,-12);assertFalse(AmbientHooksAnomalies.edit(new int[][]{{0,63,-12,0}}));assertEquals(1,e.block(0,63,-12));assertEquals(0,n(e.w,"writes"));
    }}
}
