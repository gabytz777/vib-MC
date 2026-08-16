package net.vibmc.world;

import net.vibmc.world.gen.Biome;
import net.vibmc.world.gen.TerrainGenerator;

import java.io.ByteArrayOutputStream;

/**
 * A 16x256x16 column of blocks, plus the 1.12.2 wire encoding for it.
 *
 * <p>This class stores and serialises; deciding what blocks go in a new chunk is the job
 * of a {@link net.vibmc.world.gen.ChunkGenerator}, and reloading an existing one is the
 * job of {@link net.vibmc.world.storage.WorldStorage}.
 */
public class Chunk {
    public static final int WORLD_HEIGHT = 256;
    private static final int BITS_PER_BLOCK = 13; // canonical 1.12.2 global palette (vanilla uses 13 bits); prismarine clamps to 12 for its own storage
    private static final int SECTION_LONGS = 4096 * BITS_PER_BLOCK / 64;

    private final World world;
    private final int chunkX;
    private final int chunkZ;
    private final short[] blocks = new short[16 * 16 * WORLD_HEIGHT];
    private final byte[] biomes = new byte[16 * 16];

    /** Set whenever blocks change, cleared once the chunk has been written to disk. */
    private volatile boolean dirty;

    private Chunk(World world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        computeBiomes();
    }

    /** Rebuilds a chunk from previously saved block data instead of regenerating it. */
    public static Chunk fromStored(World world, int chunkX, int chunkZ, short[] stored) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        System.arraycopy(stored, 0, chunk.blocks, 0, chunk.blocks.length);
        chunk.dirty = false;
        return chunk;
    }

    /** Builds a new chunk using the world's generator for its dimension. */
    public static Chunk generate(World world, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        world.generator().generate(world, chunk, chunkX, chunkZ);
        chunk.dirty = true;
        return chunk;
    }

    /**
     * Biomes are not part of the saved chunk format - they are a pure function of the seed
     * and coordinates, so recomputing them on load is cheaper than storing them and keeps
     * old saves readable if the biome rules ever change.
     */
    private void computeBiomes() {
        Dimension dimension = world.dimension();
        if (dimension != Dimension.OVERWORLD) {
            java.util.Arrays.fill(biomes, (byte) dimension.defaultBiomeId());
            return;
        }
        TerrainGenerator terrain = new TerrainGenerator(world.seed());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                biomes[z * 16 + x] = (byte) Biome.at(terrain, worldX, worldZ).protocolId();
            }
        }
    }

    public void setBlock(int x, int y, int z, short id) {
        if (inBounds(x, y, z)) {
            int index = index(x, y, z);
            if (blocks[index] != id) {
                blocks[index] = id;
                dirty = true;
            }
        }
    }

    /** True when this chunk holds changes that are not on disk yet. */
    public boolean isDirty() {
        return dirty;
    }

    /** Marks the chunk as needing a write on the next save. */
    public void markDirty() {
        dirty = true;
    }

    /** Called by the chunk manager once the chunk has been written out. */
    public void markSaved() {
        dirty = false;
    }

    public short getBlock(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            return Block.AIR.id();
        }
        return blocks[index(x, y, z)];
    }

    public short[] blocks() {
        return blocks;
    }

    public World world() {
        return world;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    /** Highest non-air block in a column, or -1 if the column is empty. */
    public int highestBlock(int x, int z) {
        for (int y = WORLD_HEIGHT - 1; y >= 0; y--) {
            if (getBlock(x, y, z) != Block.AIR.id()) {
                return y;
            }
        }
        return -1;
    }

    public byte[] toNetworkData() {
        boolean skyLight = world.dimension().hasSkyLight();
        LightEngine.Light block = LightEngine.blockLight(this);
        LightEngine.Light sky = skyLight ? LightEngine.skyLight(this) : null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(16 * (SECTION_LONGS * 8 + 4096));
        for (int section = 0; section < 16; section++) {
            long[] packed = new long[SECTION_LONGS];
            int baseY = section * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        short internalId = blocks[index(x, baseY + y, z)];
                        int state = Block.stateIdOf(internalId);
                        int i = (y << 8) | (z << 4) | x;
                        int bitIndex = i * BITS_PER_BLOCK;
                        int startLong = bitIndex >> 6;
                        int startOffset = bitIndex & 63;
                        packed[startLong] |= ((long) state) << startOffset;
                        int endOffset = startOffset + BITS_PER_BLOCK;
                        if (endOffset > 64) {
                            packed[startLong + 1] |= ((long) state) >> (64 - startOffset);
                        }
                    }
                }
            }
            // 1.12.2 section header: bits per block, palette length (0 = global palette), data array length
            out.write(BITS_PER_BLOCK);
            writeVarInt(out, 0);
            writeVarInt(out, SECTION_LONGS);
            for (long value : packed) {
                for (int i = 7; i >= 0; i--) {
                    out.write((int) ((value >>> (i * 8)) & 0xFF));
                }
            }
            byte[] blockLight = block.section(section);
            out.write(blockLight, 0, blockLight.length);
            // Sky light is only present in dimensions that have a sky. Sending it for the
            // Nether or End makes the client light them as if the sun reached underground.
            if (skyLight) {
                byte[] skyLightSection = sky.section(section);
                out.write(skyLightSection, 0, skyLightSection.length);
            }
        }
        // ground-up continuous: 256 bytes of biome data, one per column
        out.write(biomes, 0, biomes.length);
        return out.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        do {
            int part = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }
            out.write(part);
        } while (value != 0);
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < 16 && y >= 0 && y < WORLD_HEIGHT && z >= 0 && z < 16;
    }

    private int index(int x, int y, int z) {
        return (y * 16 + z) * 16 + x;
    }
}
