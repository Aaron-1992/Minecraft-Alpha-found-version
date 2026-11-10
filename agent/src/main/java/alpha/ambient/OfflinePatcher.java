package alpha.ambient;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Install-time patching only. Never placed on the game's class path. */
public final class OfflinePatcher {
    public static final String SUPPORTED_SHA256="fd14110691b14ea51f9320a949e2b3c1855e38af2668bf4e743c19b08de00234";
    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IOException("Expected original JAR, new output JAR and inspected chunk class");
        patch(Paths.get(args[0]),Paths.get(args[1]),args[2]);
    }
    public static void patch(Path input,Path output,String chunk)throws Exception{
        byte[] source=Files.readAllBytes(input);
        if(!hex(MessageDigest.getInstance("SHA-256").digest(source)).equals(SUPPORTED_SHA256))
            throw new IOException("Unsupported client fingerprint. This patch targets the inspected a1.0.16 client only; original unchanged.");
        if(Files.exists(output))throw new IOException("Output already exists; refusing to overwrite it");
        Path temporary=Files.createTempFile(output.toAbsolutePath().getParent(),".alpha-patch-",".tmp");
        int changes=0;
        try{
            try(JarInputStream jar=new JarInputStream(new ByteArrayInputStream(source));JarOutputStream out=new JarOutputStream(Files.newOutputStream(temporary))){
                JarEntry entry;byte[] buffer=new byte[32768];int n;
                while((entry=jar.getNextJarEntry())!=null){
                    String name=entry.getName();
                    if(name.startsWith("META-INF/")||entry.isDirectory())continue;
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    while((n=jar.read(buffer))!=-1)bytes.write(buffer,0,n);
                    byte[] original=bytes.toByteArray(),modified=original;
                    if(name.endsWith(".class"))modified=patchClass(name.substring(0,name.length()-6),original,chunk);
                    if(!Arrays.equals(original,modified))changes++;
                    out.putNextEntry(new JarEntry(name));out.write(modified);out.closeEntry();
                }
                out.putNextEntry(new JarEntry("AlphaWatcher.class"));out.write(watcherClass());out.closeEntry();
                // Only our small Java 8 hook classes, not ASM or instrumentation helpers.
                try(InputStream resource=OfflinePatcher.class.getResourceAsStream("bootstrap-hooks.jar")){
                    if(resource==null)throw new IOException("Packaged hooks missing");
                    try(JarInputStream hooks=new JarInputStream(resource)){
                        while((entry=hooks.getNextJarEntry())!=null){
                            if(!entry.getName().startsWith("alpha/ambient/AmbientHooks")||!entry.getName().endsWith(".class"))continue;
                            out.putNextEntry(new JarEntry(entry.getName()));while((n=hooks.read(buffer))!=-1)out.write(buffer,0,n);out.closeEntry();
                        }
                    }
                }
            }
            if(changes<3)throw new IOException("Required chunk, directory or sound patch was not applied");
            Files.move(temporary,output);System.out.println("Patched "+changes+" classes. Original unchanged; no runtime agent.");
        }finally{Files.deleteIfExists(temporary);}
    }
    static byte[] patchClass(final String name,byte[] input,String chunk){
        if(name.equals(chunk))input=new AmbientTransformer(chunk).instrument(name,input);
        ClassReader reader=new ClassReader(input);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
            public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions){
                MethodVisitor mv=super.visitMethod(access,method,desc,signature,exceptions);
                if(name.equals("cj")&&(method.equals("<init>")||method.equals("m")&&desc.equals("()V"))){
                    final boolean saving=method.equals("m");
                    return new MethodVisitor(Opcodes.ASM9,mv){
                        String literal;
                        public void visitLdcInsn(Object value){
                            if(saving&&"LastPlayed".equals(value)){
                                super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ALOAD,1);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooksState","save","(Ljava/lang/Object;Ljava/lang/Object;)V",false);
                            }
                            literal=value instanceof String?(String)value:null;super.visitLdcInsn(value);
                        }
                        public void visitMethodInsn(int op,String owner,String called,String descriptor,boolean itf){
                            super.visitMethodInsn(op,owner,called,descriptor,itf);
                            if(!saving&&"Data".equals(literal)&&owner.equals("hc")&&called.equals("k")&&descriptor.equals("(Ljava/lang/String;)Lhc;")){
                                super.visitInsn(Opcodes.DUP);super.visitVarInsn(Opcodes.ALOAD,0);super.visitInsn(Opcodes.SWAP);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooksState","load","(Ljava/lang/Object;Ljava/lang/Object;)V",false);
                            }
                            literal=null;
                        }
                    };
                }
                if(name.equals("bb")&&method.equals("run")&&desc.equals("()V")){
                    // The old HTTP listing fails before Alpha registers cached sounds.
                    mv.visitCode();mv.visitVarInsn(Opcodes.ALOAD,0);mv.visitVarInsn(Opcodes.ALOAD,0);
                    mv.visitFieldInsn(Opcodes.GETFIELD,"bb","a","Ljava/io/File;");mv.visitLdcInsn("");
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL,"bb","a","(Ljava/io/File;Ljava/lang/String;)V",false);
                    mv.visitInsn(Opcodes.RETURN);mv.visitMaxs(0,0);mv.visitEnd();return null;
                }
                if(name.equals("net/minecraft/client/Minecraft")&&method.equals("b")&&desc.equals("()Ljava/io/File;")){
                    mv.visitCode();mv.visitTypeInsn(Opcodes.NEW,"java/io/File");mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn("alpha.gameDir");mv.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/System","getProperty","(Ljava/lang/String;)Ljava/lang/String;",false);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/io/File","<init>","(Ljava/lang/String;)V",false);
                    mv.visitInsn(Opcodes.ARETURN);mv.visitMaxs(0,0);mv.visitEnd();return null;
                }
                if(name.equals("net/minecraft/client/Minecraft")&&method.equals("i")&&desc.equals("()V")){
                    return new MethodVisitor(Opcodes.ASM9,mv){
                        public void visitInsn(int opcode){
                            if(opcode==Opcodes.RETURN){super.visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESPECIAL,name,"alpha$ambientTick","()V",false);}
                            super.visitInsn(opcode);
                        }
                    };
                }
                if(name.equals("jt")&&desc.equals("(Ljava/lang/String;IIIZ)V"))return new MethodVisitor(Opcodes.ASM9,mv){
                    public void visitCode(){super.visitCode();super.visitVarInsn(Opcodes.ILOAD,2);super.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","textOffset","(I)I",false);super.visitVarInsn(Opcodes.ISTORE,2);}
                };
                if(name.equals("dh")&&desc.equals("(Lfv;DDDFF)V"))return new MethodVisitor(Opcodes.ASM9,mv){
                    public void visitMethodInsn(int opcode,String owner,String called,String descriptor,boolean itf){
                        if(owner.equals("ff")&&called.equals("b")&&descriptor.equals("(FFFFFF)V")){
                            super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,"AlphaWatcher","draw","(Lff;FFFFFFLjava/lang/Object;)V",false);return;
                        }
                        super.visitMethodInsn(opcode,owner,called,descriptor,itf);
                    }
                };
                return new MethodVisitor(Opcodes.ASM9,mv){
                    public void visitMethodInsn(int opcode,String owner,String called,String descriptor,boolean itf){
                        if(opcode==Opcodes.INVOKESTATIC&&owner.equals("org/lwjgl/opengl/GL11")&&called.equals("glFogf")&&descriptor.equals("(IF)V")){
                            super.visitInsn(Opcodes.DUP2);super.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","fog","(IF)F",false);super.visitInsn(Opcodes.SWAP);super.visitInsn(Opcodes.POP);
                        }
                        if(name.equals("net/minecraft/client/Minecraft")&&opcode==Opcodes.INVOKESTATIC&&owner.equals("org/lwjgl/opengl/Display")&&called.equals("update")&&descriptor.equals("()V")){
                            super.visitIntInsn(Opcodes.BIPUSH,60);super.visitMethodInsn(Opcodes.INVOKESTATIC,owner,"sync","(I)V",false);
                        }
                        super.visitMethodInsn(opcode,owner,called,descriptor,itf);
                    }
                };

            }
            public void visitEnd(){
                if(name.equals("net/minecraft/client/Minecraft"))addSoundTick(writer);
                super.visitEnd();
            }
        },0);return writer.toByteArray();
    }
    private static void addSoundTick(ClassWriter writer){
        String game="net/minecraft/client/Minecraft";
        MethodVisitor m=writer.visitMethod(Opcodes.ACC_PRIVATE,"alpha$ambientTick","()V",null,null);
        m.visitCode();Label inactive=new Label(),end=new Label();
        for(String[] f:new String[][]{{"e","Lcj;"},{"g","Lbe;"}}){
            m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,f[0],f[1]);m.visitJumpInsn(Opcodes.IFNULL,inactive);
        }
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"p","Lbd;");m.visitJumpInsn(Opcodes.IFNONNULL,inactive);
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"m","Z");m.visitJumpInsn(Opcodes.IFNE,inactive);
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"e","Lcj;");
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"g","Lbe;");
        m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","scene","(Ljava/lang/Object;Ljava/lang/Object;)V",false);
        m.visitIntInsn(Opcodes.BIPUSH,64);m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/lwjgl/input/Keyboard","isKeyDown","(I)Z",false);
        m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","previewKey","(Z)V",false);
        m.visitIntInsn(Opcodes.BIPUSH,65);m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/lwjgl/input/Keyboard","isKeyDown","(I)Z",false);
        m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","cancelKey","(Z)V",false);
        m.visitInsn(Opcodes.ICONST_1);m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","nextSound","(Z)Ljava/lang/String;",false);m.visitVarInsn(Opcodes.ASTORE,1);
        m.visitVarInsn(Opcodes.ALOAD,1);m.visitJumpInsn(Opcodes.IFNULL,end);
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"e","Lcj;");
        for(String field:new String[]{"ah","ai","aj"}){
            m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"g","Lbe;");m.visitFieldInsn(Opcodes.GETFIELD,"be",field,"D");
            if(!field.equals("ai")){
                m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"g","Lbe;");m.visitFieldInsn(Opcodes.GETFIELD,"be","an","F");
                m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks",field.equals("ah")?"soundX":"soundZ","(F)D",false);m.visitInsn(Opcodes.DADD);
            }else{m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","soundY","(D)D",false);}
        }
        m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","soundVolume","()F",false);m.visitLdcInsn(0.95f);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"cj","a","(DDDLjava/lang/String;FF)V",false);
        m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","takeParticle","()Z",false);m.visitJumpInsn(Opcodes.IFEQ,end);
        // Original smoke particles on the edge of view; no custom texture or entity.
        for(int puff=0;puff<3;puff++){
            m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"e","Lcj;");m.visitLdcInsn("smoke");
            for(String field:new String[]{"ah","ai","aj"}){
                m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"g","Lbe;");m.visitFieldInsn(Opcodes.GETFIELD,"be",field,"D");
                if(!field.equals("ai")){
                    m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,game,"g","Lbe;");m.visitFieldInsn(Opcodes.GETFIELD,"be","an","F");
                    m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks",field.equals("ah")?"visualX":"visualZ","(F)D",false);m.visitInsn(Opcodes.DADD);
                }else{m.visitLdcInsn(.5+puff*.3);m.visitInsn(Opcodes.DADD);}
            }
            m.visitInsn(Opcodes.DCONST_0);m.visitLdcInsn(.015);m.visitInsn(Opcodes.DCONST_0);
            m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"cj","a","(Ljava/lang/String;DDDDDD)V",false);
        }
        m.visitJumpInsn(Opcodes.GOTO,end);
        m.visitLabel(inactive);m.visitInsn(Opcodes.ACONST_NULL);m.visitInsn(Opcodes.ACONST_NULL);m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","scene","(Ljava/lang/Object;Ljava/lang/Object;)V",false);m.visitInsn(Opcodes.ICONST_0);m.visitMethodInsn(Opcodes.INVOKESTATIC,"alpha/ambient/AmbientHooks","nextSound","(Z)Ljava/lang/String;",false);m.visitInsn(Opcodes.POP);
        m.visitLabel(end);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    static byte[] watcherClass(){
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V1_5,Opcodes.ACC_PUBLIC,"AlphaWatcher",null,"lr",null);
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(Lcj;)V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"lr","<init>","(Lcj;)V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
        // No AI, attacks, sunlight logic, drops, collisions or serialization.
        m=w.visitMethod(Opcodes.ACC_PUBLIC,"e_","()V",null,null);m.visitCode();m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
        for(String[] method:new String[][]{{"a","(Ljx;I)Z"},{"c_","()Z"},{"d_","()Z"},{"c","(Lhc;)Z"}}){
            m=w.visitMethod(Opcodes.ACC_PUBLIC,method[0],method[1],null,null);m.visitCode();m.visitInsn(Opcodes.ICONST_0);m.visitInsn(Opcodes.IRETURN);m.visitMaxs(0,4);m.visitEnd();
        }
        m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"draw","(Lff;FFFFFFLjava/lang/Object;)V",null,null);m.visitCode();
        Label normal=new Label(),start=new Label(),finish=new Label(),handler=new Label();
        m.visitVarInsn(Opcodes.ALOAD,7);m.visitTypeInsn(Opcodes.INSTANCEOF,"AlphaWatcher");m.visitJumpInsn(Opcodes.IFEQ,normal);
        m.visitIntInsn(Opcodes.BIPUSH,65);m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/lwjgl/opengl/GL11","glPushAttrib","(I)V",false);
        m.visitLabel(start);m.visitInsn(Opcodes.FCONST_0);m.visitInsn(Opcodes.FCONST_0);m.visitInsn(Opcodes.FCONST_0);m.visitInsn(Opcodes.FCONST_1);
        m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/lwjgl/opengl/GL11","glColor4f","(FFFF)V",false);
        drawModel(m);m.visitLabel(finish);m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/lwjgl/opengl/GL11","glPopAttrib","()V",false);m.visitInsn(Opcodes.RETURN);
        m.visitLabel(handler);m.visitVarInsn(Opcodes.ASTORE,8);m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/lwjgl/opengl/GL11","glPopAttrib","()V",false);m.visitVarInsn(Opcodes.ALOAD,8);m.visitInsn(Opcodes.ATHROW);
        m.visitTryCatchBlock(start,finish,handler,null);
        m.visitLabel(normal);drawModel(m);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
        w.visitEnd();return w.toByteArray();
    }
    private static void drawModel(MethodVisitor m){
        m.visitVarInsn(Opcodes.ALOAD,0);for(int i=1;i<=6;i++)m.visitVarInsn(Opcodes.FLOAD,i);m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"ff","b","(FFFFFF)V",false);
    }
    static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}
}
