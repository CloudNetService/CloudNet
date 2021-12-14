/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.common.io;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import de.dytanic.cloudnet.common.function.ThrowableConsumer;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The FileUtils class has a lot of utility methods, for
 * <ol>
 * <li>Byte Streams IO</li>
 * <li>File IO (Coping, Deleting)</li>
 * <li>Zip IO</li>
 * </ol>
 */
@Internal
public final class FileUtils {

  public static final Path TEMP_DIR = Paths.get(System.getProperty("cloudnet.tempDir", "temp"));

  private static final Logger LOGGER = LogManager.getLogger(FileUtils.class);
  private static final DirectoryStream.Filter<Path> ACCEPTING_FILTER = $ -> true;

  private static final Map<String, String> ZIP_FILE_SYSTEM_PROPERTIES = ImmutableMap
    .of("create", "false", "encoding", "UTF-8");

  private FileUtils() {
    throw new UnsupportedOperationException();
  }

  public static void openZipFileSystem(@NotNull Path zip, @NotNull ThrowableConsumer<FileSystem, Exception> consumer) {
    try (var fs = FileSystems.newFileSystem(URI.create("jar:" + zip.toUri()), ZIP_FILE_SYSTEM_PROPERTIES)) {
      consumer.accept(fs);
    } catch (Exception throwable) {
      LOGGER.severe("Exception while opening file", throwable);
    }
  }

  public static void move(@NotNull Path from, @NotNull Path to, CopyOption @NotNull ... options) {
    try {
      Files.move(from, to, options);
    } catch (IOException exception) {
      LOGGER.severe("Unable to move file " + from + " to " + to, exception);
    }
  }

  public static void copy(@Nullable InputStream inputStream, @Nullable OutputStream outputStream) {
    if (inputStream != null && outputStream != null) {
      try {
        ByteStreams.copy(inputStream, outputStream);
      } catch (IOException exception) {
        LOGGER.severe("Exception copying InputStream to OutputStream", exception);
      }
    }
  }

  public static void copy(@NotNull Path from, @NotNull Path to) {
    try {
      // create the parent directory first
      createDirectory(to.getParent());
      Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      LOGGER.severe("Exception copying file from " + from + " to " + to, exception);
    }
  }

  public static void copyDirectory(@NotNull Path from, @NotNull Path to) {
    copyDirectory(from, to, null);
  }

  public static void copyDirectory(Path from, Path to, DirectoryStream.Filter<Path> filter) {
    walkFileTree(from, ($, current) -> {
      if (!Files.isDirectory(current)) {
        FileUtils.copy(current, to.resolve(from.relativize(current)));
      }
    }, true, filter == null ? ACCEPTING_FILTER : filter);
  }

  public static void delete(@Nullable Path path) {
    if (path != null && Files.exists(path)) {
      // delete all files in the directory
      if (Files.isDirectory(path)) {
        walkFileTree(path, ($, current) -> FileUtils.delete(current));
      }
      // remove the directory or the file
      try {
        Files.delete(path);
      } catch (IOException ignored) {
        // ignore these exceptions
      }
    }
  }

  public static @NotNull Path createTempFile() {
    if (Files.notExists(TEMP_DIR)) {
      createDirectory(TEMP_DIR);
    }

    return TEMP_DIR.resolve(UUID.randomUUID().toString());
  }

  public static @NotNull InputStream zipToStream(@NotNull Path directory) {
    return zipToStream(directory, null);
  }

