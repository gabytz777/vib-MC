package net.vibmc.network.handler;

import net.vibmc.auth.GameProfile;
import net.vibmc.auth.LegacyForwarding;
import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;
import net.vibmc.network.ProtocolState;
import net.vibmc.server.ServerConfig;
import net.vibmc.server.VibMC;

public class HandshakeHandler implements PacketHandler {
    /** The only protocol vib-MC speaks: Minecraft 1.12.2. */
    public static final int SUPPORTED_PROTOCOL = 340;

    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        if (packetId != 0x00) {
            // Legacy server list ping (0xFE) and anything else is ignored.
            return;
        }
        int protocol = buffer.readVarInt();
        String address = buffer.readString();
        buffer.readUnsignedShort(); // server port
        int nextState = buffer.readVarInt();

        if (nextState == 1) {
            // Status pings are answered for every protocol, so outdated clients can still
            // see the server in their list and read why they cannot join.
            connection.setProtocolState(ProtocolState.STATUS);
            connection.setHandler(new StatusHandler());
            return;
        }
        if (nextState != 2) {
            connection.disconnect("Unsupported login state");
            return;
        }

        connection.setProtocolState(ProtocolState.LOGIN);
        if (protocol != SUPPORTED_PROTOCOL) {
            connection.disconnect(versionKickMessage(protocol));
            return;
        }
        if (!applyProxyForwarding(connection, address)) {
            return;
        }
        connection.setHandler(new LoginHandler());
    }

    /**
     * Tells the player which side is out of date, the way vanilla does: a lower protocol
     * means their client is older than the server, a higher one means the server is older
     * than their client.
     */
    public static String versionKickMessage(int protocol) {
        return protocol < SUPPORTED_PROTOCOL
                ? "Outdated client! Please use Minecraft 1.12.2."
                : "Outdated server! This server supports Minecraft 1.12.2 only.";
    }

    /**
     * Handles the proxy side of the handshake.
     *
     * @return true if the login may continue, false if the connection was rejected
     */
    private boolean applyProxyForwarding(ClientConnection connection, String address) {
        ServerConfig config = VibMC.getInstance().getConfig();
        boolean forwarded = LegacyForwarding.looksForwarded(address);

        if (!config.proxyLegacy()) {
            if (forwarded) {
                // Someone is speaking proxy protocol at a server that is not expecting it;
                // accepting it would let them pick their own identity.
                connection.disconnect("This server is not configured to accept proxy connections");
                return false;
            }
            return true;
        }

        // Legacy forwarding carries no proof of its own - the trust comes entirely from
        // the connection's source address, so it has to be checked before anything else.
        if (!isTrustedProxy(connection, config.proxyTrustedAddress())) {
            VibMC.getInstance().getLogger().warn(
                    "Rejected a proxy login from %s (proxy-trusted-address=%s)",
                    connection.remoteAddress(), config.proxyTrustedAddress());
            connection.disconnect("You must connect through the proxy");
            return false;
        }
        if (!forwarded) {
            connection.disconnect("This server only accepts connections through its proxy");
            return false;
        }

        try {
            GameProfile profile = LegacyForwarding.parse(address);
            connection.setProfile(profile);
            return true;
        } catch (RuntimeException e) {
            VibMC.getInstance().getLogger().warn("Malformed proxy handshake from %s: %s",
                    connection.remoteAddress(), e.getMessage());
            connection.disconnect("Malformed proxy handshake");
            return false;
        }
    }

    /** A blank trusted address means "trust anything", for setups firewalled at the network. */
    private static boolean isTrustedProxy(ClientConnection connection, String trusted) {
        if (trusted == null || trusted.isEmpty()) {
            return true;
        }
        String remote = connection.remoteAddress();
        return trusted.equals(remote);
    }
}
