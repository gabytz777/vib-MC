package net.vibmc.world.gen;

import net.vibmc.world.Block;
import net.vibmc.world.Chunk;
import net.vibmc.world.World;

/**
 * The Nether: a great open cavern between a bedrock floor and a bedrock roof, with a lava
 * sea in the bottom of it.
 *
 * <p>The shape comes from two surfaces rather than from carving holes in a solid block of
 * netherrack. A rolling floor rises out of the lava and a ragged ceiling hangs down from
 * the roof, and the space between them is open by default - so the Nether is somewhere you
 * can walk about in, and the noise is left to do what it is good at: eating overhangs and
 * arches out of the two surfaces where they meet the open air, and threading caves through
 * the rock below.
 */
public class NetherGenerator implements ChunkGenerator {
    /** Vanilla's Nether roof; everything above it is solid bedrock. */
    private static final int ROOF = 127;
    /** Lava fills any open space at or below this height. */
    public static final int LAVA_LEVEL = 31;

    /** Bounds for the rolling netherrack floor that rises out of the lava sea. */
    private static final int FLOOR_MIN = 22;
    private static final int FLOOR_MAX = 58;
    /** Bounds for the underside of the hanging ceiling. */
    private static final int CEILING_MIN = 80;
    private static final int CEILING_MAX = 112;

