package net.vibmc.storage;

import net.vibmc.world.Chunk;
import net.vibmc.world.World;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class WorldStore {
    private final World world;
    private final Path root;

    public WorldStore(World world, Path root) {
        this.world = world;
        this.root = root;
    }

    public void loadAll() throws IOException {
        Files.createDirectories(root.resolve("chunks"));
    }

    public void saveAll() throws IOException {
        Path chunkDirectory = root.resolve("chunks");
        Files.createDirectories(chunkDirectory);
        for (Chunk chunk : world.chunkManager().listLoadedChunks()) {
            saveChunk(chunkDirectory, chunk);
        }
        saveMetadata();
    }

    private void saveMetadata() throws IOException {
        Files.createDirectories(root);
        Properties properties = new Properties();
        properties.setProperty("seed", Long.toString(world.seed()));
        properties.setProperty("name", world.name());
        try (DataOutputStream outputStream = new DataOutputStream(Files.newOutputStream(root.resolve("world.meta")))) {
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                outputStream.writeUTF(key);
                outputStream.writeUTF(value);
            }
        }
    }

    private void saveChunk(Path directory, Chunk chunk) throws IOException {
        Path path = directory.resolve(chunk.chunkX() + "_" + chunk.chunkZ() + ".bin");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(path))) {
            out.writeInt(chunk.chunkX());
            out.writeInt(chunk.chunkZ());
            for (short block : chunk.blocks()) {
                out.writeShort(block);
            }
        }
    }
}
