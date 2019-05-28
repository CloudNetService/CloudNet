package de.dytanic.cloudnet.common.io;

import de.dytanic.cloudnet.common.concurrent.IVoidThrowableCallback;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.Assert;
import org.junit.Test;

public final class FileUtilsTest {

  @Test
  public void testFileUtils() throws Exception {
    File testDirectory = new File("build/testDirectory");
    testDirectory.mkdirs();

    byte[] buffer = new byte[2048];

    File zip = new File(testDirectory, "test.zip");
    zip.createNewFile();

    try (OutputStream outputStream = new FileOutputStream(zip)) {
      outputStream.write(FileUtils.emptyZipByteArray());
    }

    Assert.assertEquals(FileUtils.emptyZipByteArray().length, zip.length());

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        "Hello, world! Hello Peter!".getBytes(StandardCharsets.UTF_8))) {
      Assert.assertEquals("Hello, world! Hello Peter!",
          new String(FileUtils.toByteArray(byteArrayInputStream),
              StandardCharsets.UTF_8));
    }

    FileUtils.openZipFileSystem(zip, new IVoidThrowableCallback<FileSystem>() {
      @Override
      public Void call(FileSystem fileSystem) throws Throwable {
        Path zipEntryInfoFile = fileSystem.getPath("info.txt");

        try (OutputStream outputStream = Files
            .newOutputStream(zipEntryInfoFile);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                "Info message :3".getBytes())) {
          FileUtils.copy(byteArrayInputStream, outputStream, buffer);
        }

        return null;
      }
    });

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipFile zipFile = new ZipFile(zip)) {
      ZipEntry zipEntry = zipFile.getEntry("info.txt");
      Assert.assertNotNull(zipEntry);

      try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
        FileUtils.copy(inputStream, byteArrayOutputStream);
      }

      Assert.assertEquals("Info message :3",
          new String(byteArrayOutputStream.toByteArray(),
              StandardCharsets.UTF_8));
    }

    FileUtils.delete(testDirectory);
    Assert.assertFalse(testDirectory.exists());

    zip = new File("build/test.zip");
    zip.createNewFile();

    try (OutputStream outputStream = Files.newOutputStream(zip.toPath());
        InputStream inputStream = FileUtilsTest.class.getClassLoader()
            .getResourceAsStream("file_utils_resources.zip")) {
      FileUtils.copy(inputStream, outputStream, buffer);
    }

    Path path = Paths.get("build/testDirectory");

    FileUtils.extract(zip.toPath(), path);

    Assert.assertTrue(Files.exists(path));
    Assert.assertTrue(new File(path.toFile(), "bungee/config.yml").exists());
    Assert.assertTrue(new File(path.toFile(), "nms/bukkit.yml").exists());
    Assert
        .assertTrue(new File(path.toFile(), "nms/server.properties").exists());

    FileUtils.delete(path.toFile());
    Assert.assertFalse(Files.exists(path));
  }
}