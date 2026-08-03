package net.vibmc.world;

import net.vibmc.world.gen.TerrainGenerator;

import java.io.ByteArrayOutputStream;

public class Chunk {
    private static final int WORLD_HEIGHT = 256;
    private static final int SEA_LEVEL = 62;
    private static final int BITS_PER_BLOCK = 13;
    private static final int SECTION_LONGS = 4096 * BITS_PER_BLOCK / 64;

    private final World world;
    private final int chunkX;
    private final int chunkZ;
    private final short[] blocks = new short[16 * 16 * WORLD_HEIGHT];

    private Chunk(World world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public static Chunk generate(World world, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        TerrainGenerator generator = new TerrainGenerator(world.seed());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                int height = generator.getHeight(worldX, worldZ);
                boolean underwater = height <= SEA_LEVEL;

                chunk.setBlock(x, 0, z, Block.BEDROCK.id());
                for (int y = 1; y < height - 3; y++) {
                    chunk.setBlock(x, y, z, Block.STONE.id());
                }
                if (underwater) {
                    for (int y = Math.max(1, height - 3); y < height; y++) {
                        chunk.setBlock(x, y, z, Block.GRAVEL.id());
                    }
                } else {
                    boolean beach = height <= SEA_LEVEL + 2;
                    short fill = beach ? Block.SAND.id() : Block.DIRT.id();
                    for (int y = Math.max(1, height - 3); y < height - 1; y++) {
                        chunk.setBlock(x, y, z, fill);
                    }
                    chunk.setBlock(x, height - 1, z, beach ? Block.SAND.id() : Block.GRASS.id());
                }
                if (underwater) {
                    for (int y = height; y <= SEA_LEVEL; y++) {
                        chunk.setBlock(x, y, z, Block.WATER.id());
                    }
                }
                if (!underwater && height > SEA_LEVEL + 2 && generator.hash(worldX, worldZ) % 48 == 0) {
                    placeTree(chunk, x, height, z);
                }
            }
        }
        return chunk;
    }

    private static void placeTree(Chunk chunk, int x, int baseY, int z) {
        if (x < 2 || x > 13 || z < 2 || z > 13) {
            return;
        }
        int trunkHeight = 4 + ((x + z + baseY) & 1);
        if (baseY + trunkHeight + 2 >= WORLD_HEIGHT) {
            return;
        }
        for (int i = 1; i <= trunkHeight; i++) {
            chunk.setBlock(x, baseY + i, z, Block.WOOD.id());
        }
        int topY = baseY + trunkHeight;
        for (int dy = 0; dy <= 2; dy++) {
            int radius = dy == 2 ? 1 : 2;
            int y = topY - dy;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius) {
                        continue;
                    }
                    chunk.setBlock(x + dx, y, z + dz, Block.LEAVES.id());
                }
            }
        }
        chunk.setBlock(x, topY + 1, z, Block.LEAVES.id());
        chunk.setBlock(x, topY + 2, z, Block.LEAVES.id());
    }

    public void setBlock(int x, int y, int z, short id) {
        if (inBounds(x, y, z)) {
            blocks[index(x, y, z)] = id;
        }
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

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public byte[] toNetworkData() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(16 * (SECTION_LONGS * 8 + 4096));
        for (int section = 0; section < 16; section++) {
            long[] packed = new long[SECTION_LONGS];
            int baseY = section * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        short internalId = blocks[index(x, baseY + y, z)];
                        int state = Block.protocolIdOf(internalId) << 4;
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
            // 1.12.2 section header: bits per block, palette length, data array length
            out.write(BITS_PER_BLOCK);
            writeVarInt(out, 0);
            writeVarInt(out, SECTION_LONGS);
            for (long value : packed) {
                for (int i = 7; i >= 0; i--) {
                    out.write((int) ((value >>> (i * 8)) & 0xFF));
                }
            }
            out.write(new byte[2048], 0, 2048); // block light
            byte[] skyLight = buildSkyLight(section, baseY);
            out.write(skyLight, 0, skyLight.length);
        }
        // ground-up continuous: 256 bytes of biome data (plains)
        for (int i = 0; i < 256; i++) {
            out.write(1);
        }
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

    private static byte[] buildSkyLight(int section, int baseY) {
        byte[] skyLight = new byte[2048];
        for (int y = 0; y < 16; y++) {
            int sky = (baseY + y) >= SEA_LEVEL ? 15 : 0;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int index = (y << 8) | (z << 4) | x;
                    int byteIndex = index >> 1;
                    int current = skyLight[byteIndex] & 0xFF;
                    if ((index & 1) == 1) {
                        skyLight[byteIndex] = (byte) (current | (sky << 4));
                    } else {
                        skyLight[byteIndex] = (byte) (current | sky);
                    }
                }
            }
        }
        return skyLight;
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < 16 && y >= 0 && y < WORLD_HEIGHT && z >= 0 && z < 16;
    }

    private int index(int x, int y, int z) {
        return (y * 16 + z) * 16 + x;
    }
}
