package alpha.ambient;

import java.util.Map;
import java.util.WeakHashMap;

/** Java 8 hooks. No UI, audio, textures, entities, network, reflection or file access. */
public final class AmbientHooks {
    private static final Map<Object,Pending> pending=new WeakHashMap<Object,Pending>();
    private static final java.util.Random atmosphere=new java.util.Random();
    private static int quietTicks=20*(240+atmosphere.nextInt(301));
    /** Called on the game tick, not a render thread or timer. Menus cannot trigger audio. */
    public static synchronized String nextSound(boolean active){
        if(!active||!Boolean.parseBoolean(System.getProperty("alpha.scheduledSounds","false")))return null;
        if(--quietTicks>0)return null;
        quietTicks=20*(240+atmosphere.nextInt(301));
        return new String[]{"step.grass","step.stone","step.gravel"}[atmosphere.nextInt(3)];
    }
    public static double soundX(float yaw){return -Math.sin(Math.toRadians(yaw+135))*6;}
    public static double soundZ(float yaw){return Math.cos(Math.toRadians(yaw+135))*6;}
    private static final class Pending {
        final byte[] blocks;final int x,z,kind;int attempts;
        Pending(byte[] blocks,int x,int z,int kind){this.blocks=blocks;this.x=x;this.z=z;this.kind=kind;}
    }
    private AmbientHooks(){}
    public static float fog(int parameter,float original){
        return original; // Graphics effects disabled following a reported AMD driver timeout.
    }
    public static float fogAt(int parameter,float original,double seconds){return original;}
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
