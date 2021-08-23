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
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.wrapper.Wrapper;
import de.dytanic.cloudnet.wrapper.database.IDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class WrapperDatabase implements IDatabase {

  private static final Logger LOGGER = LogManager.getLogger(WrapperDatabase.class);

  private final String name;
  private final DefaultWrapperDatabaseProvider databaseProvider;
  private final RPCSender rpcSender;

  public WrapperDatabase(String name, DefaultWrapperDatabaseProvider databaseProvider, Wrapper wrapper) {
    this.name = name;
    this.databaseProvider = databaseProvider;
    this.rpcSender = wrapper.getRPCProviderFactory().providerForClass(wrapper.getNetworkClient(), Database.class);
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
    return this.rpcSender.invokeMethod("insert", key, document).fireSync();
  }

  @Override
  public boolean update(String key, JsonDocument document) {
    return this.rpcSender.invokeMethod("update", key, document).fireSync();
  }

  @Override
  public boolean contains(String key) {
    return this.rpcSender.invokeMethod("contains", key).fireSync();
  }

  @Override
  public boolean delete(String key) {
    return this.rpcSender.invokeMethod("delete", key).fireSync();
  }

  @Override
  public JsonDocument get(String key) {
    return this.rpcSender.invokeMethod("get", key).fireSync();
  }

  @Override
  public List<JsonDocument> get(String fieldName, Object fieldValue) {
    return this.rpcSender.invokeMethod("get", fieldName, fieldValue).fireSync();
  }

  @Override
  public List<JsonDocument> get(JsonDocument filters) {
    return this.rpcSender.invokeMethod("get", filters).fireSync();
  }

  @Override
  public Collection<String> keys() {
    return this.rpcSender.invokeMethod("keys").fireSync();
  }

  @Override
  public Collection<JsonDocument> documents() {
    return this.rpcSender.invokeMethod("documents").fireSync();
  }

  @Override
  public Map<String, JsonDocument> entries() {
    return this.rpcSender.invokeMethod("entries").fireSync();
  }

  @Override
  public Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate) {
    return this.rpcSender.invokeMethod("filter", predicate).fireSync();
  }

  @Override
  public void iterate(BiConsumer<String, JsonDocument> consumer) {
    this.rpcSender.invokeMethod("iterate", consumer).fireAndForget();
  }

  @Override
  public void iterate(BiConsumer<String, JsonDocument> consumer, int chunkSize) {
    this.rpcSender.invokeMethod("iterate", consumer, chunkSize).fireAndForget();
  }

  @Override
  public void clear() {
    this.rpcSender.invokeMethod("clear").fireSync();
  }

  @Override
  public long getDocumentsCount() {
    Long result = this.rpcSender.invokeMethod("getDocumentsCount").fireSync();
    return result != null ? result : -1;
  }

  @Override
  public boolean isSynced() {
    return true;
  }

  @Override
  @NotNull
  public ITask<Boolean> insertAsync(String key, JsonDocument document) {
    return CompletableTask.supplyAsync(() -> this.insert(key, document));
  }

  @Override
  @NotNull
  public ITask<Boolean> containsAsync(String key) {
    return CompletableTask.supplyAsync(() -> this.contains(key));
  }

  @Override
  @NotNull
  public ITask<Boolean> updateAsync(String key, JsonDocument document) {
    return CompletableTask.supplyAsync(() -> this.update(key, document));
  }

  @Override
  @NotNull
  public ITask<Boolean> deleteAsync(String key) {
    return CompletableTask.supplyAsync(() -> this.delete(key));
  }

  @Override
  @NotNull
  public ITask<JsonDocument> getAsync(String key) {
    return CompletableTask.supplyAsync(() -> this.get(key));
  }

  @Override
  @NotNull
  public ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
    return CompletableTask.supplyAsync(() -> this.get(fieldName, fieldValue));
  }

  @Override
  @NotNull
  public ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
    return CompletableTask.supplyAsync(() -> this.get(filters));
  }

  @Override
  @NotNull
  public ITask<Collection<String>> keysAsync() {
    return CompletableTask.supplyAsync(this::keys);
  }

  @Override
  @NotNull
  public ITask<Collection<JsonDocument>> documentsAsync() {
    return CompletableTask.supplyAsync(this::documents);
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
    return CompletableTask.supplyAsync(this::entries);
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

  //TODO: replace this
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
      int size = packet.getContent().readInt();
      Map<String, JsonDocument> documents = new HashMap<>();
      for (int i = 0; i < size; i++) {
        documents.put(packet.getContent().readString(), packet.getBuffer().readJsonDocument());
      }
      return documents;
    };
  }

  @Override
  @NotNull
  public ITask<Void> clearAsync() {
    return CompletableTask.supplyAsync(this::clear);
  }

  @Override
  @NotNull
  public ITask<Long> getDocumentsCountAsync() {
    return CompletableTask.supplyAsync(this::getDocumentsCount);
  }

  @Override
  public void close() {
    this.rpcSender.invokeMethod("close").fireAndForget();
  }
}
