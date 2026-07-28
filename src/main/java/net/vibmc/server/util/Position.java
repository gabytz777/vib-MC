package net.vibmc.server.util;

public class Position {
    private final int x, y, z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public Position add(int dx, int dy, int dz) {
        return new Position(x + dx, y + dy, z + dz);
    }

    public Position subtract(int dx, int dy, int dz) {
        return new Position(x - dx, y - dy, z - dz);
    }

    public long toLong() {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | ((long) z & 0x3FFFFFF);
    }

    public static Position fromLong(long val) {
        int x = (int) (val >> 38);
        int y = (int) ((val >> 26) & 0xFFF);
        int z = (int) (val << 38 >> 38);
        return new Position(x, y, z);
    }

    public double distance(Position other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }

    public int distanceSquared(Position other) {
        int dx = x - other.x;
        int dy = y - other.y;
        int dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public ChunkCoord toChunkCoord() {
        return new ChunkCoord(x >> 4, z >> 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position pos = (Position) o;
        return x == pos.x && y == pos.y && z == pos.z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
