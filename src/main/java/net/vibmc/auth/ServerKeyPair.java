package net.vibmc.auth;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * The server's RSA identity for the 1.12.2 login handshake.
 *
 * <p>Generated fresh at startup (vanilla does the same - the key is per-session and never
 * stored). The client encrypts a 16-byte shared secret against the public key; from there
 * both sides switch to AES.
 */
public final class ServerKeyPair {
    private static final int KEY_SIZE = 1024; // what vanilla uses; the client accepts nothing larger

    private final KeyPair keyPair;

    public ServerKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            this.keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is required for online mode but is unavailable", e);
        }
    }

    public PublicKey publicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey privateKey() {
        return keyPair.getPrivate();
    }

    /**
     * The X.509/DER encoding of the public key. These exact bytes go on the wire and are
     * fed into the server hash, so both sides must agree on them byte for byte.
     */
    public byte[] encodedPublicKey() {
        return keyPair.getPublic().getEncoded();
    }

    /** Decrypts one of the RSA-encrypted blobs in the client's Encryption Response. */
    public byte[] decrypt(byte[] data) throws GeneralSecurityFailure {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new GeneralSecurityFailure("could not decrypt the client's key exchange", e);
        }
    }

    /** Wraps the 16-byte shared secret as an AES key. */
    public static SecretKey sharedSecret(byte[] secret) {
        return new SecretKeySpec(secret, "AES");
    }

    /**
     * The login hash vanilla expects: {@code sha1(serverId + sharedSecret + publicKey)},
     * rendered as a signed (two's-complement) hex string. Mojang's session server keys the
     * pending join off exactly this value, so the encoding has to match vanilla's quirk of
     * emitting a leading '-' for negative digests rather than zero-padding.
     */
    public static String serverHash(String serverId, byte[] sharedSecret, byte[] publicKey)
            throws GeneralSecurityFailure {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
            digest.update(sharedSecret);
            digest.update(publicKey);
            return new BigInteger(digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralSecurityFailure("SHA-1 is required for online mode", e);
        }
    }

    /** Raised when the handshake's crypto cannot be completed. */
    public static class GeneralSecurityFailure extends Exception {
        public GeneralSecurityFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
