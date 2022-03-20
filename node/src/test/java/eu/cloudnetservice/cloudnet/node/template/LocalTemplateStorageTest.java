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

package eu.cloudnetservice.cloudnet.node.template;

import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.driver.template.FileInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
class LocalTemplateStorageTest {

  private static final Path HOME_PATH = Path.of("build", "tmp", "local_ts");
  private static final ServiceTemplate TEMPLATE = ServiceTemplate.builder()
    .prefix("global")
    .name("proxy")
    .storage("local")
    .build();

  private static LocalTemplateStorage storage;

  @BeforeAll
  static void setupStorage() {
    storage = new LocalTemplateStorage(HOME_PATH);
  }

  @AfterAll
  static void closeStorage() {
    storage.close();
    FileUtil.delete(HOME_PATH);
  }

  @Test
  @Order(0)
  void testTemplateCreation() throws IOException {
    Assertions.assertTrue(storage.create(TEMPLATE));
    Assertions.assertTrue(storage.createFile(TEMPLATE, "spigot.yml"));
    Assertions.assertTrue(storage.hasFile(TEMPLATE, "spigot.yml"));
  }

  @Test
  @Order(10)
  void testHasTemplate() {
    Assertions.assertTrue(storage.has(TEMPLATE));
    Assertions.assertFalse(storage.has(ServiceTemplate.builder()
      .prefix("hello")
      .name("world")
      .storage("sftp")
      .build()));
  }

  @Test
  @Order(20)
  void testNewOutputStream() throws IOException {
    try (var stream = storage.newOutputStream(TEMPLATE, "test.txt")) {
      Assertions.assertNotNull(stream);
      stream.write("Hello".getBytes(StandardCharsets.UTF_8));
    }

    Assertions.assertTrue(storage.hasFile(TEMPLATE, "test.txt"));
    try (var stream = storage.newInputStream(TEMPLATE, "test.txt")) {
      Assertions.assertNotNull(stream);
      Assertions.assertEquals("Hello", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  @Test
  @Order(30)
  void testAppendOutputStream() throws IOException {
    try (var stream = storage.appendOutputStream(TEMPLATE, "test.txt")) {
      Assertions.assertNotNull(stream);
      stream.write("World".getBytes(StandardCharsets.UTF_8));
    }

    Assertions.assertTrue(storage.hasFile(TEMPLATE, "test.txt"));
    try (var stream = storage.newInputStream(TEMPLATE, "test.txt")) {
      Assertions.assertNotNull(stream);
      Assertions.assertEquals("HelloWorld", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  @Test
  @Order(40)
  void testNewInputStream() throws IOException {
    try (var stream = storage.newInputStream(TEMPLATE, "test.txt")) {
      Assertions.assertNotNull(stream);
      Assertions.assertArrayEquals(stream.readAllBytes(), "HelloWorld".getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test
  @Order(50)
  void testFileGetFileInfo() throws IOException {
    var info = storage.fileInfo(TEMPLATE, "spigot.yml");
    Assertions.assertNotNull(info);
    Assertions.assertEquals(0, info.size());
    Assertions.assertEquals("spigot.yml", info.path());
    Assertions.assertEquals("spigot.yml", info.name());
  }

  @Test
  @Order(60)
  void testDeleteFile() throws IOException {
    Assertions.assertTrue(storage.deleteFile(TEMPLATE, "spigot.yml"));
    Assertions.assertFalse(storage.hasFile(TEMPLATE, "spigot.yml"));
  }

  @Test
  @Order(70)
  void testCreateDirectory() throws IOException {
    Assertions.assertTrue(storage.createDirectory(TEMPLATE, "hello"));
    Assertions.assertTrue(storage.createFile(TEMPLATE, "hello/test.txt"));
    Assertions.assertTrue(storage.hasFile(TEMPLATE, "hello/test.txt"));
  }

  @Test
  @Order(80)
  void testFileListingNonDeep() {
    var files = storage.listFiles(TEMPLATE, "hello", false);
    Assertions.assertNotNull(files);
    Assertions.assertEquals(1, files.length);
    Assertions.assertEquals(0, files[0].size());
    Assertions.assertEquals("test.txt", files[0].name());
    Assertions.assertTrue(files[0].path().endsWith("hello/test.txt"));
  }

  @Test
  @Order(90)
  void testFileListingDeep() {
    var files = storage.listFiles(TEMPLATE, "", true);
    Assertions.assertNotNull(files);
    Assertions.assertEquals(3, files.length);

    // there must be one directory
    var dir = Arrays.stream(files).filter(FileInfo::directory).findFirst().orElse(null);
    Assertions.assertNotNull(dir);
    Assertions.assertEquals("hello", dir.name());
  }

  @Test
  @Order(100)
  void testTemplateListing() {
    var templates = storage.templates();
    Assertions.assertEquals(1, templates.size());
    Assertions.assertEquals(TEMPLATE, templates.iterator().next());
  }

  @Test
  @Order(110)
  void testTemplateDelete() {
    Assertions.assertTrue(storage.delete(TEMPLATE));
    Assertions.assertFalse(storage.has(TEMPLATE));
    Assertions.assertFalse(storage.hasFile(TEMPLATE, "test.txt"));
  }
}
