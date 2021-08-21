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

package de.dytanic.cloudnet.wrapper.database.defaults;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.api.RemoteDatabaseRequestType;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.wrapper.database.IDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class WrapperDatabase implements IDatabase {

  private static final Logger LOGGER = LogManager.getLogger(WrapperDatabase.class);

  private final String name;
  private final DefaultWrapperDatabaseProvider databaseProvider;

  public WrapperDatabase(String name, DefaultWrapperDatabaseProvider databaseProvider) {
    this.name = name;
    this.databaseProvider = databaseProvider;
  }

  private ProtocolBuffer writeDefaults(ProtocolBuffer buffer) {
    return buffer.writeString(this.name);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean insert(String key, JsonDocument document) {
    return this.insertAsync(key, document).getDef(false);
  }

  @Override
  public boolean update(String key, JsonDocument document) {
    return this.updateAsync(key, document).getDef(false);
  }

  @Override
  public boolean contains(String key) {
    return this.containsAsync(key).getDef(false);
  }

  @Override
  public boolean delete(String key) {
    return this.deleteAsync(key).getDef(false);
  }

  @Override
  public JsonDocument get(String key) {
    return this.getAsync(key).getDef(null);
  }

  @Override
  public List<JsonDocument> get(String fieldName, Object fieldValue) {
    return this.getAsync(fieldName, fieldValue).getDef(Collections.emptyList());
  }

  @Override
  public List<JsonDocument> get(JsonDocument filters) {
    return this.getAsync(filters).getDef(Collections.emptyList());
  }

  @Override
  public Collection<String> keys() {
    return this.keysAsync().getDef(Collections.emptyList());
  }

  @Override
  public Collection<JsonDocument> documents() {
    return this.documentsAsync().getDef(Collections.emptyList());
  }

  @Override
  public Map<String, JsonDocument> entries() {
    return this.entriesAsync().getDef(Collections.emptyMap());
  }

  @Override
  public Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate) {
    return this.filterAsync(predicate).get(5, TimeUnit.SECONDS, Collections.emptyMap());
  }

  @Override
  public void iterate(BiConsumer<String, JsonDocument> consumer) {
    this.iterateAsync(consumer).getDef(null);
  }

  @Override
  public void iterate(BiConsumer<String, JsonDocument> consumer, int chunkSize) {
    this.iterateAsync(consumer, chunkSize).getDef(null);
  }

  @Override
  public void clear() {
    this.clearAsync().getDef(null);
  }

  @Override
  public long getDocumentsCount() {
    Long result = this.getDocumentsCountAsync().getDef(-1L);
    return result != null ? result : -1;
  }

  @Override
  public boolean isSynced() {
    return true;
  }

  @Override
  @NotNull
  public ITask<Boolean> insertAsync(String key, JsonDocument document) {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<Boolean> containsAsync(String key) {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<Boolean> updateAsync(String key, JsonDocument document) {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<Boolean> deleteAsync(String key) {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<JsonDocument> getAsync(String key) {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<Collection<String>> keysAsync() {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<Collection<JsonDocument>> documentsAsync() {
    return CompletableTask.supplyAsync(() ->);
  }

  private List<JsonDocument> asJsonDocumentList(ProtocolBuffer buffer) {
    int size = buffer.readVarInt();
    List<JsonDocument> documents = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      documents.add(buffer.readJsonDocument());
    }
    return documents;
  }

  @Override
  @NotNull
  public ITask<Map<String, JsonDocument>> entriesAsync() {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  public @NotNull ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate) {
    return this.entriesAsync().map(map -> {
      Map<String, JsonDocument> result = new HashMap<>();
      map.forEach((key, document) -> {
        if (predicate.test(key, document)) {
          result.put(key, document);
        }
      });
      return result;
    });
  }

  @Override
  @NotNull
  public ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer) {
    ITask<Void> task = new ListenableTask<>(() -> null);
    this.entriesAsync().onComplete(response -> {
      response.forEach(consumer);
      try {
        task.call();
      } catch (Exception exception) {
        LOGGER.severe("Exception while calling response", exception);
      }
    });
    return task;
  }

  @Override
  public @NotNull ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer, int chunkSize) {
    CompletableTask<Void> completeTask = new CompletableTask<>();
    this.readNextDataChunk(0, chunkSize)
      .onComplete(this.chunkedIterateResultHandler(completeTask, consumer, 0, chunkSize));
    return completeTask;
  }

  private @NotNull ITask<Map<String, JsonDocument>> readNextDataChunk(long beginIndex, int chunkSize) {
    return this.databaseProvider.executeQuery(
      RemoteDatabaseRequestType.DATABASE_ITERATE_CHUNKED,
      buffer -> this.writeDefaults(buffer).writeVarLong(beginIndex).writeVarInt(chunkSize)
    ).map(this.dataMapReader());
  }

  private @NotNull Consumer<Map<String, JsonDocument>> chunkedIterateResultHandler(
    @NotNull CompletableTask<Void> completeTask,
    @NotNull BiConsumer<String, JsonDocument> consumer,
    long currentIndex,
    int chunkSize
  ) {
    return result -> {
      if (result.isEmpty()) {
        completeTask.complete(null);
        return;
      }

      result.forEach(consumer);
      this.readNextDataChunk(currentIndex + chunkSize, chunkSize)
        .onComplete(this.chunkedIterateResultHandler(completeTask, consumer, currentIndex + chunkSize, chunkSize));
    };
  }

  private @NotNull Function<IPacket, Map<String, JsonDocument>> dataMapReader() {
    return packet -> {
      int size = packet.getBuffer().readVarInt();
      Map<String, JsonDocument> documents = new HashMap<>();
      for (int i = 0; i < size; i++) {
        documents.put(packet.getBuffer().readString(), packet.getBuffer().readJsonDocument());
      }
      return documents;
    };
  }

  @Override
  @NotNull
  public ITask<Void> clearAsync() {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  @NotNull
  public ITask<Long> getDocumentsCountAsync() {
    return CompletableTask.supplyAsync(() ->);
  }

  @Override
  public void close() {
    this.databaseProvider.executeQuery(
      RemoteDatabaseRequestType.DATABASE_CLOSE,
      this::writeDefaults
    ).get(5, TimeUnit.SECONDS, null);
  }

}
