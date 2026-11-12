/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.sql.impl;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.node.impl.database.NodeDatabaseProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

abstract class SQLDatabaseTest {

  protected static NodeDatabaseProvider databaseProvider;

  @AfterEach
  void cleanupDatabases() {
    for (var dbName : databaseProvider.databaseNames()) {
      databaseProvider.deleteDatabase(dbName);
    }
  }

  @Test
  void testDatabaseProviderOperations() {
    Assertions.assertNotNull(databaseProvider.database("hello_world"));
    Assertions.assertNotNull(databaseProvider.database("hello2_world"));

    var names = databaseProvider.databaseNames();
    Assertions.assertTrue(names.contains("hello_world"));
    Assertions.assertTrue(names.contains("hello2_world"));

    Assertions.assertTrue(databaseProvider.deleteDatabase("hello_world"));
    Assertions.assertTrue(databaseProvider.deleteDatabase("hello2_world"));
    Assertions.assertTrue(databaseProvider.databaseNames().isEmpty());
  }

  @Test
  void testBasicOperations() {
    var database = databaseProvider.database("test");
    Assertions.assertNotNull(database);

    Assertions.assertTrue(database.insert("1234", Document.newJsonDocument().append("hello", "world")));
    Assertions.assertTrue(database.insert("12234", Document.newJsonDocument().append("hello", "world2")));
    Assertions.assertTrue(database.insert("122234", Document.newJsonDocument().append("hello", "world_123")));

    Assertions.assertTrue(database.contains("1234"));
    Assertions.assertTrue(database.contains("12234"));
    Assertions.assertTrue(database.contains("122234"));

    Assertions.assertFalse(database.contains("non_existent_key"));
    Assertions.assertFalse(database.contains(UUID.randomUUID().toString()));

    Assertions.assertEquals(3, database.documentCount());

    var entry = database.get("1234");
    Assertions.assertNotNull(entry);
    Assertions.assertEquals("world", entry.getString("hello"));

    var entry2 = database.get("12234");
    Assertions.assertNotNull(entry2);
    Assertions.assertEquals("world2", entry2.getString("hello"));

    Assertions.assertNull(database.get("122334"));
    Assertions.assertNull(database.get("non_existent_key"));

    Assertions.assertTrue(database.insert("1234", Document.newJsonDocument().append("hello", "updated")));
    var updatedDoc = database.get("1234");
    Assertions.assertNotNull(updatedDoc);
    Assertions.assertEquals("updated", updatedDoc.getString("hello"));
    Assertions.assertEquals(3, database.documentCount());

    Assertions.assertTrue(database.delete("12234"));
    Assertions.assertEquals(2, database.documentCount());
    Assertions.assertFalse(database.contains("12234"));

    Assertions.assertFalse(database.delete("non_existent_key"));
    Assertions.assertFalse(database.delete(UUID.randomUUID().toString()));
  }

  @Test
  void testCollectionOperations() {
    var database = databaseProvider.database("test");
    Assertions.assertNotNull(database);

    Assertions.assertTrue(database.keys().isEmpty());
    Assertions.assertTrue(database.documents().isEmpty());
    Assertions.assertTrue(database.entries().isEmpty());
    Assertions.assertEquals(0, database.documentCount());

    database.insert("key1", Document.newJsonDocument().append("data", "value1"));
    database.insert("key2", Document.newJsonDocument().append("data", "value2"));
    database.insert("key3", Document.newJsonDocument().append("data", "value3"));

    var keys = database.keys();
    Assertions.assertEquals(3, keys.size());
    Assertions.assertTrue(keys.contains("key1"));
    Assertions.assertTrue(keys.contains("key2"));
    Assertions.assertTrue(keys.contains("key3"));

    var documents = database.documents();
    Assertions.assertEquals(3, documents.size());

    var entries = database.entries();
    Assertions.assertEquals(3, entries.size());
    Assertions.assertNotNull(entries.get("key1"));
    Assertions.assertNotNull(entries.get("key2"));
    Assertions.assertNotNull(entries.get("key3"));
    Assertions.assertEquals("value1", entries.get("key1").getString("data"));
    Assertions.assertEquals("value2", entries.get("key2").getString("data"));
    Assertions.assertEquals("value3", entries.get("key3").getString("data"));
  }

