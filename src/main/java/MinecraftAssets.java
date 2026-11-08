import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Reads unmodified assets from the user's own legacy client and resources. */
final class MinecraftAssets implements AutoCloseable {
    static final class MissingSoundsException extends IOException {
        private static final long serialVersionUID=1L;
        MissingSoundsException(String message) { super(message); }
    }
    final ZipFile client;
    final Path minecraft, resources;
    final BufferedImage terrain, gui, icons, font;
    final BufferedImage logo, clouds;
    final Map<String, Path> indexed = new HashMap<>();
    final Map<String, List<Path>> sounds = new HashMap<>();
    private static final Pattern OBJECT = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{\\s*\"hash\"\\s*:\\s*\"([a-f0-9]{40})\"");

    MinecraftAssets(Path jar, Path minecraft, Path resources) throws IOException {
        this.minecraft = minecraft;
        this.resources = resources;
        client = new ZipFile(jar.toFile());
        try {
            terrain = image(256, 256, "terrain.png");
            gui = image(256, 128, "gui/gui.png", "gui.png");
            icons = image(256, 16, "gui/icons.png", "icons.png");
            // Early Alpha keeps the original font at the archive root.
            font = image(128, 128, "default.png", "font/default.png");
            logo = optionalImage("title/mclogo.png", "gui/logo.png", "logo.png");
            clouds = optionalImage("environment/clouds.png", "clouds.png");
            indexSounds();
            for (String surface : List.of("grass", "stone", "wood", "sand")) {
                List<Path> files = new ArrayList<>();
                for (int i = 1; i <= 6; i++) {
                    Path p = soundFile("step/" + surface + i + ".ogg");
                    if (p != null) files.add(p);
                }
                if (files.isEmpty()) throw new MissingSoundsException("Missing original Minecraft sounds: step/" + surface + "*.ogg.\nUse --sounds pointing to a legacy sound folder, or --minecraft-dir with legacy assets installed.\nNo replacement or generated sound will be used.");
                sounds.put(surface, List.copyOf(files));
            }
        } catch (IOException | RuntimeException e) {
            client.close();
            throw e;
        }
    }
    /** Resolves original legacy paths only; never substitutes or generates media. */
    static ZipEntry resolveImageEntry(ZipFile archive, String... names) throws IOException {
        for (String name : names) {
            ZipEntry entry = archive.getEntry(name);
            if (entry != null && !entry.isDirectory()) return entry;
        }
        throw new IOException("Missing original Minecraft asset. Tried: " + String.join(", ", names)
                + ".\nThe selected archive does not contain the required legacy asset. No replacement texture will be generated.");
    }
    private BufferedImage image(int width, int minHeight, String... names) throws IOException {
        ZipEntry entry = resolveImageEntry(client, names);
        BufferedImage result;
        try (InputStream in = client.getInputStream(entry)) { result = ImageIO.read(in); }
        if (result == null || result.getWidth() != width || result.getHeight() < minHeight)
            throw new IOException("Unsupported legacy texture dimensions: " + entry.getName());
        return result;
    }
    private BufferedImage optionalImage(String... names) throws IOException {
        for(String name:names){
            ZipEntry entry=client.getEntry(name);
            if(entry!=null&&!entry.isDirectory()){
                try(InputStream in=client.getInputStream(entry)){
                    BufferedImage image=ImageIO.read(in);
                    if(image==null)throw new IOException("Unreadable original asset: "+name);
                    return image;
                }
            }
        }
        return null; // No fabricated replacement when the original client has no such asset.
    }
    private void indexSounds() throws IOException {
        Path indexes = minecraft.resolve("assets/indexes");
        if (!Files.isDirectory(indexes)) return;
        try (var paths = Files.list(indexes)) {
            for (Path index : paths.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                Matcher m = OBJECT.matcher(Files.readString(index));
                while (m.find()) {
                    String name = m.group(1), hash = m.group(2);
                    Path object = minecraft.resolve("assets/objects/" + hash.substring(0, 2) + "/" + hash);
                    if (Files.isRegularFile(object)) indexed.putIfAbsent(name, object);
                }
            }
        }
    }
    private Path soundFile(String name) {
        for (String prefix : List.of("sound/", "sounds/", "newsound/", "minecraft/sound/", "minecraft/sounds/")) {
            Path p = indexed.get(prefix + name);
            if (p != null) return p;
        }
        List<Path> roots = new ArrayList<>();
        if (resources != null) roots.addAll(List.of(resources, resources.resolve("sound"), resources.resolve("sounds"), resources.resolve("newsound")));
        roots.add(minecraft.resolve("resources/sound"));
        roots.add(minecraft.resolve("resources/newsound"));
        roots.add(minecraft.resolve("assets/virtual/legacy/sound"));
        roots.add(minecraft.resolve("assets/virtual/legacy/sounds"));
        roots.add(minecraft.resolve("assets/virtual/legacy/newsound"));
        for (Path root : roots) {
            Path p = root.resolve(name);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }
    void report() {
        System.out.println("Textures: " + client.getName());
        sounds.forEach((surface, files) -> files.forEach(p -> System.out.println(surface + ": " + p)));
    }
    @Override public void close() throws IOException { client.close(); }
}
