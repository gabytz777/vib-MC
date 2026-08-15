package net.vibmc.world.gen.structure;

import net.vibmc.world.Block;
import net.vibmc.world.Chunk;
import net.vibmc.world.gen.TerrainGenerator;

/**
 * Places small villages: a handful of single-room houses arranged around a center
 * point, connected by gravel paths. Deterministic from the world seed alone (world
 * coordinates and a hash, nothing else), so a village stamps identically no matter
 * which chunk generation happens to trigger it first, and a chunk that only overlaps
 * the edge of a village correctly renders just its own slice of it.
 *
 * <p>Kept intentionally simple — a single building shape, one path pattern — so more
 * structure types can be added later as siblings of this class rather than by growing
 * this one.
 */
public final class VillageGenerator {
    /** World is carved into non-overlapping square regions; each may spawn one village. */
    private static final int REGION_SIZE = 48;
    /** Percent chance (0-99) that a given region has a village. */
    private static final int VILLAGE_CHANCE = 14;
    private static final int HOUSE_HALF = 2; // 5x5 footprint
    // House radius tops out at 13 (8..13) plus HOUSE_HALF: every placed block stays
    // within ~15 of the village origin, comfortably inside the +-1 region search below
    // (REGION_SIZE=48), so checking just the neighboring regions is always enough.

    private VillageGenerator() {
    }

    /** Stamps every village whose footprint could reach into this chunk. */
    public static void apply(Chunk chunk, TerrainGenerator terrain) {
        int regionX = Math.floorDiv(chunk.chunkX() * 16, REGION_SIZE);
        int regionZ = Math.floorDiv(chunk.chunkZ() * 16, REGION_SIZE);
        for (int rx = regionX - 1; rx <= regionX + 1; rx++) {
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++) {
                placeVillage(chunk, terrain, rx, rz);
            }
        }
    }

    private static void placeVillage(Chunk chunk, TerrainGenerator terrain, int regionX, int regionZ) {
        if (terrain.hash(regionX * 815341 + 91, regionZ * 815341 - 91) % 100 >= VILLAGE_CHANCE) {
            return;
        }

        int originX = regionX * REGION_SIZE + (terrain.hash(regionX, regionZ ^ 0x1234) % REGION_SIZE);
        int originZ = regionZ * REGION_SIZE + (terrain.hash(regionX ^ 0x777, regionZ) % REGION_SIZE);

        if (!isBuildable(terrain, originX, originZ)) {
            return; // unsuitable terrain: no village here, deterministically, every time
        }
        int originHeight = terrain.surfaceHeight(originX, originZ);

        int houseCount = 3 + (terrain.hash(regionX * 13, regionZ * 17) % 3); // 3..5
        for (int i = 0; i < houseCount; i++) {
            double angle = (2 * Math.PI * i / houseCount)
                    + (terrain.hash(regionX + i, regionZ - i) % 100) / 100.0;
            int radius = 8 + (terrain.hash(regionX * 31 + i, regionZ * 37 - i) % 6); // 8..13
            int houseX = originX + (int) Math.round(radius * Math.cos(angle));
            int houseZ = originZ + (int) Math.round(radius * Math.sin(angle));

            if (!isBuildable(terrain, houseX, houseZ)) {
                continue;
            }
            if (Math.abs(terrain.surfaceHeight(houseX, houseZ) - originHeight) > 3) {
                continue; // too steep a step from the village center
            }

            placePath(chunk, terrain, originX, originZ, houseX, houseZ);
            placeHouse(chunk, terrain, houseX, houseZ);
        }
    }

    private static boolean isBuildable(TerrainGenerator terrain, int worldX, int worldZ) {
        if (!terrain.isDryLand(worldX, worldZ)) {
            return false;
        }
        int center = terrain.surfaceHeight(worldX, worldZ);
        // reject steep ground: sample the footprint corners for a big elevation swing
        for (int dx = -HOUSE_HALF; dx <= HOUSE_HALF; dx += HOUSE_HALF * 2) {
            for (int dz = -HOUSE_HALF; dz <= HOUSE_HALF; dz += HOUSE_HALF * 2) {
                if (!terrain.isDryLand(worldX + dx, worldZ + dz)) {
                    return false;
                }
                if (Math.abs(terrain.surfaceHeight(worldX + dx, worldZ + dz) - center) > 2) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void placePath(Chunk chunk, TerrainGenerator terrain, int x0, int z0, int x1, int z1) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        if (steps == 0) {
            return;
        }
        for (int s = 0; s <= steps; s++) {
            int x = x0 + Math.round((x1 - x0) * (float) s / steps);
            int z = z0 + Math.round((z1 - z0) * (float) s / steps);
            int y = terrain.surfaceHeight(x, z);
            setWorldBlock(chunk, x, y, z, Block.GRAVEL.id());
        }
    }

    private static void placeHouse(Chunk chunk, TerrainGenerator terrain, int centerX, int centerZ) {
        int y = terrain.surfaceHeight(centerX, centerZ);

        // foundation
        for (int dx = -HOUSE_HALF; dx <= HOUSE_HALF; dx++) {
            for (int dz = -HOUSE_HALF; dz <= HOUSE_HALF; dz++) {
                setWorldBlock(chunk, centerX + dx, y, centerZ + dz, Block.STONE.id());
            }
        }

        // walls, 3 tall, with a door gap in the middle of the south wall
        for (int wy = y + 1; wy <= y + 3; wy++) {
            for (int dx = -HOUSE_HALF; dx <= HOUSE_HALF; dx++) {
                for (int dz = -HOUSE_HALF; dz <= HOUSE_HALF; dz++) {
                    boolean edge = dx == -HOUSE_HALF || dx == HOUSE_HALF || dz == -HOUSE_HALF || dz == HOUSE_HALF;
                    if (!edge) {
                        continue;
                    }
                    if (dx == 0 && dz == HOUSE_HALF && wy == y + 1) {
                        setWorldBlock(chunk, centerX + dx, wy, centerZ + dz, Block.DOOR.id());
                        continue;
                    }
                    setWorldBlock(chunk, centerX + dx, wy, centerZ + dz, Block.WOOD.id());
                }
            }
        }

        // flat roof
        for (int dx = -HOUSE_HALF; dx <= HOUSE_HALF; dx++) {
            for (int dz = -HOUSE_HALF; dz <= HOUSE_HALF; dz++) {
                setWorldBlock(chunk, centerX + dx, y + 4, centerZ + dz, Block.WOOD.id());
            }
        }

        // Basic decoration. CHEST is deliberately not used here: it renders via a
        // TileEntity client-side, and this server has no tile-entity/NBT plumbing yet
        // (chunk packets always claim zero block entities), so a raw chest block with
        // no matching tile entity is a known real-client crash. Revisit once tile
        // entities are actually implemented.
        setWorldBlock(chunk, centerX + 1, y + 1, centerZ - 1, Block.CRAFTING_TABLE.id());
    }

    /** Writes a block given in world coordinates, silently skipping it if it falls outside this chunk. */
    private static void setWorldBlock(Chunk chunk, int worldX, int y, int worldZ, short id) {
        int localX = worldX - chunk.chunkX() * 16;
        int localZ = worldZ - chunk.chunkZ() * 16;
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
            return;
        }
        chunk.setBlock(localX, y, localZ, id);
    }
}