    @Override
    public void generate(World world, Chunk chunk, int chunkX, int chunkZ) {
        // Offset the seed so the Nether does not mirror the overworld's noise.
        TerrainGenerator terrain = new TerrainGenerator(world.seed() ^ 0x4E45544845525F31L);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                buildColumn(chunk, terrain, x, z, worldX, worldZ);
            }
        }

        decorate(chunk, terrain, chunkX, chunkZ);
    }

    private void buildColumn(Chunk chunk, TerrainGenerator terrain, int x, int z,
                             int worldX, int worldZ) {
        int floorTop = floorHeight(terrain, worldX, worldZ);
        int ceilingBottom = ceilingHeight(terrain, worldX, worldZ);

        chunk.setBlock(x, 0, z, Block.BEDROCK.id());
        for (int y = 1; y <= 4; y++) {
            boolean bedrock = terrain.hash3(worldX, y, worldZ) % 5 < (5 - y);
            chunk.setBlock(x, y, z, bedrock ? Block.BEDROCK.id() : Block.NETHERRACK.id());
        }

        for (int y = 5; y < ROOF; y++) {
            boolean solid = isSolid(terrain, worldX, y, worldZ, floorTop, ceilingBottom);
            if (solid) {
                chunk.setBlock(x, y, z, stoneOrOre(terrain, worldX, y, worldZ));
            } else {
                // The lava sea sits in whatever the terrain left open below its level.
                chunk.setBlock(x, y, z, y <= LAVA_LEVEL ? Block.LAVA.id() : Block.AIR.id());
            }
        }

        // Solid bedrock roof, ragged on its underside like vanilla's.
        chunk.setBlock(x, ROOF, z, Block.BEDROCK.id());
        for (int y = ROOF - 4; y < ROOF; y++) {
            if (terrain.hash3(worldX, y ^ 0x77, worldZ) % 4 < (y - (ROOF - 5))) {
                chunk.setBlock(x, y, z, Block.BEDROCK.id());
            }
        }
    }

    /** Height of the netherrack floor in this column. */
    private static int floorHeight(TerrainGenerator terrain, int worldX, int worldZ) {
        double shape = terrain.fbm(worldX * 0.011, worldZ * 0.011, 4);
        int middle = (FLOOR_MIN + FLOOR_MAX) / 2;
        return clamp((int) Math.round(middle + shape * (FLOOR_MAX - FLOOR_MIN) * 0.5),
                FLOOR_MIN, FLOOR_MAX);
    }

    /** Height the hanging ceiling reaches down to in this column. */
    private static int ceilingHeight(TerrainGenerator terrain, int worldX, int worldZ) {
        // A different corner of the noise field, so ceiling and floor are unrelated and
        // the cavern is not a uniform-height tunnel.
        double shape = terrain.fbm(worldX * 0.009 + 500, worldZ * 0.009 - 500, 3);
        int middle = (CEILING_MIN + CEILING_MAX) / 2;
        return clamp((int) Math.round(middle + shape * (CEILING_MAX - CEILING_MIN) * 0.5),
                CEILING_MIN, CEILING_MAX);
    }

    /**
     * Whether this block is rock.
     *
     * <p>The two surfaces give a base value - strongly solid deep in the floor or high in
     * the ceiling, strongly open in the cavern between them - and noise is added to it.
     * Near a surface the two are comparable, so the noise wins often enough to produce
     * overhangs, pillars and arches; deep inside the rock only the second, thinner noise
     * field gets through, and that is what leaves caves down there.
     */
    private static boolean isSolid(TerrainGenerator terrain, int x, int y, int z,
                                   int floorTop, int ceilingBottom) {
        double base;
        if (y <= floorTop) {
            base = (floorTop - y) / 11.0;
        } else if (y >= ceilingBottom) {
            base = (y - ceilingBottom) / 9.0;
        } else {
            base = -Math.min(y - floorTop, ceilingBottom - y) / 7.0;
        }

        double shape = terrain.fbm3(x * 0.028, y * 0.045, z * 0.028, 3);
        if (base + shape * 1.2 <= 0.28) {
            return false;
        }

        // Winding caves through the rock, from two noise fields crossing near zero - the
        // same trick the overworld's caves use, so they connect rather than pocket.
        double a = terrain.noise3(x * 0.03 + 200, y * 0.055, z * 0.03 + 200);
        double b = terrain.noise3(x * 0.03 - 700, y * 0.055 + 90, z * 0.03 - 700);
        return a * a + b * b >= 0.016;
    }

    /** Netherrack, with the occasional vein of quartz in it. */
    private static short stoneOrOre(TerrainGenerator terrain, int x, int y, int z) {
        if (terrain.hash3(x ^ 0x5151, y, z) % 1000 < 13) {
            return Block.QUARTZ_ORE.id();
        }
        return Block.NETHERRACK.id();
    }

    /**
     * Dresses the raw shape: soul sand on the shores of the lava sea, and glowstone
     * clusters growing down out of the ceiling.
     */
    private void decorate(Chunk chunk, TerrainGenerator terrain, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                for (int y = 5; y < ROOF - 1; y++) {
                    short here = chunk.getBlock(x, y, z);
                    if (here != Block.NETHERRACK.id()) {
                        continue;
                    }
                    boolean openAbove = chunk.getBlock(x, y + 1, z) == Block.AIR.id();
                    boolean openBelow = chunk.getBlock(x, y - 1, z) == Block.AIR.id();

                    // Soul sand banks, low down where the lava is.
                    if (openAbove && y <= LAVA_LEVEL + 12
                            && terrain.fbm3(worldX * 0.06, y * 0.06, worldZ * 0.06, 2) > 0.42) {
                        chunk.setBlock(x, y, z, Block.SOUL_SAND.id());
                        continue;
                    }

                    // Glowstone hangs from the underside of the rock, in small clumps so
                    // the cavern has something to light it other than the lava.
                    if (openBelow && y > LAVA_LEVEL + 6
                            && terrain.hash3(worldX, y, worldZ) % 1400 < 3) {
                        growGlowstone(chunk, terrain, x, y, z, worldX, worldZ);
                    }
                }
            }
        }
    }

    /** A small blob of glowstone hanging below the block it grew from. */
    private void growGlowstone(Chunk chunk, TerrainGenerator terrain, int x, int y, int z,
                               int worldX, int worldZ) {
        chunk.setBlock(x, y, z, Block.GLOWSTONE.id());
        int drop = 1 + terrain.hash3(worldX, y + 1, worldZ) % 3;
        for (int dy = 1; dy <= drop; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) + dy > 2) {
                        continue;
                    }
                    int bx = x + dx;
                    int bz = z + dz;
                    if (bx < 0 || bx > 15 || bz < 0 || bz > 15 || y - dy < 1) {
                        continue;
                    }
                    if (chunk.getBlock(bx, y - dy, bz) == Block.AIR.id()) {
                        chunk.setBlock(bx, y - dy, bz, Block.GLOWSTONE.id());
                    }
                }
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
