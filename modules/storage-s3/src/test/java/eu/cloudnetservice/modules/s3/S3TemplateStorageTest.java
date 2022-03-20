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

package eu.cloudnetservice.modules.s3;

import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.modules.s3.config.S3TemplateStorageConfig;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
class S3TemplateStorageTest {

  // default localstack port, maps all services to that port
  private static final int PORT = 4566;
  private static final ServiceTemplate TEMPLATE = ServiceTemplate.builder()
    .prefix("global")
    .name("proxy")
    .storage("s3")
    .build();

  @Container
  private static final GenericContainer<?> S3 = new GenericContainer<>("localstack/localstack:latest")
    .withExposedPorts(PORT)
    .withEnv("SERVICES", "s3")
    .waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1));

  private static S3TemplateStorage storage;

  @BeforeAll
  @SuppressWarnings("HttpUrlsUsage")
  static void setupServer() throws UnknownHostException {
    var runningAddress = InetAddress.getByName(S3.getHost()).getHostAddress();

    var module = Mockito.mock(S3TemplateStorageModule.class);
    Mockito.when(module.config()).thenReturn(new S3TemplateStorageConfig(
      "s3",
      "cn-testing",
      "us-east-1",
      false,
      "accesskey",
      "secretkey",
      String.format("http://%s:%d", runningAddress, S3.getMappedPort(PORT))));

    storage = new S3TemplateStorage(module);
  }

  @AfterAll
  static void closeStorage() {
    storage.close();
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
      .storage("s3")
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
  void testFileGetFileInfo() {
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
    Assertions.assertEquals(2, files.length);
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
  void testTemplateDelete() throws IOException {
    Assertions.assertTrue(storage.delete(TEMPLATE));
    Assertions.assertFalse(storage.has(TEMPLATE));
    Assertions.assertFalse(storage.hasFile(TEMPLATE, "test.txt"));
  }
}