  @Test
  void testFindOperations() {
    var database = databaseProvider.database("test");
    Assertions.assertNotNull(database);

    database.insert("key1", Document.newJsonDocument()
      .append("name", "Alice")
      .append("age", "30")
      .append("city", "Berlin"));
    database.insert("key2", Document.newJsonDocument()
      .append("name", "Bob")
      .append("age", "30")
      .append("city", "Munich"));
    database.insert("key3", Document.newJsonDocument()
      .append("name", "Charlie")
      .append("age", "25")
      .append("city", "Berlin"));

    var findByName = database.find("name", "Alice");
    Assertions.assertEquals(1, findByName.size());
    Assertions.assertEquals("Alice", findByName.iterator().next().getString("name"));

    var findByAge = database.find(Map.of("age", "30"));
    Assertions.assertEquals(2, findByAge.size());

    var findByCity = database.find(Map.of("city", "Berlin"));
    Assertions.assertEquals(2, findByCity.size());

    Map<String, String> multiFilters = new HashMap<>();
    multiFilters.put("age", "30");
    multiFilters.put("city", "Berlin");
    var findMulti = database.find(multiFilters);
    Assertions.assertEquals(1, findMulti.size());
    Assertions.assertEquals("Alice", findMulti.iterator().next().getString("name"));

    var noMatches = database.find("name", "NonExistent");
    Assertions.assertNotNull(noMatches);
    Assertions.assertTrue(noMatches.isEmpty());

    var noMatchesMap = database.find(Map.of("name", "NonExistent"));
    Assertions.assertNotNull(noMatchesMap);
    Assertions.assertTrue(noMatchesMap.isEmpty());

    var nullResults = database.find("name", null);
    Assertions.assertNotNull(nullResults);
  }

  @Test
  void testClearOperations() {
    var database = databaseProvider.database("test");
    Assertions.assertNotNull(database);

    Assertions.assertDoesNotThrow(database::clear);
    Assertions.assertEquals(0, database.documentCount());

    database.insert("key1", Document.newJsonDocument().append("data", "1"));
    database.insert("key2", Document.newJsonDocument().append("data", "2"));
    database.insert("key3", Document.newJsonDocument().append("data", "3"));
    Assertions.assertEquals(3, database.documentCount());

    database.clear();
    Assertions.assertEquals(0, database.documentCount());
    Assertions.assertTrue(database.keys().isEmpty());
    Assertions.assertTrue(database.documents().isEmpty());
    Assertions.assertTrue(database.entries().isEmpty());
    Assertions.assertFalse(database.contains("key1"));
    Assertions.assertFalse(database.contains("key2"));
    Assertions.assertFalse(database.contains("key3"));

    Assertions.assertFalse(database.delete("key1"));
  }

  @Test
  void testChunkedDataRead() {
    var database = databaseProvider.database("test");
    Assertions.assertNotNull(database);

    var entries = 1235;
    List<String> keys = new ArrayList<>();
    var expectedReadCounts = (int) Math.ceil(entries / 50D);

    for (var i = 0; i < entries; i++) {
      var key = UUID.randomUUID().toString();
      keys.add(key);
      database.insert(key, Document.newJsonDocument().append("this_is", "a_world_test"));
    }

    Assertions.assertEquals(entries, database.documentCount());

    var index = 0;
    var readsCalled = 0;

    Map<String, Document> currentChunk;
    while ((currentChunk = database.readChunk(index, 50)) != null) {
      index += 50;
      readsCalled++;

      Assertions.assertFalse(currentChunk.size() > 50);
      Assertions.assertTrue(keys.removeAll(currentChunk.keySet()));
    }

    Assertions.assertEquals(expectedReadCounts, readsCalled);
    Assertions.assertTrue(keys.isEmpty());
  }

  @Test
  void testSpecialKeysAndValues() {
    var database = databaseProvider.database("test");
    Assertions.assertNotNull(database);

    database.insert("Key", Document.newJsonDocument().append("value", "uppercase"));
    database.insert("key", Document.newJsonDocument().append("value", "lowercase"));
    database.insert("KEY", Document.newJsonDocument().append("value", "alluppercase"));

    Assertions.assertEquals(3, database.documentCount());
    Assertions.assertTrue(database.contains("Key"));
    Assertions.assertTrue(database.contains("key"));
    Assertions.assertTrue(database.contains("KEY"));

    var upperCaseKey = database.get("Key");
    var lowerCaseKey = database.get("key");
    var fullUpperCaseKey = database.get("KEY");

    Assertions.assertNotNull(upperCaseKey);
    Assertions.assertNotNull(lowerCaseKey);
    Assertions.assertNotNull(fullUpperCaseKey);
    Assertions.assertEquals("uppercase", upperCaseKey.getString("value"));
    Assertions.assertEquals("lowercase", lowerCaseKey.getString("value"));
    Assertions.assertEquals("alluppercase", fullUpperCaseKey.getString("value"));

    database.clear();

    var specialKeys = List.of(
      "key-with-dashes",
      "key.with.dots",
      "key_with_underscores",
      "key:with:colons",
      "key/with/slashes"
    );

    for (var key : specialKeys) {
      Assertions.assertTrue(database.insert(key, Document.newJsonDocument().append("key", key)));
    }

    Assertions.assertEquals(specialKeys.size(), database.documentCount());

    for (var key : specialKeys) {
      Assertions.assertTrue(database.contains(key));
      var doc = database.get(key);
      Assertions.assertNotNull(doc);
      Assertions.assertEquals(key, doc.getString("key"));
    }

    database.clear();

    Assertions.assertTrue(database.insert("empty_value_key", Document.newJsonDocument().append("empty", "")));
    var emptyValueDoc = database.get("empty_value_key");
    Assertions.assertNotNull(emptyValueDoc);
    Assertions.assertEquals("", emptyValueDoc.getString("empty"));

    var emptyResults = database.find("empty", "");
    Assertions.assertEquals(1, emptyResults.size());
  }
}
