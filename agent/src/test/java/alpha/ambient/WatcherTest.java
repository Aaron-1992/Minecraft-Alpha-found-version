package alpha.ambient;

import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.objectweb.asm.*;

/** Authored stand-ins only: no Minecraft source/assets, native context or client dependency. */
public class WatcherTest {
    static class Loader extends ClassLoader{Class<?> put(String name,byte[] bytes){return defineClass(name,bytes,0,bytes.length);}}
    static ClassWriter type(String name){ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V1_5,Opcodes.ACC_PUBLIC,name,null,"java/lang/Object",null);return w;}
    static byte[] simple(String name,boolean parent){
        ClassWriter w=type(name);MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>",parent?"(Lcj;)V":"()V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
    static byte[] gl(){
        String name="org/lwjgl/opengl/GL11";ClassWriter w=type(name);w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"depth","I",null,null).visitEnd();
        for(String call:new String[]{"glPushAttrib","glPopAttrib","glColor4f"}){
            MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,call,call.equals("glPushAttrib")?"(I)V":call.equals("glPopAttrib")?"()V":"(FFFF)V",null,null);m.visitCode();
            if(!call.equals("glColor4f")){m.visitFieldInsn(Opcodes.GETSTATIC,name,"depth","I");m.visitInsn(Opcodes.ICONST_1);m.visitInsn(call.equals("glPushAttrib")?Opcodes.IADD:Opcodes.ISUB);m.visitFieldInsn(Opcodes.PUTSTATIC,name,"depth","I");}
            m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,4);m.visitEnd();
        }w.visitEnd();return w.toByteArray();
    }
    static byte[] model(){
        ClassWriter w=type("ff");w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"fail","Z",null,null).visitEnd();
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(Opcodes.ACC_PUBLIC,"b","(FFFFFF)V",null,null);m.visitCode();Label ok=new Label();m.visitFieldInsn(Opcodes.GETSTATIC,"ff","fail","Z");m.visitJumpInsn(Opcodes.IFEQ,ok);m.visitTypeInsn(Opcodes.NEW,"java/lang/RuntimeException");m.visitInsn(Opcodes.DUP);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/RuntimeException","<init>","()V",false);m.visitInsn(Opcodes.ATHROW);m.visitLabel(ok);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
    @Test public void watcherIsNonCollidingNonPersistentAndRestoresRenderStateEvenOnFailure()throws Exception{
        Loader l=new Loader();Class<?> world=l.put("cj",simple("cj",false));Class<?> nbt=l.put("hc",simple("hc",false));l.put("jx",simple("jx",false));l.put("lr",simple("lr",true));Class<?> gl=l.put("org.lwjgl.opengl.GL11",gl());Class<?> model=l.put("ff",model());Class<?> watcher=l.put("AlphaWatcher",OfflinePatcher.watcherClass());Object entity=watcher.getConstructor(world).newInstance(new Object[]{null});
        assertEquals(false,watcher.getMethod("c_").invoke(entity));assertEquals(false,watcher.getMethod("d_").invoke(entity));assertEquals(false,watcher.getMethod("c",nbt).invoke(entity,new Object[]{null}));watcher.getMethod("e_").invoke(entity);
        Method draw=watcher.getMethod("draw",model,float.class,float.class,float.class,float.class,float.class,float.class,Object.class);Object mesh=model.getConstructor().newInstance();
        draw.invoke(null,mesh,0f,0f,0f,0f,0f,0f,entity);assertEquals(0,gl.getField("depth").getInt(null));
        model.getField("fail").setBoolean(null,true);
        try{draw.invoke(null,mesh,0f,0f,0f,0f,0f,0f,entity);fail();}catch(InvocationTargetException expected){assertTrue(expected.getCause() instanceof RuntimeException);}
        assertEquals(0,gl.getField("depth").getInt(null));
    }
}
