package net.vibmc.server;

import net.vibmc.server.util.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EulaTest {
    private static final Logger LOGGER = new Logger("test");

    @Test
    void firstRunWritesTheFileAndRefusesToStart(@TempDir Path dir) throws IOException {
        Path eula = dir.resolve("eula.txt");

        assertFalse(Eula.accepted(LOGGER, eula), "an unaccepted EULA must stop the server");
        assertTrue(Files.exists(eula), "the file is created so there is something to accept");

        String contents = new String(Files.readAllBytes(eula), StandardCharsets.UTF_8);
        assertTrue(contents.contains("eula=false"), "it starts out unaccepted");
        assertTrue(contents.contains(Eula.URL), "and points at the terms being agreed to");
    }

    @Test
    void acceptingLetsTheServerStart(@TempDir Path dir) throws IOException {
        Path eula = dir.resolve("eula.txt");
        Files.write(eula, "eula=true\n".getBytes(StandardCharsets.UTF_8));

        assertTrue(Eula.accepted(LOGGER, eula));
    }

    @Test
    void commentsAndSpacingAndCaseAreTolerated(@TempDir Path dir) throws IOException {
        Path eula = dir.resolve("eula.txt");
        Files.write(eula, ("#agreed on some date\n\n  EULA = TRUE  \n")
                .getBytes(StandardCharsets.UTF_8));

        assertTrue(Eula.accepted(LOGGER, eula), "the file people hand-edit must parse loosely");
    }

    @Test
    void anythingOtherThanTrueIsNotAcceptance(@TempDir Path dir) throws IOException {
        Path eula = dir.resolve("eula.txt");
        Files.write(eula, "eula=yes\n".getBytes(StandardCharsets.UTF_8));

        assertFalse(Eula.accepted(LOGGER, eula), "only true is true");
    }
}
