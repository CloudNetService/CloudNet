package de.dytanic.cloudnet.common.io;

import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.concurrent.IVoidThrowableCallback;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The FileUtils class has a lot of utility methods, for
 * <ol>
 * <li>Byte Streams IO</li>
 * <li>File IO (Coping, Deleting)</li>
 * <li>Zip IO</li>
 * </ol>
 */
public final class FileUtils {

  private FileUtils() {
    throw new UnsupportedOperationException();
  }

  private static final Map<String, String> zipFileSystemProperties = Maps
    .newHashMap();

  static {
    zipFileSystemProperties.put("create", "false");
    zipFileSystemProperties.put("encoding", "UTF-8");
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

    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  public static void openZipFileSystem(File file,
    IVoidThrowableCallback<FileSystem> consumer) {
    openZipFileSystem(file.toPath(), consumer);
  }

  public static void openZipFileSystem(Path path,
    IVoidThrowableCallback<FileSystem> consumer) {
    try (FileSystem fileSystem = FileSystems
      .newFileSystem(URI.create("jar:" + path.toUri()),
        zipFileSystemProperties)) {
      consumer.call(fileSystem);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static void copy(InputStream inputStream, OutputStream outputStream)
    throws IOException {
    copy(inputStream, outputStream, new byte[8192]);
  }

  public static void copy(InputStream inputStream, OutputStream outputStream,
    byte[] buffer) throws IOException {
    copy(inputStream, outputStream, buffer, null);
  }

  public static void copy(InputStream inputStream, OutputStream outputStream,
    byte[] buffer, Consumer<Integer> lengthInputListener) throws IOException {
    int len;

    while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
      if (lengthInputListener != null) {
        lengthInputListener.accept(len);
      }

      outputStream.write(buffer, 0, len);
      outputStream.flush();
    }
  }

  public static void copy(File from, File to) throws IOException {
    copy(from.toPath(), to.toPath());
  }

  public static void copy(File from, File to, byte[] buffer)
    throws IOException {
    copy(from.toPath(), to.toPath(), buffer);
  }

  public static void copy(Path from, Path to) throws IOException {
    copy(from, to, new byte[8192]);
  }

  public static void copy(Path from, Path to, byte[] buffer)
    throws IOException {
    if (from == null || to == null || !Files.exists(from)) {
      return;
    }

    if (!Files.exists(to)) {
      to.toFile().getParentFile().mkdirs();
      to.toFile().delete();

      Files.createFile(to);
    }

    try (InputStream inputStream = Files
      .newInputStream(from); OutputStream outputStream = Files
      .newOutputStream(to)) {
      copy(inputStream, outputStream, buffer);
    }
  }

  public static void copyFilesToDirectory(File from, File to)
    throws IOException {
    copyFilesToDirectory(from, to, new byte[16384]);
  }

  public static void copyFilesToDirectory(File from, File to, byte[] buffer)
    throws IOException {
    if (to == null || from == null || !from.exists()) {
      return;
    }

    if (from.isDirectory()) {
      to.mkdirs();

      File[] list = from.listFiles();

      if (list != null && list.length > 0) {
        for (File file : list) {
          if (file == null) {
            continue;
          }

          if (file.isDirectory()) {
            copyFilesToDirectory(file,
              new File(to.getAbsolutePath() + "/" + file.getName()));
          } else {
            copy(file.toPath(),
              Paths.get(to.getAbsolutePath() + "/" + file.getName()), buffer);
          }
        }
      }
    } else {
      copy(from.toPath(),
        Paths.get(to.getAbsolutePath() + "/" + from.getName()), buffer);
    }
  }

  public static void delete(File file) {
    if (file == null || !file.exists()) {
      return;
    }

    if (file.isDirectory()) {
      File[] files = file.listFiles();

      if (files != null) {
        for (File entry : files) {
          if (entry.isDirectory()) {
            delete(entry);
          } else {
            entry.delete();
          }
        }
      }
    }

    file.delete();
  }

  public static Path convert(Path zipPath, Path... directories)
    throws IOException {
    if (directories == null) {
      return null;
    }

    if (!Files.exists(zipPath)) {
      Files.createFile(zipPath);
    }

    try (OutputStream outputStream = Files.newOutputStream(zipPath);
      ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream,
        StandardCharsets.UTF_8)) {
      for (Path dir : directories) {
        if (Files.exists(dir)) {
          convert0(zipOutputStream, dir);
        }
      }
    }
    return zipPath;
  }

  public static byte[] convert(Path... directories) //TODO
  {
    if (directories == null) {
      return emptyZipByteArray();
    }

    try (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
      try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteBuffer,
        StandardCharsets.UTF_8)) {
        for (Path dir : directories) {
          if (Files.exists(dir)) {
            if (Files.isDirectory(dir)) {
              convert0(zipOutputStream, dir);
            } else {
              zipOutputStream
                .putNextEntry(new ZipEntry(dir.toFile().getName()));
              try (InputStream inputStream = Files.newInputStream(dir)) {
                copy(inputStream, zipOutputStream);
              }
              zipOutputStream.closeEntry();
            }
          }
        }
      }

      return byteBuffer.toByteArray();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return emptyZipByteArray();
  }

