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

package eu.cloudnetservice.modules.mongodb.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.node.impl.database.AbstractDatabase;
import eu.cloudnetservice.node.impl.database.AbstractNodeDatabaseProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

public class MongoDBDatabase extends AbstractDatabase {

  protected static final String KEY_NAME = "Key";
  protected static final String VALUE_NAME = "Value";

  protected static final IndexOptions UNIQUE_KEY_OPTIONS = new IndexOptions().unique(true);
  protected static final UpdateOptions INSERT_OR_REPLACE_OPTIONS = new UpdateOptions().upsert(true);

  protected final MongoCollection<org.bson.Document> collection;

  protected MongoDBDatabase(
    @NonNull String name,
    @NonNull MongoCollection<org.bson.Document> collection,
    @NonNull AbstractNodeDatabaseProvider provider
  ) {
    super(name, provider);

    this.collection = collection;
    this.collection.createIndex(Indexes.ascending(KEY_NAME), UNIQUE_KEY_OPTIONS);
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull Document document) {
    return this.insertOrUpdate(key, document);
  }

  protected boolean insertOrUpdate(String key, Document document) {
    var result = this.collection.updateOne(
      Filters.eq(KEY_NAME, key),
      Updates.combine(
        Updates.setOnInsert(new org.bson.Document(KEY_NAME, key)),
        Updates.set(VALUE_NAME, org.bson.Document.parse(this.serializeDocumentToJsonString(document)))
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
  public @Nullable Document get(@NonNull String key) {
    var document = this.collection.find(Filters.eq(KEY_NAME, key)).first();
    return this.parseDocumentValue(document);
  }

  @Override
  public @NonNull List<Document> find(@NonNull String fieldName, @Nullable String fieldValue) {
    List<Document> documents = new ArrayList<>();
    try (var cursor = this.collection.find(this.valueEq(fieldName, fieldValue)).iterator()) {
      while (cursor.hasNext()) {
        var parsedDocument = this.parseDocumentValue(cursor.next());
        if (parsedDocument != null) {
          documents.add(parsedDocument);
        }
      }
    }

    return documents;
  }

  @Override
  public @NonNull List<Document> find(@NonNull Map<String, String> filters) {
    // the easiest way to prevent issues with json-to-json conversion is to use the in-build document of mongodb and
    // then reconvert the values as we need them
    Collection<Bson> bsonFilters = new ArrayList<>();
    for (var entry : filters.entrySet()) {
      bsonFilters.add(this.valueEq(entry.getKey(), entry.getValue()));
    }

    List<Document> documents = new ArrayList<>();
    try (var cursor = this.collection.find(Filters.and(bsonFilters)).iterator()) {
      while (cursor.hasNext()) {
        var parsedDocument = this.parseDocumentValue(cursor.next());
        if (parsedDocument != null) {
          documents.add(parsedDocument);
        }
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
  public @NonNull Collection<Document> documents() {
    Collection<Document> documents = new ArrayList<>();
    try (var cursor = this.collection.find().iterator()) {
      while (cursor.hasNext()) {
        var parsedDocument = this.parseDocumentValue(cursor.next());
        if (parsedDocument != null) {
          documents.add(parsedDocument);
        }
      }
    }

    return documents;
  }

  @Override
  public @NonNull Map<String, Document> entries() {
    Map<String, Document> entries = new HashMap<>();
    try (var cursor = this.collection.find().iterator()) {
      while (cursor.hasNext()) {
        var document = cursor.next();
        var parsedDocument = this.parseDocumentValue(document);
        if (parsedDocument != null) {
          var entryKey = document.getString(KEY_NAME);
          entries.put(entryKey, parsedDocument);
        }
      }
    }

    return entries;
  }

  @Override
  public void iterate(@NonNull BiConsumer<String, Document> consumer) {
    this.entries().forEach(consumer);
  }

  @Override
  public void clear() {
    this.collection.deleteMany(new org.bson.Document());
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
  public @Nullable Map<String, Document> readChunk(long beginIndex, int chunkSize) {
    Map<String, Document> result = new HashMap<>();
    try (var cursor = this.collection.find().skip((int) beginIndex).limit(chunkSize).iterator()) {
      while (cursor.hasNext()) {
        var document = cursor.next();
        var parsedDocument = this.parseDocumentValue(document);
        if (parsedDocument != null) {
          var entryKey = document.getString(KEY_NAME);
          result.put(entryKey, parsedDocument);
        }
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

  protected @Nullable Document parseDocumentValue(@Nullable org.bson.Document in) {
    if (in == null) {
      return null;
    }

    // get the actual document value and parse it
    var internalDocument = in.get(VALUE_NAME, org.bson.Document.class);
    return internalDocument == null ? null : DocumentFactory.json().parse(internalDocument.toJson());
  }
}
