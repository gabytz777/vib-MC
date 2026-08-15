package net.vibmc.world.gen;

import net.vibmc.world.Block;
import net.vibmc.world.Chunk;
import net.vibmc.world.World;

/**
 * The Nether: a slab of netherrack between bedrock floor and bedrock roof, hollowed out
 * into caverns, with lava seas at the bottom and soul sand and glowstone scattered about.
 */
public class NetherGenerator implements ChunkGenerator {
    /** Vanilla's Nether roof; everything above it is solid bedrock. */
    private static final int ROOF = 127;
    private static final int LAVA_LEVEL = 31;

    @Override
    public void generate(World world, Chunk chunk, int chunkX, int chunkZ) {
        // Offset the seed so the Nether does not mirror the overworld's noise.
        TerrainGenerator terrain = new TerrainGenerator(world.seed() ^ 0x4E45544845525F31L);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                chunk.setBlock(x, 0, z, Block.BEDROCK.id());
                for (int y = 1; y <= 3; y++) {
                    boolean bedrock = terrain.hash3(worldX, y, worldZ) % 4 < (4 - y);
                    chunk.setBlock(x, y, z, bedrock ? Block.BEDROCK.id() : Block.NETHERRACK.id());
                }

                for (int y = 4; y < ROOF; y++) {
                    chunk.setBlock(x, y, z, netherBlockAt(terrain, worldX, y, worldZ));
                }

                // Solid bedrock roof, ragged on its underside like vanilla's.
                for (int y = ROOF; y <= 127; y++) {
                    chunk.setBlock(x, y, z, Block.BEDROCK.id());
                }
                for (int y = ROOF - 4; y < ROOF; y++) {
                    if (terrain.hash3(worldX, y ^ 0x77, worldZ) % 4 < (y - (ROOF - 5))) {
                        chunk.setBlock(x, y, z, Block.BEDROCK.id());
                    }
                }
            }
        }
    }

    private short netherBlockAt(TerrainGenerator terrain, int x, int y, int z) {
        // Caverns come from 3D noise, so they run continuously across chunk borders.
        double cavern = terrain.fbm3(x * 0.028, y * 0.05, z * 0.028, 3);
        boolean open = cavern > 0.18;

        if (open) {
            // Below the lava level the open space fills with lava instead of air.
            return y <= LAVA_LEVEL ? Block.LAVA.id() : Block.AIR.id();
        }
        if (y <= LAVA_LEVEL + 2 && terrain.fbm3(x * 0.05, y * 0.05, z * 0.05, 2) > 0.45) {
            return Block.SOUL_SAND.id();
        }
        if (terrain.hash3(x, y, z) % 1000 < 4 && y > LAVA_LEVEL) {
            return Block.GLOWSTONE.id();
        }
        return Block.NETHERRACK.id();
    }
}
