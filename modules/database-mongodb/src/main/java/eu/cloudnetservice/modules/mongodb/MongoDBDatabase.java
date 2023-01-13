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

package eu.cloudnetservice.modules.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.node.database.AbstractDatabase;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

public class MongoDBDatabase extends AbstractDatabase {

  protected static final String KEY_NAME = "Key";
  protected static final String VALUE_NAME = "Value";

  protected static final IndexOptions UNIQUE_KEY_OPTIONS = new IndexOptions().unique(true);
  protected static final UpdateOptions INSERT_OR_REPLACE_OPTIONS = new UpdateOptions().upsert(true);

  protected final MongoCollection<Document> collection;

  protected MongoDBDatabase(
    @NonNull String name,
    @NonNull MongoCollection<Document> collection,
    @NonNull NodeDatabaseProvider provider
  ) {
    super(name, provider);

    this.collection = collection;
    this.collection.createIndex(Indexes.ascending(KEY_NAME), UNIQUE_KEY_OPTIONS);
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull JsonDocument document) {
    return this.insertOrUpdate(key, document);
  }

  protected boolean insertOrUpdate(String key, JsonDocument document) {
    var result = this.collection.updateOne(
      Filters.eq(KEY_NAME, key),
      Updates.combine(
        Updates.setOnInsert(new Document(KEY_NAME, key)),
        Updates.set(VALUE_NAME, Document.parse(document.toString()))
      ),
      INSERT_OR_REPLACE_OPTIONS);
    return result.getUpsertedId() != null || result.getMatchedCount() > 0;
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.collection.find(Filters.eq(KEY_NAME, key)).first() != null;
  }

  @Override
  public boolean delete(@NonNull String key) {
    return this.collection.deleteOne(Filters.eq(KEY_NAME, key)).getDeletedCount() > 0;
  }

  @Override
  public JsonDocument get(@NonNull String key) {
    var document = this.collection.find(Filters.eq(KEY_NAME, key)).first();
    return document == null ? null : JsonDocument.fromJsonString(document.get(VALUE_NAME, Document.class).toJson());
  }

  @Override
  public @NonNull List<JsonDocument> find(@NonNull String fieldName, @Nullable String fieldValue) {
    List<JsonDocument> documents = new ArrayList<>();
    try (var cursor = this.collection.find(this.valueEq(fieldName, fieldValue)).iterator()) {
      while (cursor.hasNext()) {
        documents.add(JsonDocument.fromJsonString(cursor.next().get(VALUE_NAME, Document.class).toJson()));
      }
    }
    return documents;
  }

  @Override
  public @NonNull List<JsonDocument> find(@NonNull Map<String, String> filters) {
    // the easiest way to prevent issues with json-to-json conversion is to use the in-build document of mongodb and
    // then reconvert the values as we need them
    Collection<Bson> bsonFilters = new ArrayList<>();
    for (var entry : filters.entrySet()) {
      bsonFilters.add(this.valueEq(entry.getKey(), entry.getValue()));
    }

    List<JsonDocument> documents = new ArrayList<>();
    try (var cursor = this.collection.find(Filters.and(bsonFilters)).iterator()) {
      while (cursor.hasNext()) {
        documents.add(JsonDocument.fromJsonString(cursor.next().get(VALUE_NAME, Document.class).toJson()));
      }
    }
    return documents;
  }

  @Override
  public @NonNull Collection<String> keys() {
    Collection<String> keys = new ArrayList<>();
    try (var cursor = this.collection.find().iterator()) {
      while (cursor.hasNext()) {
        keys.add(cursor.next().getString(KEY_NAME));
      }
    }
    return keys;
  }

  @Override
  public @NonNull Collection<JsonDocument> documents() {
    Collection<JsonDocument> documents = new ArrayList<>();
    try (var cursor = this.collection.find().iterator()) {
      while (cursor.hasNext()) {
        documents.add(JsonDocument.fromJsonString(cursor.next().get(VALUE_NAME, Document.class).toJson()));
      }
    }
    return documents;
  }

  @Override
  public @NonNull Map<String, JsonDocument> entries() {
    Map<String, JsonDocument> entries = new HashMap<>();
    try (var cursor = this.collection.find().iterator()) {
      while (cursor.hasNext()) {
        var document = cursor.next();
        var value = JsonDocument.fromJsonString(document.get(VALUE_NAME, Document.class).toJson());

        entries.put(document.getString(KEY_NAME), value);
      }
    }
    return entries;
  }

  @Override
  public void iterate(@NonNull BiConsumer<String, JsonDocument> consumer) {
    this.entries().forEach(consumer);
  }

  @Override
  public void clear() {
    this.collection.deleteMany(new Document());
  }

  @Override
  public long documentCount() {
    return this.collection.estimatedDocumentCount();
  }

  @Override
  public boolean synced() {
    return true;
  }

  @Override
  public @Nullable Map<String, JsonDocument> readChunk(long beginIndex, int chunkSize) {
    Map<String, JsonDocument> result = new HashMap<>();
    try (var cursor = this.collection.find().skip((int) beginIndex).limit(chunkSize).iterator()) {
      while (cursor.hasNext()) {
        var document = cursor.next();
        var key = document.getString(KEY_NAME);
        var value = JsonDocument.fromJsonString(document.get(VALUE_NAME, Document.class).toJson());

        result.put(key, value);
      }
    }

    return result.isEmpty() ? null : result;
  }

  @Override
  public void close() {
  }

  protected @NonNull <T> Bson valueEq(@NonNull String fieldName, @Nullable final T value) {
    return Filters.eq(VALUE_NAME + '.' + fieldName, value);
  }
}
