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

import de.dytanic.cloudnet.common.document.property.FunctionalDocProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonDocumentTest {

  @Test
  void testDocumentSize() {
    Assertions.assertEquals(3, this.getDummyDocument().size());
  }

  @Test
  void testDocumentRead() {
    var document = this.getDummyDocument();

    Assertions.assertEquals("bar", document.getString("foo"));
    Assertions.assertEquals(4, document.getInt("number"));
    Assertions.assertEquals("myData", document.get("test", TestClass.class).data);
  }

  @Test
  void testDocumentRemove() {
    var document = this.getDummyDocument();

    Assertions.assertNull(document.remove("foo").getString("foo"));
    Assertions.assertNull(document.remove("test").get("test", TestClass.class));
    Assertions.assertEquals(0, document.remove("number").getInt("number"));
  }

  @Test
  void testClear() {
    Assertions.assertTrue(this.getDummyDocument().clear().isEmpty());
  }

  @Test
  void testJsonDocPropertyAppend() {
    Assertions.assertEquals(4, this.getDummyDocument().setProperty(this.getJsonDocProperty(), "test124").size());
  }

  @Test
  void testJsonDocPropertyRead() {
    var document = this.getDummyDocument().setProperty(this.getJsonDocProperty(), "test124");
    Assertions.assertEquals("test124", document.getProperty(this.getJsonDocProperty()));
  }

  @Test
  void testJsonDocPropertyRemove() {
    var document = this.getDummyDocument().setProperty(this.getJsonDocProperty(), "test124");
    Assertions.assertNull(document.removeProperty(this.getJsonDocProperty()).getProperty(this.getJsonDocProperty()));
  }

  private JsonDocument getDummyDocument() {
    return JsonDocument.newDocument()
      .append("foo", "bar")
      .append("number", 4)
      .append("test", new TestClass("myData"));
  }

  private FunctionalDocProperty<String> getJsonDocProperty() {
    return new FunctionalDocProperty<>(
      document -> document.getString("content"),
      (val, doc) -> doc.append("content", val),
      document -> document.remove("content"),
      document -> document.contains("content")
    );
  }

  private static class TestClass {

    private final String data;

    public TestClass(String data) {
      this.data = data;
    }
  }
}
