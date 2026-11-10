package alpha.ambient;

import java.io.*;
import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Runs our real scene adapter against authored API stand-ins, not proprietary client code. */
public class SceneIntegrationTest {
    static class Loader extends ClassLoader {
        protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(name.equals("AlphaWatcher")||name.equals("cj")||name.equals("jx")||name.equals("lr")||name.equals("hc")||name.equals("ff")){
                Class<?> c=findLoadedClass(name);if(c==null){try{
                    byte[] bytes;if(name.equals("AlphaWatcher"))bytes=OfflinePatcher.watcherClass();else try(InputStream in=getClass().getResourceAsStream("/"+name+".class")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();}
                    c=defineClass(name,bytes,0,bytes.length);
                }catch(Exception e){throw new ClassNotFoundException(name,e);}}if(resolve)resolveClass(c);return c;
            }return super.loadClass(name,resolve);
        }
    }
    static void set(String name,Object value)throws Exception{Field field=AmbientHooks.class.getDeclaredField(name);field.setAccessible(true);field.set(null,value);}
    static void reset()throws Exception{AmbientHooks.scene(null,null);set("previewCursor",0);set("forceEvent",false);set("replaceScene",false);set("encounterRequest",-1);set("activeTicks",0L);set("quietTicks",20000);set("echoes",0);AmbientHooks.previewKey(false);}
    static void tick(Object world,Object player){AmbientHooks.scene(world,player);AmbientHooks.nextSound(true);}
    @Test public void realAdapterSpawnsMovesFreezesAndCleansUp()throws Exception{
        reset();Loader loader=new Loader();Class<?> worldType=loader.loadClass("cj"),playerType=loader.loadClass("jx");Object world=worldType.getConstructor().newInstance(),player=playerType.getConstructor().newInstance();
        try{
            tick(world,player);AmbientHooks.previewKey(true);AmbientHooks.nextSound(true);AmbientHooks.previewKey(false);
            for(int i=0;i<75;i++)tick(world,player);
            Object figure=worldType.getField("figure").get(world);assertNotNull("director scene did not spawn",figure);assertEquals(1,worldType.getField("spawned").getInt(world));
            double before=playerType.getField("aj").getDouble(figure);for(int i=0;i<12;i++)tick(world,player);double after=playerType.getField("aj").getDouble(figure);assertTrue("unwatched figure never moved",after<before);
            double fx=playerType.getField("ah").getDouble(figure),fz=playerType.getField("aj").getDouble(figure);playerType.getField("an").setFloat(player,(float)Math.toDegrees(Math.atan2(-fx,fz)));
            // The pitch correction aims at its torso, not over/under the target.
            playerType.getField("ao").setFloat(player,(float)Math.toDegrees(Math.atan2(.62,Math.hypot(fx,fz))));
            for(int i=0;i<8;i++)tick(world,player);double frozen=playerType.getField("aj").getDouble(figure);assertEquals(after,frozen,.0001);
            AmbientHooks.cancelKey(true);tick(world,player);AmbientHooks.cancelKey(false);assertTrue(playerType.getField("removed").getBoolean(figure));
            assertTrue("unbounded reflection/block scan",worldType.getField("reads").getInt(world)<30000);
        }finally{reset();}
    }
    @Test public void wallOcclusionIsNotMistakenForPlayerObservation()throws Exception{
        reset();Loader loader=new Loader();Class<?> wc=loader.loadClass("cj"),pc=loader.loadClass("jx");Object world=wc.getConstructor().newInstance(),player=pc.getConstructor().newInstance();wc.getField("wall").setBoolean(world,true);
        try{
            tick(world,player);AmbientHooks.previewKey(true);AmbientHooks.nextSound(true);AmbientHooks.previewKey(false);for(int i=0;i<75;i++)tick(world,player);
            Object figure=wc.getField("figure").get(world);assertNotNull(figure);double fx=pc.getField("ah").getDouble(figure),fz=pc.getField("aj").getDouble(figure);
            pc.getField("an").setFloat(player,(float)Math.toDegrees(Math.atan2(-fx,fz)));pc.getField("ao").setFloat(player,(float)Math.toDegrees(Math.atan2(.62,Math.hypot(fx,fz))));
            for(int i=0;i<12;i++)tick(world,player);assertTrue(pc.getField("aj").getDouble(figure)<fz);
        }finally{reset();}
    }
    @Test public void obstacleEndsMovementRatherThanWalkingThroughBlocks()throws Exception{
        reset();Loader loader=new Loader();Class<?> wc=loader.loadClass("cj"),pc=loader.loadClass("jx");Object world=wc.getConstructor().newInstance(),player=pc.getConstructor().newInstance();
        try{
            tick(world,player);AmbientHooks.previewKey(true);AmbientHooks.nextSound(true);AmbientHooks.previewKey(false);for(int i=0;i<75;i++)tick(world,player);
            Object figure=wc.getField("figure").get(world);assertNotNull(figure);wc.getField("blockedZ").setInt(world,(int)Math.floor(pc.getField("aj").getDouble(figure))-1);
            for(int i=0;i<24;i++)tick(world,player);assertTrue(pc.getField("removed").getBoolean(figure));
        }finally{reset();}
    }
    @Test public void unloadedPlacementNeverReadsOrGeneratesTheChunk()throws Exception{
        reset();Loader loader=new Loader();Class<?> wc=loader.loadClass("cj"),pc=loader.loadClass("jx");Object world=wc.getConstructor().newInstance(),player=pc.getConstructor().newInstance();pc.getField("ah").setDouble(player,200);
        try{
            tick(world,player);AmbientHooks.previewKey(true);AmbientHooks.nextSound(true);AmbientHooks.previewKey(false);for(int i=0;i<80;i++)tick(world,player);
            assertEquals(0,wc.getField("spawned").getInt(world));assertEquals(0,wc.getField("reads").getInt(world));
        }finally{reset();}
    }
    @Test public void queuedSoundSourceStaysFixedWhenPlayerMoves()throws Exception{
        reset();Loader loader=new Loader();Class<?> wc=loader.loadClass("cj"),pc=loader.loadClass("jx");Object world=wc.getConstructor().newInstance(),player=pc.getConstructor().newInstance();
        try{
            tick(world,player);AmbientHooks.previewKey(true);AmbientHooks.nextSound(true);AmbientHooks.previewKey(false);
            tick(world,player);double anchor=AmbientHooks.soundX(0);
            pc.getField("ah").setDouble(player,3);
            for(int i=0;i<22;i++)tick(world,player);
            assertEquals(anchor,AmbientHooks.soundX(0)+3,.0001);
        }finally{reset();}
    }
}
