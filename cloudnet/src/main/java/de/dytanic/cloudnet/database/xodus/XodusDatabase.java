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

import com.google.gson.JsonElement;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XodusDatabase extends AbstractDatabase {

  protected final Environment environment;
  protected final AtomicReference<Store> store;

  protected XodusDatabase(String name, ExecutorService executorService, Store store, XodusDatabaseProvider provider) {
    super(name, executorService, provider);

    this.environment = store.getEnvironment();
    this.store = new AtomicReference<>(store);
  }

  @Override
  public boolean insert(String key, JsonDocument document) {
    if (this.databaseProvider.getDatabaseHandler() != null) {
      this.databaseProvider.getDatabaseHandler().handleInsert(this, key, document);
    }

    return this.insert0(key, document);
  }

  @Override
  public boolean update(String key, JsonDocument document) {
    if (this.databaseProvider.getDatabaseHandler() != null) {
      this.databaseProvider.getDatabaseHandler().handleUpdate(this, key, document);
    }

    return this.insert0(key, document);
  }

  protected boolean insert0(String key, JsonDocument document) {
    return this.environment.computeInExclusiveTransaction(
      txn -> this.getStore().put(txn, StringBinding.stringToEntry(key), new ArrayByteIterable(document.toByteArray())));
  }

  @Override
  public boolean contains(String key) {
    return this.environment.computeInReadonlyTransaction(
      txn -> this.getStore().get(txn, StringBinding.stringToEntry(key)) != null);
  }

  @Override
  public boolean delete(String key) {
    if (this.databaseProvider.getDatabaseHandler() != null) {
      this.databaseProvider.getDatabaseHandler().handleDelete(this, key);
    }

    return this.delete0(key);
  }

  protected boolean delete0(String key) {
    return this.environment.computeInTransaction(txn -> this.getStore().delete(txn, StringBinding.stringToEntry(key)));
  }

  @Override
  public JsonDocument get(String key) {
    return this.environment.computeInReadonlyTransaction(txn -> {
      ByteIterable entry = this.getStore().get(txn, StringBinding.stringToEntry(key));
      return entry == null ? null : JsonDocument.newDocument(entry.getBytesUnsafe());
    });
  }

  @Override
  public List<JsonDocument> get(String fieldName, Object fieldValue) {
    JsonElement like = JsonDocument.GSON.toJsonTree(fieldValue);
    return this.handleWithCursor(($, document) -> {
      if (document.contains(fieldName) && document.getElement(fieldName).equals(like)) {
        return document;
      }
      return null;
    });
  }

  @Override
  public List<JsonDocument> get(JsonDocument filters) {
    Map<String, Object> filterObjects = new HashMap<>();
    if (!filters.isEmpty()) {
      for (String key : filters) {
        filterObjects.put(key, filters.getElement(key));
      }
    }

    Set<Entry<String, Object>> entries = filterObjects.entrySet();
    return this.handleWithCursor(($, document) -> {
      for (Entry<String, Object> entry : entries) {
        if (document.contains(entry.getKey()) && document.getElement(entry.getKey()).equals(entry.getValue())) {
          return document;
        }
      }
      return null;
    });
  }

  @Override
  public Collection<String> keys() {
    return this.handleWithCursor((key, $) -> key);
  }

  @Override
  public Collection<JsonDocument> documents() {
    return this.handleWithCursor(($, document) -> document);
  }

  @Override
  public Map<String, JsonDocument> entries() {
    Map<String, JsonDocument> result = new HashMap<>();
    this.acceptWithCursor(result::put);
    return result;
  }

  @Override
  public Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate) {
    Map<String, JsonDocument> result = new HashMap<>();
    this.acceptWithCursor((key, document) -> {
      if (predicate.test(key, document)) {
        result.put(key, document);
      }
    });
    return result;
  }

  @Override
  public void iterate(BiConsumer<String, JsonDocument> consumer) {
    this.acceptWithCursor(consumer);
  }

  @Override
  public void clear() {
    if (this.databaseProvider.getDatabaseHandler() != null) {
      this.databaseProvider.getDatabaseHandler().handleClear(this);
    }

    this.clearWithoutHandlerCall();
  }

  @Override
  public long getDocumentsCount() {
    return this.environment.computeInReadonlyTransaction(txn -> this.getStore().count(txn));
  }

  @Override
  public boolean isSynced() {
    return false;
  }

  @Override
  public void close() throws Exception {
  }

  protected @NotNull <T> List<T> handleWithCursor(@NotNull BiFunction<String, JsonDocument, T> mapper) {
    List<T> result = new ArrayList<>();
    this.acceptWithCursor((key, document) -> {
      T computed = mapper.apply(key, document);
      if (computed != null) {
        result.add(computed);
      }
    });
    return result;
  }

  protected void acceptWithCursor(@NotNull BiConsumer<String, JsonDocument> handler) {
    this.environment.executeInReadonlyTransaction(txn -> {
      try (Cursor cursor = this.getStore().openCursor(txn)) {
        while (cursor.getNext()) {
          handler.accept(StringBinding.entryToString(cursor.getKey()),
            JsonDocument.newDocument(cursor.getValue().getBytesUnsafe()));
        }
      }
    });
  }

  protected @NotNull Store getStore() {
    return this.store.get();
  }

  @Override
  public void insertWithoutHandlerCall(@NotNull String key, @NotNull JsonDocument document) {
    this.insert0(key, document);
  }

  @Override
  public void updateWithoutHandlerCall(@NotNull String key, @NotNull JsonDocument document) {
    this.insert0(key, document);
  }

  @Override
  public void deleteWithoutHandlerCall(@NotNull String key) {
    this.delete0(key);
  }

  @Override
  public void clearWithoutHandlerCall() {
    this.environment.executeInExclusiveTransaction(txn -> {
      this.environment.truncateStore(this.name, txn);
      this.store.set(this.environment.openStore(this.name, this.getStore().getConfig(), txn));
    });
  }

  @Override
  public @Nullable Map<String, JsonDocument> readChunk(long beginIndex, int chunkSize) {
    return this.environment.computeInReadonlyTransaction(txn -> {
      try (Cursor cursor = this.getStore().openCursor(txn)) {
        // skip to the begin index
        for (long i = 1; i < beginIndex; i++) {
          if (!cursor.getNext()) {
            return null;
          }
        }

        Map<String, JsonDocument> result = new HashMap<>();

        long currentReadCount = 0;
        while (chunkSize > currentReadCount && cursor.getNext()) {
          result.put(StringBinding.entryToString(cursor.getKey()),
            JsonDocument.newDocument(cursor.getValue().getBytesUnsafe()));
          currentReadCount++;
        }

        return result.isEmpty() ? null : result;
      }
    });
  }
}
