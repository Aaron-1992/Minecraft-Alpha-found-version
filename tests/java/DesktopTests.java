import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Headless engine tests. No fabricated textures, recordings, or game asset fixtures. */
public final class DesktopTests {
    private static int passed;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void test(String name,Task task)throws Exception{task.run();passed++;System.out.println("PASS "+name);}
    private interface Task{void run()throws Exception;}
    public static void main(String[] args)throws Exception{
        test("seed determinism and variation",()->{
            VoxelWorld a=new VoxelWorld(27419),b=new VoxelWorld(27419),c=new VoxelWorld(983);
            check(Arrays.equals(a.blocks,b.blocks),"equal seeds differ");
            check(!Arrays.equals(a.blocks,c.blocks),"different seeds identical");
        });
        test("ground, water, trees and clear spawn",()->{
            VoxelWorld w=new VoxelWorld(27419);int water=0,logs=0;
            for(int x=0;x<VoxelWorld.SIZE;x++)for(int z=0;z<VoxelWorld.SIZE;z++){
                check(w.get(x,0,z)==3,"ground missing");
                for(int y=0;y<VoxelWorld.HEIGHT;y++){int b=w.get(x,y,z);if(b==10)water++;if(b==4)logs++;}
            }
            check(water>100,"water missing");check(logs>100,"trees missing");check(!w.collides(w.x,w.y,w.z),"spawn obstructed");
        });
        test("safe block edits and collision",()->{
            VoxelWorld w=new VoxelWorld(12);w.set(20,40,20,9);check(w.get(20,40,20)==9,"edit lost");
            check(w.collides(20.5,41,20.5),"collision missing");w.set(20,40,20,0);
            check(!w.collides(20.5,41,20.5),"removed block still collides");
            w.set(-1,99,0,8);check(w.get(-1,99,0)==0,"overflow edit");
            check(!VoxelWorld.solid(10)&&VoxelWorld.solid(5),"water/leaves solidity");
        });
        test("compressed save preserves blocks and player",()->{
            Path p=Files.createTempFile("alpha-world-test-",".dat");
            try{
                VoxelWorld w=new VoxelWorld(991);w.set(22,39,22,7);w.yaw=1.25;w.pitch=-.3;w.selected=8;w.save(p);
                VoxelWorld loaded=VoxelWorld.load(p);
                check(Arrays.equals(w.blocks,loaded.blocks),"blocks changed");check(loaded.seed==w.seed,"seed changed");
                check(loaded.x==w.x&&loaded.y==w.y&&loaded.z==w.z&&loaded.yaw==1.25&&loaded.pitch==-.3&&loaded.selected==8,"player changed");
            }finally{Files.deleteIfExists(p);}
        });
        test("reject corrupt save rather than regenerating it",()->{
            Path p=Files.createTempFile("alpha-bad-save-",".dat");
            try{Files.writeString(p,"not a world");try{VoxelWorld.load(p);throw new AssertionError("bad save accepted");}catch(IOException expected){}}
            finally{Files.deleteIfExists(p);}
        });
        test("DDA hits block face with placement normal",()->{
            VoxelWorld w=new VoxelWorld(3);Arrays.fill(w.blocks,(byte)0);w.set(22,30,20,3);
            VoxelRenderer.Hit h=new VoxelRenderer.Hit();
            check(VoxelRenderer.trace(w,20.5,30.5,20.5,1,0,0,5,false,h),"ray missed");
            check(h.x==22&&h.y==30&&h.z==20&&h.axis==0&&h.normal==-1,"wrong face");check(Math.abs(h.t-1.5)<1e-8,"wrong distance");
            check(!VoxelRenderer.trace(w,20.5,30.5,20.5,-1,0,0,5,false,h),"ray hit behind");
        });
        test("water visibility and targeting are separate",()->{
            VoxelWorld w=new VoxelWorld(3);Arrays.fill(w.blocks,(byte)0);w.set(21,30,20,10);w.set(23,30,20,3);
            VoxelRenderer.Hit h=new VoxelRenderer.Hit();
            check(VoxelRenderer.trace(w,20.5,30.5,20.5,1,0,0,5,false,h)&&h.block==10,"water not rendered");
            check(VoxelRenderer.trace(w,20.5,30.5,20.5,1,0,0,5,true,h)&&h.block==3,"water prevents mining");
        });
        test("legacy terrain atlas addresses",()->{
            check(VoxelRenderer.tile(1,1,1)==0,"grass top");check(VoxelRenderer.tile(1,1,-1)==2,"grass bottom");
            check(VoxelRenderer.tile(1,0,1)==3,"grass side");check(VoxelRenderer.tile(4,1,1)==21,"log top");
            check(VoxelRenderer.tile(4,2,1)==20,"log side");
        });
        test("missing original assets fail explicitly without fallback",()->{
            Path p=Files.createTempFile("alpha-empty-client-",".jar");
            try{
                try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(p))){zip.finish();}
                try(MinecraftAssets ignored=new MinecraftAssets(p,p.getParent(),null)){throw new AssertionError("empty jar accepted");}
                catch(IOException expected){check(expected.getMessage().contains("terrain.png"),"unclear asset error");}
            }finally{Files.deleteIfExists(p);}
        });
        System.out.println(passed+" desktop tests passed.");
    }
}
