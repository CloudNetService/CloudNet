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
import eu.cloudnetservice.driver.document.property.DocProperty;
import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.service.ProcessConfiguration;
import io.leangen.geantyref.TypeFactory;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@EnableServicesInject
public class DocumentPropertyTest {

  @ParameterizedTest
  @MethodSource("eu.cloudnetservice.driver.impl.document.DocumentTest#documentTypeProvider")
  void testDocPropertyWrite(Document.Mutable document) {
    var docProperty = DocProperty.property("world", String.class);
    docProperty.writeTo(document, "Some other World!");

    Assertions.assertEquals(1, document.elementCount());
    Assertions.assertEquals("Some other World!", docProperty.readFrom(document));

    docProperty.writeTo(document, "Google");
    Assertions.assertEquals(1, document.elementCount());
    Assertions.assertEquals("Google", docProperty.readFrom(document));
  }

  @ParameterizedTest
  @MethodSource("eu.cloudnetservice.driver.impl.document.DocumentTest#documentTypeProvider")
  void testAbsentDocProperty(Document.Mutable document) {
    var docProperty = DocProperty.property("world", String.class);
    var otherDocProperty = DocProperty.property("test", Integer.class);

    docProperty.writeTo(document, "Some other World!");
    Assertions.assertEquals(1, document.elementCount());
    Assertions.assertEquals("Some other World!", docProperty.readFrom(document));

    Assertions.assertNull(otherDocProperty.readFrom(document));
    Assertions.assertTrue(document.propertyAbsent(otherDocProperty));
    Assertions.assertFalse(document.propertyPresent(otherDocProperty));
    Assertions.assertEquals(1234, document.readPropertyOrDefault(otherDocProperty, 1234));
    Assertions.assertEquals(1234, document.readPropertyOrGet(otherDocProperty, () -> 1234));
    Assertions.assertThrows(NoSuchElementException.class, () -> document.readPropertyOrThrow(otherDocProperty));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> document.readPropertyOrThrow(otherDocProperty, IllegalArgumentException::new));

    otherDocProperty.writeTo(document, 12345);
    Assertions.assertEquals(2, document.elementCount());
    Assertions.assertEquals(12345, otherDocProperty.readFrom(document));

    Assertions.assertTrue(document.propertyPresent(otherDocProperty));
    Assertions.assertFalse(document.propertyAbsent(otherDocProperty));
    Assertions.assertEquals(12345, document.readPropertyOrDefault(otherDocProperty, 1234));
    Assertions.assertEquals(12345, document.readPropertyOrGet(otherDocProperty, () -> 1234));

    var docReadResultA = Assertions.assertDoesNotThrow(() -> document.readPropertyOrThrow(otherDocProperty));
    Assertions.assertEquals(12345, docReadResultA);

    var docReadResultB = Assertions.assertDoesNotThrow(() -> document.readPropertyOrThrow(
      otherDocProperty,
      IllegalArgumentException::new));
    Assertions.assertEquals(12345, docReadResultB);
  }

  @ParameterizedTest
  @MethodSource("eu.cloudnetservice.driver.impl.document.DocumentTest#documentTypeProvider")
  void testAbsentDocPropertyWrite(Document.Mutable document) {
    var docProperty = DocProperty.property("world", String.class);

    document.writePropertyIfAbsent(docProperty, "World!");
    Assertions.assertEquals("World!", docProperty.readFrom(document));

    document.writePropertyIfAbsent(docProperty, "Google");
    Assertions.assertEquals("World!", docProperty.readFrom(document));

    document.writePropertyIfPresent(docProperty, "Bing");
    Assertions.assertEquals("Bing", docProperty.readFrom(document));

    Assertions.assertEquals("Bing", document.removeProperty(docProperty));
    Assertions.assertNull(docProperty.readFrom(document));

    document.writePropertyIfPresent(docProperty, "Bing");
    Assertions.assertNull(docProperty.readFrom(document));

    document.writePropertyIfAbsent(docProperty, (Supplier<? extends String>) () -> "Bing");
    Assertions.assertEquals("Bing", docProperty.readFrom(document));

    document.writePropertyIfPresent(docProperty, (Supplier<? extends String>) () -> "Google");
    Assertions.assertEquals("Google", docProperty.readFrom(document));
  }

  @ParameterizedTest
  @MethodSource("eu.cloudnetservice.driver.impl.document.DocumentTest#documentTypeProvider")
  void testCollectionsPropertyWrite(Document.Mutable document) {
    var listPropertyType = TypeFactory.parameterizedClass(List.class, String.class);
    DocProperty<List<String>> docProperty = DocProperty.genericProperty("listProperty", listPropertyType);

    List<String> list = List.of("Hello", "World", "!!");
    document.writeProperty(docProperty, list);
    Assertions.assertIterableEquals(list, document.readProperty(docProperty));

    var mapPropertyType = TypeFactory.parameterizedClass(Map.class, String.class, Integer.class);
    DocProperty<Map<String, Integer>> mapDocProperty = DocProperty.genericProperty("mapProperty", mapPropertyType);

    Map<String, Integer> map = Map.of("Hello", 1234, "World", 9876);
    document.writeProperty(mapDocProperty, map);
    Assertions.assertEquals(map, document.readProperty(mapDocProperty));
  }

  @ParameterizedTest
  @MethodSource("eu.cloudnetservice.driver.impl.document.DocumentTest#documentTypeProvider")
  void testObjectsPropertyWrite(Document.Mutable document) {
    var property = DocProperty.property("config", ProcessConfiguration.class);
    var processConfiguration = new ProcessConfiguration(
      "jvm",
      512,
      List.of("World", ":)"),
      List.of("--port", "12345"),
      Map.of("HELLO", "world", "ANOTHER", "google"));

    document.writeProperty(property, processConfiguration);
    Assertions.assertEquals(processConfiguration, document.readProperty(property));
  }

  @ParameterizedTest
  @MethodSource("eu.cloudnetservice.driver.impl.document.DocumentTest#documentTypeProvider")
  void testReadOnlyProperty(Document.Mutable document) {
    var docProperty = DocProperty.property("prop", String.class).asReadOnly();
    Assertions.assertThrows(UnsupportedOperationException.class, () -> docProperty.writeTo(document, "Hello!"));
    Assertions.assertNull(document.readProperty(docProperty));
  }

  @ParameterizedTest
  @MethodSource("eu.cloudnetservice.driver.impl.document.DocumentTest#documentTypeProvider")
  void testDefaultingOnlyProperty(Document.Mutable document) {
    var docProperty = DocProperty.property("prop", String.class).withDefault("World!");
    Assertions.assertEquals("World!", document.readProperty(docProperty));

    document.writeProperty(docProperty, "Bing");
    Assertions.assertEquals("Bing", document.readProperty(docProperty));
  }

  @ParameterizedTest
  @MethodSource("eu.cloudnetservice.driver.impl.document.DocumentTest#documentTypeProvider")
  void testRewritingProperty(Document.Mutable document) {
    var docProperty = DocProperty.property("prop", String.class).withReadWriteRewrite(
      in -> 123,
      in -> "12345");
    Assertions.assertNull(document.readProperty(docProperty));

    document.writeProperty(docProperty, 98765);
    Assertions.assertEquals("12345", document.getString(docProperty.key()));
    Assertions.assertEquals(123, document.readProperty(docProperty));
  }
}
