package alpha.ambient;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/** Optional original-client mod; changes memory only, never the original JAR. */
public final class AmbientAgent {
    public static void premain(String chunkClass,Instrumentation instrumentation){
        if(chunkClass==null||!chunkClass.matches("[A-Za-z0-9_$/]+")){
            System.err.println("[Alpha ambient] No compatible inspected chunk class; hooks disabled.");return;
        }
        try{
            File self=new File(AmbientAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // LaunchWrapper's isolated loader must resolve our hook without modifying its classes.
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(self));
            instrumentation.addTransformer(new AmbientTransformer(chunkClass));
            System.err.println("[Alpha ambient] Optional in-memory hooks registered. No client file modified.");
        }catch(Exception failure){System.err.println("[Alpha ambient] Optional hooks disabled: "+failure.getClass().getSimpleName());}
    }
}
