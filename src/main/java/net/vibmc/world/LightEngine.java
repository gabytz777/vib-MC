package net.vibmc.world;

/**
 * Works out the light levels a chunk is sent with.
 *
 * <p>The old approach set sky light per column - full brightness above the highest block,
 * nothing below it - which is why a tree was a black lump: every column the canopy covered
 * went dark from the leaves all the way down, with no light reaching in from the open air
 * a block away. Light does not work per column; it flows. So this floods it properly, out
 * from the sky and out from anything that glows, losing a level per block it travels.
 *
 * <p>Light is flooded within one chunk. A chunk has no say in its neighbour's blocks, so
 * light does not cross the seam between them - open sky is unaffected, since every column
 * lights itself, and the cost is that a cave running under a chunk border can be a shade
 * darker on one side.
 */
public final class LightEngine {
    /** Attenuation of a block that stops light outright. */
    private static final int OPAQUE = 16;
    private static final int MAX_LEVEL = 15;
    private static final int SIZE = 16 * 16 * Chunk.WORLD_HEIGHT;

    private LightEngine() {
    }

    /** Computed light levels for one chunk, ready to be cut into per-section nibbles. */
    public static final class Light {
        private final byte[] levels;

        private Light(byte[] levels) {
            this.levels = levels;
        }

        /** Level at a block, 0-15. */
        public int at(int x, int y, int z) {
            if (y < 0 || y >= Chunk.WORLD_HEIGHT) {
                return 0;
            }
            return levels[index(x, y, z)];
        }

        /** The 2048-byte nibble array for one 16-block section, as the protocol wants it. */
        public byte[] section(int section) {
            byte[] packed = new byte[2048];
            int baseY = section * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int level = levels[index(x, baseY + y, z)];
                        if (level == 0) {
                            continue;
                        }
                        int i = (y << 8) | (z << 4) | x;
                        if ((i & 1) == 0) {
                            packed[i >> 1] |= (byte) level;
                        } else {
                            packed[i >> 1] |= (byte) (level << 4);
                        }
                    }
                }
            }
            return packed;
        }
    }

    /**
     * Sky light: every column starts at full brightness and dims as it passes down through
     * anything that is not air, then the whole lot is flooded sideways.
     */
    public static Light skyLight(Chunk chunk) {
        byte[] levels = new byte[SIZE];
        IntQueue queue = new IntQueue();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int level = MAX_LEVEL;
                for (int y = Chunk.WORLD_HEIGHT - 1; y >= 0; y--) {
                    int opacity = opacity(chunk.getBlock(x, y, z));
                    if (opacity >= OPAQUE) {
                        break;  // the ground; everything under it is lit sideways or not at all
                    }
                    level = Math.max(0, level - opacity);
                    if (level == 0) {
                        break;
                    }
                    int index = index(x, y, z);
                    levels[index] = (byte) level;
                    queue.add(index);
                }
            }
        }

        flood(chunk, levels, queue);
        return new Light(levels);
    }

    /** Block light: flooded out from glowstone, lava and the portals. */
    public static Light blockLight(Chunk chunk) {
        byte[] levels = new byte[SIZE];
        IntQueue queue = new IntQueue();

        for (int y = 0; y < Chunk.WORLD_HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int emission = emission(chunk.getBlock(x, y, z));
                    if (emission <= 0) {
                        continue;
                    }
                    int index = index(x, y, z);
                    levels[index] = (byte) emission;
                    queue.add(index);
                }
            }
        }

        flood(chunk, levels, queue);
        return new Light(levels);
    }

    /**
     * Spreads what is already lit into its neighbours until nothing gets brighter.
     *
     * <p>Breadth-first from every source at once, so a cell is only revisited when a
     * brighter path reaches it - which is what keeps this linear in practice rather than
     * re-walking the chunk once per light source.
     */
    private static void flood(Chunk chunk, byte[] levels, IntQueue queue) {
        while (!queue.isEmpty()) {
            int index = queue.poll();
            int level = levels[index];
            if (level <= 1) {
                continue;
            }
            int x = index & 15;
            int z = (index >> 4) & 15;
            int y = index >> 8;

            spread(chunk, levels, queue, x - 1, y, z, level);
            spread(chunk, levels, queue, x + 1, y, z, level);
            spread(chunk, levels, queue, x, y, z - 1, level);
            spread(chunk, levels, queue, x, y, z + 1, level);
            spread(chunk, levels, queue, x, y - 1, z, level);
            spread(chunk, levels, queue, x, y + 1, z, level);
        }
    }

    private static void spread(Chunk chunk, byte[] levels, IntQueue queue,
                               int x, int y, int z, int fromLevel) {
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y >= Chunk.WORLD_HEIGHT) {
            return;  // the chunk edge; the neighbour lights its own side
        }
        int next = fromLevel - Math.max(1, opacity(chunk.getBlock(x, y, z)));
        if (next <= 0) {
            return;
        }
        int index = index(x, y, z);
        if (next <= levels[index]) {
            return;
        }
        levels[index] = (byte) next;
        queue.add(index);
    }

    /** How much a block dims light passing through it; {@link #OPAQUE} stops it dead. */
    private static int opacity(short block) {
        if (block == Block.AIR.id() || Block.isNetherPortal(block)
                || block == Block.END_PORTAL.id()) {
            return 0;
        }
        if (block == Block.LEAVES.id()) {
            return 1;  // a canopy shades what is under it without blacking it out
        }
        if (block == Block.WATER.id()) {
            return 3;
        }
        return OPAQUE;
    }

    /** How brightly a block glows on its own. */
    private static int emission(short block) {
        if (block == Block.GLOWSTONE.id() || block == Block.LAVA.id()
                || block == Block.END_PORTAL.id()) {
            return 15;
        }
        if (Block.isNetherPortal(block)) {
            return 11;
        }
        return 0;
    }

    private static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    /** A queue of block indices without the boxing an {@code ArrayDeque<Integer>} would cost. */
    private static final class IntQueue {
        private int[] items = new int[8192];
        private int head;
        private int tail;

        void add(int value) {
            if (tail == items.length) {
                compactOrGrow();
            }
            items[tail++] = value;
        }

        boolean isEmpty() {
            return head == tail;
        }

        int poll() {
            return items[head++];
        }

        private void compactOrGrow() {
            int size = tail - head;
            if (size * 2 < items.length) {
                System.arraycopy(items, head, items, 0, size);
            } else {
                int[] bigger = new int[items.length * 2];
                System.arraycopy(items, head, bigger, 0, size);
                items = bigger;
            }
            head = 0;
            tail = size;
        }
    }
}
