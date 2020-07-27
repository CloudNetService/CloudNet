package de.dytanic.cloudnet.driver.network.protocol.chunk;

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

public abstract class CachedChunkedPacketListener extends ChunkedPacketListener {
    @Override
    protected @NotNull OutputStream createOutputStream(ChunkedPacket startPacket, Map<String, Object> properties) throws IOException {
        Path path = Paths.get(System.getProperty("cloudnet.tempDir", "temp"), startPacket.getUniqueId().toString());
        Files.createDirectories(path.getParent());

        properties.put("path", path);

        return Files.newOutputStream(path);
    }

    @Override
    protected void handleComplete(ChunkedPacketSession session) throws IOException {
        Path path = (Path) session.getProperties().get("path");
        Preconditions.checkArgument(Files.exists(path), "Path of the cache doesn't exist");

        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE)) {
            this.handleComplete(session, inputStream);
        }
    }

    protected abstract void handleComplete(ChunkedPacketSession session, InputStream inputStream) throws IOException;

}
