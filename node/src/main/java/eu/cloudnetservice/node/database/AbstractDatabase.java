/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.database;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.database.Database;
import java.util.function.BiConsumer;
import lombok.NonNull;

public abstract class AbstractDatabase implements LocalDatabase, Database {

  protected final String name;
  protected final NodeDatabaseProvider databaseProvider;

  protected AbstractDatabase(@NonNull String name, @NonNull NodeDatabaseProvider databaseProvider) {
    this.name = name;
    this.databaseProvider = databaseProvider;
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  @Override
  public void iterate(@NonNull BiConsumer<String, JsonDocument> consumer, int chunkSize) {
    var documentCount = this.documentCount();
    if (documentCount != 0) {
      long currentIndex = 0;
      while (currentIndex < documentCount) {
        var result = this.readChunk(currentIndex, chunkSize);
        if (result != null) {
          result.forEach(consumer);
          currentIndex += chunkSize;
          continue;
        }

        break;
      }
    }
  }
}
