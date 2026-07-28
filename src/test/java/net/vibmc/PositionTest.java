package net.vibmc;

import net.vibmc.server.util.Position;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {

    @Test
    public void testPositionCreation() {
        Position pos = new Position(10, 20, 30);
        assertEquals(10, pos.getX());
        assertEquals(20, pos.getY());
        assertEquals(30, pos.getZ());
    }

    @Test
    public void testPositionEquality() {
        assertEquals(new Position(1, 2, 3), new Position(1, 2, 3));
        assertNotEquals(new Position(1, 2, 3), new Position(4, 5, 6));
    }

    @Test
    public void testPositionHashCode() {
        assertEquals(new Position(1, 2, 3).hashCode(), new Position(1, 2, 3).hashCode());
    }

    @Test
    public void testPositionAdd() {
        Position result = new Position(1, 2, 3).add(10, 20, 30);
        assertEquals(11, result.getX());
        assertEquals(22, result.getY());
        assertEquals(33, result.getZ());
    }

    @Test
    public void testPositionToChunkCoord() {
        Position pos = new Position(30, 50, 70);
        var chunk = pos.toChunkCoord();
        assertEquals(1, chunk.getX());
        assertEquals(4, chunk.getZ());
    }

    @Test
    public void testDistance() {
        Position a = new Position(0, 0, 0);
        Position b = new Position(3, 0, 4);
        assertEquals(5.0, a.distance(b), 0.0001);
    }
}
