package de.dytanic.cloudnet.common.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class FileUtilsTest {

    @Test
    public void testFileUtils() throws Exception {
        Path testDirectory = Paths.get("build", "testDirectory");
        FileUtils.createDirectoryReported(testDirectory);

        Path zip = testDirectory.resolve("test.zip");
        try (OutputStream outputStream = Files.newOutputStream(zip)) {
            outputStream.write(FileUtils.emptyZipByteArray());
        }

        Assert.assertEquals(FileUtils.emptyZipByteArray().length, Files.size(zip));

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Hello, world! Hello Peter!".getBytes(StandardCharsets.UTF_8))) {
            Assert.assertEquals("Hello, world! Hello Peter!", new String(FileUtils.toByteArray(byteArrayInputStream), StandardCharsets.UTF_8));
        }

        byte[] buffer = new byte[2048];
        FileUtils.openZipFileSystem(zip, fileSystem -> {
            Path zipEntryInfoFile = fileSystem.getPath("info.txt");

            try (OutputStream outputStream = Files.newOutputStream(zipEntryInfoFile);
                 ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Info message :3".getBytes())) {
                FileUtils.copy(byteArrayInputStream, outputStream, buffer);
            }

            return null;
        });

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipFile zipFile = new ZipFile(zip.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry("info.txt");
            Assert.assertNotNull(zipEntry);

            try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                FileUtils.copy(inputStream, out);
            }

            Assert.assertEquals("Info message :3", out.toString(StandardCharsets.UTF_8.name()));
        }

        FileUtils.delete(testDirectory);
        Assert.assertFalse(Files.exists(testDirectory));

        zip = Paths.get("build", "test.zip");
        try (OutputStream outputStream = Files.newOutputStream(zip);
             InputStream inputStream = FileUtilsTest.class.getClassLoader().getResourceAsStream("file_utils_resources.zip")) {
            FileUtils.copy(inputStream, outputStream, buffer);
        }

        FileUtils.extract(zip, testDirectory);

        Assert.assertTrue(Files.exists(testDirectory));
        Assert.assertTrue(Files.exists(testDirectory.resolve("bungee/config.yml")));
        Assert.assertTrue(Files.exists(testDirectory.resolve("nms/bukkit.yml")));
        Assert.assertTrue(Files.exists(testDirectory.resolve("nms/server.properties")));

        FileUtils.delete(testDirectory);
        Assert.assertTrue(Files.notExists(testDirectory));
    }
}