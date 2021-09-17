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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public final class FileUtilsTest {

  private static final Path TEST_DIR = Paths.get("build", "testDirectory");

  @BeforeAll
  static void setupTestDirectories() {
    FileUtils.createDirectoryReported(TEST_DIR);
  }

  @AfterAll
  static void removeTestDirectories() {
    FileUtils.delete(TEST_DIR);
  }

  @Test
  void testByteArrayUtils() throws Exception {
    try (ByteArrayInputStream is = new ByteArrayInputStream("Hello, world!".getBytes(StandardCharsets.UTF_8))) {
      Assertions.assertArrayEquals(
        "Hello, world!".getBytes(StandardCharsets.UTF_8),
        FileUtils.toByteArray(is));
    }
  }

  @Test
  void testZipUtils() throws Exception {
    Path zipFilePath = TEST_DIR.resolve("test.zip");
    Files.write(zipFilePath, FileUtils.emptyZipByteArray());

    FileUtils.openZipFileSystem(zipFilePath, fileSystem -> {
      Path zipEntryInfoFile = fileSystem.getPath("info.txt");

      try (
        OutputStream out = Files.newOutputStream(zipEntryInfoFile);
        ByteArrayInputStream is = new ByteArrayInputStream("Info message :3".getBytes())
      ) {
        FileUtils.copy(is, out);
      }

      return null;
    });

    try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
      ZipEntry zipEntry = zipFile.getEntry("info.txt");
      Assertions.assertNotNull(zipEntry);

      try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
        FileUtils.copy(inputStream, out);
      }

      Assertions.assertEquals("Info message :3", out.toString(StandardCharsets.UTF_8.name()));
    }

    FileUtils.delete(TEST_DIR);
    Assertions.assertFalse(Files.exists(TEST_DIR));
  }

  @Test
  void testExtractZip() throws Exception {
    Path zipFilePath = TEST_DIR.resolve("test.zip");

    try (
      OutputStream outputStream = Files.newOutputStream(zipFilePath);
      InputStream is = FileUtilsTest.class.getClassLoader().getResourceAsStream("file_utils_resources.zip")
    ) {
      FileUtils.copy(is, outputStream);
    }

    FileUtils.extract(zipFilePath, TEST_DIR);

    Assertions.assertTrue(Files.exists(TEST_DIR));
    Assertions.assertTrue(Files.exists(TEST_DIR.resolve("bungee/config.yml")));
    Assertions.assertTrue(Files.exists(TEST_DIR.resolve("nms/bukkit.yml")));
    Assertions.assertTrue(Files.exists(TEST_DIR.resolve("nms/server.properties")));
  }
}
