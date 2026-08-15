package net.vibmc.auth;

import net.vibmc.util.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Checks a joining player against Mojang's session server.
 *
 * <p>The client tells Mojang it is joining a server identified by the login hash; the
 * server then asks Mojang whether that join is real. A 204 means "no such pending join",
 * i.e. the client could not prove it owns the account.
 */
public class SessionAuthenticator {
    private static final String ENDPOINT =
            "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    /**
     * Verifies a join and returns the authenticated profile.
     *
     * @return the profile Mojang vouched for, or null when the session is not valid
     * @throws IOException if Mojang could not be reached at all - the caller should treat
     *                     this as a temporary failure rather than a failed login
     */
    public GameProfile authenticate(String username, String serverHash) throws IOException {
        String url = ENDPOINT
                + "?username=" + URLEncoder.encode(username, "UTF-8")
                + "&serverId=" + URLEncoder.encode(serverHash, "UTF-8");

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("GET");
        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NO_CONTENT) {
                return null; // Mojang has no pending join for this hash
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("session server returned HTTP " + status);
            }
            return parseProfile(readBody(connection));
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(HttpURLConnection connection) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    /** Visible for testing: turns a hasJoined response body into a profile. */
    @SuppressWarnings("unchecked")
    public static GameProfile parseProfile(String body) throws IOException {
        try {
            Map<String, Object> root = Json.parseObject(body);
            String id = Json.string(root, "id");
            String name = Json.string(root, "name");
            if (id == null || name == null) {
                throw new IOException("session response is missing id or name");
            }

            String texturesValue = null;
            String texturesSignature = null;
            Object properties = root.get("properties");
            if (properties instanceof List) {
                for (Object entry : (List<Object>) properties) {
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
            return new GameProfile(undashedToUuid(id), name, texturesValue, texturesSignature, true);
        } catch (IllegalArgumentException e) {
            throw new IOException("malformed session response: " + e.getMessage(), e);
        }
    }

    /** Mojang sends UUIDs without dashes; the protocol and our world data use dashed form. */
    public static UUID undashedToUuid(String id) {
        String undashed = id.replace("-", "");
        if (undashed.length() != 32) {
            throw new IllegalArgumentException("not a UUID: " + id);
        }
        return UUID.fromString(undashed.substring(0, 8) + "-"
                + undashed.substring(8, 12) + "-"
                + undashed.substring(12, 16) + "-"
                + undashed.substring(16, 20) + "-"
                + undashed.substring(20));
    }
}
