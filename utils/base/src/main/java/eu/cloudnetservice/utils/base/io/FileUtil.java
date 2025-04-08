/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.utils.base.io;

import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction1;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This file utility class wraps convenient non-blocking-io methods that use checked exceptions into methods that catch
 * those exceptions and redirect them into the error log.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class FileUtil {

  public static final Path TEMP_DIR = Path.of(System.getProperty("cloudnet.tempDir", "temp"));

  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
  private static final DirectoryStream.Filter<Path> ACCEPTING_FILTER = $ -> true;

  private static final FileSystemProvider JAR_FILE_SYSTEM_PROVIDER;
  private static final Map<String, String> ZIP_FILE_SYSTEM_PROPERTIES = Map.of("create", "false", "encoding", "UTF-8");

  static {
    // caching this and calling newFileSystem reduces the lookup load if multiple file systems are registered
    // We cannot call newFileSystem using an url (simpler way via FileSystems.newFileSystem) because the zip provider
    // does not allow the creation of multiple file systems when created via url, but when created via path that is ok
    JAR_FILE_SYSTEM_PROVIDER = FileSystemProvider.installedProviders().stream()
      .filter(prov -> prov.getScheme().equalsIgnoreCase("jar"))
      .findFirst()
      .orElseThrow(() -> new ExceptionInInitializerError("Unable to find a file system provider supporting jars"));

    // pre-create the temporary directory
    FileUtil.createDirectory(TEMP_DIR);
  }

  private FileUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Opens the given zip into a {@link FileSystem} and passes it into the given consumer. The file system is closed
   * automatically after all operations are done.
   *
   * @param zip      the path to open as zip file system.
   * @param consumer the consumer accepting the file system.
   * @throws NullPointerException if the given zip path or consumer is null.
   */
  public static void openZipFile(@NonNull Path zip, @NonNull CheckedConsumer<FileSystem> consumer) {
    try (var fs = JAR_FILE_SYSTEM_PROVIDER.newFileSystem(zip, ZIP_FILE_SYSTEM_PROPERTIES)) {
      consumer.accept(fs);
    } catch (Throwable throwable) {
      LOGGER.error("Exception opening zip file system on {}", zip, throwable);
    }
  }

  /**
   * Opens the given zip into a {@link FileSystem} and passes it into the given function. The file system is closed
   * automatically after all operations are done.
   *
   * @param zip    the path to open as zip file system.
   * @param mapper the mapper to apply to the given zip file.
   * @param def    the return value to return from the method when an exception occurs.
   * @param <T>    the return type of the mapping function.
   * @return the mapped value based on the file system, or def if an exception occurs.
   * @throws NullPointerException if the given zip path or consumer is null.
   */
  public static @UnknownNullability <T> T mapZipFile(
    @NonNull Path zip,
    @NonNull CheckedFunction1<FileSystem, T> mapper,
    @Nullable T def
  ) {
    try (var fs = JAR_FILE_SYSTEM_PROVIDER.newFileSystem(zip, ZIP_FILE_SYSTEM_PROPERTIES)) {
      return mapper.apply(fs);
    } catch (Throwable throwable) {
      return def;
    }
  }

  /**
   * Moves the file at the source path to the given destination path. Use the copy options to specify the behavior when
   * moving. This method is equivalent with {@code Files.move(from, to, options...)} but catches the thrown exceptions
   * and just prints them into the error log.
   *
   * @param from    the file to move.
   * @param to      the destination path.
   * @param options options that specify the moving behavior.
   * @throws NullPointerException if the given paths are null or null values for the options are passed.
   */
  public static void move(@NonNull Path from, @NonNull Path to, CopyOption @NonNull ... options) {
    try {
      Files.move(from, to, options);
    } catch (IOException exception) {
      LOGGER.error("Exception moving file from {} to {}", from, to, exception);
    }
  }

  /**
   * Copies the input stream to the output stream. This method is equivalent with
   * {@code inputStream.transferTo(outputStream)} but catches the thrown exceptions and just prints them into the error
   * log.
   *
   * @param inputStream  the input stream to copy from.
   * @param outputStream the target output stream.
   */
  public static void copy(@Nullable InputStream inputStream, @Nullable OutputStream outputStream) {
    if (inputStream != null && outputStream != null) {
      try {
        inputStream.transferTo(outputStream);
      } catch (IOException exception) {
        LOGGER.error("Exception copying input stream to output stream", exception);
      }
    }
  }

  /**
   * Copies the input stream to given target path. This method creates all needed parent directories first and then uses
   * {@code FileUtil.copy(inputStream, out)} to copy to the destination, by converting the path into a new output
   * stream.
   *
   * @param inputStream the input stream to copy from.
   * @param target      the destination path.
   */
  public static void copy(@Nullable InputStream inputStream, @Nullable Path target) {
    if (inputStream != null && target != null) {
      createDirectory(target.getParent());
      try (var out = Files.newOutputStream(target)) {
        FileUtil.copy(inputStream, out);
      } catch (IOException exception) {
        LOGGER.error("Exception copying input stream to {}", target, exception);
      }
    }
  }

  /**
   * Copies the source file to the given destination. This method creates all needed parent directories first and then
   * uses {@code Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)} to copy the file, therefore existing files
   * are replaced. All thrown I/O exceptions are caught and redirected into the error log.
   *
   * @param from the source path.
   * @param to   the destination path.
   * @throws NullPointerException if any of the given paths is null.
   */
  public static void copy(@NonNull Path from, @NonNull Path to) {
    try {
      // create the parent directory first
      createDirectory(to.getParent());
      Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      LOGGER.error("Exception copying file from {} to {}", from, to, exception);
    }
  }

  /**
   * Copies the target directory to the given destination and creates all needed parent directories. It walks the whole
   * file tree and copies every file and subdirectory without any filtering. This method is equivalent to
   * {@code FileUtil.copyDirectory(from, to, null)}.
   *
   * @param from the source path.
   * @param to   the destination path.
   * @throws NullPointerException if any of the given paths is null.
   */
  public static void copyDirectory(@NonNull Path from, @NonNull Path to) {
    copyDirectory(from, to, null);
  }

  /**
   * Copies the target directory to the given destination and creates all needed parent directories. It walks the whole
   * file tree and copies every file and subdirectory. If the given filter is null no filtering is done, otherwise the
   * filter is applied while walking the file tree.
   *
   * @param from   the source path.
   * @param to     the destination path.
   * @param filter the filter to use while walking the file tree.
   * @throws NullPointerException if any of the given paths is null.
   */
  public static void copyDirectory(
    @NonNull Path from,
    @NonNull Path to,
    @Nullable DirectoryStream.Filter<Path> filter
  ) {
    walkFileTree(from, ($, current) -> {
      if (!Files.isDirectory(current)) {
        FileUtil.copy(current, to.resolve(from.relativize(current)));
      }
    }, true, filter == null ? ACCEPTING_FILTER : filter);
  }

  /**
   * Deletes the file or directory at the given path. If the path points to a file it is deleted using
   * {@code Files.delete(path)} occurring exceptions are ignored here, otherwise if the path points to a directory this
   * walks the file tree and deletes the files and directories recursively.
   *
   * @param path the target to delete.
   */
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

  /**
   * Resolves a random path in the temp directory of the cloud and creates all needed parents directories for a
   * temporary file including the {@link FileUtil#TEMP_DIR}.
   *
   * @return the path to the temporary file.
   */
  public static @NonNull Path createTempFile() {
    createDirectory(TEMP_DIR);
    return TEMP_DIR.resolve(UUID.randomUUID().toString());
  }

  /**
   * Walks the file tree, while visiting directories too, starting at the given path and passes the root directory
   * together with the next file or directory into the bi consumer. This method is equivalent to
   * {@code FileUtil.walkFileTree(root, consumer, true)}.
   *
   * @param root     the root path to start at.
   * @param consumer the consumer accepting all files and directories.
   * @throws NullPointerException if the given path or consumer is null.
   */
  public static void walkFileTree(@NonNull Path root, @NonNull BiConsumer<Path, Path> consumer) {
    walkFileTree(root, consumer, true);
  }

  /**
   * Walks the file tree, but only visits directories if specified, starting at the given path and passes the root
   * directory together with the next file or directory into the bi consumer. This method is equivalent to
   * {@code FileUtil.walkFileTree(root, consumer, visitDirs, "*")}.
   *
   * @param root      the root path to start at.
   * @param consumer  the consumer accepting all files and directories.
   * @param visitDirs whether to visit subdirectories too or not.
   * @throws NullPointerException if the given path or consumer is null.
   */
  public static void walkFileTree(@NonNull Path root, @NonNull BiConsumer<Path, Path> consumer, boolean visitDirs) {
    walkFileTree(root, consumer, visitDirs, "*");
  }

  /**
   * Walks the file tree while filtering for the given glob and only visiting directories if specified. This starts at
   * the given root path and passes the root directory together with the next file or directory into the bi consumer.
   *
   * @param root             the root path to start at.
   * @param consumer         the consumer accepting all files and directories.
   * @param visitDirectories whether to visit subdirectories too or not.
   * @param glob             the glob pattern that each file has to match, use {@code "*"} to match everything.
   * @throws NullPointerException if the given path, consumer or glob is null.
   */
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

  /**
   * Walks the file tree while filtering with the given filter and only visiting directories if specified. This starts
   * at the given root path and passes the root directory together with the next file or directory into the bi
   * consumer.
   *
   * @param root             the root path to start at.
   * @param consumer         the consumer accepting all files and directories.
   * @param visitDirectories whether to visit subdirectories too or not.
   * @param filter           the filter to filter against.
   * @throws NullPointerException if the given path, consumer or glob is null.
   */
  public static void walkFileTree(
    @NonNull Path root,
    @NonNull BiConsumer<Path, Path> consumer,
    boolean visitDirectories,
    @NonNull DirectoryStream.Filter<Path> filter
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
        LOGGER.error("Exception walking down directory tree starting at {}", root, exception);
      }
    }
  }

  /**
   * Creates all needed parent directories and the given directory only if the given path does not exist already. This
   * method is equivalent to {@code Files.createDirectories(directoryPath)} but catches thrown exceptions and just
   * prints them into the error log.
   *
   * @param directoryPath the directory to create.
   */
  public static void createDirectory(@Nullable Path directoryPath) {
    if (directoryPath != null && Files.notExists(directoryPath)) {
      try {
        Files.createDirectories(directoryPath);
      } catch (IOException exception) {
        LOGGER.error("Exception creating directory at {}", directoryPath, exception);
      }
    }
  }

  /**
   * Ensures that the given child path is a child path of the given root path and throwing an exception otherwise.
   *
   * @param root  the root path.
   * @param child the child path to check.
   * @throws IllegalStateException if the child is not an actual child of the root path.
   * @throws NullPointerException  if the given root or child path is null.
   */
  public static void ensureChild(@NonNull Path root, @NonNull Path child) {
    var rootNormal = root.normalize().toAbsolutePath();
    var childNormal = child.normalize().toAbsolutePath();

    if (childNormal.getNameCount() <= rootNormal.getNameCount() || !childNormal.startsWith(rootNormal)) {
      throw new IllegalStateException("Child " + childNormal + " is not in root path " + rootNormal);
    }
  }

  /**
   * Resolves the given array for the base path and returns the final path after all children are resolved one after
   * another.
   *
   * @param base the base path to start at.
   * @param more all children to resolve onto the base path.
   * @return the resolved path.
   * @throws NullPointerException if the base path or of the children is null.
   */
  public static @NonNull Path resolve(@NonNull Path base, String @NonNull ... more) {
    for (var child : more) {
      base = base.resolve(child);
    }
    return base;
  }
}
