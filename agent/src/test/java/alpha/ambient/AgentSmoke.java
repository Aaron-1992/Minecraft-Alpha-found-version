package alpha.ambient;

/** Runs in a fresh JVM with -javaagent to exercise actual instrumentation/loader isolation. */
public final class AgentSmoke {
    public static void main(String[] args)throws Exception{
        byte[] blocks=new byte[32768];
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=60;y++)blocks[(x<<11)|(z<<7)|y]=(byte)(y==0?7:y==60?2:1);
        int cx=0,cz=0;boolean found=false;
        outer:for(int x=4;x<200;x++)for(int z=4;z<200;z++){
            long h=mix(x,z);if((h&127)==0&&((h>>>9)%4)==0){cx=x;cz=z;found=true;break outer;}
        }
        if(!found)throw new AssertionError("Fixture coordinates missing");
        Chunk chunk=new Chunk(new Object(),blocks,cx,cz);
        java.lang.reflect.Field field=AmbientHooks.class.getDeclaredField("pending");field.setAccessible(true);
        java.util.Map<?,?> pending=(java.util.Map<?,?>)field.get(null);
        if(!pending.containsKey(chunk))throw new AssertionError("Constructor hook did not register chunk");
        chunk.initialize();
        if(pending.containsKey(chunk))throw new AssertionError("Ready hook did not consume chunk");
        if(chunk.calls!=1||blocks[(8<<11)|(8<<7)|52]!=1)throw new AssertionError("Live agent hook did not execute");
        if(AmbientHooks.class.getClassLoader()!=null)throw new AssertionError("Hooks were not made visible to isolated loaders");
        System.out.println("Agent JVM smoke test passed: live chunk instrumentation and bootstrap hook resolution.");
    }
    private static long mix(int x,int z){long n=x*341873128712L+z*132897987541L+0x5b7c913dL;n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return (n^(n>>>31))&Long.MAX_VALUE;}
    public static class Chunk {
        public byte[] blocks;public int calls;
        public Chunk(Object world,byte[] blocks,int x,int z){this.blocks=blocks;}
        public void initialize(){calls++;}
    }
}
