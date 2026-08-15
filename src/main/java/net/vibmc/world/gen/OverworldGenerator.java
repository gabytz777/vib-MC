package net.vibmc.world.gen;

import net.vibmc.world.Block;
import net.vibmc.world.Chunk;
import net.vibmc.world.World;
import net.vibmc.world.gen.structure.VillageGenerator;

/**
 * The overworld: oceans and continents around vanilla's sea level, with beaches, layered
 * soil over stone, cave systems, ores, trees and villages.
 */
public class OverworldGenerator implements ChunkGenerator {
    private static final int SEA_LEVEL = TerrainGenerator.SEA_LEVEL;
    /** Depth of the dirt/sand layer that sits between the surface block and the stone. */
    private static final int SOIL_DEPTH = 4;

    @Override
    public void generate(World world, Chunk chunk, int chunkX, int chunkZ) {
        TerrainGenerator terrain = new TerrainGenerator(world.seed());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                buildColumn(chunk, terrain, x, z, worldX, worldZ);
            }
        }

        carveCaves(chunk, terrain, chunkX, chunkZ);
        VillageGenerator.apply(chunk, terrain);
        TreeGenerator.apply(chunk, terrain);
    }

    private void buildColumn(Chunk chunk, TerrainGenerator terrain, int x, int z,
                             int worldX, int worldZ) {
        int surface = terrain.surfaceHeight(worldX, worldZ);
        Biome biome = Biome.at(terrain, worldX, worldZ);
        boolean underwater = surface <= SEA_LEVEL;
        boolean beach = terrain.isBeach(worldX, worldZ);

        // Bedrock floor: a solid layer plus a ragged couple of layers above it, so it
        // looks like vanilla's rather than a flat plate.
        chunk.setBlock(x, 0, z, Block.BEDROCK.id());
        for (int y = 1; y <= 4; y++) {
            if (terrain.hash3(worldX, y, worldZ) % 5 < (5 - y)) {
                chunk.setBlock(x, y, z, Block.BEDROCK.id());
            } else {
                chunk.setBlock(x, y, z, stoneOrOre(terrain, worldX, y, worldZ));
            }
        }

        int soilTop = surface - 1;
        int stoneTop = Math.max(5, surface - SOIL_DEPTH);
        for (int y = 5; y <= stoneTop; y++) {
            chunk.setBlock(x, y, z, stoneOrOre(terrain, worldX, y, worldZ));
        }

        // Soil band, then the surface block itself.
        short soil = (underwater || beach) ? Block.SAND.id()
                : (biome == Biome.DESERT ? Block.SAND.id() : Block.DIRT.id());
        for (int y = stoneTop + 1; y <= soilTop; y++) {
            chunk.setBlock(x, y, z, soil);
        }

        short top;
        if (underwater || beach) {
            top = Block.SAND.id();
        } else if (biome == Biome.DESERT) {
            top = Block.SAND.id();
        } else {
            top = Block.GRASS.id();
        }
        chunk.setBlock(x, surface, z, top);

        // Fill the sea, and cap cold land with snow.
        if (surface < SEA_LEVEL) {
            for (int y = surface + 1; y <= SEA_LEVEL; y++) {
                chunk.setBlock(x, y, z, Block.WATER.id());
            }
        } else if (biome == Biome.SNOW && !beach) {
            chunk.setBlock(x, surface + 1, z, Block.SNOW.id());
        }
    }

    private static short stoneOrOre(TerrainGenerator terrain, int x, int y, int z) {
        // Ore bands mirror vanilla's depth ranges: coal is common and shallow, iron sits
        // lower and is rarer. Both come from the seed, so a given block is always the same.
        if (y <= 128 && terrain.hash3(x, y, z) % 1000 < 11) {
            return Block.COAL_ORE.id();
        }
        if (y <= 64 && terrain.hash3(x ^ 0x1A2B, y, z) % 1000 < 7) {
            return Block.IRON_ORE.id();
        }
        return stoneMix(terrain, x, y, z);
    }

    /** Stone with occasional andesite and diorite patches, as blobs rather than noise. */
    private static short stoneMix(TerrainGenerator terrain, int x, int y, int z) {
        double blob = terrain.fbm3(x * 0.08, y * 0.08, z * 0.08, 2);
        if (blob > 0.62) {
            return Block.ANDESITE.id();
        }
        if (blob < -0.62) {
            return Block.DIORITE.id();
        }
        return Block.STONE.id();
    }

    /**
     * Carves caves from 3D noise. Two offset noise fields are multiplied so tunnels form
     * where both are near zero, which produces winding connected passages instead of
     * spherical bubbles - and, being coordinate-based, they cross chunk borders seamlessly.
     */
    private void carveCaves(Chunk chunk, TerrainGenerator terrain, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                int surface = terrain.surfaceHeight(worldX, worldZ);

                for (int y = 5; y < Math.min(surface, 120); y++) {
                    double a = terrain.noise3(worldX * 0.035, y * 0.07, worldZ * 0.035);
                    double b = terrain.noise3(worldX * 0.035 + 100, y * 0.07 + 100, worldZ * 0.035 + 100);
                    double density = a * a + b * b;

                    // Taper the caves off near the surface so the world is not perforated,
                    // except where an entrance is allowed to break through.
                    double threshold = 0.020;
                    if (y > surface - 8) {
                        threshold *= (surface - y) / 8.0;
                    }
                    if (density < threshold) {
                        // Never open the floor of the sea into a cave.
                        if (y >= surface && surface <= SEA_LEVEL) {
                            continue;
                        }
                        chunk.setBlock(x, y, z, Block.AIR.id());
                    }
                }
            }
        }
    }
}
