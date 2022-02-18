/*
 * Copyright 2019-2022 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.cloudnet.common.io;

import eu.cloudnetservice.cloudnet.common.function.ThrowableConsumer;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
public final class FileUtil {

  public static final Path TEMP_DIR = Path.of(System.getProperty("cloudnet.tempDir", "temp"));

  private static final Logger LOGGER = LogManager.logger(FileUtil.class);
  private static final Filter<Path> ACCEPTING_FILTER = $ -> true;
  private static final Map<String, String> ZIP_FILE_SYSTEM_PROPERTIES = Map.of("create", "false", "encoding", "UTF-8");

  private FileUtil() {
    throw new UnsupportedOperationException();
  }

  public static void openJarFileSystem(@NonNull Path jar, @NonNull ThrowableConsumer<FileSystem, Exception> consumer) {
    try (var fs = FileSystems.newFileSystem(URI.create("jar:" + jar.toUri()), ZIP_FILE_SYSTEM_PROPERTIES)) {
      consumer.accept(fs);
    } catch (Exception throwable) {
      LOGGER.severe("Exception opening jar file system on %s", throwable, jar);
    }
  }

  public static void move(@NonNull Path from, @NonNull Path to, CopyOption @NonNull ... options) {
    try {
      Files.move(from, to, options);
    } catch (IOException exception) {
      LOGGER.severe("Exception moving file from %s to %s", exception, from, to);
    }
  }

  public static void copy(@Nullable InputStream inputStream, @Nullable OutputStream outputStream) {
    if (inputStream != null && outputStream != null) {
      try {
        inputStream.transferTo(outputStream);
      } catch (IOException exception) {
        LOGGER.severe("Exception copying input stream to output stream", exception);
      }
    }
  }

  public static void copy(@Nullable InputStream inputStream, @Nullable Path target) {
    if (inputStream != null && target != null) {
      createDirectory(target.getParent());
      try (var out = Files.newOutputStream(target)) {
        FileUtil.copy(inputStream, out);
      } catch (IOException exception) {
        LOGGER.severe("Exception copying input stream to %s", exception, target);
      }
    }
  }

  public static void copy(@NonNull Path from, @NonNull Path to) {
    try {
      // create the parent directory first
      createDirectory(to.getParent());
      Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      LOGGER.severe("Exception copying file from %s to %s", exception, from, to);
    }
  }

  public static void copyDirectory(@NonNull Path from, @NonNull Path to) {
    copyDirectory(from, to, null);
  }

  public static void copyDirectory(@NonNull Path from, @NonNull Path to, @Nullable Filter<Path> filter) {
    walkFileTree(from, ($, current) -> {
      if (!Files.isDirectory(current)) {
        FileUtil.copy(current, to.resolve(from.relativize(current)));
      }
    }, true, filter == null ? ACCEPTING_FILTER : filter);
  }

  public static void delete(@Nullable Path path) {
    if (path != null && Files.exists(path)) {
      // delete all files in the directory
      if (Files.isDirectory(path)) {
        walkFileTree(path, ($, current) -> FileUtil.delete(current));
      }

      try {
        // remove the directory or the file
        Files.delete(path);
      } catch (IOException ignored) {
        // ignore these exceptions
      }
    }
  }

  public static @NonNull Path createTempFile() {
    createDirectory(TEMP_DIR);
    return TEMP_DIR.resolve(UUID.randomUUID().toString());
  }

  public static void walkFileTree(@NonNull Path root, @NonNull BiConsumer<Path, Path> consumer) {
    walkFileTree(root, consumer, true);
  }

  public static void walkFileTree(@NonNull Path root, @NonNull BiConsumer<Path, Path> consumer, boolean visitDirs) {
    walkFileTree(root, consumer, visitDirs, "*");
  }

  public static void walkFileTree(
    @NonNull Path root,
    @NonNull BiConsumer<Path, Path> consumer,
    boolean visitDirectories,
    @NonNull String glob
  ) {
    if (Files.exists(root)) {
      // create a glob path matcher and redirect the request
      var matcher = root.getFileSystem().getPathMatcher("glob:" + glob);
      walkFileTree(root, consumer, visitDirectories, path -> matcher.matches(path.getFileName()));
    }
  }

  public static void walkFileTree(
    @NonNull Path root,
    @NonNull BiConsumer<Path, Path> consumer,
    boolean visitDirectories,
    @NonNull Filter<Path> filter
  ) {
    if (Files.exists(root)) {
      try (var stream = Files.newDirectoryStream(root, filter)) {
        for (var path : stream) {
          // visit directories recursively if requested
          if (Files.isDirectory(path)) {
            // prevent posting of directories if not requested
            if (visitDirectories) {
              walkFileTree(path, consumer, true, filter);
            } else {
              continue;
            }
          }
          // accepts all files and directories
          consumer.accept(root, path);
        }
      } catch (IOException exception) {
        LOGGER.severe("Exception walking down directory tree starting at %s", exception, root);
      }
    }
  }

  public static void createDirectory(@Nullable Path directoryPath) {
    if (directoryPath != null && Files.notExists(directoryPath)) {
      try {
        Files.createDirectories(directoryPath);
      } catch (IOException exception) {
        LOGGER.severe("Exception creating directory at %s", exception, directoryPath);
      }
    }
  }

  public static void ensureChild(@NonNull Path root, @NonNull Path child) {
    var rootNormal = root.normalize().toAbsolutePath();
    var childNormal = child.normalize().toAbsolutePath();

    if (childNormal.getNameCount() <= rootNormal.getNameCount() || !childNormal.startsWith(rootNormal)) {
      throw new IllegalStateException("Child " + childNormal + " is not in root path " + rootNormal);
    }
  }

  public static @NonNull Path resolve(@NonNull Path base, String @NonNull ... more) {
    for (var child : more) {
      base = base.resolve(child);
    }
    return base;
  }
}
