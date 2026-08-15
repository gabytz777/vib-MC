package net.vibmc.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Who a connected player is: their id, name, and (when authenticated or forwarded by a
 * proxy) the signed skin/cape properties Mojang issued for them.
 *
 * <p>A profile is either <em>authenticated</em> - the id and textures came from Mojang's
 * session server, so the signature is intact and vanilla clients render the real skin -
 * or offline, where the id is derived from the name the same way vanilla offline mode
 * does it.
 */
public final class GameProfile {
    private final UUID uuid;
    private final String name;
    private final String texturesValue;
    private final String texturesSignature;
    private final boolean authenticated;

    public GameProfile(UUID uuid, String name, String texturesValue, String texturesSignature,
                       boolean authenticated) {
        this.uuid = uuid;
        this.name = name;
        this.texturesValue = texturesValue;
        this.texturesSignature = texturesSignature;
        this.authenticated = authenticated;
    }

    /**
     * The profile an offline-mode server uses: the same {@code OfflinePlayer:<name>} id
     * vanilla derives, so worlds stay compatible with other offline servers.
     */
    public static GameProfile offline(String name) {
        return new GameProfile(offlineUuid(name), name, null, null, false);
    }

    public static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A copy carrying the given name. Proxy-forwarded profiles arrive without one - the
     * name only shows up later in Login Start - so it is filled in at that point.
     */
    public GameProfile withName(String newName) {
        return new GameProfile(uuid, newName, texturesValue, texturesSignature, authenticated);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    /** Base64 textures blob from Mojang, or null when this profile has none. */
    public String texturesValue() {
        return texturesValue;
    }

    /** Mojang's signature over {@link #texturesValue()}, or null when unsigned. */
    public String texturesSignature() {
        return texturesSignature;
    }

    public boolean hasTextures() {
        return texturesValue != null && !texturesValue.isEmpty();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public String toString() {
        return name + " (" + uuid + (authenticated ? ", authenticated)" : ", offline)");
    }
}
