package alpha.ambient;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/** Fail-open startup: only the tiny hook JAR enters bootstrap, never ASM. */
public final class AmbientAgent {
    public static void premain(String chunkClass,Instrumentation instrumentation){
        if(chunkClass==null||!chunkClass.matches("[A-Za-z0-9_$/]+")){
            System.err.println("[Alpha ambient] Invalid target; optional hooks disabled.");return;
        }
        try{
            File hooks=File.createTempFile("alpha-ambient-hooks-", ".jar");hooks.deleteOnExit();
            try(InputStream in=AmbientAgent.class.getResourceAsStream("/alpha/ambient/bootstrap-hooks.jar");OutputStream out=new FileOutputStream(hooks)){
                if(in==null)throw new IOException("Packaged bootstrap hooks missing");
                byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            }
            // Export hook types only. The previous whole-agent export could expose
            // a different ASM version to LaunchWrapper and other game libraries.
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(hooks));
            Class<?> hook=Class.forName("alpha.ambient.AmbientHooks",true,null);
            if(hook.getClassLoader()!=null)throw new IllegalStateException("Hooks not bootstrap-visible");
            instrumentation.addTransformer(new AmbientTransformer(chunkClass));
            System.err.println("[Alpha ambient] Isolated hooks registered; original client file unchanged.");
        }catch(Throwable failure){
            if(failure instanceof ThreadDeath)throw (ThreadDeath)failure;
            if(failure instanceof VirtualMachineError)throw (VirtualMachineError)failure;
            System.err.println("[Alpha ambient] Optional hooks disabled after initialization failure:");failure.printStackTrace(System.err);
        }
    }
}
