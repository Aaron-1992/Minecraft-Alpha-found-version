import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;
import javax.swing.*;

/** Local single-player launch. No launcher accounts, tokens, downloads or profile edits. */
final class DirectAlphaLaunch {
    static void open()throws Exception{
        Path data=DesktopSetup.dataDirectory();Path jar=StartupRecovery.savedJar(data);
        if(!Files.isRegularFile(jar)){
            Path[] chosen={null};SwingUtilities.invokeAndWait(()->{
                JFileChooser picker=new JFileChooser();picker.setDialogTitle("Select installed a1.0.16.jar (matching JSON and libraries required)");
                if(picker.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)chosen[0]=picker.getSelectedFile().toPath();
            });if(chosen[0]==null)return;jar=chosen[0];
        }
        OriginalAlphaInstallation.Client client=OriginalAlphaInstallation.inspect(jar);
        int[] consent={-1};SwingUtilities.invokeAndWait(()->consent[0]=JOptionPane.showConfirmDialog(null,
                "Experimental build: an AMD driver timeout was reported in the previous release.\nFog modifications are disabled; this is NOT a verified driver fix.\nCancel if your graphics driver is still unstable. Do not repeatedly retry after a reset.\n\nSingle-player only: this tool does not sign in or read account tokens.\nUses your locally installed client/libraries and bundled Java 8 on Windows.\nOriginal JAR, launcher profiles and Recovery worlds remain untouched.\nA separate patched-engine folder holds this mod's worlds.\n\nOriginal sound recordings are copied when locally available; nothing is downloaded.\nTerrain changes persist in saved worlds. Use a new world for testing.",
                "Alpha - Play directly",JOptionPane.OK_CANCEL_OPTION));if(consent[0]!=JOptionPane.OK_OPTION)return;
        Path game=data.resolve("patched-engine").resolve(client.version());Files.createDirectories(game);
        Path runtime=LegacyRuntime.installIfAvailable(game);
        if(runtime==null)throw new IOException("Direct play currently requires the Windows distribution with bundled Java 8. Recovery is unchanged.");
        LegacyRuntime.verifyJava8(runtime);
        Map<String,Object> metadata=LauncherJson.object(Files.readString(client.jar().resolveSibling(client.version()+".json")));
        List<Path> libraries=resolveLibraries(client.minecraft(),metadata,currentOs(),game.resolve("natives"));
        Path helper=AlphaAgentSupport.install(game.resolve("patch-tools"));
        // Fingerprint the original and helper so a new patch release never silently reuses an old result.
        String key=digest(client.jar())+"-"+digest(helper);
        Path patched=game.resolve("client-"+key+".jar");
        if(!Files.isRegularFile(patched)){
            Path output=game.resolve("patch-install.log");
            Process patch=new ProcessBuilder(runtime.toString(),"-cp",helper.toString(),"alpha.ambient.OfflinePatcher",
                    client.jar().toString(),patched.toString(),AlphaAgentSupport.findChunkClass(client.jar()))
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if(!patch.waitFor(60,TimeUnit.SECONDS)){patch.destroyForcibly();throw new IOException("Patch installation timed out. See "+output);}
            if(patch.exitValue()!=0)throw new IOException("Local patch installation failed. Original untouched.\n"+Files.readString(output));
        }
        Files.createDirectories(game.resolve("resources"));
        OriginalResourceCopy.copy(client.minecraft(),game.resolve("resources"));
        long recordings;
        try(var files=Files.walk(game.resolve("resources"))){recordings=files.filter(p->Files.isRegularFile(p)&&p.toString().endsWith(".ogg")).count();}
        if(recordings==0){
            int[] proceed={-1};SwingUtilities.invokeAndWait(()->proceed[0]=JOptionPane.showConfirmDialog(null,
                    "No original legacy .ogg recordings were found locally.\nThe local sound-loader fix is installed, but cannot invent missing audio.\n\nYou may select a legacy resources folder to import, or cancel setup.\nChoose Yes to select it; No to explicitly test without sound.","Original recordings missing",JOptionPane.YES_NO_CANCEL_OPTION));
            if(proceed[0]==JOptionPane.CANCEL_OPTION||proceed[0]<0)return;
            if(proceed[0]==JOptionPane.YES_OPTION){
                Path[] folder={null};SwingUtilities.invokeAndWait(()->{JFileChooser picker=new JFileChooser();picker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);picker.setDialogTitle("Select legacy resources folder (contains sound/newsound/music)");if(picker.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)folder[0]=picker.getSelectedFile().toPath();});
                if(folder[0]==null)return;
                int imported=OriginalResourceCopy.copyLegacyFolder(folder[0],game.resolve("resources"));
                if(imported==0)throw new IOException("No compatible original recordings found in selected folder. No replacement audio was installed.");
            }
        }
        Path options=game.resolve("options.txt");
        if(!Files.exists(options))Files.writeString(options,"renderDistance:2\nfancyGraphics:false\nsound:1.0\nmusic:0.5\n",StandardOpenOption.CREATE_NEW);
        List<String> command=command(runtime,patched,libraries,game);
        LaunchSession session=LaunchSession.begin(data);
        command=new ArrayList<>(command);command.add(1,"-XX:ErrorFile="+session.directory.resolve("hs_err_pid%p.log").toAbsolutePath());
        try{
            Process process=new ProcessBuilder(command).directory(game.toFile()).redirectErrorStream(true).redirectOutput(session.directory.resolve("game.log").toFile()).start();
            Properties saved=new Properties();saved.setProperty("client",client.jar().toString());
            try(OutputStream out=Files.newOutputStream(data.resolve("original-engine.properties"))){saved.store(out,"Client path only. No account data.");}
            int result=process.waitFor();session.finished(result);
            if(result!=0)throw new IOException("Alpha exited with code "+result+". Use Save latest session report in this launcher. If the AMD driver reset, stop testing; its cause is unresolved.");
        }catch(Exception e){session.failed(e);throw e;}

    }
    static String currentOs(){String os=System.getProperty("os.name").toLowerCase(Locale.ROOT);return os.contains("win")?"windows":os.contains("mac")?"osx":"linux";}
    static String digest(Path file)throws Exception{
        java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");
        try(InputStream in=Files.newInputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)hash.update(b,0,n);}
        return HexFormat.of().formatHex(hash.digest());
    }
    static List<String> command(Path java,Path client,List<Path> libraries,Path game){
        List<String> cp=new ArrayList<>();cp.add(client.toAbsolutePath().toString());for(Path path:libraries)cp.add(path.toAbsolutePath().toString());
        return List.of(java.toString(),"-Xmx1G","-Dalpha.scheduledSounds=false","-Djava.util.Arrays.useLegacyMergeSort=true","-Dalpha.gameDir="+game.toAbsolutePath(),"-Djava.library.path="+game.resolve("natives").toAbsolutePath(),
                "-Dorg.lwjgl.librarypath="+game.resolve("natives").toAbsolutePath(),"-cp",String.join(File.pathSeparator,cp),"net.minecraft.client.Minecraft","Player","-");
    }
    static List<Path> resolveLibraries(Path minecraft,Map<String,Object> metadata,String os,Path natives)throws IOException{
        if(metadata.containsKey("inheritsFrom"))throw new IOException("Inherited metadata is not supported for direct Alpha play");
        if(!(metadata.get("libraries") instanceof List<?> entries))throw new IOException("Version metadata has no library list");
        List<Path> cp=new ArrayList<>(),nativeJars=new ArrayList<>();
        Path root=minecraft.resolve("libraries").toAbsolutePath().normalize();
        for(Object item:entries){
            if(!(item instanceof Map<?,?> library))throw new IOException("Invalid library metadata");
            if(!allowed(library.get("rules"),os))continue;
            if(!(library.get("downloads") instanceof Map<?,?> downloads))throw new IOException("Library download paths missing. Refresh this version once with a compatible launcher.");
            if(downloads.get("artifact") instanceof Map<?,?> artifact)cp.add(localArtifact(root,artifact));
            if(library.get("natives") instanceof Map<?,?> classifiers&&classifiers.get(os) instanceof String classifier){
                classifier=classifier.replace("${arch}",System.getProperty("os.arch").contains("64")?"64":"32");
                if(!(downloads.get("classifiers") instanceof Map<?,?> nativeDownloads)||!(nativeDownloads.get(classifier) instanceof Map<?,?> artifact))throw new IOException("Native library metadata missing: "+classifier);
                nativeJars.add(localArtifact(root,artifact));
            }
        }
        if(cp.isEmpty()||nativeJars.isEmpty())throw new IOException("Required local Alpha libraries/natives were not found. No files were downloaded.");
        Files.createDirectories(natives);for(Path jar:nativeJars)extractNatives(jar,natives);
        return List.copyOf(cp);
    }
    static boolean allowed(Object value,String os)throws IOException{
        if(value==null)return true;if(!(value instanceof List<?> rules))throw new IOException("Invalid library rules");boolean allow=false;
        for(Object item:rules){
            if(!(item instanceof Map<?,?> rule))throw new IOException("Invalid library rule");
            if(rule.containsKey("features"))throw new IOException("Unsupported feature-dependent library rule");
            boolean matches=true;
            if(rule.get("os") instanceof Map<?,?> spec){
                matches=!spec.containsKey("name")||os.equals(spec.get("name"));
                if(spec.get("arch") instanceof String regex)matches&=System.getProperty("os.arch").matches(regex);
                if(spec.get("version") instanceof String regex)matches&=System.getProperty("os.version").matches(regex);
            }
            if(matches){Object action=rule.get("action");if(!Set.of("allow","disallow").contains(action))throw new IOException("Unknown library rule action");allow="allow".equals(action);}
        }return allow;
    }
    static Path localArtifact(Path root,Map<?,?> artifact)throws IOException{
        if(!(artifact.get("path") instanceof String name)||name.contains("\\"))throw new IOException("Invalid local library path");
        Path file=root.resolve(name).normalize();
        if(!file.startsWith(root)||!Files.isRegularFile(file)||!file.toRealPath().startsWith(root.toRealPath()))throw new IOException("Required installed library missing or unsafe: "+name+". Install/launch Alpha once with your existing launcher.");
        if(artifact.get("sha1") instanceof String expected){
            try{java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-1");try(InputStream in=Files.newInputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)hash.update(b,0,n);}if(!HexFormat.of().formatHex(hash.digest()).equals(expected))throw new IOException("Local library checksum mismatch: "+name);}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        }return file;
    }
    static void extractNatives(Path jar,Path destination)throws IOException{
        destination=destination.toAbsolutePath().normalize();long total=0;
        try(ZipInputStream zip=new ZipInputStream(Files.newInputStream(jar))){ZipEntry entry;byte[] buffer=new byte[32768];
            while((entry=zip.getNextEntry())!=null){
                String name=entry.getName();Path target=destination.resolve(name).normalize();
                if(!target.startsWith(destination)||name.contains("\\"))throw new IOException("Unsafe native archive entry");
                if(entry.isDirectory()||!(name.endsWith(".dll")||name.endsWith(".so")||name.endsWith(".dylib")||name.endsWith(".jnilib")))continue;
                Files.createDirectories(target.getParent());if(!target.getParent().toRealPath().startsWith(destination.toRealPath())||Files.isSymbolicLink(target))throw new IOException("Unsafe native destination");
                try(OutputStream out=Files.newOutputStream(target)){int n;while((n=zip.read(buffer))!=-1){total+=n;if(total>128L*1024*1024)throw new IOException("Native archive too large");out.write(buffer,0,n);}}
            }
        }
    }
}
