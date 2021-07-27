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

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
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
  public String getName() {
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
  public @NotNull ITask<Boolean> insertAsync(String key, JsonDocument document) {
    return this.schedule(() -> this.insert(key, document));
  }

  @Override
  public @NotNull ITask<Boolean> updateAsync(String key, JsonDocument document) {
    return this.schedule(() -> this.update(key, document));
  }

  @Override
  public @NotNull ITask<Boolean> containsAsync(String key) {
    return this.schedule(() -> this.contains(key));
  }

  @Override
  public @NotNull ITask<Boolean> deleteAsync(String key) {
    return this.schedule(() -> this.delete(key));
  }

  @Override
  public @NotNull ITask<JsonDocument> getAsync(String key) {
    return this.schedule(() -> this.get(key));
  }

  @Override
  public @NotNull ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
    return this.schedule(() -> this.get(fieldName, fieldValue));
  }

  @Override
  public @NotNull ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
    return this.schedule(() -> this.get(filters));
  }

  @Override
  public @NotNull ITask<Collection<String>> keysAsync() {
    return this.schedule(this::keys);
  }

  @Override
  public @NotNull ITask<Collection<JsonDocument>> documentsAsync() {
    return this.schedule(this::documents);
  }

  @Override
  public @NotNull ITask<Map<String, JsonDocument>> entriesAsync() {
    return this.schedule(this::entries);
  }

  @Override
  public @NotNull ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate) {
    return this.schedule(() -> this.filter(predicate));
  }

  @Override
  public @NotNull ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer) {
    return this.schedule(() -> this.iterate(consumer));
  }

  @Override
  public @NotNull ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer, int chunkSize) {
    return this.schedule(() -> this.iterate(consumer, chunkSize));
  }

  @Override
  public @NotNull ITask<Void> clearAsync() {
    return this.schedule(this::clear);
  }

  @Override
  public @NotNull ITask<Long> getDocumentsCountAsync() {
    return this.schedule(this::getDocumentsCount);
  }

  @Override
  public AbstractDatabaseProvider getDatabaseProvider() {
    return this.databaseProvider;
  }

  @NotNull
  protected ITask<Void> schedule(Runnable runnable) {
    return this.schedule(() -> {
      runnable.run();
      return null;
    });
  }

  @NotNull
  protected <T> ITask<T> schedule(Callable<T> callable) {
    ITask<T> task = new ListenableTask<>(callable);
    this.executorService.submit(task);
    return task;
  }
}
