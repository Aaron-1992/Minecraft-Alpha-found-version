package alpha.ambient;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/** Instruments a pre-inspected Alpha chunk class and LWJGL's fog entry point only. */
public final class AmbientTransformer implements ClassFileTransformer {
    private final String chunkClass;
    public AmbientTransformer(String chunkClass){this.chunkClass=chunkClass;}
    public byte[] transform(ClassLoader loader,String name,Class<?> oldClass,ProtectionDomain domain,byte[] bytes){
        if(name==null)return null;
        if(!name.equals(chunkClass)&&!name.equals("org/lwjgl/opengl/GL11"))return null;
        try{return instrument(name,bytes);}catch(RuntimeException failure){
            System.err.println("[Alpha ambient] Optional hook skipped for "+name+": "+failure.getClass().getSimpleName());return null;
        }
    }
    public byte[] instrument(final String name,byte[] original){
        final boolean chunk=name.equals(chunkClass);
        ClassReader reader=new ClassReader(original);
        // Hooks have balanced stacks and no branches, so existing frame types are preserved.
        ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
            public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions){
                MethodVisitor mv=super.visitMethod(access,method,descriptor,signature,exceptions);
                if((access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT))!=0)return mv;
                final boolean constructor=chunk&&method.equals("<init>")&&descriptor.matches("\\(L[^;]+;\\[BII\\)V");
                final boolean ready=chunk&&!method.startsWith("<")&&(access&Opcodes.ACC_STATIC)==0&&descriptor.equals("()V");
                final boolean fog=!chunk&&method.equals("glFogf")&&descriptor.equals("(IF)V")&&(access&Opcodes.ACC_STATIC)!=0;
                if(!constructor&&!ready&&!fog)return mv;
                return new AdviceAdapter(Opcodes.ASM9,mv,access,method,descriptor){
                    protected void onMethodEnter(){
                        if(ready){loadThis();visitMethodInsn(INVOKESTATIC,"alpha/ambient/AmbientHooks","ready","(Ljava/lang/Object;)V",false);}
                        if(fog){loadArg(0);loadArg(1);visitMethodInsn(INVOKESTATIC,"alpha/ambient/AmbientHooks","fog","(IF)F",false);storeArg(1);}
                    }
                    protected void onMethodExit(int opcode){
                        if(constructor&&opcode==RETURN){
                            loadThis();loadArg(1);loadArg(2);loadArg(3);
                            visitMethodInsn(INVOKESTATIC,"alpha/ambient/AmbientHooks","register","(Ljava/lang/Object;[BII)V",false);
                        }
                    }
                };
            }
        },ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }
}
