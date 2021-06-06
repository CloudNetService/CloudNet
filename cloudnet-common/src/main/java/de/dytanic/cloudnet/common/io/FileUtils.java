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
import de.dytanic.cloudnet.common.concurrent.IVoidThrowableCallback;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.ApiStatus;
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
@ApiStatus.Internal
public final class FileUtils {

  public static final InputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);
  private static final Map<String, String> ZIP_FILE_SYSTEM_PROPERTIES = ImmutableMap
    .of("create", "false", "encoding", "UTF-8");

  private FileUtils() {
    throw new UnsupportedOperationException();
  }

  public static byte[] toByteArray(InputStream inputStream) {
    return toByteArray(inputStream, new byte[8192]);
  }

  public static byte[] toByteArray(InputStream inputStream, byte[] buffer) {
    if (inputStream == null) {
      return null;
    }

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      copy(inputStream, byteArrayOutputStream, buffer);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    return null;
  }

  public static void openZipFileSystem(Path path, IVoidThrowableCallback<FileSystem> consumer) {
    try (FileSystem fileSystem = FileSystems
      .newFileSystem(URI.create("jar:" + path.toUri()), ZIP_FILE_SYSTEM_PROPERTIES)) {
      consumer.call(fileSystem);
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
  }

  public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
    copy(inputStream, outputStream, new byte[8192]);
  }

  public static void copy(InputStream inputStream, OutputStream outputStream, byte[] buffer) throws IOException {
    copy(inputStream, outputStream, buffer, null);
  }

  public static void copy(InputStream inputStream, OutputStream outputStream, byte[] buffer,
    Consumer<Integer> lengthInputListener) throws IOException {
    int len;
    while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
      if (lengthInputListener != null) {
        lengthInputListener.accept(len);
      }

      outputStream.write(buffer, 0, len);
      outputStream.flush();
    }
  }

  public static void copy(Path from, Path to) throws IOException {
    copy(from, to, new byte[8192]);
  }

  public static void copy(Path from, Path to, byte[] buffer) throws IOException {
    if (from == null || to == null || !Files.exists(from)) {
      return;
    }

    if (Files.notExists(to)) {
      createDirectoryReported(to.getParent());
    }

    try (InputStream stream = Files.newInputStream(from); OutputStream target = Files.newOutputStream(to)) {
      copy(stream, target, buffer);
    }
  }

  public static void copyFilesToDirectory(Path from, Path to) {
    walkFileTree(from, (root, current) -> {
      if (!Files.isDirectory(current)) {
        try {
          FileUtils.copy(current, to.resolve(from.relativize(current)));
        } catch (IOException exception) {
          exception.printStackTrace();
        }
      }
    });
  }

  public static void copyFilesToDirectory(Path from, Path to, DirectoryStream.Filter<Path> filter) {
    if (filter == null) {
      copyFilesToDirectory(from, to);
    } else {
      walkFileTree(from, (root, current) -> {
        if (!Files.isDirectory(current)) {
          try {
            FileUtils.copy(current, to.resolve(from.relativize(current)));
          } catch (IOException exception) {
            exception.printStackTrace();
          }
        }
      }, true, filter);
    }
  }

  public static void delete(Path file) {
    if (file == null || Files.notExists(file)) {
      return;
    }

    if (Files.isDirectory(file)) {
      walkFileTree(file, (root, current) -> FileUtils.deleteFileReported(current));
    }

    FileUtils.deleteFileReported(file);
  }

  /**
   * Converts a bunch of directories to a byte array
   *
   * @param directories The directories which should get converted
   * @return A byte array of a zip file created from the provided directories
   * @deprecated May cause a heap space (over)load
   */
  @Deprecated
  public static byte[] convert(Path... directories) {
    if (directories == null) {
      return emptyZipByteArray();
    }

    try (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
      try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteBuffer,
        StandardCharsets.UTF_8)) {
        for (Path dir : directories) {
          zipDir(zipOutputStream, dir, null);
        }
      }

      return byteBuffer.toByteArray();

    } catch (IOException exception) {
      exception.printStackTrace();
    }

    return emptyZipByteArray();
  }

  public static Path createTempFile() {
    Path tempDir = Paths.get(System.getProperty("cloudnet.tempDir", "temp"));
    if (Files.notExists(tempDir)) {
      try {
        Files.createDirectories(tempDir);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    return tempDir.resolve(UUID.randomUUID().toString());
  }

  @NotNull
  public static InputStream zipToStream(@NotNull Path directory) throws IOException {
    return zipToStream(directory, null);
  }

  @NotNull
  public static InputStream zipToStream(@NotNull Path directory, Predicate<Path> fileFilter) throws IOException {
    Path target = createTempFile();
    zipToFile(directory, target, path -> !target.equals(path) && (fileFilter == null || fileFilter.test(path)));
    return Files.newInputStream(target, StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);
  }

  @Nullable
  public static Path zipToFile(Path directory, Path target) {
    return zipToFile(directory, target, null);
  }

  @Nullable
  public static Path zipToFile(Path directory, Path target, Predicate<Path> fileFilter) {
    if (directory == null || !Files.exists(directory)) {
      return null;
    }

    delete(target);
    try (OutputStream outputStream = Files.newOutputStream(target, StandardOpenOption.CREATE)) {
      zipStream(directory, outputStream, fileFilter);
      return target;
    } catch (final IOException exception) {
      exception.printStackTrace();
    }

    return null;
  }

  private static void zipStream(Path source, OutputStream buffer, Predicate<Path> fileFilter) throws IOException {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
      if (Files.exists(source)) {
        zipDir(zipOutputStream, source, fileFilter);
      }
    }
  }

  private static void zipDir(ZipOutputStream zipOutputStream, Path directory, Predicate<Path> fileFilter)
    throws IOException {
    Files.walkFileTree(
      directory,
      new SimpleFileVisitor<Path>() {
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (fileFilter != null && !fileFilter.test(file)) {
            return FileVisitResult.CONTINUE;
          }

          try {
            zipOutputStream.putNextEntry(new ZipEntry(directory.relativize(file).toString().replace("\\", "/")));
            Files.copy(file, zipOutputStream);
            zipOutputStream.closeEntry();
          } catch (IOException ex) {
            zipOutputStream.closeEntry();
            throw ex;
          }
          return FileVisitResult.CONTINUE;
        }
      }
    );
  }

  public static Path extract(Path zipPath, Path targetDirectory) throws IOException {
    if (zipPath == null || targetDirectory == null || !Files.exists(zipPath)) {
      return targetDirectory;
    }

    try (InputStream inputStream = Files.newInputStream(zipPath)) {
      return extract(inputStream, targetDirectory);
    }
  }

  public static Path extract(InputStream inputStream, Path targetDirectory) throws IOException {
    if (inputStream == null || targetDirectory == null) {
      return targetDirectory;
    }

    extract0(new ZipInputStream(inputStream, StandardCharsets.UTF_8), targetDirectory);

    return targetDirectory;
  }

  public static Path extract(byte[] zipData, Path targetDirectory) throws IOException {
    if (zipData == null || zipData.length == 0 || targetDirectory == null) {
      return targetDirectory;
    }

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipData)) {
      extract0(new ZipInputStream(byteArrayInputStream, StandardCharsets.UTF_8), targetDirectory);
    }

    return targetDirectory;
  }

  public static void extract0(ZipInputStream zipInputStream, Path targetDirectory) throws IOException {
    ZipEntry zipEntry;
    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
      extractEntry(zipInputStream, zipEntry, targetDirectory);
      zipInputStream.closeEntry();
    }
  }

  public static byte[] emptyZipByteArray() {
    byte[] bytes = null;

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8);
      zipOutputStream.close();

      bytes = byteArrayOutputStream.toByteArray();
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    return bytes;
  }

  private static void extractEntry(ZipInputStream zipInputStream, ZipEntry zipEntry, Path targetDirectory)
    throws IOException {
    Path file = targetDirectory.resolve(zipEntry.getName());
    ensureChild(targetDirectory, file);

    if (zipEntry.isDirectory()) {
      if (!Files.exists(file)) {
        Files.createDirectories(file);
      }
    } else {
      Path parent = file.getParent();
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }

      if (Files.exists(file)) {
        Files.delete(file);
      }

      Files.createFile(file);
      try (OutputStream outputStream = Files.newOutputStream(file)) {
        copy(zipInputStream, outputStream);
      }
    }
  }

  public static void walkFileTree(Path rootDirectoryPath, BiConsumer<Path, Path> consumer) {
    walkFileTree(rootDirectoryPath, consumer, true);
  }

  public static void walkFileTree(Path rootDirectoryPath, BiConsumer<Path, Path> consumer, boolean visitDirectories) {
    walkFileTree(rootDirectoryPath, consumer, visitDirectories, "*");
  }

  public static void walkFileTree(Path rootDirectoryPath, BiConsumer<Path, Path> consumer, boolean visitDirectories,
    String glob) {
    if (Files.notExists(rootDirectoryPath)) {
      return;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootDirectoryPath, glob)) {
      for (Path path : stream) {
        if (Files.isDirectory(path) && visitDirectories) {
          walkFileTree(path, consumer, true, glob);
        }
        consumer.accept(rootDirectoryPath, path);
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  public static void walkFileTree(Path rootDirectoryPath, BiConsumer<Path, Path> consumer, boolean visitDirectories,
    DirectoryStream.Filter<Path> filter) {
    if (Files.notExists(rootDirectoryPath)) {
      return;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootDirectoryPath, filter)) {
      for (Path path : stream) {
        if (Files.isDirectory(path) && visitDirectories) {
          walkFileTree(path, consumer, true, filter);
        }
        consumer.accept(rootDirectoryPath, path);
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  public static void createDirectoryReported(@Nullable Path directoryPath) {
    if (directoryPath != null && Files.notExists(directoryPath)) {
      try {
        Files.createDirectories(directoryPath);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
  }

  public static void deleteFileReported(Path file) {
    try {
      Files.deleteIfExists(file);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  private static void ensureChild(Path root, Path child) {
    Path rootNormal = root.normalize().toAbsolutePath();
    Path childNormal = child.normalize().toAbsolutePath();

    if (childNormal.getNameCount() <= rootNormal.getNameCount() || !childNormal.startsWith(rootNormal)) {
      throw new IllegalStateException("Child " + childNormal + " is not in root path " + rootNormal);
    }
  }
}
