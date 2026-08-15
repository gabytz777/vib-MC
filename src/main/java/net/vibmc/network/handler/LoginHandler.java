package net.vibmc.network.handler;

import net.vibmc.auth.GameProfile;
import net.vibmc.auth.ServerKeyPair;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.network.ClientConnection;
import net.vibmc.network.Packet;
import net.vibmc.network.PacketBuffer;
import net.vibmc.network.ProtocolState;
import net.vibmc.server.VibMC;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Drives the login sequence.
 *
 * <p>Offline mode is a single step: Login Start, then Login Success with a name-derived id.
 *
 * <p>Online mode adds the 1.12.2 encryption handshake:
 * <pre>
 *   client  Login Start (0x00)
 *   server  Encryption Request (0x01)  public key + verify token
 *   client  Encryption Response (0x02) RSA(shared secret) + RSA(verify token)
 *   server  verifies the token, switches both directions to AES, asks Mojang about the
 *           join, then sends Login Success with the authenticated id
 * </pre>
 */
public class LoginHandler implements PacketHandler {
    /** Vanilla sends an empty server id; the login hash is what actually identifies the join. */
    private static final String SERVER_ID = "";
    private static final int VERIFY_TOKEN_LENGTH = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        switch (packetId) {
            case 0x00:
                handleLoginStart(connection, buffer);
                break;
            case 0x01:
                handleEncryptionResponse(connection, buffer);
                break;
            default:
                break;
        }
    }

    private void handleLoginStart(ClientConnection connection, PacketBuffer buffer) {
        String username = buffer.readString();
        if (!isValidUsername(username)) {
            connection.disconnect("Invalid username");
            return;
        }
        connection.setUsername(username);

        VibMC server = VibMC.getInstance();
        // A proxy has already authenticated the player and passed their real profile
        // through the handshake, so the encryption handshake is skipped entirely.
        GameProfile forwarded = connection.getProfile();
        if (forwarded != null) {
            // The forwarded payload has the id and signed skin; the name only arrives here.
            finishLogin(connection, forwarded.withName(username));
            return;
        }
        if (!server.getConfig().onlineMode()) {
            finishLogin(connection, GameProfile.offline(username));
            return;
        }
        sendEncryptionRequest(connection);
    }

    private void sendEncryptionRequest(ClientConnection connection) {
        byte[] verifyToken = new byte[VERIFY_TOKEN_LENGTH];
        RANDOM.nextBytes(verifyToken);
        connection.setVerifyToken(verifyToken);

        byte[] publicKey = VibMC.getInstance().getKeyPair().encodedPublicKey();
        connection.sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x01; // Encryption Request
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeString(SERVER_ID);
                b.writeVarInt(publicKey.length);
                b.writeBytes(publicKey);
                b.writeVarInt(verifyToken.length);
                b.writeBytes(verifyToken);
            }
        });
    }

    private void handleEncryptionResponse(ClientConnection connection, PacketBuffer buffer) {
        byte[] expectedToken = connection.getVerifyToken();
        if (expectedToken == null) {
            connection.disconnect("Unexpected encryption response");
            return;
        }

        VibMC server = VibMC.getInstance();
        ServerKeyPair keyPair = server.getKeyPair();
        try {
            byte[] encryptedSecret = buffer.readBytes(buffer.readVarInt());
            byte[] encryptedToken = buffer.readBytes(buffer.readVarInt());

            byte[] token = keyPair.decrypt(encryptedToken);
            if (!Arrays.equals(token, expectedToken)) {
                // Someone replayed or forged the exchange; they never held the private key.
                connection.disconnect("Invalid verify token");
                return;
            }

            byte[] secret = keyPair.decrypt(encryptedSecret);
            String hash = ServerKeyPair.serverHash(SERVER_ID, secret, keyPair.encodedPublicKey());

            // From here on the wire is encrypted - including the kick below if auth fails.
            SecretKey sharedSecret = ServerKeyPair.sharedSecret(secret);
            connection.enableEncryption(sharedSecret);
            connection.setVerifyToken(null);

            authenticate(connection, hash);
        } catch (ServerKeyPair.GeneralSecurityFailure e) {
            server.getLogger().warn("Key exchange failed for %s: %s", connection.getUsername(), e.getMessage());
            connection.disconnect("Encryption failed");
        } catch (RuntimeException e) {
            server.getLogger().warn("Malformed encryption response from %s: %s",
                    connection.getUsername(), e);
            connection.disconnect("Malformed encryption response");
        }
    }

    private void authenticate(ClientConnection connection, String hash) {
        VibMC server = VibMC.getInstance();
        String username = connection.getUsername();
        GameProfile profile;
        try {
            profile = server.getSessionAuthenticator().authenticate(username, hash);
        } catch (IOException e) {
            server.getLogger().warn("Could not reach the session server for %s: %s", username, e.getMessage());
            connection.disconnect("Authentication servers are unreachable, please try again");
            return;
        }
        if (profile == null) {
            connection.disconnect("Failed to verify username!");
            return;
        }
        finishLogin(connection, profile);
    }

    private void finishLogin(ClientConnection connection, GameProfile profile) {
        VibMC server = VibMC.getInstance();

        // One session per profile: a second login for the same id replaces nothing, it is
        // simply refused, so the original player is never silently booted.
        if (server.getPlayerManager().getPlayer(profile.uuid()) != null) {
            connection.disconnect("That account is already playing on this server");
            return;
        }

        connection.setProfile(profile);
        connection.sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x02; // Login Success
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeString(profile.uuid().toString());
                b.writeString(profile.name());
            }
        });

        connection.setProtocolState(ProtocolState.PLAY);

        PlayerEntity player = new PlayerEntity(
                server.getWorldManager().getMainWorld(), connection, profile.name(), profile.uuid());
        player.setProfile(profile);
        player.spawnAtSpawn();
        connection.setHandler(new PlayHandler(player));
        server.getPlayerManager().addPlayer(player);
        player.getWorld().addEntity(player);
    }

    /** Mojang names are 3-16 characters of {@code [A-Za-z0-9_]}. */
    public static boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 16) {
            return false;
        }
        for (int i = 0; i < username.length(); i++) {
            char c = username.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }
}
