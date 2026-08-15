package net.vibmc.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationTest {
    @Test
    void offlineUuidMatchesVanillasConvention() {
        // Vanilla derives offline ids from "OfflinePlayer:<name>"; matching it means worlds
        // stay compatible with other offline servers and with the launcher's offline mode.
        UUID expected = UUID.nameUUIDFromBytes(
                "OfflinePlayer:Notch".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, GameProfile.offlineUuid("Notch"));

        GameProfile profile = GameProfile.offline("Notch");
        assertFalse(profile.isAuthenticated());
        assertFalse(profile.hasTextures());
    }

    @Test
    void undashedMojangIdsBecomeRealUuids() {
        UUID uuid = SessionAuthenticator.undashedToUuid("f1d38adb465c4764b82af29f57a3ff09");
        assertEquals(UUID.fromString("f1d38adb-465c-4764-b82a-f29f57a3ff09"), uuid);

        // Already-dashed input is accepted too, so callers do not have to care.
        assertEquals(uuid, SessionAuthenticator.undashedToUuid("f1d38adb-465c-4764-b82a-f29f57a3ff09"));
        assertThrows(IllegalArgumentException.class, () -> SessionAuthenticator.undashedToUuid("nope"));
    }

    @Test
    void sessionResponseYieldsASignedAuthenticatedProfile() throws IOException {
        String body = "{"
                + "\"id\":\"f1d38adb465c4764b82af29f57a3ff09\","
                + "\"name\":\"_poisoned\","
                + "\"properties\":[{"
                + "\"name\":\"textures\","
                + "\"value\":\"BASE64TEXTURES\","
                + "\"signature\":\"MOJANGSIGNATURE\"}]}";

        GameProfile profile = SessionAuthenticator.parseProfile(body);

        assertEquals("_poisoned", profile.name());
        assertEquals(UUID.fromString("f1d38adb-465c-4764-b82a-f29f57a3ff09"), profile.uuid());
        assertTrue(profile.isAuthenticated());
        assertTrue(profile.hasTextures());
        assertEquals("BASE64TEXTURES", profile.texturesValue());
        // The signature must survive intact: it cannot be regenerated, and without it the
        // client refuses to render the skin.
        assertEquals("MOJANGSIGNATURE", profile.texturesSignature());
    }

    @Test
    void sessionResponseWithoutTexturesStillAuthenticates() throws IOException {
        String body = "{\"id\":\"f1d38adb465c4764b82af29f57a3ff09\",\"name\":\"x\",\"properties\":[]}";

        GameProfile profile = SessionAuthenticator.parseProfile(body);

        assertTrue(profile.isAuthenticated());
        assertFalse(profile.hasTextures());
        assertNull(profile.texturesSignature());
    }

    @Test
    void malformedSessionResponseIsRejected() {
        assertThrows(IOException.class, () -> SessionAuthenticator.parseProfile("not json"));
        assertThrows(IOException.class, () -> SessionAuthenticator.parseProfile("{\"name\":\"x\"}"));
    }

    @Test
    void serverHashMatchesVanillasKnownVectors() throws Exception {
        // Mojang publishes these as the reference cases for the login hash. Getting the
        // negative case right matters: vanilla emits a signed hex string rather than
        // zero-padding, and the session server keys the join off the exact text.
        assertEquals("4ed1f46bbe04bc756bcb17c0c7ce3e4632f06a48",
                ServerKeyPair.serverHash("Notch", new byte[0], new byte[0]));
        assertEquals("-7c9d5b0044c130109a5d7b5fb5c317c02b4e28c1",
                ServerKeyPair.serverHash("jeb_", new byte[0], new byte[0]));
        assertEquals("88e16a1019277b15d58faf0541e11910eb756f6",
                ServerKeyPair.serverHash("simon", new byte[0], new byte[0]));
    }

    @Test
    void keyPairRoundTripsAnEncryptedSecret() throws Exception {
        ServerKeyPair keyPair = new ServerKeyPair();
        byte[] secret = new byte[16];
        for (int i = 0; i < secret.length; i++) {
            secret[i] = (byte) i;
        }

        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keyPair.publicKey());
        byte[] encrypted = cipher.doFinal(secret);

        assertArrayEqualsBytes(secret, keyPair.decrypt(encrypted));
        assertNotNull(keyPair.encodedPublicKey());
        assertTrue(keyPair.encodedPublicKey().length > 0,
                "the encoded public key is what the client hashes, so it must be present");
    }

    @Test
    void aesCfb8SurvivesBeingFedInArbitraryChunks() throws Exception {
        // The connection encrypts and decrypts incrementally as bytes arrive, so a message
        // split across reads has to come back identical to one encrypted in a single go.
        byte[] secret = new byte[16];
        new java.util.Random(7).nextBytes(secret);
        javax.crypto.SecretKey key = ServerKeyPair.sharedSecret(secret);
        javax.crypto.spec.IvParameterSpec iv = new javax.crypto.spec.IvParameterSpec(secret);

        javax.crypto.Cipher encrypt = javax.crypto.Cipher.getInstance("AES/CFB8/NoPadding");
        encrypt.init(javax.crypto.Cipher.ENCRYPT_MODE, key, iv);
        javax.crypto.Cipher decrypt = javax.crypto.Cipher.getInstance("AES/CFB8/NoPadding");
        decrypt.init(javax.crypto.Cipher.DECRYPT_MODE, key, iv);

        byte[] message = "a login packet split across several socket reads".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encrypt.update(message);

        // Feed the ciphertext back in three uneven pieces.
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write(decrypt.update(encrypted, 0, 5));
        out.write(decrypt.update(encrypted, 5, 20));
        out.write(decrypt.update(encrypted, 25, encrypted.length - 25));

        assertArrayEqualsBytes(message, out.toByteArray());
    }

    @Test
    void usernamesAreValidatedBeforeUse() {
        assertTrue(net.vibmc.network.handler.LoginHandler.isValidUsername("Notch"));
        assertTrue(net.vibmc.network.handler.LoginHandler.isValidUsername("_poisoned"));
        assertTrue(net.vibmc.network.handler.LoginHandler.isValidUsername("a_1"));

        assertFalse(net.vibmc.network.handler.LoginHandler.isValidUsername(null));
        assertFalse(net.vibmc.network.handler.LoginHandler.isValidUsername("ab"), "too short");
        assertFalse(net.vibmc.network.handler.LoginHandler.isValidUsername("seventeen_chars_x"), "too long");
        assertFalse(net.vibmc.network.handler.LoginHandler.isValidUsername("has space"));
        assertFalse(net.vibmc.network.handler.LoginHandler.isValidUsername("drop;table"));
    }

    private static void assertArrayEqualsBytes(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length, "length");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "byte " + i);
        }
    }
}
