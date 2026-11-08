import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Structural compatibility check only. Does not decompile or execute client classes. */
final class AlphaAgentSupport {
    static final String RESOURCE="/mods/alpha-ambient-agent-0.5.0.jar";
    static String findChunkClass(Path jar)throws IOException{
        List<String> candidates=new ArrayList<>();
        try(ZipFile archive=new ZipFile(jar.toFile())){
            Enumeration<? extends ZipEntry> entries=archive.entries();
            while(entries.hasMoreElements()){
                ZipEntry entry=entries.nextElement();
                if(!entry.getName().endsWith(".class")||entry.getSize()>2_000_000)continue;
                try(InputStream input=archive.getInputStream(entry)){
                    if(isChunkShape(input))candidates.add(entry.getName().substring(0,entry.getName().length()-6));
                }catch(EOFException ignored){} // Empty metadata fixtures are not compatible classes.
            }
        }
        if(candidates.size()!=1)throw new IOException("This Alpha JAR has "+candidates.size()+" matching chunk layouts; the optional mod cannot be installed safely. Vanilla setup is still available. Please share the exact client version for a compatible hook.");
        return candidates.get(0);
    }
    static boolean isChunkShape(InputStream input)throws IOException{
        DataInputStream in=new DataInputStream(input);if(in.readInt()!=0xcafebabe)return false;
        in.readUnsignedShort();in.readUnsignedShort();int count=in.readUnsignedShort();String[] utf=new String[count];
        for(int i=1;i<count;i++){
            int tag=in.readUnsignedByte();switch(tag){
                case 1 -> utf[i]=in.readUTF();case 3,4 -> in.skipNBytes(4);case 5,6 -> {in.skipNBytes(8);i++;}
                case 7,8,16,19,20 -> in.skipNBytes(2);case 9,10,11,12,17,18 -> in.skipNBytes(4);case 15 -> in.skipNBytes(3);
                default -> throw new IOException("Unsupported client constant pool tag");
            }
        }
        in.skipNBytes(6);int interfaces=in.readUnsignedShort();in.skipNBytes(interfaces*2L);
        boolean blocks=false,constructor=false,ready=false;
        int fields=in.readUnsignedShort();
        for(int i=0;i<fields;i++){in.skipNBytes(4);int descriptor=in.readUnsignedShort();if(descriptor<count&&"[B".equals(utf[descriptor]))blocks=true;skipAttributes(in);}
        int methods=in.readUnsignedShort();
        for(int i=0;i<methods;i++){
            int access=in.readUnsignedShort(),nameIndex=in.readUnsignedShort(),descriptorIndex=in.readUnsignedShort();
            if(nameIndex>=count||descriptorIndex>=count)throw new IOException("Invalid class metadata indices");
            String name=utf[nameIndex],descriptor=utf[descriptorIndex];
            if("<init>".equals(name)&&descriptor!=null&&descriptor.matches("\\(L[^;]+;\\[BII\\)V"))constructor=true;
            if(name!=null&&!name.startsWith("<")&&"()V".equals(descriptor)&&(access&(0x0008|0x0100|0x0400))==0)ready=true;
            skipAttributes(in);
        }
        return blocks&&constructor&&ready;
    }
    private static void skipAttributes(DataInputStream in)throws IOException{
        int count=in.readUnsignedShort();for(int i=0;i<count;i++){
            in.readUnsignedShort();long length=Integer.toUnsignedLong(in.readInt());if(length>2_000_000)throw new IOException("Unexpected class attribute size");in.skipNBytes(length);
        }
    }
    static Path install(Path directory)throws IOException{
        Files.createDirectories(directory);Path target=directory.resolve("alpha-ambient-agent-0.5.0.jar");
        try(InputStream input=AlphaAgentSupport.class.getResourceAsStream(RESOURCE)){
            if(input==null)throw new IOException("Optional agent is not packaged. Use the complete 0.5.0 release, not an old or partial build.");
            byte[] packaged=input.readAllBytes();
            if(Files.exists(target)){
                if(!Arrays.equals(Files.readAllBytes(target),packaged))throw new IOException("Existing optional mod differs from this release; it was not overwritten: "+target);
            }else Files.write(target,packaged,StandardOpenOption.CREATE_NEW);
        }return target.toAbsolutePath().normalize();
    }
    static String arguments(String existing,Path agent,String chunkClass){
        if(existing==null)existing="";
        // Only replace this project's own prior hook, preserving other agents/JVM tuning.
        existing=existing.replaceAll("(?:^|\\s)-javaagent:(?:\"[^\"]*alpha-ambient-agent-[^\"]*\\.jar\"|[^\\s\"]*alpha-ambient-agent-[^\\s\"]*\\.jar)(?:=[^\\s]+)?", "").trim();
        return (existing.isEmpty()?"":existing+" ")+"-javaagent:\""+agent+"\"="+chunkClass;
    }
}
