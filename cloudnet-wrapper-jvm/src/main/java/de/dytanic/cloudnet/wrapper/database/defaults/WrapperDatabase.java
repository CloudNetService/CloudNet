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
    this.rpcSender.invokeMethod("iterate", consumer).fireSync();
  }

  @Override
  public void iterate(BiConsumer<String, JsonDocument> consumer, int chunkSize) {
    this.rpcSender.invokeMethod("iterate", consumer, chunkSize).fireSync();
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

  private List<JsonDocument> asJsonDocumentList(ProtocolBuffer buffer) {
    int size = buffer.readVarInt();
    List<JsonDocument> documents = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      documents.add(buffer.readJsonDocument());
    }
    return documents;
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
        //TODO: resolve this
        documents.put(packet.getContent().readString(), packet.getBuffer().readJsonDocument());
      }
      return documents;
    };
  }

  @Override
  public void close() {
    this.rpcSender.invokeMethod("close").fireSync();
  }
}
