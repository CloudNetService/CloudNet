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

package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.database.Database;
import java.util.List;

public final class ExampleDatabaseProviderHandling {

  public void testDatabaseProvider() throws Throwable {
    AbstractDatabaseProvider databaseProvider = CloudNet.getInstance().getDatabaseProvider();

    Database database = databaseProvider.getDatabase("My custom Database");
    database.insert("Peter", new JsonDocument()
      .append("name", "Peter")
      .append("lastName", "Parker")
      .append("age", 17)
      .append("registered", System.currentTimeMillis())
    );

    if (database.contains("Peter")) {
      database.getAsync("Peter").onComplete(document -> {
        System.out.println(document.getString("name"));
        System.out.println(document.getString("lastName"));
        System.out.println(document.getInt("age"));
      }).fireExceptionOnFailure();
    }

    List<JsonDocument> responses = database.get("name", "Peter"); //filter with a key/value pair in value
    System.out.println("Founded items: " + responses.size()); //Founded items: 1

    responses = database
      .get(new JsonDocument("age", 17).append("lastName", "Parker")); //Filter with JsonDocument properties
    System.out.println("Founded items: " + responses.size()); //Founded items: 1

    database.clearAsync().get();
  }
}
