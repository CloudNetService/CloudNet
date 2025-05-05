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

package eu.cloudnetservice.node.impl.database.h2;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.node.impl.junit.EnableServicesInject;
import eu.cloudnetservice.utils.base.io.FileUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@EnableServicesInject
class H2DatabaseTest {

  private H2DatabaseProvider databaseProvider;

  @BeforeEach
  void setup() throws Exception {
    var baseDirectory = Path.of("build", "tmp", "h2");
    FileUtil.delete(baseDirectory);

    this.databaseProvider = new H2DatabaseProvider(baseDirectory.resolve("db").toString());
    this.databaseProvider.init();
  }

  @AfterEach
  void closeEnvironment() throws Exception {
    this.databaseProvider.close();
  }

  @Test
  void testAccessCreatesDatabase() {
    Assertions.assertNotNull(this.databaseProvider.database("hello_world"));
    Assertions.assertNotNull(this.databaseProvider.database("hello2_world"));

    var names = this.databaseProvider.databaseNames();
    Assertions.assertTrue(names.contains("hello_world"));
    Assertions.assertTrue(names.contains("hello2_world"));
  }

  @Test
  void testDatabaseDeletion() {
    Assertions.assertNotNull(this.databaseProvider.database("hello_world"));
    Assertions.assertNotNull(this.databaseProvider.database("hello2_world"));

    var names = this.databaseProvider.databaseNames();
    Assertions.assertTrue(names.contains("hello_world"));
    Assertions.assertTrue(names.contains("hello2_world"));

    Assertions.assertTrue(this.databaseProvider.deleteDatabase("hello_world"));
    Assertions.assertTrue(this.databaseProvider.deleteDatabase("hello2_world"));

    Assertions.assertTrue(this.databaseProvider.databaseNames().isEmpty());
  }

  @Test
  void testBasicDatabaseOperations() {
    var database = this.databaseProvider.database("test");
    Assertions.assertNotNull(database);

    Assertions.assertTrue(database.insert("1234", Document.newJsonDocument().append("hello", "world")));
    Assertions.assertTrue(database.insert("12234", Document.newJsonDocument().append("hello", "world2")));

    Assertions.assertTrue(database.contains("1234"));
    Assertions.assertTrue(database.contains("12234"));

    Assertions.assertEquals(2, database.documentCount());

    var keys = database.keys();
    Assertions.assertEquals(2, keys.size());
    Assertions.assertTrue(keys.contains("1234"));
    Assertions.assertTrue(keys.contains("12234"));

    var entry = database.get("1234");
    Assertions.assertNotNull(entry);
    Assertions.assertEquals("world", entry.getString("hello"));

    var entry2 = database.get("12234");
    Assertions.assertNotNull(entry2);
    Assertions.assertEquals("world2", entry2.getString("hello"));

    var entry3 = database.get("122334");
    Assertions.assertNull(entry3);

    //var entry4 = database.find("hello", "world");
    //Assertions.assertEquals(1, entry4.size());
    //Assertions.assertEquals("world", entry4.iterator().next().getString("hello"));

    var entry5 = database.find(Map.of("hello", "world2"));
    Assertions.assertEquals(1, entry5.size());
    Assertions.assertEquals("world2", entry5.iterator().next().getString("hello"));

    var entries = database.entries();
    Assertions.assertEquals(2, entries.size());
    Assertions.assertEquals("world", entries.get("1234").getString("hello"));
    Assertions.assertEquals("world2", entries.get("12234").getString("hello"));

    var documents = database.documents();
    Assertions.assertEquals(2, documents.size());

    Assertions.assertTrue(database.delete("12234"));
    Assertions.assertEquals(1, database.documentCount());

    database.clear();
    Assertions.assertEquals(0, database.documentCount());

    Assertions.assertFalse(database.delete("1234"));
  }

  @Test
  void testChunkedDataRead() {
    var database = this.databaseProvider.database("test");
    Assertions.assertNotNull(database);

    // fill in some data
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
}
