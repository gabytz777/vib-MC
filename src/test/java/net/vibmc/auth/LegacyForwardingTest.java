package net.vibmc.auth;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyForwardingTest {
    private static final String UUID_TEXT = "f1d38adb465c4764b82af29f57a3ff09";
    /** The NUL byte BungeeCord uses between the fields it packs into the handshake. */
    private static final String SEP = String.valueOf((char) 0);

    /** Builds the null-separated payload a proxy puts in the handshake address field. */
    private static String forwarded(String properties) {
        return "mc.example.com" + SEP + "12.34.56.78" + SEP + UUID_TEXT
                + (properties == null ? "" : SEP + properties);
    }

    @Test
    void plainHostnamesAreNotMistakenForForwardedData() {
        assertFalse(LegacyForwarding.looksForwarded("mc.example.com"));
        assertFalse(LegacyForwarding.looksForwarded("localhost"));
        assertFalse(LegacyForwarding.looksForwarded(null));
        assertTrue(LegacyForwarding.looksForwarded(forwarded(null)));
    }

    @Test
    void forwardedHandshakeYieldsTheProxysProfile() {
        GameProfile profile = LegacyForwarding.parse(forwarded(null));

        assertEquals(UUID.fromString("f1d38adb-465c-4764-b82a-f29f57a3ff09"), profile.uuid());
        // The proxy already authenticated the player, so the profile is trusted.
        assertTrue(profile.isAuthenticated());
        // The name is not in the payload; it arrives with Login Start.
        assertNull(profile.name());
    }

    @Test
    void signedSkinPropertiesSurviveForwarding() {
        String properties = "[{\"name\":\"textures\",\"value\":\"TEX\",\"signature\":\"SIG\"}]";

        GameProfile profile = LegacyForwarding.parse(forwarded(properties));

        assertTrue(profile.hasTextures());
        assertEquals("TEX", profile.texturesValue());
        assertEquals("SIG", profile.texturesSignature(),
                "the proxy's signature must be passed through or the skin will not render");
    }

    @Test
    void originalHostAndClientAddressAreRecoverable() {
        assertEquals("mc.example.com", LegacyForwarding.originalHost(forwarded(null)));
        assertEquals("12.34.56.78", LegacyForwarding.clientAddress(forwarded(null)));
        assertEquals("plain.host", LegacyForwarding.originalHost("plain.host"));
    }

    @Test
    void truncatedForwardedDataIsRejected() {
        // Missing the uuid field: better to refuse than to invent an identity.
        assertThrows(IllegalArgumentException.class,
                () -> LegacyForwarding.parse("host" + SEP + "12.34.56.78"));
        assertThrows(IllegalArgumentException.class, () -> LegacyForwarding.parse(null));
    }

    @Test
    void profileNameIsFilledInAtLoginStart() {
        GameProfile named = LegacyForwarding.parse(forwarded(null)).withName("_poisoned");

        assertEquals("_poisoned", named.name());
        assertEquals(UUID.fromString("f1d38adb-465c-4764-b82a-f29f57a3ff09"), named.uuid());
        assertTrue(named.isAuthenticated(), "adding the name must not drop the trusted flag");
    }
}
