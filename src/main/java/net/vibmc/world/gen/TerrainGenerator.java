package net.vibmc.world.gen;

public class TerrainGenerator {
    private static final long DEFAULT_SEED = 0x1B2C3D4E5F6A7B8CL;

    /**
     * Vanilla's sea level. Terrain is built around this so the world reads at the same
     * scale as a real Minecraft world rather than the flat strip the first generator made.
     */
    public static final int SEA_LEVEL = 63;

    /** Deepest ocean floor and highest hilltop the overworld generator will produce. */
    private static final int MIN_SURFACE = 48;
    private static final int MAX_SURFACE = 104;

    private final long seed;

    public TerrainGenerator(long seed) {
        this.seed = seed != 0 ? seed : DEFAULT_SEED;
    }

    /**
     * Surface height for a column, in world coordinates.
     *
     * <p>Three scales stack up: a very low-frequency continent field decides land from
     * ocean, a mid-frequency field gives hills and valleys, and a small high-frequency
     * field keeps slopes from looking machined. Because it is a pure function of world
     * coordinates, neighbouring chunks agree at their shared edge and there are no seams.
     */
    public int surfaceHeight(int worldX, int worldZ) {
        double continent = fbm(worldX * 0.0016, worldZ * 0.0016, 4);
        double hills = fbm(worldX * 0.012, worldZ * 0.012, 4);
        double detail = fbm(worldX * 0.045, worldZ * 0.045, 2);

        // Push the continent field away from zero so coastlines are decisive rather than
        // leaving huge areas hovering exactly at sea level.
        double shaped = Math.signum(continent) * Math.pow(Math.abs(continent), 0.75);

        double height = SEA_LEVEL + shaped * 26.0 + hills * 9.0 + detail * 2.5;

        // Hills get taller the further inland they are, so mountains rise from the middle
        // of landmasses instead of straight out of the sea.
        if (shaped > 0.25) {
            height += (shaped - 0.25) * hills * 26.0;
        }
        return clamp((int) Math.round(height), MIN_SURFACE, MAX_SURFACE);
    }

    public boolean isDryLand(int worldX, int worldZ) {
        return surfaceHeight(worldX, worldZ) > SEA_LEVEL;
    }

    /**
     * True where the surface is close enough to sea level for sand rather than grass:
     * beaches, and the shallow floor just offshore.
     */
    public boolean isBeach(int worldX, int worldZ) {
        int surface = surfaceHeight(worldX, worldZ);
        return surface >= SEA_LEVEL - 2 && surface <= SEA_LEVEL + 1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getHeight(int x, int z) {
        double continents = fbm(x * 0.004, z * 0.004, 4);
        double hills = fbm(x * 0.02, z * 0.02, 3);
        double height = 62.0 + continents * 18.0 + hills * 5.0;
        return Math.max(8, Math.min(240, (int) height));
    }

    public int hash(int x, int z) {
        long h = seed;
        h = h * 0x9E3779B97F4A7C15L + x;
        h = h * 0x9E3779B97F4A7C15L + z;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h = h ^ (h >>> 31);
        return (int) (h & 0x7FFFFFFFL);
    }

    public double noise(double x, double z) {
        int ix = floor(x);
        int iz = floor(z);
        double fx = x - ix;
        double fz = z - iz;
        double sx = smoothstep(fx);
        double sz = smoothstep(fz);

        double a = valueAt(ix, iz);
        double b = valueAt(ix + 1, iz);
        double c = valueAt(ix, iz + 1);
        double d = valueAt(ix + 1, iz + 1);

        double top = lerp(a, b, sx);
        double bottom = lerp(c, d, sx);
        return lerp(top, bottom, sz);
    }

    /**
     * 3D value noise. Caves are carved from this rather than from per-chunk random walks,
     * so a tunnel continues correctly into the next chunk no matter which order chunks are
     * generated in.
     */
    public double noise3(double x, double y, double z) {
        int ix = floor(x);
        int iy = floor(y);
        int iz = floor(z);
        double sx = smoothstep(x - ix);
        double sy = smoothstep(y - iy);
        double sz = smoothstep(z - iz);

        double c000 = valueAt(ix, iy, iz);
        double c100 = valueAt(ix + 1, iy, iz);
        double c010 = valueAt(ix, iy + 1, iz);
        double c110 = valueAt(ix + 1, iy + 1, iz);
        double c001 = valueAt(ix, iy, iz + 1);
        double c101 = valueAt(ix + 1, iy, iz + 1);
        double c011 = valueAt(ix, iy + 1, iz + 1);
        double c111 = valueAt(ix + 1, iy + 1, iz + 1);

        double x00 = lerp(c000, c100, sx);
        double x10 = lerp(c010, c110, sx);
        double x01 = lerp(c001, c101, sx);
        double x11 = lerp(c011, c111, sx);
        return lerp(lerp(x00, x10, sy), lerp(x01, x11, sy), sz);
    }

    public double fbm3(double x, double y, double z, int octaves) {
        double total = 0;
        double amplitude = 1;
        double frequency = 1;
        double max = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise3(x * frequency, y * frequency, z * frequency) * amplitude;
            max += amplitude;
            amplitude *= 0.5;
            frequency *= 2;
        }
        return total / max;
    }

    private double valueAt(int x, int y, int z) {
        long h = seed;
        h = h * 0x9E3779B97F4A7C15L + x;
        h = h * 0x9E3779B97F4A7C15L + y;
        h = h * 0x9E3779B97F4A7C15L + z;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h = h ^ (h >>> 31);
        return ((h & 0x7FFFFFFFL) / (double) Integer.MAX_VALUE) * 2.0 - 1.0;
    }

    /** Deterministic hash including a vertical coordinate, for ore placement. */
    public int hash3(int x, int y, int z) {
        long h = seed;
        h = h * 0x9E3779B97F4A7C15L + x;
        h = h * 0x9E3779B97F4A7C15L + y;
        h = h * 0x9E3779B97F4A7C15L + z;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h = h ^ (h >>> 31);
        return (int) (h & 0x7FFFFFFFL);
    }

    public double fbm(double x, double z, int octaves) {
        double total = 0;
        double amplitude = 1;
        double frequency = 1;
        double max = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, z * frequency) * amplitude;
            max += amplitude;
            amplitude *= 0.5;
            frequency *= 2;
        }
        return total / max;
    }

    private double valueAt(int x, int z) {
        return (hash(x, z) / (double) Integer.MAX_VALUE) * 2.0 - 1.0;
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
