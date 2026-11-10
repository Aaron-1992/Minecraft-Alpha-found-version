package alpha.ambient;

import java.io.*;

/** Standalone preflight. Exercises our own fixture, never loads Minecraft or its account data. */
public final class AgentProbe {
    public static void main(String[] args)throws Exception{
        if(AmbientHooks.class.getClassLoader()!=null)throw new AssertionError("Bootstrap hooks unavailable");
        for(String name:new String[]{"org.objectweb.asm.ClassReader","alpha.ambient.internal.asm.ClassReader"}){
            try{Class.forName(name,false,null);throw new AssertionError("Instrumentation library leaked into bootstrap: "+name);}
            catch(ClassNotFoundException expected){}
        }
        byte[] bytes=resource();Class<?> isolated=new IsolatedLoader().define(bytes);
        byte[] blocks=new byte[32768];
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=60;y++)blocks[(x<<11)|(z<<7)|y]=(byte)(y==0?7:y==60?2:1);
        int cx=0,cz=0;boolean found=false;
        outer:for(int x=4;x<200;x++)for(int z=4;z<200;z++){
            long h=mix(x,z);if((h&127)==0&&((h>>>9)%4)==0){cx=x;cz=z;found=true;break outer;}
        }
        if(!found)throw new AssertionError("Probe coordinates unavailable");
        Object chunk=isolated.getConstructor(Object.class,byte[].class,int.class,int.class).newInstance(new Object(),blocks,cx,cz);
        java.lang.reflect.Field field=AmbientHooks.class.getDeclaredField("pending");field.setAccessible(true);
        java.util.Map<?,?> pending=(java.util.Map<?,?>)field.get(null);
        if(!pending.containsKey(chunk))throw new AssertionError("Isolated constructor hook missing");
        isolated.getMethod("initialize").invoke(chunk);
        if(pending.containsKey(chunk))throw new AssertionError("Isolated ready hook missing");
        if(isolated.getField("calls").getInt(chunk)!=1||blocks[(8<<11)|(8<<7)|52]!=1)throw new AssertionError("Isolated-loader instrumentation failed");
        System.out.println("ALPHA_AGENT_PREFLIGHT_OK Java="+System.getProperty("java.version")+" isolatedLoader=true bootstrapASM=false");
    }
    private static byte[] resource()throws IOException{
        try(InputStream in=AgentProbe.class.getResourceAsStream("/alpha/ambient/AgentProbe$Chunk.class");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException("Packaged preflight class missing");byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();
        }
    }
    private static long mix(int x,int z){long n=x*341873128712L+z*132897987541L+0x5b7c913dL;n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return (n^(n>>>31))&Long.MAX_VALUE;}
    private static final class IsolatedLoader extends ClassLoader {
        IsolatedLoader(){super(null);}
        Class<?> define(byte[] bytes){return defineClass("alpha.ambient.AgentProbe$Chunk",bytes,0,bytes.length);}
    }
    public static class Chunk {
        public byte[] blocks;public int calls;
        public Chunk(Object world,byte[] blocks,int x,int z){this.blocks=blocks;}
        public void initialize(){calls++;}
    }
}
