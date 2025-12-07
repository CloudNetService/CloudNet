/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.postgres.impl;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.node.impl.database.sql.SQLDatabase;
import eu.cloudnetservice.node.impl.database.sql.SQLDatabaseProvider;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class PostgresDatabase extends SQLDatabase {

  public PostgresDatabase(@NonNull SQLDatabaseProvider provider, @NonNull String name) {
    super(provider, name);

    // create the table with jsonb column
    provider.executeUpdate(String.format(
      "CREATE TABLE IF NOT EXISTS \"%s\" (\"%s\" VARCHAR(512) PRIMARY KEY, \"%s\" JSONB NOT NULL);",
      name,
      TABLE_COLUMN_KEY,
      TABLE_COLUMN_VAL));
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull Document document) {
    var json = this.serializeDocumentToJsonString(document);
    // upsert using ON CONFLICT
    return this.databaseProvider.executeUpdate(
      String.format(
        "INSERT INTO \"%s\" (\"%s\", \"%s\") VALUES (?, ?::jsonb) ON CONFLICT (\"%s\") DO UPDATE SET \"%s\" = EXCLUDED.\"%s\";",
        this.name,
        TABLE_COLUMN_KEY,
        TABLE_COLUMN_VAL,
        TABLE_COLUMN_KEY,
        TABLE_COLUMN_VAL,
        TABLE_COLUMN_VAL),
      key, json) > 0;
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT 1 FROM \"%s\" WHERE \"%s\" = ?;", this.name, TABLE_COLUMN_KEY),
      ResultSet::next,
      false,
      key);
  }

  @Override
  public boolean delete(@NonNull String key) {
    return this.databaseProvider.executeUpdate(
      String.format("DELETE FROM \"%s\" WHERE \"%s\" = ?;", this.name, TABLE_COLUMN_KEY),
      key) > 0;
  }

  @Override
  public @Nullable Document get(@NonNull String key) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT \"%s\" FROM \"%s\" WHERE \"%s\" = ?;", TABLE_COLUMN_VAL, this.name, TABLE_COLUMN_KEY),
      resultSet -> {
        if (resultSet.next()) {
          return DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL));
        }
        return null;
      }, null, key);
  }

  @Override
  public @NonNull Collection<Document> find(@NonNull String fieldName, @Nullable String fieldValue) {
    return this.databaseProvider.executeQuery(
      String.format(
        "SELECT \"%s\" FROM \"%s\" WHERE \"%s\" @> ?::jsonb;",
        TABLE_COLUMN_VAL,
        this.name,
        TABLE_COLUMN_VAL),
      resultSet -> {
        List<Document> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }
        return results;
      },
      List.of(),
      DocumentFactory.json().newDocument(fieldName, Objects.toString(fieldValue)).serializeToString());
  }

  @Override
  public @NonNull Collection<Document> find(@NonNull Map<String, String> filters) {
    if (filters.isEmpty()) {
      return List.of();
    }
    var whereDoc = DocumentFactory.json().newDocument();
    for (var entry : filters.entrySet()) {
      whereDoc.append(entry.getKey(), entry.getValue());
    }
    var whereJson = whereDoc.serializeToString();

    return this.databaseProvider.executeQuery(
      String.format("SELECT \"%s\" FROM \"%s\" WHERE \"%s\" @> ?::jsonb;", TABLE_COLUMN_VAL, this.name, TABLE_COLUMN_VAL),
      resultSet -> {
        List<Document> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }
        return results;
      },
      List.of(),
      whereJson);
  }

  @Override
  public @NonNull Collection<String> keys() {
    return this.databaseProvider.executeQuery(
      String.format("SELECT \"%s\" FROM \"%s\";", TABLE_COLUMN_KEY, this.name),
      resultSet -> {
        List<String> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(resultSet.getString(TABLE_COLUMN_KEY));
        }
        return results;
      }, Set.of());
  }

  @Override
  public @NonNull Collection<Document> documents() {
    return this.databaseProvider.executeQuery(
      String.format("SELECT \"%s\" FROM \"%s\";", TABLE_COLUMN_VAL, this.name),
      resultSet -> {
        List<Document> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }
        return results;
      }, Set.of());
  }

  @Override
  public @NonNull Map<String, Document> entries() {
    return this.databaseProvider.executeQuery(
      String.format("SELECT \"%s\", \"%s\" FROM \"%s\";", TABLE_COLUMN_KEY, TABLE_COLUMN_VAL, this.name),
      resultSet -> {
        Map<String, Document> results = new HashMap<>();
        while (resultSet.next()) {
          results.put(resultSet.getString(TABLE_COLUMN_KEY), DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }
        return results;
      }, Map.of());
  }

  @Override
  public void clear() {
    this.databaseProvider.executeUpdate(String.format("TRUNCATE TABLE \"%s\";", this.name));
  }

  @Override
  public long documentCount() {
    return this.databaseProvider.executeQuery("SELECT COUNT(*) FROM \"" + this.name + "\";", resultSet -> {
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
      return -1L;
    }, -1L);
  }

  @Override
  public boolean synced() {
    return true;
  }

  @Override
  public void iterate(@NonNull BiConsumer<String, Document> consumer) {
    this.databaseProvider.executeQuery(
      String.format("SELECT \"%s\", \"%s\" FROM \"%s\";", TABLE_COLUMN_KEY, TABLE_COLUMN_VAL, this.name),
      resultSet -> {
        while (resultSet.next()) {
          consumer.accept(resultSet.getString(TABLE_COLUMN_KEY), DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }
        return null;
      }, null);
  }

  @Override
  public @Nullable Map<String, Document> readChunk(long beginIndex, int chunkSize) {
    return this.databaseProvider.executeQuery(
      String.format(
        "SELECT \"%s\", \"%s\" FROM \"%s\" ORDER BY \"%s\" LIMIT ? OFFSET ?;",
        TABLE_COLUMN_KEY,
        TABLE_COLUMN_VAL,
        this.name,
        TABLE_COLUMN_KEY),
      resultSet -> {
        Map<String, Document> result = new HashMap<>();
        while (resultSet.next()) {
          result.put(resultSet.getString(TABLE_COLUMN_KEY), DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }
        return result.isEmpty() ? null : result;
      }, null, chunkSize, beginIndex);
  }

  @Override
  public void close() {
  }
}
