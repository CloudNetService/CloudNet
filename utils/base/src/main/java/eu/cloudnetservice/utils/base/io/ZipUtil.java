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

import eu.cloudnetservice.utils.base.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This zip utility class makes the process of zipping files and directories as well as extracting from zips easier and
 * prevents malicious zip names.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class ZipUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipUtil.class);
  private static final boolean IS_WINDOWS = StringUtil.toLower(System.getProperty("os.name")).contains("windows");

  private ZipUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Zips the given directory into an input stream without filtering any files and returns it. This method is equivalent
   * to {@code ZipUtil.zipToStream(directory, null)}.
   *
   * @param directory the directory to zip.
   * @return the new input stream for the zip.
   * @throws NullPointerException  if the given directory is null.
   * @throws IllegalStateException if the opening of the zip file failed.
   */
  public static @NonNull InputStream zipToStream(@NonNull Path directory) {
    return zipToStream(directory, null);
  }

  /**
   * Zips the given directory into a zip input stream while filtering with the given filter and returning the new input
   * stream.
   *
   * @param directory  the directory to zip.
   * @param fileFilter the filter to filter against.
   * @return the new input stream for the zip.
   * @throws NullPointerException  if the given directory is null.
   * @throws IllegalStateException if the opening of the zip file failed.
   */
  public static @NonNull InputStream zipToStream(@NonNull Path directory, @Nullable Predicate<Path> fileFilter) {
    var target = FileUtil.createTempFile();
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

  /**
   * Walks the file tree of the given directory and copies all files and directories without any filtering into a new
   * zip output stream at the given target destination. This method is equivalent to
   * {@code ZipUtil.zipToFile(dir, target, null)}.
   *
   * @param dir    the directory to zip.
   * @param target the destination of the zip file.
   * @return the target destination on success, null if the input directory does not exist.
   */
  public static @Nullable Path zipToFile(@NonNull Path dir, @NonNull Path target) {
    return zipToFile(dir, target, null);
  }

  /**
   * Walks the file tree of the given directory and copies all files and directories that match the filter into a new
   * zip output stream at the given target destination.
   *
   * @param dir    the directory to zip.
   * @param target the destination of the zip file.
   * @param filter the filter to filter against.
   * @return the target destination on success, null if the input directory does not exist.
   */
  public static @Nullable Path zipToFile(@NonNull Path dir, @NonNull Path target, @Nullable Predicate<Path> filter) {
    if (Files.exists(dir)) {
      try (var out = new ZipOutputStream(Files.newOutputStream(target), StandardCharsets.UTF_8)) {
        zipDir(out, dir, filter);
        return target;
      } catch (IOException exception) {
        LOGGER.debug("Exception while processing new zip entry from directory {}", dir, exception);
      }
    }

    return null;
  }

  /**
   * Walks the file tree of the given directory and copies all files and directories that match the filter into the zip
   * output stream.
   *
   * @param out    the stream to copy the individual zip entries to.
   * @param dir    the directory to zip.
   * @param filter the filter to filter against.
   * @throws IOException          if the writing process of the new zip entry fails.
   * @throws NullPointerException if the zip output stream or the directory is null.
   */
  private static void zipDir(
    @NonNull ZipOutputStream out,
    @NonNull Path dir,
    @Nullable Predicate<Path> filter
  ) throws IOException {
    Files.walkFileTree(
      dir,
      new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
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

  /**
   * Extracts all entries from the zip file at the given zip path to the given target directory while catching all
   * occurring exceptions and redirecting them into the debug log.
   *
   * @param zipPath         the path to the zip file.
   * @param targetDirectory the destination to extract to.
   * @return the given target directory, null if the zip path does not exist or the extraction failed.
   * @throws NullPointerException if the given zip or directory path is null.
   */
  public static @Nullable Path extract(@NonNull Path zipPath, @NonNull Path targetDirectory) {
    if (Files.exists(zipPath)) {
      try (var inputStream = Files.newInputStream(zipPath)) {
        return extract(inputStream, targetDirectory);
      } catch (IOException exception) {
        LOGGER.debug("Unable to extract zip from {} to {}", zipPath, targetDirectory, exception);
      }
    }
    return null;
  }

  /**
   * Extracts all entries from the given input stream to the given target directory while catching all occurring
   * exceptions and redirecting them into the debug log.
   * <p>
   * Note: If the given input stream is not a zip input stream, the stream is wrapped into one.
   *
   * @param in              the input stream to extract from.
   * @param targetDirectory the destination to extract to.
   * @return the given target directory on success, null if the extraction failed.
   * @throws NullPointerException if the given input stream or directory is null.
   */
  public static @Nullable Path extract(@NonNull InputStream in, @NonNull Path targetDirectory) {
    return extractZipStream(
      in instanceof ZipInputStream zipInputStream ? zipInputStream : new ZipInputStream(in, StandardCharsets.UTF_8),
      targetDirectory);
  }

  /**
   * Extracts all entries from the zip stream to the given target directory while catching all occurring exceptions and
   * redirecting them into the debug log.
   *
   * @param zipInputStream  the input stream to extract from.
   * @param targetDirectory the destination to extract to.
   * @return the given target directory on success, null if the extraction failed.
   * @throws NullPointerException if the given zip input stream or directory is null.
   */
  public static @Nullable Path extractZipStream(@NonNull ZipInputStream zipInputStream, @NonNull Path targetDirectory) {
    try {
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        extractEntry(zipInputStream, zipEntry, targetDirectory);
        zipInputStream.closeEntry();
      }

      return targetDirectory;
    } catch (IOException exception) {
      LOGGER.debug("Exception unzipping zip file to {}", targetDirectory, exception);
      return null;
    }
  }

  /**
   * Extracts the zip entry from the given zip input stream and copies it to the given destination.
   * <p>
   * Note: If the zip entry is a directory the content of the directory is not extracted.
   *
   * @param in              the zip input stream to extract the entry from.
   * @param zipEntry        the entry to extract from the zip stream.
   * @param targetDirectory the target destination of the extracted entry.
   * @throws IOException           if the creation of the output stream for the entry fails.
   * @throws IllegalStateException if the zip entry has a malicious name.
   * @throws NullPointerException  if the given input stream, zip entry or target directory is null.
   */
  private static void extractEntry(
    @NonNull ZipInputStream in,
    @NonNull ZipEntry zipEntry,
    @NonNull Path targetDirectory
  ) throws IOException {
    // checks first if the zip entry name is malicious before extracting
    ensureSafeZipEntryName(zipEntry.getName());
    var file = targetDirectory.resolve(zipEntry.getName());

    if (zipEntry.isDirectory()) {
      FileUtil.createDirectory(file);
    } else {
      FileUtil.createDirectory(file.getParent());
      try (var outputStream = Files.newOutputStream(file)) {
        FileUtil.copy(in, outputStream);
      }
    }
  }

  /**
   * Ensures that the given name does not contain any characters that might lead to path traversal or other malicious
   * behavior.
   *
   * @param name the name to check.
   * @throws NullPointerException  if the given name is null.
   * @throws IllegalStateException if the name contains an illegal character.
   */
  private static void ensureSafeZipEntryName(@NonNull String name) {
    if (name.isEmpty()
      || name.startsWith("/")
      || name.startsWith("\\")
      || name.contains("..")
      || (name.contains(":") && IS_WINDOWS)) {
      throw new IllegalStateException(String.format("zip entry name %s contains unsafe characters", name));
    }
  }
}
