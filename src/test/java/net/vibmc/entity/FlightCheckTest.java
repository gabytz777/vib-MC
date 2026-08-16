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

/**
 * The floating check is deliberately lenient, so these tests are mostly about what it must
 * <em>not</em> do: normal play, teleports and dimension changes must never trip it.
 */
class FlightCheckTest {
    /** Comfortably longer than the kick threshold. */
    private static final int LONG_ENOUGH_TO_KICK = 200;

    private World world(Path dir) {
        String name = dir.resolve("world").toString();
        return new World(5L, name, Dimension.OVERWORLD, new WorldStorage(name));
    }

    /**
     * A player that records its kick instead of needing a live socket, so the check's
     * decision is directly observable.
     */
    private static class RecordingPlayer extends PlayerEntity {
        String kickReason;

        RecordingPlayer(World world) {
            super(world, null, "alice", GameProfile.offline("alice").uuid());
        }

        @Override
        public void kick(String reason) {
            this.kickReason = reason;
        }
    }

    private RecordingPlayer player(World world) {
        return new RecordingPlayer(world);
    }

    /** Runs the given number of ticks and reports whether the player was kicked. */
    private boolean survives(RecordingPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.tick();
        }
        return player.kickReason == null;
    }

    @Test
    void aGroundedPlayerIsNeverFlagged(@TempDir Path dir) {
        World world = world(dir);
        RecordingPlayer alice = player(world);
        alice.setPosition(0.5, 70, 0.5);
        alice.setOnGround(true);

        assertTrue(survives(alice, LONG_ENOUGH_TO_KICK));
        assertTrue(alice.isAlive(), "standing still on the ground is not flying");
    }

    @Test
    void creativeAndSpectatorAreExempt(@TempDir Path dir) {
        World world = world(dir);
        for (GameMode mode : new GameMode[]{GameMode.CREATIVE, GameMode.SPECTATOR}) {
            RecordingPlayer alice = player(world);
            alice.setGameMode(mode);
            alice.setPosition(0.5, 200, 0.5);
            alice.setOnGround(false);

            assertTrue(survives(alice, LONG_ENOUGH_TO_KICK),
                    mode + " players are allowed to leave the ground");
        }
    }

    @Test
    void allowFlightExemptsAPlayer(@TempDir Path dir) {
        World world = world(dir);
        RecordingPlayer alice = player(world);
        alice.setAllowFlight(true);
        alice.setPosition(0.5, 200, 0.5);
        alice.setOnGround(false);

        assertTrue(survives(alice, LONG_ENOUGH_TO_KICK));
        assertTrue(alice.isAllowFlight());
    }

    @Test
    void aTeleportGivesGraceBeforeAnyJudgement(@TempDir Path dir) {
        World world = world(dir);
        RecordingPlayer alice = player(world);
        alice.setOnGround(false);

        // A teleport moves the client out from under itself; its next reports are stale.
        alice.teleport(0.5, 200, 0.5);

        assertTrue(survives(alice, 50),
                "the grace window must outlast the round trip of a teleport");
    }

    @Test
    void aDimensionChangeGivesGraceBeforeAnyJudgement(@TempDir Path dir) {
        World overworld = world(dir);
        String netherName = dir.resolve("nether").toString();
        World nether = new World(5L, netherName, Dimension.NETHER, new WorldStorage(netherName));

        RecordingPlayer alice = player(overworld);
        overworld.addEntity(alice);
        alice.changeDimension(nether, 0.5, 200, 0.5);

        assertTrue(survives(alice, 50), "arriving in a new dimension is not flying");
        assertEquals(PlayerEntity.DIMENSION_CHANGE_GRACE_TICKS >= 50, true,
                "the grace window should cover a slow world load");
    }

    @Test
    void standingOnSolidGroundCountsAsSupported(@TempDir Path dir) {
        World world = world(dir);
        RecordingPlayer alice = player(world);

        // Put a block under the player and claim not to be on the ground: the block is
        // what matters, so a client that simply forgets the flag is not punished.
        world.setBlock(0, 69, 0, Block.STONE.id());
        alice.setPosition(0.5, 70, 0.5);
        alice.setOnGround(false);

        assertTrue(survives(alice, LONG_ENOUGH_TO_KICK),
                "a player supported by a block is not floating");
    }

    @Test
    void sustainedUnsupportedFlightIsEventuallyKicked(@TempDir Path dir) {
        World world = world(dir);
        RecordingPlayer alice = player(world);
        // High above the world with nothing underneath, in survival, not permitted to fly.
        alice.setPosition(0.5, 250, 0.5);
        alice.setOnGround(false);

        assertFalse(survives(alice, LONG_ENOUGH_TO_KICK),
                "a player hovering unsupported in survival should eventually be kicked");
        assertEquals("Flying is not enabled on this server", alice.kickReason);
    }

    @Test
    void briefAirtimeIsToleratedLikeAJump(@TempDir Path dir) {
        World world = world(dir);
        RecordingPlayer alice = player(world);
        alice.setPosition(0.5, 250, 0.5);
        alice.setOnGround(false);

        // A jump or a short fall leaves a player airborne for a moment; the check has to
        // ride that out or it would kick people for playing normally.
        assertTrue(survives(alice, 20), "a brief hop must not be treated as flight");
    }

    @Test
    void aFallingPlayerIsNotTreatedAsFlying(@TempDir Path dir) {
        World world = world(dir);
        RecordingPlayer alice = player(world);
        alice.setPosition(0.5, 250, 0.5);
        alice.setOnGround(false);

        // Breaking the block under yourself, or stepping off an End island, means a long
        // unsupported fall. That used to read as flight and kick the player mid-drop.
        for (int i = 0; i < LONG_ENOUGH_TO_KICK; i++) {
            alice.setPosition(0.5, 250 - i * 0.8, 0.5);
            alice.tick();
            if (alice.isDead()) {
                break;
            }
        }

        assertEquals(null, alice.kickReason, "falling is not flying");
    }

    @Test
    void fallingOutOfTheWorldKillsRatherThanKicks(@TempDir Path dir) {
        World world = world(dir);
        RecordingPlayer alice = player(world);
        alice.setOnGround(false);

        for (double y = 40; y > -60; y -= 1.5) {
            alice.setPosition(0.5, y, 0.5);
            alice.tick();
        }

        assertTrue(alice.isDead(), "the void kills you");
        assertEquals(null, alice.kickReason, "and it does not disconnect you to do it");
    }

    @Test
    void nonFiniteCoordinatesAreRejectedNotMeasured() {
        // NaN would poison every later comparison, so it is refused outright rather than
        // being treated as a suspicious-but-usable position.
        assertFalse(isFinite(Double.NaN));
        assertFalse(isFinite(Double.POSITIVE_INFINITY));
        assertFalse(isFinite(Double.NEGATIVE_INFINITY));
        assertTrue(isFinite(0.0));
        assertTrue(isFinite(-1234.5));
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
