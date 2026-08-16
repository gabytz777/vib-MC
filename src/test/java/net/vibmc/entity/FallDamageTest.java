package net.vibmc.entity;

import net.vibmc.auth.GameProfile;
import net.vibmc.player.GameMode;
import net.vibmc.world.Block;
import net.vibmc.world.Dimension;
import net.vibmc.world.World;
import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallDamageTest {
    private World world(Path dir) {
        String name = dir.resolve("falls").toString();
        return new World(11L, name, Dimension.OVERWORLD, new WorldStorage(name));
    }

    private PlayerEntity player(World world) {
        return new PlayerEntity(world, null, "alice", GameProfile.offline("alice").uuid());
    }

    /** Drops a player from {@code fromY} to {@code toY} and lands them. */
    private void fall(PlayerEntity player, double fromY, double toY) {
        player.setPosition(0.5, fromY, 0.5);
        player.setOnGround(false);
        player.tick();
        for (double y = fromY - 1; y > toY; y -= 1) {
            player.setPosition(0.5, y, 0.5);
            player.tick();
        }
        player.setPosition(0.5, toY, 0.5);
        player.setOnGround(true);
        player.tick();
    }

    @Test
    void shortDropsAreFree(@TempDir Path dir) {
        PlayerEntity alice = player(world(dir));
        fall(alice, 70, 67);  // three blocks

        assertEquals(20.0f, alice.getHealth(), "the first three blocks cost nothing");
    }

    @Test
    void aLongDropCostsHalfAHeartPerBlockPastThree(@TempDir Path dir) {
        PlayerEntity alice = player(world(dir));
        fall(alice, 80, 70);  // ten blocks, so seven of them hurt

        assertEquals(13.0f, alice.getHealth(), 0.001, "ten blocks should cost seven health");
        assertFalse(alice.isDead());
    }

    @Test
    void aFatalDropKills(@TempDir Path dir) {
        PlayerEntity alice = player(world(dir));
        fall(alice, 130, 70);

        assertTrue(alice.isDead(), "sixty blocks is lethal");
        assertEquals(0.0f, alice.getHealth());
    }

    @Test
    void waterBreaksTheFall(@TempDir Path dir) {
        World world = world(dir);
        PlayerEntity alice = player(world);
        world.setBlock(0, 70, 0, Block.WATER.id());

        fall(alice, 120, 70);

        assertEquals(20.0f, alice.getHealth(), "landing in water hurts nobody");
    }

    @Test
    void walkingDownASlopeIsNotAFall(@TempDir Path dir) {
        PlayerEntity alice = player(world(dir));
        alice.setOnGround(true);

        // Twenty blocks of descent, one at a time, on the ground the whole way.
        for (int i = 0; i < 20; i++) {
            alice.setPosition(0.5, 90 - i, 0.5);
            alice.setOnGround(true);
            alice.tick();
        }

        assertEquals(20.0f, alice.getHealth(), "a hillside is not a cliff");
    }

    @Test
    void creativeAndSpectatorTakeNoFallDamage(@TempDir Path dir) {
        World world = world(dir);
        for (GameMode mode : new GameMode[]{GameMode.CREATIVE, GameMode.SPECTATOR}) {
            PlayerEntity alice = player(world);
            alice.setGameMode(mode);
            fall(alice, 200, 70);

            assertEquals(20.0f, alice.getHealth(), mode + " players do not take fall damage");
        }
    }

    @Test
    void abilitiesFollowTheGameModeSoFlightSurvivesARespawn(@TempDir Path dir) {
        World world = world(dir);
        PlayerEntity alice = player(world);

        // The bug: a Respawn packet said "creative" while the abilities packet was built
        // from fields /gamemode never set, leaving a creative HUD that could not fly.
        alice.setGameMode(GameMode.CREATIVE);
        assertTrue((alice.abilityFlags() & 0x04) != 0, "creative may fly");
        assertTrue((alice.abilityFlags() & 0x08) != 0, "and is flagged as creative");

        alice.setGameMode(GameMode.SPECTATOR);
        assertTrue((alice.abilityFlags() & 0x02) != 0, "spectator is already flying");

        alice.setGameMode(GameMode.SURVIVAL);
        assertEquals(0, alice.abilityFlags() & 0x04, "survival may not fly");
    }
}
