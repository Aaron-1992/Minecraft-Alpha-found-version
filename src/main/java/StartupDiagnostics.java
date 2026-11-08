import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Read-only, bounded inspection of known game/launcher logs. Never uploads anything. */
final class StartupDiagnostics {
    static final int MAX_TAIL=262144;
    record Finding(String category,String explanation,List<String> evidence){}
    record Report(Finding finding,String text){}
    static Finding analyze(String input){
        List<String> lines=Arrays.asList(input.split("\\R",-1));int start=-1;
        for(int i=0;i<lines.size();i++){
            String line=lines.get(i).trim(),low=line.toLowerCase(Locale.ROOT);
            boolean error=line.matches(".*\\b[A-Za-z0-9_.$]+(?:Exception|Error)(?::.*)?$");
            if(!low.contains("caused by:")&&!low.startsWith("at ")&&(error||low.contains("exception in thread")||low.contains("fatal error")||low.contains("error:")||low.contains("unrecognized vm option")||low.contains("error opening zip file")||low.contains("could not create the java virtual machine")))start=i;
        }
        if(start<0)for(int i=0;i<lines.size();i++)if(lines.get(i).toLowerCase(Locale.ROOT).contains("caused by:"))start=i;
        int end=start<0?lines.size():Math.min(lines.size(),start+60);
        String block=start<0?String.join("\n",lines.subList(Math.max(0,lines.size()-80),lines.size())):String.join("\n",lines.subList(start,end));
        String lower=block.toLowerCase(Locale.ROOT);
        String category="UNKNOWN",explanation="No specific Java/native failure was found. Exit code 1 by itself is not a diagnosis.";
        if(lower.contains("unsupportedclassversionerror")||lower.contains("unsupported major.minor")||lower.contains("class file version")){
            category="JAVA_VERSION";explanation="The log indicates incompatible Java bytecode/runtime versions. The clean recovery profile uses bundled Java 8 on Windows.";
        }else if(lower.contains("error opening zip file")||lower.contains("agent library failed to init")||lower.contains("error occurred during initialization of vm")&&lower.contains("instrument")){
            category="AGENT_STARTUP";explanation="The log indicates a JVM-agent startup/packaging failure. The recovery installation removes runtime injection entirely.";
        }else if(lower.contains("unrecognized vm option")||lower.contains("unrecognized option:")||lower.contains("could not reserve enough space")||lower.contains("could not create the java virtual machine")){
            category="JVM_OPTIONS";explanation="The log indicates invalid JVM options or a JVM memory reservation failure. The recovery profile starts with clean options and a 1 GB maximum heap.";
        }else if(lower.contains("unsatisfiedlinkerror")||lower.contains("failed to load library")||lower.contains("no lwjgl in java.library.path")){
            category="NATIVE_LIBRARY";explanation="The log indicates a missing/incompatible native library. This is not established to be an agent problem; the official launcher must provide matching Alpha/LWJGL native files.";
        }else if(lower.contains("pixel format not accelerated")||lower.contains("failed to create display")||lower.contains("could not initialize opengl")||lower.contains("no opengl context")){
            category="GRAPHICS";explanation="The log indicates an OpenGL/display initialization failure. A clean profile can isolate settings/mods, but does not repair a graphics driver.";
        }else if(lower.contains("verifyerror")||lower.contains("incompatibleclasschangeerror")||lower.contains("illegalaccesserror")||lower.contains("nosuchmethoderror")){
            category="CLASS_COMPATIBILITY";explanation="The log indicates a class/linkage compatibility error. The clean recovery profile disables all agents rather than guessing another transformation.";
        }else if(lower.contains("classnotfoundexception")||lower.contains("noclassdeffounderror")||lower.contains("could not find or load main class")){
            category="MISSING_CLASS";explanation="The log indicates a missing/unloadable class. Matching launcher libraries or client metadata may be absent; this does not prove the optional mod is the cause.";
        }else if(lower.contains("exception in thread")||lower.contains("caused by:")||lower.contains("fatal error")){
            category="OTHER_EXCEPTION";explanation="A concrete exception is present, but it does not match a safe automatic repair rule. The redacted excerpt below contains more information than exit code 1.";
        }
        List<String> evidence=new ArrayList<>();
        if(start>=0)for(int i=Math.max(0,start-2);i<Math.min(lines.size(),start+45);i++){
            String line=lines.get(i);
            if(i>=start||line.toLowerCase(Locale.ROOT).contains("error"))evidence.add(redact(line));
        }
        return new Finding(category,explanation,List.copyOf(evidence));
    }
    static String redact(String input){
        String lower=input.toLowerCase(Locale.ROOT);
        for(String sensitive:List.of("token","authorization","bearer ","cookie","session","password","username","setting user:","--uuid","--xuid","authenticationdatabase","--usertype","--userproperties"))
            if(lower.contains(sensitive))return "[sensitive launcher line omitted]";
        if(lower.contains("command line")||lower.contains("commandline:")||lower.contains("jvm arguments")||lower.contains("jvm_args:")||lower.contains("java_command:")||lower.contains("launching with arguments"))return "[launcher argument line omitted]";
        String home=System.getProperty("user.home","");
        if(!home.isEmpty()){
            input=input.replace(home,"<home>").replace(home.replace('\\','/'),"<home>");
            input=input.replaceAll("(?i)"+Pattern.quote(home),"<home>");
        }
        input=input.replaceAll("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+","<token omitted>");
        input=input.replaceAll("(?i)\\b[0-9a-f]{32,}\\b","<identifier omitted>");
        input=input.replaceAll("(?i)\\b[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}\\b","<identifier omitted>");
        input=input.replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}","<email omitted>");
        input=input.replaceAll("(?<![A-Za-z0-9])(?:[A-Za-z0-9_+/-]{40,}={0,2})(?![A-Za-z0-9])","<opaque value omitted>");
        return input.length()>1200?input.substring(0,1200)+" [truncated]":input;
    }
    static Report collect(Path minecraft,Path data)throws IOException{
        List<Path> candidates=new ArrayList<>();
        for(String name:List.of("launcher_log.txt","launcher_log_microsoft_store.txt","launcher_log_mojang.txt","minecraft-crash.log","minecraft_crash.log"))add(candidates,minecraft.resolve(name));
        add(candidates,minecraft.resolve("logs/latest.log"));addReports(candidates,minecraft.resolve("crash-reports"));
        for(String directory:List.of("ambient-engine","compatibility-engine","original-engine","recovery-engine")){
            Path parent=data.resolve(directory);if(!Files.isDirectory(parent))continue;
            try(var versions=Files.list(parent)){
                for(Path version:versions.filter(p->Files.isDirectory(p)&&!Files.isSymbolicLink(p)).limit(12).toList()){
                    for(String name:List.of("logs/latest.log","output-client.log","client.log","minecraft-crash.log","mods/agent-preflight.log"))add(candidates,version.resolve(name));
                    addReports(candidates,version.resolve("crash-reports"));
                    try(var files=Files.list(version)){for(Path file:files.filter(p->p.getFileName().toString().matches("hs_err_pid[0-9]+\\.log")).limit(3).toList())add(candidates,file);}
                }
            }
        }
        candidates=candidates.stream().distinct().sorted((a,b)->Long.compare(modified(b),modified(a))).limit(24).toList();
        Finding selected=new Finding("UNKNOWN","No specific failure was found in the available local log excerpts. Exit code 1 alone is not a diagnosis.",List.of());
        StringBuilder report=new StringBuilder("Alpha Found 0.5.2 - local startup diagnostic\nGenerated: "+Instant.now()+"\nOS: "+System.getProperty("os.name")+" "+System.getProperty("os.arch")+"\nSetup Java (not proof of game Java): "+System.getProperty("java.version")+"\n\n");
        report.append("Read-only scan of known launcher/game log paths. Tails are bounded; only redacted failure excerpts are exported.\nNo account files, full launch commands or original logs are copied. Nothing is uploaded.\nA log excerpt may describe an older/different launch; its timestamp is shown, not treated as proof of the current cause.\n\n");
        int excerpts=0;
        for(Path file:candidates){
            try{
                Finding finding=analyze(readTail(file));
                if(finding.category.equals("UNKNOWN")&&finding.evidence.isEmpty())continue;
                if(selected.category.equals("UNKNOWN")&&!finding.category.equals("UNKNOWN"))selected=finding;
                if(excerpts++>=5)continue;
                report.append("Source: ").append(redact(file.toString())).append("\nModified: ").append(Instant.ofEpochMilli(modified(file))).append("\nCategory: ").append(finding.category).append("\n").append(finding.explanation).append("\n");
                for(String line:finding.evidence)report.append(line).append('\n');report.append('\n');
            }catch(IOException|SecurityException e){report.append("One candidate log could not be read; its content was not exported.\n");}
        }
        report.append("Summary (possible cause, not verified): ").append(selected.category).append("\n").append(selected.explanation).append("\n\nThe recovery installation has no agent and uses a separate directory. It is a diagnostic baseline, not the horror mod. Original client execution remains unverified.\n");
        return new Report(selected,report.toString());
    }
    static String readTail(Path file)throws IOException{
        try(SeekableByteChannel channel=Files.newByteChannel(file,StandardOpenOption.READ)){
            long size=channel.size();ByteBuffer header=ByteBuffer.allocate(2);channel.read(header);byte[] first=header.array();
            Charset encoding=first[0]==(byte)0xff&&first[1]==(byte)0xfe?StandardCharsets.UTF_16LE:first[0]==(byte)0xfe&&first[1]==(byte)0xff?StandardCharsets.UTF_16BE:StandardCharsets.UTF_8;
            long offset=Math.max(0,size-MAX_TAIL);channel.position(offset);ByteBuffer buffer=ByteBuffer.allocate((int)(size-offset));while(buffer.hasRemaining()&&channel.read(buffer)!=-1){}
            String text=new String(buffer.array(),0,buffer.position(),encoding);if(offset>0){int line=text.indexOf('\n');if(line>=0)text=text.substring(line+1);}return text;
        }
    }
    static Path save(Report report,Path folder)throws IOException{
        Files.createDirectories(folder);Path file=Files.createTempFile(folder,"alpha-startup-", ".txt");Files.writeString(file,report.text);return file;
    }
    private static void add(List<Path> files,Path path){if(Files.isRegularFile(path)&&!Files.isSymbolicLink(path))files.add(path);}
    private static long modified(Path file){try{return Files.getLastModifiedTime(file).toMillis();}catch(IOException e){return 0;}}
    private static void addReports(List<Path> files,Path folder)throws IOException{
        if(!Files.isDirectory(folder))return;
        try(var paths=Files.list(folder)){for(Path file:paths.filter(p->p.getFileName().toString().endsWith(".txt")).sorted((a,b)->Long.compare(modified(b),modified(a))).limit(3).toList())add(files,file);}
    }
}
