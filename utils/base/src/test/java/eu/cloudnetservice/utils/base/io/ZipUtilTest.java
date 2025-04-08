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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public final class ZipUtilTest {

  private static final Path TEST_DIR = Path.of("build", "testDirectory");

  @BeforeAll
  static void setupTestDirectories() {
    FileUtil.createDirectory(TEST_DIR);
  }

  @AfterAll
  static void removeTestDirectories() {
    FileUtil.delete(TEST_DIR);
  }

  @Test
  void testZipUtils() throws Exception {
    var zipFilePath = TEST_DIR.resolve("test.zip").toAbsolutePath();

    try (
      var out = Files.newOutputStream(zipFilePath);
      var is = ZipUtilTest.class.getClassLoader().getResourceAsStream("empty_zip_file.zip")
    ) {
      FileUtil.copy(is, out);
    }

    FileUtil.openZipFile(zipFilePath, fileSystem -> {
      var zipEntryInfoFile = fileSystem.getPath("info.txt");

      try (
        var out = Files.newOutputStream(zipEntryInfoFile);
        var is = new ByteArrayInputStream("Info message :3".getBytes())
      ) {
        FileUtil.copy(is, out);
      }
    });

    try (var out = new ByteArrayOutputStream(); var zipFile = new ZipFile(zipFilePath.toFile())) {
      var zipEntry = zipFile.getEntry("info.txt");
      Assertions.assertNotNull(zipEntry);

      try (var inputStream = zipFile.getInputStream(zipEntry)) {
        FileUtil.copy(inputStream, out);
      }

      Assertions.assertEquals("Info message :3", out.toString(StandardCharsets.UTF_8));
    }

    FileUtil.delete(TEST_DIR);
    Assertions.assertFalse(Files.exists(TEST_DIR));
  }

  @Test
  void testExtractZip() throws Exception {
    var zipFilePath = TEST_DIR.resolve("test.zip");

    try (
      var outputStream = Files.newOutputStream(zipFilePath);
      var is = ZipUtilTest.class.getClassLoader().getResourceAsStream("file_utils_resources.zip")
    ) {
      FileUtil.copy(is, outputStream);
    }

    ZipUtil.extract(zipFilePath, TEST_DIR);

    Assertions.assertTrue(Files.exists(TEST_DIR));
    Assertions.assertTrue(Files.exists(TEST_DIR.resolve("bungee/config.yml")));
    Assertions.assertTrue(Files.exists(TEST_DIR.resolve("nms/bukkit.yml")));
    Assertions.assertTrue(Files.exists(TEST_DIR.resolve("nms/server.properties")));
  }
}
