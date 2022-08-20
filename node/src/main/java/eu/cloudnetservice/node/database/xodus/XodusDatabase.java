/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.node.database.xodus;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.node.database.AbstractDatabase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class XodusDatabase extends AbstractDatabase {

  protected final Environment environment;
  protected final AtomicReference<Store> store;

  protected XodusDatabase(
    @NonNull String name,
    @NonNull ExecutorService executorService,
    @NonNull Store store,
    @NonNull XodusDatabaseProvider provider
  ) {
    super(name, executorService, provider);

    this.environment = store.getEnvironment();
    this.store = new AtomicReference<>(store);
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull JsonDocument document) {
    this.databaseProvider.databaseHandler().handleInsert(this, key, document);

    return this.insert0(key, document);
  }

  protected boolean insert0(String key, JsonDocument document) {
    return this.environment.computeInExclusiveTransaction(
      txn -> this.store().put(
        txn,
        StringBinding.stringToEntry(key),
        new ArrayByteIterable(document.toString().getBytes(StandardCharsets.UTF_8))));
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.environment.computeInReadonlyTransaction(
      txn -> this.store().get(txn, StringBinding.stringToEntry(key)) != null);
  }

  @Override
  public boolean delete(@NonNull String key) {
    this.databaseProvider.databaseHandler().handleDelete(this, key);

    return this.delete0(key);
  }

  protected boolean delete0(String key) {
    return this.environment.computeInTransaction(txn -> this.store().delete(txn, StringBinding.stringToEntry(key)));
  }

  @Override
  public @Nullable JsonDocument get(@NonNull String key) {
    return this.environment.computeInReadonlyTransaction(txn -> {
      var entry = this.store().get(txn, StringBinding.stringToEntry(key));
      return entry == null ? null : JsonDocument.fromJsonBytes(entry.getBytesUnsafe());
    });
  }

  @Override
  public @NonNull List<JsonDocument> find(@NonNull String fieldName, @Nullable String fieldValue) {
    return this.handleWithCursor(($, document) -> {
      if (Objects.equals(document.getString(fieldName), fieldValue)) {
        return document;
      }
      return null;
    });
  }

  @Override
  public @NonNull List<JsonDocument> find(@NonNull Map<String, String> filters) {
    var entries = filters.entrySet();
    return this.handleWithCursor(($, document) -> {
      for (var entry : entries) {
        if (!Objects.equals(document.getString(entry.getKey()), entry.getValue())) {
          return null;
        }
      }
      return document;
    });
  }

  @Override
  public @NonNull Collection<String> keys() {
    return this.handleWithCursor((key, $) -> key);
  }

  @Override
  public @NonNull Collection<JsonDocument> documents() {
    return this.handleWithCursor(($, document) -> document);
  }

  @Override
  public @NonNull Map<String, JsonDocument> entries() {
    Map<String, JsonDocument> result = new HashMap<>();
    this.acceptWithCursor(result::put);
    return result;
  }

  @Override
  public void iterate(@NonNull BiConsumer<String, JsonDocument> consumer) {
    this.acceptWithCursor(consumer);
  }

  @Override
  public void clear() {
    this.databaseProvider.databaseHandler().handleClear(this);
    this.environment.executeInExclusiveTransaction(txn -> {
      this.environment.truncateStore(this.name, txn);
      this.store.set(this.environment.openStore(this.name, this.store().getConfig(), txn));
    });
  }

  @Override
  public long documentCount() {
    return this.environment.computeInReadonlyTransaction(txn -> this.store().count(txn));
  }

  @Override
  public boolean synced() {
    return false;
  }

  @Override
  public void close() {
  }

  protected @NonNull <T> List<T> handleWithCursor(@NonNull BiFunction<String, JsonDocument, T> mapper) {
    List<T> result = new ArrayList<>();
    this.acceptWithCursor((key, document) -> {
      var computed = mapper.apply(key, document);
      if (computed != null) {
        result.add(computed);
      }
    });
    return result;
  }

  protected void acceptWithCursor(@NonNull BiConsumer<String, JsonDocument> handler) {
    this.environment.executeInReadonlyTransaction(txn -> {
      try (var cursor = this.store().openCursor(txn)) {
        while (cursor.getNext()) {
          handler.accept(
            StringBinding.entryToString(cursor.getKey()),
            JsonDocument.fromJsonBytes(cursor.getValue().getBytesUnsafe()));
        }
      }
    });
  }

  @Override
  public @Nullable Map<String, JsonDocument> readChunk(long beginIndex, int chunkSize) {
    return this.environment.computeInReadonlyTransaction(txn -> {
      try (var cursor = this.store().openCursor(txn)) {
        // skip to the begin index
        for (long i = 1; i < beginIndex; i++) {
          if (!cursor.getNext()) {
            return null;
          }
        }

        Map<String, JsonDocument> result = new HashMap<>();

        long currentReadCount = 0;
        while (chunkSize > currentReadCount && cursor.getNext()) {
          result.put(
            StringBinding.entryToString(cursor.getKey()),
            JsonDocument.fromJsonBytes(cursor.getValue().getBytesUnsafe()));
          currentReadCount++;
        }

        return result.isEmpty() ? null : result;
      }
    });
  }

  protected @NonNull Store store() {
    return this.store.get();
  }
}
