package alpha.ambient;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.Arrays;

/** Original code fixtures, not Minecraft bytecode or generated media. */
public class AmbientAgentTest {
    private static int index(int x,int y,int z){return (x<<11)|(z<<7)|y;}
    private static byte[] terrain(){byte[] b=new byte[32768];for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=60;y++)b[index(x,y,z)]=(byte)(y==0?7:y==60?2:1);return b;}
    @Test public void tunnelIsSmallAndUnderground(){
        byte[] b=terrain(),before=b.clone();assertTrue(AmbientHooks.apply(b,10,10,0));int edits=0;
        for(int i=0;i<b.length;i++)if(b[i]!=before[i]){edits++;assertEquals(0,b[i]);assertEquals(1,before[i]);}
        assertEquals(48,edits);assertEquals(7,b[index(8,0,8)]);assertFalse(AmbientHooks.apply(b,10,10,0));
    }
    @Test public void manufacturedBlocksProtectWholeChunk(){
        for(int id:new int[]{4,5,20,50,54,58,61,62,63,68,64,71}){
            byte[] b=terrain();b[index(0,65,0)]=(byte)id;byte[] before=b.clone();assertFalse(AmbientHooks.apply(b,10,10,0));assertArrayEquals(before,b);
        }
    }
    @Test public void leavesOnlyNoReplacementSkin(){
        byte[] b=terrain();for(int y=61;y<=65;y++)b[index(8,y,8)]=17;
        for(int x=6;x<=10;x++)for(int z=6;z<=10;z++)b[index(x,66,z)]=18;
        byte[] before=b.clone();assertFalse(AmbientHooks.apply(b,10,10,1));assertArrayEquals(before,b);
        for(int i=0;i<b.length;i++)if(b[i]!=before[i]){assertEquals(18,before[i]);assertEquals(0,b[i]);}
        assertEquals(17,b[index(8,65,8)]);
    }
    @Test public void sandStructureUsesOriginalSandAndStaysBounded(){
        byte[] b=terrain();for(int x=0;x<16;x++)for(int z=0;z<16;z++)b[index(x,60,z)]=12;
        assertTrue(AmbientHooks.apply(b,10,10,2));assertEquals(12,b[index(8,63,8)]);assertEquals(0,b[index(8,64,8)]);assertFalse(AmbientHooks.apply(b,10,10,2));
    }
    @Test public void seamUsesExistingGravelId(){byte[] b=terrain();assertTrue(AmbientHooks.apply(b,10,10,3));assertEquals(13,b[index(8,55,8)]);assertFalse(AmbientHooks.apply(b,10,10,3));}
    @Test public void invalidChunkArraysAreIgnored(){assertFalse(AmbientHooks.apply(new byte[10],0,0,0));assertFalse(AmbientHooks.apply(null,0,0,0));}
    @Test public void fogHasNoSuddenJumpAndLeavesOtherParametersAlone(){
        float previous=AmbientHooks.fogAt(0x0B64,100,0);assertEquals(100,previous,0);
        for(int seconds=1;seconds<3600;seconds++){
            float value=AmbientHooks.fogAt(0x0B64,100,seconds);assertTrue(value>=96&&value<=100);assertTrue(Math.abs(value-previous)<.03);previous=value;
            assertEquals(100,AmbientHooks.fogAt(0x0B62,100,seconds),0);
            assertTrue(AmbientHooks.fogAt(0x0B63,20,seconds)<value);
        }
    }
    @Test public void soundScheduleIsSparseAndInactiveTicksAreSilent(){
        for(int i=0;i<20000;i++)assertNull(AmbientHooks.nextSound(false));
        System.setProperty("alpha.scheduledSounds","true");
        int events=0;
        for(int i=0;i<20*60*60;i++){
            String sound=AmbientHooks.nextSound(true);
            if(sound!=null){events++;assertTrue(java.util.Arrays.asList("step.grass","step.stone","step.gravel","random.pop","step.wood").contains(sound));}
        }
        System.clearProperty("alpha.scheduledSounds");
        assertTrue(events>=10&&events<=55);
        assertEquals(6,Math.hypot(AmbientHooks.soundX(80),AmbientHooks.soundZ(80)),.0001);
    }
    @Test public void previewIsEdgeTriggeredAndEscalationIsBounded(){
        System.setProperty("alpha.scheduledSounds","true");
        try{
            AmbientHooks.previewKey(false);AmbientHooks.previewKey(true);
            assertNotNull(AmbientHooks.nextSound(true));assertTrue(AmbientHooks.takeParticle());
            AmbientHooks.previewKey(true);assertNull(AmbientHooks.nextSound(true));
            AmbientHooks.previewKey(false);AmbientHooks.previewKey(true);assertNotNull(AmbientHooks.nextSound(true));
            AmbientHooks.previewKey(false);
            for(int i=0;i<100;i++){
                int early=AmbientHooks.intervalSeconds(0),late=AmbientHooks.intervalSeconds(20L*60*40);
                assertTrue(early>=240&&early<=540);assertTrue(late>=45&&late<=120);
            }
        }finally{System.clearProperty("alpha.scheduledSounds");}
    }
    @Test public void nonSelectedAndSpawnChunksStayUnchanged(){
        byte[] b=terrain(),before=b.clone();Object chunk=new Object();AmbientHooks.register(chunk,b,0,0);AmbientHooks.ready(chunk);assertArrayEquals(before,b);
    }
    @Test public void transformedChunkRetainsOriginalBehaviorAndRunsHook()throws Exception{
        String name=FixtureChunk.class.getName();byte[] bytes=resource(name);
        byte[] changed=new AmbientTransformer(name.replace('.','/')).instrument(name.replace('.','/'),bytes);
        Class<?> fixture=new Loader().define(name,changed);
        int x=4,z=4;boolean found=false;
        outer:for(int a=4;a<200;a++)for(int c=4;c<200;c++){long h=AmbientHooks.mix(a,c);if((h&127)==0&&((h>>>9)%4)==0){x=a;z=c;found=true;break outer;}}
        assertTrue(found);byte[] b=terrain();Object object=fixture.getConstructor(Object.class,byte[].class,int.class,int.class).newInstance(new Object(),b,x,z);
        fixture.getMethod("initialize").invoke(object);
        assertEquals(1,fixture.getField("calls").getInt(object));assertEquals(0,b[index(8,52,8)]);
        assertEquals(52,changed[7]&255); // Java 8 classfile, not the setup app's Java 17.
    }
    @Test public void transformedFogRetainsOriginalMethod()throws Exception{
        String name=FixtureFog.class.getName();byte[] changed=new AmbientTransformer("not/the/chunk").instrument("org/lwjgl/opengl/GL11",resource(name));
        Class<?> fixture=new Loader().define(name,changed);fixture.getMethod("glFogf",int.class,float.class).invoke(null,0x0B62,40f);
        assertEquals(40f,fixture.getField("captured").getFloat(null),0);
    }
    @Test public void unrelatedClassesAreNotTransformed(){assertNull(new AmbientTransformer("chunk").transform(null,"unrelated",null,null,new byte[0]));}
    @Test public void malformedTargetBytecodeFailsOpen(){
        assertNull(new AmbientTransformer("chunk").transform(null,"chunk",null,null,new byte[0]));
    }
    @Test public void bootstrapArchiveContainsOnlyOurHookClasses()throws Exception{
        InputStream resource=AmbientAgentTest.class.getResourceAsStream("/alpha/ambient/bootstrap-hooks.jar");assertNotNull(resource);int classes=0;
        try(java.util.zip.ZipInputStream zip=new java.util.zip.ZipInputStream(resource)){
            java.util.zip.ZipEntry entry;while((entry=zip.getNextEntry())!=null){
                if(entry.getName().endsWith(".class")){classes++;assertTrue(entry.getName().startsWith("alpha/ambient/AmbientHooks"));}
            }
        }assertTrue(classes>=2);
    }
    public static class FixtureChunk {
        public byte[] blocks;public int calls;
        public FixtureChunk(Object world,byte[] blocks,int x,int z){this.blocks=blocks;}
        public void initialize(){calls++;}
    }
    public static class FixtureFog {public static float captured;public static void glFogf(int parameter,float value){captured=value;}}
    private static class Loader extends ClassLoader {Class<?> define(String name,byte[] bytes){return defineClass(name,bytes,0,bytes.length);}}
    private static byte[] resource(String name)throws IOException{
        InputStream in=AmbientAgentTest.class.getResourceAsStream("/"+name.replace('.','/')+".class");ByteArrayOutputStream out=new ByteArrayOutputStream();
        try{byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}finally{in.close();}return out.toByteArray();
    }
}
