package de.dytanic.cloudnet.driver.network.protocol.chunk.client;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

public abstract class CachedChunkedPacketListener extends ChunkedPacketListener {

    @Override
    protected @NotNull OutputStream createOutputStream(@NotNull UUID sessionUniqueId, @NotNull Map<String, Object> properties) throws IOException {
        Path path = Paths.get(System.getProperty("cloudnet.tempDir", "temp"), sessionUniqueId.toString());
        Files.createDirectories(path.getParent());

        properties.put("path", path);
        return Files.newOutputStream(path);
    }

    @Override
    protected void handleComplete(@NotNull ClientChunkedPacketSession session) throws IOException {
        Path path = (Path) session.getProperties().get("path");
        Preconditions.checkArgument(Files.exists(path), "Path of the cache doesn't exist");

        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE)) {
            this.handleComplete(session, inputStream);
        }
    }

    protected abstract void handleComplete(@NotNull ClientChunkedPacketSession session, @NotNull InputStream inputStream) throws IOException;
}