  public static @NotNull InputStream zipToStream(@NotNull Path directory, @Nullable Predicate<Path> fileFilter) {
    var target = createTempFile();
    zipToFile(
      directory,
      target,
      path -> !target.equals(path) && (fileFilter == null || fileFilter.test(path)));

    try {
      return Files.newInputStream(target, StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to open input stream to zip file " + target, exception);
    }
  }

  public static @Nullable Path zipToFile(@NotNull Path directory, @NotNull Path target) {
    return zipToFile(directory, target, null);
  }

  public static @Nullable Path zipToFile(@NotNull Path dir, @NotNull Path target, @Nullable Predicate<Path> filter) {
    if (Files.exists(dir)) {
      try (var out = new ZipOutputStream(Files.newOutputStream(target), StandardCharsets.UTF_8)) {
        zipDir(out, dir, filter);
        return target;
      } catch (IOException exception) {
        LOGGER.severe("Exception while processing new zip entry from directory " + dir, exception);
      }
    }

    return null;
  }

  private static void zipDir(
    @NotNull ZipOutputStream out,
    @NotNull Path dir,
    @Nullable Predicate<Path> filter
  ) throws IOException {
    Files.walkFileTree(
      dir,
      new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
          if (filter == null || filter.test(file)) {
            try {
              out.putNextEntry(new ZipEntry(dir.relativize(file).toString().replace("\\", "/")));
              Files.copy(file, out);
            } finally {
              out.closeEntry();
            }
          }
          // continue search
          return FileVisitResult.CONTINUE;
        }
      }
    );
  }

  public static @Nullable Path extract(@NotNull Path zipPath, @NotNull Path targetDirectory) {
    if (Files.exists(zipPath)) {
      try (var inputStream = Files.newInputStream(zipPath)) {
        return extract(inputStream, targetDirectory);
      } catch (IOException exception) {
        LOGGER.severe("Unable to extract zip from " + zipPath + " to " + targetDirectory, exception);
      }
    }
    return null;
  }

  public static @Nullable Path extract(@NotNull InputStream in, @NotNull Path targetDirectory) {
    return extractZipStream(
      in instanceof ZipInputStream ? (ZipInputStream) in : new ZipInputStream(in, StandardCharsets.UTF_8),
      targetDirectory);
  }

  public static @Nullable Path extractZipStream(@NotNull ZipInputStream zipInputStream, @NotNull Path targetDirectory) {
    try {
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        extractEntry(zipInputStream, zipEntry, targetDirectory);
        zipInputStream.closeEntry();
      }

      return targetDirectory;
    } catch (IOException exception) {
      LOGGER.severe("Exception unzipping zip file to " + targetDirectory, exception);
      return null;
    }
  }

  private static void extractEntry(
    @NotNull ZipInputStream in,
    @NotNull ZipEntry zipEntry,
    @NotNull Path targetDirectory
  ) throws IOException {
    // get the target path and ensure that there is no path traversal
    var file = targetDirectory.resolve(zipEntry.getName());
    ensureChild(targetDirectory, file);

    if (zipEntry.isDirectory()) {
      FileUtils.createDirectory(file);
    } else {
      FileUtils.createDirectory(file.getParent());
      try (var outputStream = Files.newOutputStream(file)) {
        copy(in, outputStream);
      }
    }
  }

  public static void walkFileTree(@NotNull Path root, @NotNull BiConsumer<Path, Path> consumer) {
    walkFileTree(root, consumer, true);
  }

  public static void walkFileTree(@NotNull Path root, @NotNull BiConsumer<Path, Path> consumer, boolean visitDirs) {
    walkFileTree(root, consumer, visitDirs, "*");
  }

  public static void walkFileTree(
    @NotNull Path root,
    @NotNull BiConsumer<Path, Path> consumer,
    boolean visitDirectories,
    @NotNull String glob
  ) {
    if (Files.exists(root)) {
      // create a glob path matcher and redirect the request
      var matcher = root.getFileSystem().getPathMatcher("glob:" + glob);
      walkFileTree(root, consumer, visitDirectories, path -> matcher.matches(path.getFileName()));
    }
  }

  public static void walkFileTree(
    @NotNull Path root,
    @NotNull BiConsumer<Path, Path> consumer,
    boolean visitDirectories,
    @NotNull DirectoryStream.Filter<Path> filter
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
              return;
            }
          }
          // accepts all files and directories
          consumer.accept(root, path);
        }
      } catch (IOException exception) {
        LOGGER.severe("Exception walking directory tree from " + root, exception);
      }
    }
  }

  public static void createDirectory(@Nullable Path directoryPath) {
    if (directoryPath != null && Files.notExists(directoryPath)) {
      try {
        Files.createDirectories(directoryPath);
      } catch (IOException exception) {
        LOGGER.severe("Exception while creating directory", exception);
      }
    }
  }

  public static void ensureChild(@NotNull Path root, @NotNull Path child) {
    var rootNormal = root.normalize().toAbsolutePath();
    var childNormal = child.normalize().toAbsolutePath();

    if (childNormal.getNameCount() <= rootNormal.getNameCount() || !childNormal.startsWith(rootNormal)) {
      throw new IllegalStateException("Child " + childNormal + " is not in root path " + rootNormal);
    }
  }

  public static @NotNull Path resolve(@NotNull Path base, String @NotNull ... more) {
    for (var child : more) {
      base = base.resolve(child);
    }
    return base;
  }
}
