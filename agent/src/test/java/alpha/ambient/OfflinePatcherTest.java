package alpha.ambient;

import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.objectweb.asm.*;

public class OfflinePatcherTest {
    static byte[] fixture(String name,String method,String descriptor){
        ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V1_6,Opcodes.ACC_PUBLIC,name,null,"java/lang/Object",null);
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|(name.contains("Minecraft")?Opcodes.ACC_STATIC:0),method,descriptor,null,null);
        m.visitCode();if(descriptor.endsWith("Ljava/io/File;"))m.visitInsn(Opcodes.ACONST_NULL);
        m.visitInsn(descriptor.endsWith("Ljava/io/File;")?Opcodes.ARETURN:Opcodes.RETURN);m.visitMaxs(1,1);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
    static List<String> calls(byte[] bytes){
        final List<String> result=new ArrayList<String>();new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){
            public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){return new MethodVisitor(Opcodes.ASM9){
                public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf){result.add(owner+"."+name+desc);}
            };}
        },0);return result;
    }
    @Test public void cachedSoundsRegisterWithoutHttp(){
        List<String> calls=calls(OfflinePatcher.patchClass("bb",fixture("bb","run","()V"),"fr"));
        assertEquals(Collections.singletonList("bb.a(Ljava/io/File;Ljava/lang/String;)V"),calls);
    }
    @Test public void gameDirectoryIsExplicit(){
        List<String> calls=calls(OfflinePatcher.patchClass("net/minecraft/client/Minecraft",fixture("net/minecraft/client/Minecraft","b","()Ljava/io/File;"),"fr"));
        assertTrue(calls.contains("java/lang/System.getProperty(Ljava/lang/String;)Ljava/lang/String;"));
        assertFalse(calls.toString().contains("getenv"));
    }
    @Test public void fogIsPatchedInGameNotLwjgl(){
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V1_6,Opcodes.ACC_PUBLIC,"FogFixture",null,"java/lang/Object",null);
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"render","()V",null,null);m.visitCode();m.visitIntInsn(Opcodes.SIPUSH,2915);m.visitInsn(Opcodes.FCONST_1);
        m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/lwjgl/opengl/GL11","glFogf","(IF)V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();
        List<String> calls=calls(OfflinePatcher.patchClass("FogFixture",w.toByteArray(),"fr"));
        assertEquals(Collections.singletonList("org/lwjgl/opengl/GL11.glFogf(IF)V"),calls);
    }
    @Test public void frameCapPrecedesDisplayUpdate(){
        String game="net/minecraft/client/Minecraft";
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V1_5,Opcodes.ACC_PUBLIC,game,null,"java/lang/Object",null);
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"run","()V",null,null);m.visitCode();m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/lwjgl/opengl/Display","update","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();
        List<String> calls=calls(OfflinePatcher.patchClass(game,w.toByteArray(),"fr"));
        assertEquals("org/lwjgl/opengl/Display.sync(I)V",calls.get(0));assertEquals("org/lwjgl/opengl/Display.update()V",calls.get(1));
    }
    @Test public void unknownClientFailsBeforeOutputCreation()throws Exception{
        Path root=Files.createTempDirectory("alpha-patch-test");Path in=root.resolve("original.jar"),out=root.resolve("patched.jar");
        try{Files.write(in,new byte[]{1,2,3});try{OfflinePatcher.patch(in,out,"fr");fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("fingerprint"));}assertFalse(Files.exists(out));assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(in));}
        finally{Files.deleteIfExists(in);Files.deleteIfExists(out);Files.delete(root);}
    }
}
