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

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
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
import org.jetbrains.annotations.Nullable;

public final class ZipUtil {

  private static final Logger LOGGER = LogManager.logger(ZipUtil.class);
  private static final boolean IS_WINDOWS = System.getProperty("os.name").contains("windows");

  public static @NonNull InputStream zipToStream(@NonNull Path directory) {
    return zipToStream(directory, null);
  }

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

  public static @Nullable Path zipToFile(@NonNull Path directory, @NonNull Path target) {
    return zipToFile(directory, target, null);
  }

  public static @Nullable Path zipToFile(@NonNull Path dir, @NonNull Path target, @Nullable Predicate<Path> filter) {
    if (Files.exists(dir)) {
      try (var out = new ZipOutputStream(Files.newOutputStream(target), StandardCharsets.UTF_8)) {
        zipDir(out, dir, filter);
        return target;
      } catch (IOException exception) {
        LOGGER.fine("Exception while processing new zip entry from directory " + dir, exception);
      }
    }

    return null;
  }

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

  public static @Nullable Path extract(@NonNull Path zipPath, @NonNull Path targetDirectory) {
    if (Files.exists(zipPath)) {
      try (var inputStream = Files.newInputStream(zipPath)) {
        return extract(inputStream, targetDirectory);
      } catch (IOException exception) {
        LOGGER.fine("Unable to extract zip from " + zipPath + " to " + targetDirectory, exception);
      }
    }
    return null;
  }

  public static @Nullable Path extract(@NonNull InputStream in, @NonNull Path targetDirectory) {
    return extractZipStream(
      in instanceof ZipInputStream ? (ZipInputStream) in : new ZipInputStream(in, StandardCharsets.UTF_8),
      targetDirectory);
  }

  public static @Nullable Path extractZipStream(@NonNull ZipInputStream zipInputStream, @NonNull Path targetDirectory) {
    try {
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        extractEntry(zipInputStream, zipEntry, targetDirectory);
        zipInputStream.closeEntry();
      }

      return targetDirectory;
    } catch (IOException exception) {
      LOGGER.fine("Exception unzipping zip file to " + targetDirectory, exception);
      return null;
    }
  }

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
