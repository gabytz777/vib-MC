package net.vibmc.world;

import net.vibmc.world.gen.Biome;
import net.vibmc.world.gen.TerrainGenerator;
import net.vibmc.world.gen.structure.VillageGenerator;

import java.io.ByteArrayOutputStream;

public class Chunk {
    private static final int WORLD_HEIGHT = 256;
    private static final int SEA_LEVEL = 13;      // water fills up to 4 deep (surface 9 -> y10..13)
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
    }

    /** Rebuilds a chunk from previously saved block data instead of regenerating it. */
    public static Chunk fromStored(World world, int chunkX, int chunkZ, short[] stored) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        System.arraycopy(stored, 0, chunk.blocks, 0, chunk.blocks.length);
        // Biomes aren't part of the saved chunk format; they're cheap to recompute
        // deterministically from the seed, same as everything else on a fresh load.
        computeBiomes(chunk, new TerrainGenerator(world.seed()));
        chunk.dirty = false;
        return chunk;
    }

    public static Chunk generate(World world, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        TerrainGenerator terrain = new TerrainGenerator(world.seed());
        computeBiomes(chunk, terrain);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                Biome biome = chunk.biomeAt(x, z);

                // surface 9..15 from noise: grass top at y=surface
                int surface = terrain.surfaceHeight(worldX, worldZ);

                chunk.setBlock(x, 0, z, Block.BEDROCK.id());
                for (int y = 1; y <= surface - 2; y++) {
                    chunk.setBlock(x, y, z, stoneOrOre(terrain, worldX, y, worldZ));
                }
                // puddle bed: sand instead of grass, water up to 4 deep
                if (surface < SEA_LEVEL) {
                    chunk.setBlock(x, surface - 1, z, Block.SAND.id());
                    chunk.setBlock(x, surface, z, Block.SAND.id());
                    for (int y = surface + 1; y <= SEA_LEVEL; y++) {
                        chunk.setBlock(x, y, z, Block.WATER.id());
                    }
                } else if (biome == Biome.DESERT) {
                    // desert: sand instead of grass, no snow cap
                    chunk.setBlock(x, surface - 1, z, Block.SAND.id());
                    chunk.setBlock(x, surface, z, Block.SAND.id());
                } else {
                    // grass: 2 layers
                    chunk.setBlock(x, surface - 1, z, Block.GRASS.id());
                    chunk.setBlock(x, surface, z, Block.GRASS.id());
                    if (biome == Biome.SNOW) {
                        chunk.setBlock(x, surface + 1, z, Block.SNOW.id());
                    }
                }
            }
        }

        carveCaves(chunk, terrain);
        VillageGenerator.apply(chunk, terrain);

        // trees: biome drives density. Deserts get none, forests get more.
        Biome centerBiome = chunk.biomeAt(8, 8);
        int trees;
        if (centerBiome == Biome.DESERT) {
            trees = 0;
        } else {
            int roll = terrain.hash(chunkX, chunkZ ^ 0x7E5A) % 100;
            if (centerBiome == Biome.FOREST) {
                trees = roll < 70 ? 2 : (roll < 95 ? 3 : 1); // dense: 1-3 trees
            } else {
                trees = roll < 40 ? 1 : (roll < 50 ? 2 : 0); // plains/snow: sparse
            }
        }
        for (int t = 0; t < trees; t++) {
            int h = terrain.hash(chunkX * 31 + t, chunkZ ^ 0x3F);
            int x = h % 16;
            int z = (h / 16) % 16;
            int topY = -1;
            for (int y = 15; y >= 1; y--) {
                if (chunk.getBlock(x, y, z) != Block.AIR.id()) {
                    topY = y;
                    break;
                }
            }
            if (topY < 0 || chunk.getBlock(x, topY, z) != Block.GRASS.id()) {
                continue;
            }
            int height = 4 + (terrain.hash(x, z ^ 0x11) % 3); // 4..6
            placeTree(chunk, terrain, x, topY, z, height);
        }
        chunk.dirty = true;
        return chunk;
    }

    private static void computeBiomes(Chunk chunk, TerrainGenerator terrain) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.chunkX * 16 + x;
                int worldZ = chunk.chunkZ * 16 + z;
                chunk.biomes[z * 16 + x] = (byte) Biome.at(terrain, worldX, worldZ).protocolId();
            }
        }
    }

    private Biome biomeAt(int x, int z) {
        int id = biomes[z * 16 + x] & 0xFF;
        for (Biome biome : Biome.values()) {
            if (biome.protocolId() == id) {
                return biome;
            }
        }
        return Biome.PLAINS;
    }

    private static void placeTree(Chunk chunk, TerrainGenerator terrain, int x, int topY, int z, int height) {
        int trunkTop = topY + height;
        for (int y = topY + 1; y <= trunkTop; y++) {
            chunk.setBlock(x, y, z, Block.WOOD.id());
        }
        // organic canopy: ragged 3x3 blob, random per cell
        for (int dy = -2; dy <= 1; dy++) {
            if (dy == -2 && height < 5) {
                continue; // lowest scatter layer only for tall trees
            }
            int y = trunkTop + dy;
            if (y < 1 || y >= 255) {
                continue;
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue; // trunk column
                    }
                    int bx = x + dx;
                    int bz = z + dz;
                    if (bx < 0 || bx > 15 || bz < 0 || bz > 15) {
                        continue;
                    }
                    if (chunk.getBlock(bx, y, bz) != Block.AIR.id()) {
                        continue;
                    }
                    int dist = Math.max(Math.abs(dx), Math.abs(dz));
                    int chance;
                    if (dy == 1) {
                        chance = dist == 0 ? 90 : 40;              // ragged cap
                    } else if (dy == 0) {
                        chance = dist == 1 ? 75 : 0;                // ring around trunk top
                    } else if (dy == -1) {
                        chance = dist == 1 ? 55 : 12;               // fill layer + random nubs
                    } else {
                        chance = dist == 1 ? 30 : 10;               // bottom scatter
                    }
                    if (chance == 0) {
                        continue;
                    }
                    if (terrain.hash(bx * 517 + y, bz * 433) % 100 < chance) {
                        chunk.setBlock(bx, y, bz, Block.LEAVES.id());
                    }
                }
            }
        }
    }

    private static short stoneMix(TerrainGenerator terrain, int x, int y, int z) {
        int h = terrain.hash(x, z ^ (y * 7919));
        switch (h % 10) {
            case 0:
            case 1:
            case 2:
            case 3:
                return Block.STONE.id();
            case 4:
            case 5:
            case 6:
                return Block.ANDESITE.id();
            default:
                return Block.DIORITE.id();
        }
    }

    private static short stoneOrOre(TerrainGenerator terrain, int x, int y, int z) {
        // coal: down to y=8, ~7% of stone blocks; iron: down to y=5, ~4%
        if (y <= 8 && terrain.hash(x, z ^ (y * 7919)) % 100 < 7) {
            return Block.COAL_ORE.id();
        }
        if (y <= 5 && terrain.hash(x, z ^ (y * 7919) ^ 0x1A2B) % 100 < 4) {
            return Block.IRON_ORE.id();
        }
        return stoneMix(terrain, x, y, z);
    }

    private static void carveCaves(Chunk chunk, TerrainGenerator terrain) {
        // ~55% of chunks get one cave worm, ~15% get two
        int roll = terrain.hash(chunk.chunkX, chunk.chunkZ ^ 0xC0FFEE) % 100;
        int caves = roll < 55 ? 1 : (roll < 70 ? 2 : 0);
        for (int c = 0; c < caves; c++) {
            int h = terrain.hash(chunk.chunkX * 131 + c, chunk.chunkZ ^ 0xBEEF);
            int x = h % 16;
            int z = (h / 16) % 16;
            // ~35% of caves open at the surface (dry land only) and dig down
            boolean entrance = terrain.hash(chunk.chunkX + c * 7, chunk.chunkZ ^ 0xDEAD) % 100 < 35;
            int y;
            int dy = -1;
            if (entrance) {
                int topY = -1;
                for (int yy = 15; yy >= 1; yy--) {
                    short b = chunk.getBlock(x, yy, z);
                    if (b != Block.AIR.id() && b != Block.WATER.id()) {
                        topY = yy;
                        break;
                    }
                }
                if (topY >= SEA_LEVEL) {
                    y = topY;
                } else {
                    y = 4 + (terrain.hash(x * 7, z * 13) % 8);
                    entrance = false;
                }
            } else {
                y = 4 + (terrain.hash(x * 7, z * 13) % 8);
            }
            int steps = 25 + (terrain.hash(x + c, z * 3) % 25); // 25..49
            int dx = 0;
            int dz = 0;
            for (int s = 0; s < steps; s++) {
                carvePocket(chunk, x, y, z, 1 + (terrain.hash(x * 31 + s, z * 17 + s) % 2), entrance);
                int t = terrain.hash(x * 37 + s * 7, z * 53 + s * 3) % 10;
                if (t < 3) {
                    dx = 1; dz = 0; dy = 0;
                } else if (t < 5) {
                    dx = -1; dz = 0; dy = 0;
                } else if (t < 7) {
                    dx = 0; dz = 1; dy = 0;
                } else if (t < 9) {
                    dx = 0; dz = -1; dy = 0;
                } else {
                    dx = 0; dz = 0;
                    dy = terrain.hash(x, z ^ s) % 2 == 0 ? 1 : -1;
                }
                x += dx;
                z += dz;
                y += dy;
                if (y < 1) {
                    y = 1;
                }
                if (y > 12) {
                    y = 12;
                }
                // bounce off chunk edges so worms stay inside the chunk
                if (x < 0) { x = 0; }
                if (x > 15) { x = 15; }
                if (z < 0) { z = 0; }
                if (z > 15) { z = 15; }
            }
        }
    }

    private static void carvePocket(Chunk chunk, int cx, int cy, int cz, int radius, boolean entrance) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius + 1) {
                        continue; // rough sphere
                    }
                    int x = cx + dx;
                    int y = cy + dy;
                    int z = cz + dz;
                    if (x < 0 || x > 15 || z < 0 || z > 15 || y < 1 || y > 13) {
                        continue;
                    }
                    short b = chunk.getBlock(x, y, z);
                    if (b == Block.STONE.id() || b == Block.ANDESITE.id() || b == Block.DIORITE.id()
                            || b == Block.COAL_ORE.id() || b == Block.IRON_ORE.id()) {
                        chunk.setBlock(x, y, z, Block.AIR.id());
                    } else if (entrance && (b == Block.GRASS.id() || b == Block.SAND.id() || b == Block.SNOW.id())) {
                        // surface caves can breach the top layers
                        chunk.setBlock(x, y, z, Block.AIR.id());
                    }
                }
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
            out.write(new byte[2048], 0, 2048); // block light
            byte[] skyLight = buildSkyLight(section, baseY);
            out.write(skyLight, 0, skyLight.length);
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
