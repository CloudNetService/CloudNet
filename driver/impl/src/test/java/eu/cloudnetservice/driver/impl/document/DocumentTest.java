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
import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.rpc.object.AllPrimitiveTypesDataClass;
import eu.cloudnetservice.driver.service.ProcessConfiguration;
import io.leangen.geantyref.TypeFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@EnableServicesInject
public class DocumentTest {

  static Stream<Arguments> documentTypeProvider() {
    return Stream.of(Arguments.of(Document.newJsonDocument()));
  }

  static Stream<Arguments> documentTypeFactoryNameProvider() {
    return Stream.of(
      Arguments.of("json", Document.newJsonDocument()),
      Arguments.of("empty", Document.emptyDocument()));
  }

  @ParameterizedTest
  @MethodSource("documentTypeFactoryNameProvider")
  void testDocumentFactoryTypeIsConsistent(String factoryName, Document document) {
    Assertions.assertEquals(factoryName, document.factoryName());
    Assertions.assertEquals(factoryName, document.immutableCopy().factoryName());
    Assertions.assertEquals(factoryName, document.mutableCopy().factoryName());
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testPrimitiveElementWrite(Document.Mutable document) {
    document.append("char", 'f');
    document.appendNull("null");
    document.append("boolean", true);
    document.append("int", 123456789);
    document.append("float", 1234.34F);
    document.append("byte", (byte) 123);
    document.append("long", 123456789L);
    document.append("short", (short) 123456);
    document.append("double", 123456789.123D);
    document.append("string", "Hello World!");

    Assertions.assertEquals("f", document.getString("char"));
    Assertions.assertNull(document.getString("null"));
    Assertions.assertTrue(document.getBoolean("boolean"));
    Assertions.assertEquals(123456789, document.getInt("int"));
    Assertions.assertEquals(1234.34F, document.getFloat("float"));
    Assertions.assertEquals((byte) 123, document.getByte("byte"));
    Assertions.assertEquals(123456789L, document.getLong("long"));
    Assertions.assertEquals((short) 123456, document.getShort("short"));
    Assertions.assertEquals(123456789.123D, document.getDouble("double"));
    Assertions.assertEquals("Hello World!", document.getString("string"));
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testDocumentElementRemove(Document.Mutable document) {
    document.append("stringA", "World");
    document.append("stringB", "Hello");

    Assertions.assertFalse(document.empty());
    Assertions.assertEquals(2, document.elementCount());
    Assertions.assertEquals("World", document.getString("stringA"));
    Assertions.assertEquals("Hello", document.getString("stringB"));

    var keySet = document.keys();
    Assertions.assertTrue(keySet.contains("stringA"));
    Assertions.assertTrue(keySet.contains("stringB"));

    document.append("stringB", "Test");
    Assertions.assertEquals(2, document.elementCount());
    Assertions.assertEquals("Test", document.getString("stringB"));

    document.remove("stringA");
    Assertions.assertNull(document.getString("stringA"));
    Assertions.assertEquals(1, document.elementCount());

    document.append("stringC", "CloudNet");
    document.append("stringD", "My beloved");
    Assertions.assertFalse(document.empty());
    Assertions.assertTrue(document.contains("stringC"));
    Assertions.assertEquals(3, document.elementCount());

    document.clear();
    Assertions.assertTrue(document.empty());
    Assertions.assertEquals(0, document.elementCount());
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testAppendObjects(Document.Mutable document) {
    var primitiveObjectClass = new AllPrimitiveTypesDataClass();
    document.appendTree(primitiveObjectClass);

    Assertions.assertFalse(document.empty());
    Assertions.assertEquals(5F, document.getFloat("f"));
    Assertions.assertEquals("Hello, World!", document.getString("string"));

    var readPrimitiveObjectClass = document.toInstanceOf(AllPrimitiveTypesDataClass.class);
    Assertions.assertEquals(primitiveObjectClass, readPrimitiveObjectClass);
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testContains(Document.Mutable document) {
    document.append("stringA", "Hello");
    document.appendNull("stringB");

    Assertions.assertTrue(document.contains("stringA"));
    Assertions.assertTrue(document.contains("stringB"));
    Assertions.assertFalse(document.contains("stringC"));

    Assertions.assertTrue(document.containsNonNull("stringA"));
    Assertions.assertFalse(document.containsNonNull("stringB"));
    Assertions.assertFalse(document.containsNonNull("stringC"));
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testAppendDocument(Document.Mutable document) {
    var someDocument = Document.newJsonDocument();
    someDocument.append("stringA", "Hello");
    someDocument.append("stringB", "World");
    document.append("someDoc", someDocument);

    Assertions.assertEquals(1, document.elementCount());
    Assertions.assertEquals(2, someDocument.elementCount());
    Assertions.assertTrue(document.contains("someDoc"));

    var readDocument = document.readDocument("someDoc");
    Assertions.assertEquals(someDocument, readDocument);

    var defaultDocument = Document.newJsonDocument().append("hello", "world");
    var readOtherDocument = document.readDocument("someOtherDoc", defaultDocument);
    Assertions.assertEquals(defaultDocument, readOtherDocument);
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testAppendDocumentTree(Document.Mutable document) {
    var someOtherDocument = Document.newJsonDocument();
    someOtherDocument.append("stringA", "Hello");
    someOtherDocument.append("stringB", "World");

    document.append(someOtherDocument);
    Assertions.assertFalse(document.empty());
    Assertions.assertEquals(2, document.elementCount());

    Assertions.assertEquals("Hello", document.getString("stringA"));
    Assertions.assertEquals("World", document.getString("stringB"));
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testAppendCollections(Document.Mutable document) {
    List<String> someList = List.of("Hello", "World", "!");
    document.append("someList", someList);

    var listType = TypeFactory.parameterizedClass(List.class, String.class);
    List<String> someReadList = document.readObject("someList", listType);
    Assertions.assertIterableEquals(someList, someReadList);

    Map<String, Integer> someMap = Map.of("Test", 1234, "World", 9876);
    document.append("someMap", someMap);

    var mapType = TypeFactory.parameterizedClass(Map.class, String.class, Integer.class);
    Map<String, Integer> someReadMap = document.readObject("someMap", mapType);
    Assertions.assertEquals(someMap, someReadMap);
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testAppendComplexObjects(Document.Mutable document) {
    var processConfiguration = new ProcessConfiguration(
      "jvm",
      512,
      List.of("World", ":)"),
      List.of("--port", "12345"),
      Map.of("HELLO", "world", "ANOTHER", "google"));
    document.append("processConfiguration", processConfiguration);

    var readProcessConfiguration = document.readObject("processConfiguration", ProcessConfiguration.class);
    Assertions.assertNotNull(readProcessConfiguration);
    Assertions.assertEquals(processConfiguration, readProcessConfiguration);
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testDocumentCopy(Document.Mutable document) {
    document.append("stringA", "World");
    document.append("stringB", "Hello");

    var immutableCopy = document.immutableCopy();
    Assertions.assertFalse(immutableCopy.empty());
    Assertions.assertEquals(2, immutableCopy.elementCount());
    Assertions.assertEquals("World", immutableCopy.getString("stringA"));

    document.append("stringC", ":)");
    Assertions.assertEquals(3, document.elementCount());
    Assertions.assertEquals(2, immutableCopy.elementCount());
    Assertions.assertNull(immutableCopy.getString("stringC"));

    var mutableCopy = document.mutableCopy();
    Assertions.assertFalse(mutableCopy.empty());
    Assertions.assertEquals(3, mutableCopy.elementCount());
    Assertions.assertEquals("World", mutableCopy.getString("stringA"));
    Assertions.assertEquals("Hello", mutableCopy.getString("stringB"));
    Assertions.assertEquals(":)", mutableCopy.getString("stringC"));

    mutableCopy.append("stringD", "!!");
    Assertions.assertEquals(4, mutableCopy.elementCount());
    Assertions.assertEquals(3, document.elementCount());
    Assertions.assertEquals(2, immutableCopy.elementCount());
    Assertions.assertNull(document.getString("stringD"));
    Assertions.assertNull(immutableCopy.getString("stringD"));
  }

  @ParameterizedTest
  @MethodSource("documentTypeProvider")
  void testClassMemberAnnotations(Document.Mutable document) {
    var testClassInstance = new DocumentValueTestClass(
      new DocumentValueTestClass.SomeInnerClass("World", "I", "don't", "care"),
      new DocumentValueTestClass.SomeExcludedInnerClass());
    Assertions.assertDoesNotThrow(() -> document.appendTree(testClassInstance));

    Assertions.assertFalse(document.contains("excludedInnerClass"));

    var innerClassDocument = document.readDocument("innerClass");
    Assertions.assertFalse(innerClassDocument.empty());

    // validate field renaming
    Assertions.assertFalse(innerClassDocument.contains("helloWorld"));
    Assertions.assertTrue(innerClassDocument.contains("worldHello"));
    Assertions.assertEquals("World", innerClassDocument.getString("worldHello"));

    // validate field exclusions
    Assertions.assertFalse(innerClassDocument.contains("fullyIgnoredField"));
    Assertions.assertFalse(innerClassDocument.contains("serializedIgnoredField"));
    Assertions.assertTrue(innerClassDocument.contains("deserializedIgnoredField"));
    Assertions.assertEquals("care", innerClassDocument.getString("deserializedIgnoredField"));

    // deserialize the inner class from the document
    var innerClassInstance = innerClassDocument.toInstanceOf(DocumentValueTestClass.SomeInnerClass.class);
    Assertions.assertEquals("World", innerClassInstance.helloWorld());
    Assertions.assertNull(innerClassInstance.fullyIgnoredField());
    Assertions.assertNull(innerClassInstance.serializedIgnoredField());
    Assertions.assertNull(innerClassInstance.deserializedIgnoredField());

    // check manual deserialization
    var serializedDocumentValueClass = Document.newJsonDocument()
      .append("innerClass", Document.newJsonDocument()
        .append("worldHello", "Test1")
        .append("fullyIgnoredField", "YepTree")
        .append("serializedIgnoredField", "Google")
        .append("deserializedIgnoredField", "Bing"))
      .append("excludedInnerClass", Document.newJsonDocument());

    var valueTestClassInstance = serializedDocumentValueClass.toInstanceOf(DocumentValueTestClass.class);
    Assertions.assertNotNull(valueTestClassInstance);

    Assertions.assertEquals("Test1", valueTestClassInstance.innerClass().helloWorld());
    Assertions.assertNull(valueTestClassInstance.innerClass().fullyIgnoredField());
    Assertions.assertEquals("Google", valueTestClassInstance.innerClass().serializedIgnoredField());
    Assertions.assertNull(valueTestClassInstance.innerClass().deserializedIgnoredField());

    Assertions.assertNotNull(valueTestClassInstance.excludedInnerClass());
  }
}
