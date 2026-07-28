package net.vibmc.server.util;

public class ChunkCoord {
    private final int x, z;

    public ChunkCoord(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() { return x; }
    public int getZ() { return z; }

    public ChunkCoord add(int dx, int dz) {
        return new ChunkCoord(x + dx, z + dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkCoord)) return false;
        ChunkCoord that = (ChunkCoord) o;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return (x * 31) ^ z;
    }

    @Override
    public String toString() {
        return "Chunk(" + x + ", " + z + ")";
    }
}
