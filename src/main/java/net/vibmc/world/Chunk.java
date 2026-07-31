package net.vibmc.world;

import java.util.Arrays;

public class Chunk {
    private static final int WORLD_HEIGHT = 256;
    private static final int SEA_LEVEL = 62;
    private final World world;
    private final int chunkX;
    private final int chunkZ;
    private final short[] blocks = new short[16 * 16 * WORLD_HEIGHT];
    private final boolean[] light = new boolean[16 * 16 * WORLD_HEIGHT];

    private Chunk(World world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public static Chunk generate(World world, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        long seed = world.seed();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                int height = sampleHeight(seed, worldX, worldZ);
                for (int y = 0; y < height - 4; y++) {
                    chunk.setBlock(x, y, z, Block.STONE.id());
                }
                for (int y = Math.max(0, height - 4); y < height - 1; y++) {
                    chunk.setBlock(x, y, z, Block.DIRT.id());
                }
                if (height >= 0 && height < WORLD_HEIGHT) {
                    chunk.setBlock(x, height - 1, z, Block.GRASS.id());
                }
                if (height < SEA_LEVEL) {
                    for (int y = height; y <= SEA_LEVEL && y < WORLD_HEIGHT; y++) {
                        chunk.setBlock(x, y, z, Block.WATER.id());
                    }
                }
            }
        }
        return chunk;
    }

    public void tick(long tick) {
        if (tick % 20 == 0) {
            Arrays.fill(light, false);
        }
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

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < 16 && y >= 0 && y < WORLD_HEIGHT && z >= 0 && z < 16;
    }

    private int index(int x, int y, int z) {
        return (y * 16 + z) * 16 + x;
    }

    private static int sampleHeight(long seed, int worldX, int worldZ) {
        double baseNoise = Math.sin((worldX + seed * 0.0001) * 0.08)
                + Math.cos((worldZ - seed * 0.0001) * 0.08);
        double detailNoise = Math.sin((worldX + worldZ + seed) * 0.02) * 4.0;
        int height = 64 + (int) (baseNoise * 8.0 + detailNoise);
        return Math.max(1, Math.min(WORLD_HEIGHT - 1, height));
    }
}
