package net.vibmc.world.gen;

import net.vibmc.world.Block;
import net.vibmc.world.Chunk;

/**
 * Scatters oak trees over the chunk. Density follows the biome - forests are dense,
 * plains sparse, deserts bare - and every placement is derived from the world seed, so a
 * chunk always grows the same trees.
 */
public final class TreeGenerator {
    private TreeGenerator() {
    }

    public static void apply(Chunk chunk, TerrainGenerator terrain) {
        int chunkX = chunk.chunkX();
        int chunkZ = chunk.chunkZ();
        Biome biome = Biome.at(terrain, chunkX * 16 + 8, chunkZ * 16 + 8);

        int attempts;
        switch (biome) {
            case DESERT:
                return;
            case FOREST:
                attempts = 8;
                break;
            case SNOW:
                attempts = 2;
                break;
            default:
                attempts = 3;
                break;
        }

        for (int t = 0; t < attempts; t++) {
            int h = terrain.hash(chunkX * 341 + t * 79, chunkZ * 977 - t * 131);
            int x = (h >> 3) % 16;
            int z = (h >> 11) % 16;
            if (x < 0 || z < 0) {
                continue;
            }
            // Keep the canopy inside this chunk: trees are placed per-chunk, so a trunk
            // near the edge would have its leaves silently clipped.
            if (x < 2 || x > 13 || z < 2 || z > 13) {
                continue;
            }
            plantAt(chunk, terrain, x, z);
        }
    }

    private static void plantAt(Chunk chunk, TerrainGenerator terrain, int x, int z) {
        int worldX = chunk.chunkX() * 16 + x;
        int worldZ = chunk.chunkZ() * 16 + z;
        int surface = terrain.surfaceHeight(worldX, worldZ);

        // Only on grass, and never in water or on a block a cave has eaten away.
        if (chunk.getBlock(x, surface, z) != Block.GRASS.id()) {
            return;
        }
        if (chunk.getBlock(x, surface + 1, z) != Block.AIR.id()) {
            return;
        }

        int height = 4 + (terrain.hash(worldX, worldZ ^ 0x5EED) % 3); // 4..6
        int base = surface + 1;
        int trunkTop = base + height - 1;

        for (int y = base; y <= trunkTop; y++) {
            chunk.setBlock(x, y, z, Block.WOOD.id());
        }

        // Canopy: a full ring at the two layers below the top, then a smaller ragged cap.
        for (int dy = -2; dy <= 1; dy++) {
            int y = trunkTop + dy;
            if (y < 1 || y > 254) {
                continue;
            }
            int radius = (dy <= -1) ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0 && dy < 1) {
                        continue; // leave the trunk alone
                    }
                    // Round the corners off so the canopy is not an obvious cube.
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius
                            && terrain.hash3(worldX + dx, y, worldZ + dz) % 100 < 60) {
                        continue;
                    }
                    int bx = x + dx;
                    int bz = z + dz;
                    if (bx < 0 || bx > 15 || bz < 0 || bz > 15) {
                        continue;
                    }
                    if (chunk.getBlock(bx, y, bz) == Block.AIR.id()) {
                        chunk.setBlock(bx, y, bz, Block.LEAVES.id());
                    }
                }
            }
        }
    }
}
