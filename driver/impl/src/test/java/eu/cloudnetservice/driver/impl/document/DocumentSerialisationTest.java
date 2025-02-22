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

package eu.cloudnetservice.driver.impl.document;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.SerialisationStyle;
import eu.cloudnetservice.driver.document.StandardSerialisationStyle;
import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.rpc.object.AllPrimitiveTypesDataClass;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@EnableServicesInject
public class DocumentSerialisationTest {

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  private Path tempDir;

  static Stream<Arguments> serialisationInputSource() {
    return Stream.of(
      Arguments.of(Document.newJsonDocument(), StandardSerialisationStyle.PRETTY),
      Arguments.of(
        Document.newJsonDocument()
          .append("test", 1234)
          .append("world", 5.999)
          .append("boolean", false)
          .append("hello", "world"),
        StandardSerialisationStyle.COMPACT),
      Arguments.of(
        Document.newJsonDocument()
          .append("google", "Bing")
          .append("world", new AllPrimitiveTypesDataClass()),
        StandardSerialisationStyle.PRETTY),
      Arguments.of(
        Document.newJsonDocument()
          .appendTree(new AllPrimitiveTypesDataClass())
          .append("cloud", List.of("Ben?", "Yes", "No", "HoHoHoHo"))
          .append("world", Map.of("hello", "world", "this", "is", "insane", "!")),
        StandardSerialisationStyle.PRETTY),
      Arguments.of(
        Document.newJsonDocument()
          .appendNull("testing")
          .appendTree(new AllPrimitiveTypesDataClass())
          .append("key", List.of("the", "best", "value"))
          .append("other", Document.newJsonDocument().append("hello", "world")),
        StandardSerialisationStyle.COMPACT));
  }

  static Stream<Arguments> documentTypeProvider() {
    return Stream.of(Arguments.of(Document.newJsonDocument(), Document.emptyDocument()));
  }

  @Test
  void testFileReadReturnsNewDocumentIfMissing() {
    var targetFile = Path.of("random_test_file_data_" + UUID.randomUUID());
    Assertions.assertFalse(Files.exists(targetFile));
    Assertions.assertFalse(Files.isRegularFile(targetFile));

    var deserialized = Assertions.assertDoesNotThrow(() -> DocumentFactory.json().parse(targetFile));
    Assertions.assertTrue(deserialized.empty());
  }

  @ParameterizedTest
  @MethodSource("serialisationInputSource")
  void testFileSerialisation(Document input, SerialisationStyle style) {
    var targetFile = this.tempDir.resolve("world").resolve("out.json");
    Assertions.assertDoesNotThrow(() -> input.writeTo(targetFile, style));

    Assertions.assertTrue(Files.exists(targetFile));
    Assertions.assertTrue(Files.isRegularFile(targetFile));

    var deserialized = Assertions.assertDoesNotThrow(() -> DocumentFactory.json().parse(targetFile));
    Assertions.assertEquals(input, deserialized);
  }

  @ParameterizedTest
  @MethodSource("serialisationInputSource")
  void testOutputStreamAndByteArraySerialisation(Document input, SerialisationStyle style) throws IOException {
    try (var out = new ByteArrayOutputStream()) {
      Assertions.assertDoesNotThrow(() -> input.writeTo(out, style));
      out.flush();

      try (var in = new ByteArrayInputStream(out.toByteArray())) {
        var deserializedFromStream = Assertions.assertDoesNotThrow(() -> DocumentFactory.json().parse(in));
        Assertions.assertEquals(input, deserializedFromStream);
      }

      var deserializedFromBytes = Assertions.assertDoesNotThrow(() -> DocumentFactory.json().parse(out.toByteArray()));
      Assertions.assertEquals(input, deserializedFromBytes);
    }
  }

  @ParameterizedTest
  @MethodSource("serialisationInputSource")
  void testDataBufSerialisation(Document input, SerialisationStyle style) {
    try (var buf = DataBuf.empty()) {
      Assertions.assertDoesNotThrow(() -> input.writeTo(buf, style));

      var deserialized = Assertions.assertDoesNotThrow(() -> DocumentFactory.json().parse(buf));
      Assertions.assertEquals(input, deserialized);
    }
  }

  @ParameterizedTest
  @MethodSource("serialisationInputSource")
  void testStringSerialisation(Document input, SerialisationStyle style) {
    var encoded = Assertions.assertDoesNotThrow(() -> input.serializeToString(style));

    var deserialized = Assertions.assertDoesNotThrow(() -> DocumentFactory.json().parse(encoded));
    Assertions.assertEquals(input, deserialized);
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testJavaIoSerialization(Document.Mutable document) throws IOException, ClassNotFoundException {
    document.append("hello", "world").append("test", 1234);
    var immutableCopy = document.immutableCopy();

    try (var byteStream = new ByteArrayOutputStream(); var objectOutStream = new ObjectOutputStream(byteStream)) {
      objectOutStream.writeObject(document);
      objectOutStream.writeObject(immutableCopy);

      try (var objectInStream = new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()))) {
        var mutableDoc = objectInStream.readObject();
        var immutableDoc = objectInStream.readObject();

        Assertions.assertInstanceOf(Document.Mutable.class, mutableDoc);
        Assertions.assertInstanceOf(Document.class, immutableDoc);

        Assertions.assertEquals(document, mutableDoc);
        Assertions.assertEquals(immutableCopy, immutableDoc);
      }
    }
  }
}
