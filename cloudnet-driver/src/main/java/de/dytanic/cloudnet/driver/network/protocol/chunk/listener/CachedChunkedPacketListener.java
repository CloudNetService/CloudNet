package de.dytanic.cloudnet.driver.network.protocol.chunk.listener;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public abstract class CachedChunkedPacketListener extends ChunkedPacketListener {

  @Override
  protected @NotNull OutputStream createOutputStream(@NotNull UUID sessionUniqueId,
    @NotNull Map<String, Object> properties) throws IOException {
    Path path = Paths.get(System.getProperty("cloudnet.tempDir", "temp"), sessionUniqueId.toString());
    Files.createDirectories(path.getParent());

    properties.put("path", path);
    return Files.newOutputStream(path);
  }

  @Override
  protected void handleComplete(@NotNull ChunkedPacketSession session) throws IOException {
    Path path = (Path) session.getProperties().get("path");
    Preconditions.checkArgument(Files.exists(path), "Path of the cache doesn't exist");

    this.handleComplete(session, Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE));
  }

  protected abstract void handleComplete(@NotNull ChunkedPacketSession session, @NotNull InputStream inputStream)
    throws IOException;
}
