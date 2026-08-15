package net.vibmc.world.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * On-disk layout for a world.
 *
 * <pre>
 * &lt;level-name&gt;/
 *   level.dat              seed, elapsed time, time of day, weather
 *   region/
 *     r.&lt;x&gt;.&lt;z&gt;.chunk     gzipped block data for one chunk
 * </pre>
 *
 * Chunk payloads are the raw {@code short[]} block array, so a saved chunk restores
 * exactly what was in memory rather than being re-derived from the seed. Writes go to
 * a temporary file and are then moved into place, so an interrupted save cannot leave
 * a half-written chunk behind.
 */
public class WorldStorage {
    private static final int LEVEL_MAGIC = 0x56424C56;  // "VBLV"
    private static final int CHUNK_MAGIC = 0x5642434B;  // "VBCK"
    private static final int FORMAT_VERSION = 1;

    /** 16 x 16 x 256, matching Chunk's block array. */
    private static final int BLOCKS_PER_CHUNK = 16 * 16 * 256;

    private final Path worldDir;
    private final Path regionDir;
    private final Path levelFile;

    public WorldStorage(String levelName) {
        this.worldDir = Paths.get(levelName);
        this.regionDir = worldDir.resolve("region");
        this.levelFile = worldDir.resolve("level.dat");
    }

    /** Creates the world and region directories if they do not exist yet. */
    public void prepare() throws IOException {
        Files.createDirectories(regionDir);
    }

    public Path worldDir() {
        return worldDir;
    }

    public boolean hasLevelData() {
        return Files.isRegularFile(levelFile);
    }

    public LevelData readLevel() throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(levelFile)))) {
            int magic = in.readInt();
            if (magic != LEVEL_MAGIC) {
                throw new IOException("not a vib-MC level.dat (magic " + Integer.toHexString(magic) + ")");
            }
            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("unsupported level.dat version " + version);
            }
            long seed = in.readLong();
            long worldTime = in.readLong();
            long timeOfDay = in.readLong();
            String weather = in.readUTF();
            return new LevelData(seed, worldTime, timeOfDay, weather);
        }
    }

    public void writeLevel(LevelData level) throws IOException {
        prepare();
        Path tmp = levelFile.resolveSibling("level.dat.tmp");
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            out.writeInt(LEVEL_MAGIC);
            out.writeInt(FORMAT_VERSION);
            out.writeLong(level.seed());
            out.writeLong(level.worldTime());
            out.writeLong(level.timeOfDay());
            out.writeUTF(level.weather());
        }
        moveIntoPlace(tmp, levelFile);
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        return Files.isRegularFile(chunkPath(chunkX, chunkZ));
    }

    /**
     * Reads a stored chunk's block array, or returns {@code null} if the chunk has
     * never been saved. A corrupt or truncated file is reported as an IOException so
     * the caller can fall back to generating fresh terrain.
     */
    public short[] readChunk(int chunkX, int chunkZ) throws IOException {
        Path path = chunkPath(chunkX, chunkZ);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (InputStream raw = Files.newInputStream(path);
             DataInputStream in = new DataInputStream(
                     new BufferedInputStream(new GZIPInputStream(raw)))) {
            int magic = in.readInt();
            if (magic != CHUNK_MAGIC) {
                throw new IOException("bad chunk magic " + Integer.toHexString(magic));
            }
            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("unsupported chunk version " + version);
            }
            int storedX = in.readInt();
            int storedZ = in.readInt();
            if (storedX != chunkX || storedZ != chunkZ) {
                throw new IOException("chunk claims to be " + storedX + "," + storedZ
                        + " but was stored as " + chunkX + "," + chunkZ);
            }
            short[] blocks = new short[BLOCKS_PER_CHUNK];
            for (int i = 0; i < BLOCKS_PER_CHUNK; i++) {
                blocks[i] = in.readShort();
            }
            return blocks;
        }
    }

    public void writeChunk(int chunkX, int chunkZ, short[] blocks) throws IOException {
        if (blocks.length != BLOCKS_PER_CHUNK) {
            throw new IOException("expected " + BLOCKS_PER_CHUNK + " blocks, got " + blocks.length);
        }
        prepare();
        Path dest = chunkPath(chunkX, chunkZ);
        Path tmp = dest.resolveSibling(dest.getFileName() + ".tmp");
        try (OutputStream raw = Files.newOutputStream(tmp);
             DataOutputStream out = new DataOutputStream(
                     new BufferedOutputStream(new GZIPOutputStream(raw)))) {
            out.writeInt(CHUNK_MAGIC);
            out.writeInt(FORMAT_VERSION);
            out.writeInt(chunkX);
            out.writeInt(chunkZ);
            for (int i = 0; i < BLOCKS_PER_CHUNK; i++) {
                out.writeShort(blocks[i]);
            }
        }
        moveIntoPlace(tmp, dest);
    }

    private Path chunkPath(int chunkX, int chunkZ) {
        return regionDir.resolve("r." + chunkX + "." + chunkZ + ".chunk");
    }

    private static void moveIntoPlace(Path tmp, Path dest) throws IOException {
        try {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
