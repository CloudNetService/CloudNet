package de.dytanic.cloudnet.common.document;

import de.dytanic.cloudnet.common.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface is interesting to persistence data, of the implement object
 */
public interface IPersistable {

  @NotNull
  IPersistable write(Writer writer);

  @NotNull
  default IPersistable write(OutputStream outputStream) {
    try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      return this.write(writer);
    } catch (IOException exception) {
      exception.printStackTrace();
      return this;
    }
  }

  @NotNull
  default IPersistable write(@Nullable Path path) {
    if (path != null) {
      FileUtils.createDirectoryReported(path.getParent());
      try (OutputStream stream = Files.newOutputStream(path)) {
        return this.write(stream);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    return this;
  }

  @NotNull
  default IPersistable write(@Nullable String path) {
    if (path != null) {
      return this.write(Paths.get(path));
    }
    return this;
  }

  @NotNull
  default IPersistable write(@Nullable String... paths) {
    if (paths != null) {
      for (String path : paths) {
        this.write(path);
      }
    }
    return this;
  }

  @NotNull
  @Deprecated
  default IPersistable write(@Nullable File file) {
    if (file != null) {
      return this.write(file.toPath());
    }
    return this;
  }

  @NotNull
  @Deprecated
  default IPersistable write(@Nullable File... files) {
    if (files != null) {
      for (File file : files) {
        this.write(file);
      }
    }
    return this;
  }

  @NotNull
  default IPersistable write(@Nullable Path... paths) {
    if (paths != null) {
      for (Path path : paths) {
        this.write(path);
      }
    }
    return this;
  }
}
