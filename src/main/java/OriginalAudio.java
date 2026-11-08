import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

/** Playback only: decodes original Ogg recordings, never creates sound effects. */
final class OriginalAudio implements AutoCloseable {
    private final Map<String, List<byte[]>> pcm = new HashMap<>();
    private final AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "original-minecraft-audio"); t.setDaemon(true); return t;
    });
    private final Random random = new Random();
    private volatile boolean busy;
    boolean enabled = true;
    OriginalAudio(MinecraftAssets assets) throws IOException {
        for (var entry : assets.sounds.entrySet()) {
            List<byte[]> samples = new ArrayList<>();
            for (Path file : entry.getValue()) {
                try (AudioInputStream ogg = AudioSystem.getAudioInputStream(file.toFile())) {
                    AudioFormat source = ogg.getFormat();
                    AudioFormat decoded = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                            source.getSampleRate(), 16, source.getChannels(), source.getChannels() * 2,
                            source.getSampleRate(), false);
                    // Convert through decoded PCM before normalizing to a common playback format.
                    try (AudioInputStream raw = AudioSystem.getAudioInputStream(decoded, ogg);
                         AudioInputStream normalized = AudioSystem.getAudioInputStream(format, raw)) {
                        samples.add(normalized.readAllBytes());
                    }
                } catch (UnsupportedAudioFileException | IllegalArgumentException e) {
                    throw new IOException("Cannot decode original Ogg sound " + file + ". Build the executable JAR with Maven so the Vorbis decoder is included. No synthesized fallback is provided.", e);
                }
            }
            pcm.put(entry.getKey(), samples);
        }
        if (!AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, format)))
            throw new IOException("No compatible desktop audio output is available.");
    }
    void play(int block) {
        if (!enabled || busy) return;
        String surface = switch (block) { case 1, 2, 5 -> "grass"; case 4, 7 -> "wood"; case 6 -> "sand"; default -> "stone"; };
        List<byte[]> samples = pcm.get(surface);
        byte[] sample = samples.get(random.nextInt(samples.size()));
        busy = true;
        worker.execute(() -> {
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format); line.start();
                line.write(sample, 0, sample.length); line.drain();
            } catch (LineUnavailableException e) {
                System.err.println("Original sound playback failed: " + e.getMessage());
            } finally { busy = false; }
        });
    }
    @Override public void close() { worker.shutdownNow(); }
}