  private static void convert0(ZipOutputStream zipOutputStream, Path directory)
    throws IOException {
    Files.walkFileTree(
      directory,
      EnumSet.noneOf(FileVisitOption.class),
      Integer.MAX_VALUE,
      new SimpleFileVisitor<Path>() {
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
          throws IOException {
          try (InputStream inputStream = Files.newInputStream(file)) {
            zipOutputStream.putNextEntry(
              new ZipEntry(directory.relativize(file).toString()));
            copy(inputStream, zipOutputStream);
            zipOutputStream.closeEntry();
          } catch (Exception ex) {
            zipOutputStream.closeEntry();
          }
          return FileVisitResult.CONTINUE;
        }
      });
  }

  public static Path extract(Path zipPath, Path targetDirectory)
    throws IOException {
    if (zipPath == null || targetDirectory == null || !Files.exists(zipPath)) {
      return targetDirectory;
    }

    try (InputStream inputStream = Files.newInputStream(zipPath)) {
      extract0(inputStream, targetDirectory);
    }

    return targetDirectory;
  }

  public static Path extract(byte[] zipData, Path targetDirectory)
    throws IOException {
    if (zipData == null || zipData.length == 0 || targetDirectory == null) {
      return targetDirectory;
    }

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
      zipData)) {
      extract0(byteArrayInputStream, targetDirectory);
    }

    return targetDirectory;
  }

  public static void extract0(InputStream inputStream, Path targetDirectory)
    throws IOException {
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream,
      StandardCharsets.UTF_8)) {
      ZipEntry zipEntry = null;

      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        extract1(zipInputStream, zipEntry, targetDirectory);
        zipInputStream.closeEntry();
      }
    }
  }

  public static byte[] emptyZipByteArray() {
    byte[] bytes = null;

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      ZipOutputStream zipOutputStream = new ZipOutputStream(
        byteArrayOutputStream, StandardCharsets.UTF_8);
      zipOutputStream.close();

      bytes = byteArrayOutputStream.toByteArray();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return bytes;
  }

  private static void extract1(ZipInputStream zipInputStream, ZipEntry zipEntry,
    Path targetDirectory) throws IOException {
    Path file = Paths.get(targetDirectory.toString(), zipEntry.getName());

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

  public static void workFileTree(Path path, Consumer<File> consumer) {
    workFileTree(path.toFile(), consumer);
  }

  public static void workFileTree(File file, Consumer<File> consumer) {
    if (!file.exists()) {
      return;
    }

    consumer.accept(file);

    if (file.isDirectory()) {
      File[] files = file.listFiles();

      if (files != null && files.length > 0) {
        for (File entry : files) {
          workFileTree(entry, consumer);
        }
      }
    }
  }
}