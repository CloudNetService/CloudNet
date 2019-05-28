package de.dytanic.cloudnet.common.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This interface is interesting to read data, of the implement object
 */
public interface IReadable {

  IReadable read(InputStream inputStream);

  IReadable read(Reader reader);

  IReadable read(byte[] bytes);

  IReadable append(InputStream inputStream);

  IReadable append(Reader reader);

  /*= --------------------------------------------------------------- =*/

  default IReadable read(Path path) {
    if (Files.exists(path)) {
      try (InputStream inputStream = new FileInputStream(path.toFile())) {
        this.read(inputStream);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return this;
  }

  default IReadable read(String path) {
    if (path == null) {
      return this;
    }

    return this.read(Paths.get(path));
  }

  default IReadable read(String... paths) {
    if (paths == null) {
      return this;
    }
    for (String path : paths) {
      this.read(path);
    }

    return this;
  }

  default IReadable read(File file) {
    if (file == null) {
      return this;
    }

    return this.read(file.toPath());
  }

  default IReadable read(File... files) {
    if (files == null) {
      return this;
    }
    for (File file : files) {
      this.read(file);
    }

    return this;
  }

  default IReadable read(Path... paths) {
    if (paths == null) {
      return this;
    }
    for (Path path : paths) {
      this.read(path);
    }

    return this;
  }

  default IReadable append(Path path) {
    this.read(path);
    return this;
  }

  default IReadable append(String path) {
    if (path == null) {
      return this;
    }

    return this.append(Paths.get(path));
  }

  default IReadable append(String... paths) {
    if (paths == null) {
      return this;
    }
    for (String path : paths) {
      this.append(path);
    }

    return this;
  }

  default IReadable append(File file) {
    if (file == null) {
      return this;
    }

    return this.append(file.toPath());
  }

  default IReadable append(File... files) {
    if (files == null) {
      return this;
    }
    for (File file : files) {
      this.append(file);
    }

    return this;
  }

  default IReadable append(Path... paths) {
    if (paths == null) {
      return this;
    }
    for (Path path : paths) {
      this.append(path);
    }

    return this;
  }
}