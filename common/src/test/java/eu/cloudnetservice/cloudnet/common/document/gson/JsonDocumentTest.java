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

package eu.cloudnetservice.cloudnet.common.document.gson;

import eu.cloudnetservice.cloudnet.common.document.property.FunctionalDocProperty;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonDocumentTest {

  @Test
  void testDocumentSize() {
    Assertions.assertEquals(3, this.dummyDocument().size());
  }

  @Test
  void testDocumentRead() {
    var document = this.dummyDocument();

    Assertions.assertEquals("bar", document.getString("foo"));
    Assertions.assertEquals(4, document.getInt("number"));
  }

  @Test
  void testRecordReading() {
    var document = this.dummyDocument();
    var record = document.get("test", TestRecord.class);

    Assertions.assertEquals("myData", record.data);

    Assertions.assertEquals(4, record.worldItems().get("1").size());
    Assertions.assertEquals(5, record.worldItems().get("2").size());

    Assertions.assertArrayEquals(new Integer[]{1, 2, 3, 4}, record.worldItems().get("1").toArray(new Integer[0]));
    Assertions.assertArrayEquals(new Integer[]{5, 6, 7, 8, 9}, record.worldItems().get("2").toArray(new Integer[0]));
  }

  @Test
  void testDocumentRemove() {
    var document = this.dummyDocument();

    Assertions.assertNull(document.remove("foo").getString("foo"));
    Assertions.assertNull(document.remove("test").get("test", TestRecord.class));
    Assertions.assertEquals(0, document.remove("number").getInt("number"));
  }

  @Test
  void testClear() {
    Assertions.assertTrue(this.dummyDocument().clear().empty());
  }

  @Test
  void testJsonDocPropertyAppend() {
    Assertions.assertEquals(4, this.dummyDocument().property(this.jsonDocProperty(), "test124").size());
  }

  @Test
  void testJsonDocPropertyRead() {
    var document = this.dummyDocument().property(this.jsonDocProperty(), "test124");
    Assertions.assertEquals("test124", document.property(this.jsonDocProperty()));
  }

  @Test
  void testJsonDocPropertyRemove() {
    var document = this.dummyDocument().property(this.jsonDocProperty(), "test124");
    Assertions.assertNull(document.removeProperty(this.jsonDocProperty()).property(this.jsonDocProperty()));
  }

  private JsonDocument dummyDocument() {
    return JsonDocument.newDocument()
      .append("foo", "bar")
      .append("number", 4)
      .append("test", new TestRecord("myData", Map.of("1", List.of(1, 2, 3, 4), "2", List.of(5, 6, 7, 8, 9))));
  }

  private FunctionalDocProperty<String> jsonDocProperty() {
    return new FunctionalDocProperty<>(
      document -> document.getString("content"),
      (val, doc) -> doc.append("content", val),
      document -> document.remove("content"),
      document -> document.contains("content")
    );
  }

  private record TestRecord(String data, Map<String, List<Integer>> worldItems) {

  }
}
