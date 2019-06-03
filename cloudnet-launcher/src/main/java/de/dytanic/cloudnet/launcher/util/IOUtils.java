package de.dytanic.cloudnet.launcher.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IOUtils {

  private IOUtils() {
    throw new UnsupportedOperationException();
  }

  public static void copy(byte[] buffer, InputStream inputStream, Path path)
      throws Exception {
    if (Files.exists(path)) {
      Files.delete(path);
    }

    Files.createFile(path);

    try (OutputStream outputStream = Files.newOutputStream(path)) {
      copy(buffer, inputStream, outputStream);
    }
  }

  public static void copy(byte[] buffer, InputStream inputStream,
      OutputStream outputStream) throws Exception {
    int len;
    while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
      outputStream.write(buffer, 0, len);
      outputStream.flush();
    }
  }

}