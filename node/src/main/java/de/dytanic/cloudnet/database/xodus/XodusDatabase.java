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

package de.dytanic.cloudnet.database.xodus;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XodusDatabase extends AbstractDatabase {

  protected final Environment environment;
  protected final AtomicReference<Store> store;

  protected XodusDatabase(
    @NotNull String name,
    @NotNull ExecutorService executorService,
    @NotNull Store store,
    @NotNull XodusDatabaseProvider provider
  ) {
    super(name, executorService, provider);

    this.environment = store.getEnvironment();
    this.store = new AtomicReference<>(store);
  }

  @Override
  public boolean insert(@NotNull String key, @NotNull JsonDocument document) {
    this.databaseProvider.databaseHandler().handleInsert(this, key, document);

    return this.insert0(key, document);
  }

  @Override
  public boolean update(@NotNull String key, @NotNull JsonDocument document) {
    this.databaseProvider.databaseHandler().handleUpdate(this, key, document);

    return this.insert0(key, document);
  }

  protected boolean insert0(String key, JsonDocument document) {
    return this.environment.computeInExclusiveTransaction(
      txn -> this.getStore().put(
        txn,
        StringBinding.stringToEntry(key),
        new ArrayByteIterable(document.toString().getBytes(StandardCharsets.UTF_8))));
  }

  @Override
  public boolean contains(@NotNull String key) {
    return this.environment.computeInReadonlyTransaction(
      txn -> this.getStore().get(txn, StringBinding.stringToEntry(key)) != null);
  }

  @Override
  public boolean delete(@NotNull String key) {
    this.databaseProvider.databaseHandler().handleDelete(this, key);

    return this.delete0(key);
  }

  protected boolean delete0(String key) {
    return this.environment.computeInTransaction(txn -> this.getStore().delete(txn, StringBinding.stringToEntry(key)));
  }

  @Override
  public JsonDocument get(String key) {
    return this.environment.computeInReadonlyTransaction(txn -> {
      var entry = this.getStore().get(txn, StringBinding.stringToEntry(key));
      return entry == null ? null : JsonDocument.fromJsonBytes(entry.getBytesUnsafe());
    });
  }

  @Override
  public @NotNull List<JsonDocument> get(@NotNull String fieldName, Object fieldValue) {
    var like = JsonDocument.GSON.toJsonTree(fieldValue);
    return this.handleWithCursor(($, document) -> {
      if (document.contains(fieldName) && document.get(fieldName).equals(like)) {
        return document;
      }
      return null;
    });
  }

  @Override
  public @NotNull List<JsonDocument> get(@NotNull JsonDocument filters) {
    Map<String, Object> filterObjects = new HashMap<>();
    if (!filters.empty()) {
      for (var key : filters) {
        filterObjects.put(key, filters.get(key));
      }
    }

    var entries = filterObjects.entrySet();
    return this.handleWithCursor(($, document) -> {
      for (var entry : entries) {
        if (document.contains(entry.getKey()) && document.get(entry.getKey()).equals(entry.getValue())) {
          return document;
        }
      }
      return null;
    });
  }

  @Override
  public @NotNull Collection<String> keys() {
    return this.handleWithCursor((key, $) -> key);
  }

  @Override
  public @NotNull Collection<JsonDocument> documents() {
    return this.handleWithCursor(($, document) -> document);
  }

  @Override
  public @NotNull Map<String, JsonDocument> entries() {
    Map<String, JsonDocument> result = new HashMap<>();
    this.acceptWithCursor(result::put);
    return result;
  }

  @Override
  public @NotNull Map<String, JsonDocument> filter(@NotNull BiPredicate<String, JsonDocument> predicate) {
    Map<String, JsonDocument> result = new HashMap<>();
    this.acceptWithCursor((key, document) -> {
      if (predicate.test(key, document)) {
        result.put(key, document);
      }
    });
    return result;
  }

  @Override
  public void iterate(@NotNull BiConsumer<String, JsonDocument> consumer) {
    this.acceptWithCursor(consumer);
  }

  @Override
  public void clear() {
    this.databaseProvider.databaseHandler().handleClear(this);
    this.environment.executeInExclusiveTransaction(txn -> {
      this.environment.truncateStore(this.name, txn);
      this.store.set(this.environment.openStore(this.name, this.getStore().getConfig(), txn));
    });
  }

  @Override
  public long documentCount() {
    return this.environment.computeInReadonlyTransaction(txn -> this.getStore().count(txn));
  }

  @Override
  public boolean synced() {
    return false;
  }

  @Override
  public void close() {
  }

  protected @NotNull <T> List<T> handleWithCursor(@NotNull BiFunction<String, JsonDocument, T> mapper) {
    List<T> result = new ArrayList<>();
    this.acceptWithCursor((key, document) -> {
      var computed = mapper.apply(key, document);
      if (computed != null) {
        result.add(computed);
      }
    });
    return result;
  }

  protected void acceptWithCursor(@NotNull BiConsumer<String, JsonDocument> handler) {
    this.environment.executeInReadonlyTransaction(txn -> {
      try (var cursor = this.getStore().openCursor(txn)) {
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
      try (var cursor = this.getStore().openCursor(txn)) {
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

  protected @NotNull Store getStore() {
    return this.store.get();
  }
}
