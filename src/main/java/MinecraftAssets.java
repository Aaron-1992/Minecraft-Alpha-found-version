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
    final Map<String, Path> indexed = new HashMap<>();
    final Map<String, List<Path>> sounds = new HashMap<>();
    private static final Pattern OBJECT = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{\\s*\"hash\"\\s*:\\s*\"([a-f0-9]{40})\"");

    MinecraftAssets(Path jar, Path minecraft, Path resources) throws IOException {
        this.minecraft = minecraft;
        this.resources = resources;
        client = new ZipFile(jar.toFile());
        try {
            terrain = image("terrain.png", 256, 256);
            gui = image("gui/gui.png", 256, 128);
            icons = image("gui/icons.png", 256, 16);
            font = image("font/default.png", 128, 128);
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
    private BufferedImage image(String name, int width, int minHeight) throws IOException {
        ZipEntry entry = client.getEntry(name);
        if (entry == null) throw new IOException("Missing original Minecraft asset: " + name + ".\nSelect a legacy Alpha client JAR, not a modern client. No replacement textures are included.");
        BufferedImage result;
        try (InputStream in = client.getInputStream(entry)) { result = ImageIO.read(in); }
        if (result == null || result.getWidth() != width || result.getHeight() < minHeight)
            throw new IOException("Unsupported legacy texture dimensions: " + name);
        return result;
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
        for (String prefix : List.of("sound/", "sounds/", "minecraft/sound/", "minecraft/sounds/")) {
            Path p = indexed.get(prefix + name);
            if (p != null) return p;
        }
        List<Path> roots = new ArrayList<>();
        if (resources != null) roots.addAll(List.of(resources, resources.resolve("sound"), resources.resolve("sounds")));
        roots.add(minecraft.resolve("resources/sound"));
        roots.add(minecraft.resolve("assets/virtual/legacy/sound"));
        roots.add(minecraft.resolve("assets/virtual/legacy/sounds"));
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
