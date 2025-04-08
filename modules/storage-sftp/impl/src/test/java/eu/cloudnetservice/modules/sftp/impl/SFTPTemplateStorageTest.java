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

package eu.cloudnetservice.modules.sftp.impl;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.FileInfo;
import eu.cloudnetservice.modules.sftp.config.SFTPTemplateStorageConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class SFTPTemplateStorageTest {

  private static final ServiceTemplate TEMPLATE = ServiceTemplate.builder()
    .prefix("global")
    .name("proxy")
    .storage("sftp")
    .build();

  @Container
  private static final GenericContainer<?> SFTP = new GenericContainer<>("atmoz/sftp:latest")
    .withExposedPorts(22)
    .withCommand("cloud:secret:::templates");

  private static SFTPTemplateStorage storage;

  @BeforeAll
  static void setupStorage() {
    storage = new SFTPTemplateStorage(new SFTPTemplateStorageConfig(
      new HostAndPort(SFTP.getHost(), SFTP.getFirstMappedPort()),
      "sftp",
      "cloud",
      "secret",
      null,
      null,
      null,
      "templates",
      1));
  }

  @AfterAll
  static void closeStorage() throws Exception {
    storage.close();
  }

  @Test
  @Order(0)
  void testTemplateCreation() {
    Assertions.assertTrue(storage.create(TEMPLATE));
    Assertions.assertTrue(storage.createFile(TEMPLATE, "spigot.yml"));
    Assertions.assertTrue(storage.hasFile(TEMPLATE, "spigot.yml"));

    Assertions.assertTrue(storage.createFile(TEMPLATE, "deep/rummel.yml"));
    Assertions.assertTrue(storage.hasFile(TEMPLATE, "deep/rummel.yml"));
  }

  @Test
  @Order(10)
  void testHasTemplate() {
    Assertions.assertTrue(storage.contains(TEMPLATE));
    Assertions.assertFalse(storage.contains(ServiceTemplate.builder()
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

    try (var stream = storage.newOutputStream(TEMPLATE, "test/test.txt")) {
      Assertions.assertNotNull(stream);
      stream.write("Hello".getBytes(StandardCharsets.UTF_8));
    }

    Assertions.assertTrue(storage.hasFile(TEMPLATE, "test/test.txt"));
    try (var stream = storage.newInputStream(TEMPLATE, "test/test.txt")) {
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
  void testFileGetFileInfo() {
    var info = storage.fileInfo(TEMPLATE, "spigot.yml");
    Assertions.assertNotNull(info);
    Assertions.assertEquals(0, info.size());
    Assertions.assertEquals("spigot.yml", info.path());
    Assertions.assertEquals("spigot.yml", info.name());
  }

  @Test
  @Order(60)
  void testDeleteFile() {
    Assertions.assertTrue(storage.deleteFile(TEMPLATE, "spigot.yml"));
    Assertions.assertFalse(storage.hasFile(TEMPLATE, "spigot.yml"));
  }

  @Test
  @Order(70)
  void testCreateDirectory() {
    Assertions.assertTrue(storage.createDirectory(TEMPLATE, "hello"));
    Assertions.assertTrue(storage.createFile(TEMPLATE, "hello/test.txt"));
    Assertions.assertTrue(storage.hasFile(TEMPLATE, "hello/test.txt"));
  }

  @Test
  @Order(80)
  void testFileListingNonDeep() {
    var files = storage.listFiles(TEMPLATE, "hello", false);
    Assertions.assertNotNull(files);
    Assertions.assertEquals(1, files.size());

    var info = files.iterator().next();
    Assertions.assertEquals(0, info.size());
    Assertions.assertEquals("test.txt", info.name());
    Assertions.assertTrue(info.path().endsWith("hello/test.txt"));
  }

  @Test
  @Order(90)
  void testFileListingDeep() {
    var files = storage.listFiles(TEMPLATE, "", true);
    Assertions.assertNotNull(files);
    Assertions.assertEquals(7, files.size());

    // there must be three directories
    var dir = files.stream().filter(FileInfo::directory).count();
    Assertions.assertEquals(3, dir);
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
    Assertions.assertFalse(storage.contains(TEMPLATE));
    Assertions.assertFalse(storage.hasFile(TEMPLATE, "test.txt"));
  }
}
