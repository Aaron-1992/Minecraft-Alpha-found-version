import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Structural compatibility check only. Does not decompile or execute client classes. */
final class AlphaAgentSupport {
    static final String RESOURCE="/mods/alpha-ambient-agent-0.13.0.jar";
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
        Files.createDirectories(directory);Path target=directory.resolve("alpha-ambient-agent-0.13.0.jar");
        try(InputStream input=AlphaAgentSupport.class.getResourceAsStream(RESOURCE)){
            if(input==null)throw new IOException("Optional agent is not packaged. Use the complete 0.13.0 release, not an old or partial build.");
            byte[] packaged=input.readAllBytes();
            if(Files.exists(target)){
                if(!Arrays.equals(Files.readAllBytes(target),packaged))throw new IOException("Existing optional mod differs from this release; it was not overwritten: "+target);
            }else Files.write(target,packaged,StandardOpenOption.CREATE_NEW);
        }return target.toAbsolutePath().normalize();
    }
    static String removeOwnArguments(String existing){
        if(existing==null)return "";
        List<String> kept=new ArrayList<>();for(String raw:rawArguments(existing))if(!isOwnAgent(unquote(raw)))kept.add(raw);
        return String.join(" ",kept);
    }
    static String arguments(String existing,Path agent,String chunkClass){
        String kept=removeOwnArguments(existing);
        if(kept.isEmpty())kept="-Xmx1G";
        // Quote the COMPLETE JVM argument; do not rely on a launcher accepting
        // a partially quoted path in the middle of an option.
        return kept+" \"-javaagent:"+agent+"="+chunkClass+"\"";
    }
    private static boolean isOwnAgent(String argument){
        if(!argument.startsWith("-javaagent:"))return false;
        String path=argument.substring(11);int equals=path.indexOf('=');if(equals>=0)path=path.substring(0,equals);
        path=path.replace('\\','/');String name=path.substring(path.lastIndexOf('/')+1);
        return name.startsWith("alpha-ambient-agent-")&&name.endsWith(".jar");
    }
    static List<String> rawArguments(String value){
        List<String> result=new ArrayList<>();int start=-1;boolean quoted=false;
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);if(c=='\"')quoted=!quoted;
            if(Character.isWhitespace(c)&&!quoted){if(start>=0){result.add(value.substring(start,i));start=-1;}}
            else if(start<0)start=i;
        }
        if(quoted)throw new IllegalArgumentException("Unclosed quote in this installation's JVM arguments");
        if(start>=0)result.add(value.substring(start));return result;
    }
    private static String unquote(String value){return value.replace("\"", "");}
    static void verifyPackagedAgent()throws IOException{
        Path folder=Files.createTempDirectory("Alpha preflight space ");
        Path agent=install(folder.resolve("mods"));
        Path runtime=LegacyRuntime.installIfAvailable(folder);
        Path log=preflight(runtime==null?LegacyRuntime.currentJava():runtime,agent,arguments(null,agent,"probe"));
        System.out.println(Files.readString(log));
        try(var paths=Files.walk(folder)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
    }
    static Path preflight(Path executable,Path agent,String profileArguments)throws IOException{
        List<String> command=new ArrayList<>();command.add(executable.toString());
        for(String raw:rawArguments(profileArguments)){
            String arg=unquote(raw);
            if(arg.startsWith("-javaagent:"))continue; // Never execute unrelated agents as part of our probe.
            command.add(arg);
        }
        command.add("-javaagent:"+agent+"=alpha/ambient/AgentProbe$Chunk");
        command.add("-jar");command.add(agent.toString());
        Path log=agent.getParent().resolve("agent-preflight.log");
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try{
            if(!process.waitFor(40,java.util.concurrent.TimeUnit.SECONDS)){
                process.destroyForcibly();process.waitFor(5,java.util.concurrent.TimeUnit.SECONDS);
                throw new IOException("Agent startup check timed out. Launcher profile was not enabled. Local details: "+log);
            }
            String output=Files.readString(log);
            if(process.exitValue()!=0||!output.contains("ALPHA_AGENT_PREFLIGHT_OK"))
                throw new IOException("Agent startup check failed (exit "+process.exitValue()+"). Launcher profile was not enabled. Local details: "+log);
        }catch(InterruptedException e){process.destroyForcibly();Thread.currentThread().interrupt();throw new IOException("Agent preflight interrupted",e);}
        return log;
    }
}
