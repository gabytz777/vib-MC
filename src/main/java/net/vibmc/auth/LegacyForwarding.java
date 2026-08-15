package net.vibmc.auth;

import net.vibmc.util.Json;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads the profile a proxy packs into the handshake's server-address field.
 *
 * <p>BungeeCord - and Velocity configured with
 * {@code player-info-forwarding-mode = "legacy"} - replaces the address with
 * null-separated fields:
 *
 * <pre>host \0 client-ip \0 undashed-uuid \0 properties-json</pre>
 *
 * <p>The proxy has already authenticated the player, so the profile is taken at face
 * value. That is exactly why the backend must not be reachable by anyone but the proxy:
 * without {@code proxy-trusted-address} (or a firewall) anyone could claim any identity.
 *
 * <p>Velocity's <em>modern</em> forwarding cannot be supported here: it negotiates over
 * the Login Plugin Request/Response packets, which do not exist in protocol 340.
 */
public final class LegacyForwarding {
    /** The separator BungeeCord uses inside the handshake address field. */
    private static final char SEPARATOR = '\0';

    private LegacyForwarding() {
    }

    /** True when the handshake address carries forwarded data rather than a plain hostname. */
    public static boolean looksForwarded(String handshakeAddress) {
        return handshakeAddress != null && handshakeAddress.indexOf(SEPARATOR) >= 0;
    }

    /**
     * Parses forwarded handshake data into a profile.
     *
     * @return the forwarded profile, treated as authenticated because the proxy vouched for it
     * @throws IllegalArgumentException if the payload is not well-formed forwarded data
     */
    @SuppressWarnings("unchecked")
    public static GameProfile parse(String handshakeAddress) {
        if (handshakeAddress == null) {
            throw new IllegalArgumentException("no handshake data");
        }
        String[] parts = handshakeAddress.split("\0");
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "expected host, client ip and uuid but got " + parts.length + " field(s)");
        }

        UUID uuid = SessionAuthenticator.undashedToUuid(parts[2]);

        String texturesValue = null;
        String texturesSignature = null;
        if (parts.length >= 4 && !parts[3].isEmpty()) {
            Object parsed = Json.parse(parts[3]);
            if (parsed instanceof List) {
                for (Object entry : (List<Object>) parsed) {
                    if (!(entry instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> property = (Map<String, Object>) entry;
                    if ("textures".equals(Json.string(property, "name"))) {
                        texturesValue = Json.string(property, "value");
                        texturesSignature = Json.string(property, "signature");
                        break;
                    }
                }
            }
        }

        // The name is not in the forwarded payload; it arrives in Login Start, so it is
        // filled in there. Everything identity-critical (id, signed textures) is here.
        return new GameProfile(uuid, null, texturesValue, texturesSignature, true);
    }

    /** The real hostname the player connected to, with the forwarded fields stripped off. */
    public static String originalHost(String handshakeAddress) {
        int separator = handshakeAddress.indexOf(SEPARATOR);
        return separator < 0 ? handshakeAddress : handshakeAddress.substring(0, separator);
    }

    /** The player's real IP as seen by the proxy, or null when absent. */
    public static String clientAddress(String handshakeAddress) {
        String[] parts = handshakeAddress.split("\0");
        return parts.length >= 2 ? parts[1] : null;
    }
}
