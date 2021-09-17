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

package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDatabase implements LocalDatabase, IDatabase {

  protected final String name;
  protected final ExecutorService executorService;
  protected final AbstractDatabaseProvider databaseProvider;

  protected AbstractDatabase(String name, ExecutorService executorService, AbstractDatabaseProvider databaseProvider) {
    this.name = name;
    this.executorService = executorService;
    this.databaseProvider = databaseProvider;
  }

  @Override
  public @NotNull String getName() {
    return this.name;
  }

  @Override
  public void iterate(BiConsumer<String, JsonDocument> consumer, int chunkSize) {
    long documentCount = this.getDocumentsCount();
    if (documentCount != 0) {
      long currentIndex = 0;
      while (currentIndex < documentCount) {
        Map<String, JsonDocument> result = this.readChunk(currentIndex, chunkSize);
        if (result != null) {
          result.forEach(consumer);
          currentIndex += chunkSize;
          continue;
        }

        break;
      }
    }
  }

  @Override
  public AbstractDatabaseProvider getDatabaseProvider() {
    return this.databaseProvider;
  }
}
