package net.vibmc.server;

import net.vibmc.server.util.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The Minecraft EULA gate, in the shape operators already know from vanilla.
 *
 * <p>vib-MC speaks Minecraft's protocol to Minecraft's client, so running one means
 * agreeing to Mojang's terms. The first start writes {@code eula.txt} with the agreement
 * unaccepted and stops; the operator reads the terms, sets {@code eula=true}, and starts
 * again. Accepting on their behalf would defeat the entire point of asking.
 */
public final class Eula {
    public static final String URL = "https://aka.ms/MinecraftEULA";
    private static final String FILE = "eula.txt";

    private Eula() {
    }

    /**
     * Checks the agreement, creating the file the first time.
     *
     * @return true if the operator has accepted and the server may start
     */
    public static boolean accepted(Logger logger) {
        return accepted(logger, Paths.get(FILE));
    }

    /** Testable form: the same check against a given file. */
    public static boolean accepted(Logger logger, Path file) {
        if (!Files.exists(file)) {
            write(logger, file);
            report(logger, file);
            return false;
        }

        boolean accepted = readAccepted(logger, file);
        if (!accepted) {
            report(logger, file);
        }
        return accepted;
    }

    private static boolean readAccepted(Logger logger, Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equals = trimmed.indexOf('=');
                if (equals < 0) {
                    continue;
                }
                if (!trimmed.substring(0, equals).trim().equalsIgnoreCase("eula")) {
                    continue;
                }
                return Boolean.parseBoolean(trimmed.substring(equals + 1).trim());
            }
        } catch (IOException e) {
            warn(logger, "Could not read %s: %s", file, e.getMessage());
        }
        return false;
    }

    private static void write(Logger logger, Path file) {
        String contents = "#By changing the setting below to TRUE you are indicating your agreement"
                + " to the Minecraft EULA (" + URL + ")." + System.lineSeparator()
                + "#vib-MC is an unofficial server implementation and is not affiliated with"
                + " Mojang or Microsoft." + System.lineSeparator()
                + "eula=false" + System.lineSeparator();
        try {
            Files.write(file, contents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            warn(logger, "Could not write %s: %s", file, e.getMessage());
        }
    }

    private static void report(Logger logger, Path file) {
        warn(logger, "You need to agree to the Minecraft EULA (%s) to run this server.", URL);
        warn(logger, "Read it, then set eula=true in %s and start the server again.",
                file.toAbsolutePath());
    }

    private static void warn(Logger logger, String message, Object... args) {
        if (logger != null) {
            logger.warn(message, args);
        }
    }
}
