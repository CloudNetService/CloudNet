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

package eu.cloudnetservice.node.impl.database;

import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.StandardSerialisationStyle;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDatabase implements LocalDatabase, Database {

  protected final String name;
  protected final AbstractNodeDatabaseProvider databaseProvider;

  protected AbstractDatabase(@NonNull String name, @NonNull AbstractNodeDatabaseProvider databaseProvider) {
    this.name = name;
    this.databaseProvider = databaseProvider;
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  @Override
  public void iterate(@NonNull BiConsumer<String, Document> consumer, int chunkSize) {
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

  @Override
  public @NonNull CompletableFuture<Long> documentCountAsync() {
    return TaskUtil.supplyAsync(this::documentCount);
  }

  @Override
  public @NonNull CompletableFuture<Void> clearAsync() {
    return TaskUtil.runAsync(this::clear);
  }

  @Override
  public @NonNull CompletableFuture<Map<String, Document>> entriesAsync() {
    return TaskUtil.supplyAsync(this::entries);
  }

  @Override
  public @NonNull CompletableFuture<Collection<Document>> documentsAsync() {
    return TaskUtil.supplyAsync(this::documents);
  }

  @Override
  public @NonNull CompletableFuture<Collection<String>> keysAsync() {
    return TaskUtil.supplyAsync(this::keys);
  }

  @Override
  public @NonNull CompletableFuture<Collection<Document>> findAsync(@NonNull Map<String, String> filters) {
    return TaskUtil.supplyAsync(() -> this.find(filters));
  }

  @Override
  public @NonNull CompletableFuture<Collection<Document>> findAsync(
    @NonNull String fieldName,
    @Nullable String fieldValue
  ) {
    return TaskUtil.supplyAsync(() -> this.find(fieldName, fieldValue));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deleteAsync(@NonNull String key) {
    return TaskUtil.supplyAsync(() -> this.delete(key));
  }

  @Override
  public @NonNull CompletableFuture<Document> getAsync(@NonNull String key) {
    return TaskUtil.supplyAsync(() -> this.get(key));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> containsAsync(@NonNull String key) {
    return TaskUtil.supplyAsync(() -> this.contains(key));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> insertAsync(@NonNull String key, @NonNull Document document) {
    return TaskUtil.supplyAsync(() -> this.insert(key, document));
  }

  protected @NonNull String serializeDocumentToJsonString(@NonNull Document document) {
    // send the given document into a new json document
    var jsonDocument = Document.newJsonDocument();
    jsonDocument.receive(document.send());

    // serialize the json document
    return jsonDocument.serializeToString(StandardSerialisationStyle.COMPACT);
  }
}
