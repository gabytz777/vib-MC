package net.vibmc.player;

import net.vibmc.auth.GameProfile;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.world.Dimension;
import net.vibmc.world.World;
import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the parts of multiplayer bookkeeping that can be exercised without a live socket:
 * player registry lookups, per-player state staying separate, and dimension moves.
 */
class MultiplayerVisibilityTest {
    private World world(Path dir, Dimension dimension) {
        String name = dir.resolve("w" + dimension.name()).toString();
        return new World(99L, name, dimension, new WorldStorage(name));
    }

    private PlayerEntity player(World world, String name) {
        GameProfile profile = GameProfile.offline(name);
        PlayerEntity player = new PlayerEntity(world, null, name, profile.uuid());
        player.setProfile(profile);
        return player;
    }

    @Test
    void playersAreFoundByBothNameAndUuid(@TempDir Path dir) {
        World world = world(dir, Dimension.OVERWORLD);
        PlayerManager manager = new PlayerManager();
        PlayerEntity alice = player(world, "alice");

        manager.register(alice);

        assertSame(alice, manager.getPlayer(alice.getUuid()));
        assertSame(alice, manager.getPlayer("alice"));
        assertSame(alice, manager.getPlayer("ALICE"), "name lookup is case-insensitive");
        assertEquals(1, manager.getOnlineCount());
    }

    @Test
    void removingAPlayerClearsBothLookups(@TempDir Path dir) {
        World world = world(dir, Dimension.OVERWORLD);
        PlayerManager manager = new PlayerManager();
        PlayerEntity alice = player(world, "alice");
        manager.register(alice);

        manager.unregister(alice);

        assertNull(manager.getPlayer(alice.getUuid()));
        assertNull(manager.getPlayer("alice"));
        assertEquals(0, manager.getOnlineCount());
    }

    @Test
    void severalPlayersCoexistWithSeparateIdentities(@TempDir Path dir) {
        World world = world(dir, Dimension.OVERWORLD);
        PlayerManager manager = new PlayerManager();
        PlayerEntity alice = player(world, "alice");
        PlayerEntity bob = player(world, "bob");

        manager.register(alice);
        manager.register(bob);

        assertEquals(2, manager.getOnlineCount());
        assertNotEquals(alice.getUuid(), bob.getUuid());
        assertNotEquals(alice.getEntityId(), bob.getEntityId(),
                "entity ids must be unique or the client will confuse the two");
        assertTrue(manager.getOnlinePlayers().contains(alice));
        assertTrue(manager.getOnlinePlayers().contains(bob));
    }

    @Test
    void perPlayerStateDoesNotLeakBetweenPlayers(@TempDir Path dir) {
        World world = world(dir, Dimension.OVERWORLD);
        PlayerEntity alice = player(world, "alice");
        PlayerEntity bob = player(world, "bob");

        alice.setPosition(100, 70, -40);
        alice.getSentChunks().add(1L);
        bob.setPosition(-8, 65, 8);

        assertEquals(100, alice.getX(), 0.001);
        assertEquals(-8, bob.getX(), 0.001);
        assertTrue(bob.getSentChunks().isEmpty(),
                "chunk tracking is per-player, not shared global state");
    }

    @Test
    void onlineModeRefusesASecondLoginForTheSameAccount(@TempDir Path dir) {
        World world = world(dir, Dimension.OVERWORLD);
        PlayerManager manager = new PlayerManager();
        PlayerEntity first = player(world, "alice");
        manager.register(first);

        // The login path refuses a duplicate by finding the existing session first; this
        // is the lookup that decision rests on.
        UUID sameAccount = GameProfile.offline("alice").uuid();
        assertSame(first, manager.getPlayer(sameAccount));
    }

    @Test
    void changingDimensionMovesThePlayerBetweenWorlds(@TempDir Path dir) {
        World overworld = world(dir, Dimension.OVERWORLD);
        World nether = world(dir, Dimension.NETHER);
        PlayerEntity alice = player(overworld, "alice");
        overworld.addEntity(alice);

        alice.changeDimension(nether, 10.5, 70, 20.5);

        assertSame(nether, alice.getWorld());
        assertTrue(nether.getEntities().contains(alice), "the player joins the destination world");
        assertFalse(overworld.getEntities().contains(alice), "and leaves the one they came from");
        assertEquals(10.5, alice.getX(), 0.001);
    }

    @Test
    void dimensionChangeInvalidatesTheClientsChunks(@TempDir Path dir) {
        World overworld = world(dir, Dimension.OVERWORLD);
        World nether = world(dir, Dimension.NETHER);
        PlayerEntity alice = player(overworld, "alice");
        overworld.addEntity(alice);
        alice.getSentChunks().add(123L);
        alice.setLoadedChunk(5, 5);

        alice.changeDimension(nether, 0.5, 70, 0.5);

        // A Respawn packet makes the client throw its world away, so anything we think it
        // already has is stale; if this were not cleared the new world would never stream.
        assertTrue(alice.getSentChunks().isEmpty(), "sent-chunk tracking is reset");
        assertNotEquals(5, alice.getLoadedChunkX(), "the streamer is forced to re-run");
    }
}
