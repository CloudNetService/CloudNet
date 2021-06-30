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

package de.dytanic.cloudnet.common.document.gson;

import de.dytanic.cloudnet.common.collection.Pair;
import org.junit.Assert;
import org.junit.Test;

public class JsonDocumentTest {

  @Test
  public void testDocument() {
    JsonDocument document = new JsonDocument();

    Assert.assertNotNull(document.append("foo", "bar"));

    document.append("number", 4).append("test", new TestClass("myData"));

    Assert.assertNotNull(document);
    Assert.assertEquals("bar", document.getString("foo"));
    Assert.assertEquals(4, document.getInt("number"));
    Assert.assertEquals("myData", document.get("test", TestClass.class).data);
    Assert.assertEquals("Hello, world!", new String(document.getBinary("test_binary", "Hello, world!".getBytes())));
  }

  @Test
  public void testProperties() {
    JsonDocProperty<Pair<String, String>> docProperty = new JsonDocProperty<>(

      (stringStringPair, document) -> document
        .append("firstProp", stringStringPair.getFirst())
        .append("secondProp", stringStringPair.getSecond()),
      document -> {
        if (!document.contains("firstProp") || !document.contains("secondProp")) {
          return null;
        }

        return new Pair<>(document.getString("firstProp"), document.getString("secondProp"));
      },
      document -> {
        document.remove("firstProp");
        document.remove("secondProp");
      },
      jsonDocument -> jsonDocument.contains("firstProp") && jsonDocument.contains("secondProp")
    );

    JsonDocument document = new JsonDocument();
    document.setProperty(docProperty, new Pair<>("foo", "bar"));

    Assert.assertTrue(document.hasProperty(docProperty));
    Assert.assertEquals("foo", document.getProperty(docProperty).getFirst());
    Assert.assertEquals("bar", document.getProperty(docProperty).getSecond());

    document.removeProperty(docProperty);

    Assert.assertEquals(0, document.size());
  }

  private static class TestClass {

    private final String data;

    public TestClass(String data) {
      this.data = data;
    }

    public String getData() {
      return this.data;
    }
  }

}
