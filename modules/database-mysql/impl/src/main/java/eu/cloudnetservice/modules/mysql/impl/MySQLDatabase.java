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

package eu.cloudnetservice.modules.mysql.impl;

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

public final class MySQLDatabase extends SQLDatabase {

  public MySQLDatabase(@NonNull SQLDatabaseProvider provider, @NonNull String name) {
    super(provider, name);

    // create the table
    provider.executeUpdate(String.format(
      "CREATE TABLE IF NOT EXISTS `%s` (%s VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY, %s JSON NOT NULL);",
      name,
      TABLE_COLUMN_KEY,
      TABLE_COLUMN_VAL));

    // alter mysql tables
    provider.executeUpdate(String.format("ALTER TABLE `%s` MODIFY `%s` VARCHAR(512), MODIFY %s JSON NOT NULL",
      name,
      TABLE_COLUMN_KEY,
      TABLE_COLUMN_VAL));
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull Document document) {
    var serializedDocument = this.serializeDocumentToJsonString(document);
    return this.databaseProvider.executeUpdate(
      String.format(
        "INSERT INTO `%s` (%s, %s) VALUES (?, ?) ON DUPLICATE KEY UPDATE %s = ?;",
        this.name,
        TABLE_COLUMN_KEY,
        TABLE_COLUMN_VAL,
        TABLE_COLUMN_VAL),
      key, serializedDocument, serializedDocument) > 0;
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s` WHERE %s = ?;", TABLE_COLUMN_KEY, this.name, TABLE_COLUMN_KEY),
      ResultSet::next,
      false,
      key);
  }

  @Override
  public boolean delete(@NonNull String key) {
    return this.databaseProvider.executeUpdate(
      String.format("DELETE FROM %s WHERE `%s` = ?;", this.name, TABLE_COLUMN_KEY),
      key) > 0;
  }

  @Override
  public @Nullable Document get(@NonNull String key) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s` WHERE %s = ?;", TABLE_COLUMN_VAL, this.name, TABLE_COLUMN_KEY),
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
        "SELECT %s FROM `%s` WHERE JSON_SEARCH(%s, 'one', '%s', NULL, '$.%s') IS NOT NULL;",
        TABLE_COLUMN_VAL,
        this.name,
        TABLE_COLUMN_VAL,
        Objects.toString(fieldValue).replaceAll("([_%])", "\\\\$1"),
        fieldName),
      resultSet -> {
        List<Document> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }

        return results;
      }, List.of());
  }

  @Override
  public @NonNull Collection<Document> find(@NonNull Map<String, String> filters) {
    var stringBuilder = new StringBuilder("SELECT ")
      .append(TABLE_COLUMN_VAL)
      .append(" FROM `")
      .append(this.name)
      .append('`');

    if (!filters.isEmpty()) {
      stringBuilder.append(" WHERE ");
      var iterator = filters.entrySet().iterator();
      while (iterator.hasNext()) {
        var entry = iterator.next();
        stringBuilder
          .append("JSON_SEARCH(")
          .append(TABLE_COLUMN_VAL)
          .append(", 'one', '")
          .append(entry.getValue().replaceAll("([_%])", "\\\\$1"))
          .append("', NULL, '$.")
          .append(entry.getKey())
          .append("') IS NOT NULL")
          .append(iterator.hasNext() ? " AND " : ';');
      }
    }

    return this.databaseProvider.executeQuery(stringBuilder.toString(), resultSet -> {
      List<Document> results = new ArrayList<>();
      while (resultSet.next()) {
        results.add(DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
      }

      return results;
    }, List.of());
  }

  @Override
  public @NonNull Collection<String> keys() {
    return this.databaseProvider.executeQuery(String.format("SELECT %s FROM `%s`;", TABLE_COLUMN_KEY, this.name),
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
    return this.databaseProvider.executeQuery(String.format("SELECT %s FROM `%s`;", TABLE_COLUMN_VAL, this.name),
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
    return this.databaseProvider.executeQuery(String.format("SELECT * FROM `%s`;", this.name), resultSet -> {
      Map<String, Document> results = new HashMap<>();
      while (resultSet.next()) {
        results.put(
          resultSet.getString(TABLE_COLUMN_KEY),
          DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
      }

      return results;
    }, Map.of());
  }

  @Override
  public void clear() {
    this.databaseProvider.executeUpdate(String.format("TRUNCATE TABLE `%s`;", this.name));
  }

  @Override
  public long documentCount() {
    return this.databaseProvider.executeQuery("SELECT COUNT(*) FROM `" + this.name + "`;", resultSet -> {
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
      String.format("SELECT * FROM `%s`;", this.name),
      resultSet -> {
        while (resultSet.next()) {
          var key = resultSet.getString(TABLE_COLUMN_KEY);
          var document = DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL));
          consumer.accept(key, document);
        }

        return null;
      }, null);
  }

  @Override
  public @Nullable Map<String, Document> readChunk(long beginIndex, int chunkSize) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT * FROM `%s` ORDER BY `%s` LIMIT ? OFFSET ?;", this.name, TABLE_COLUMN_KEY),
      resultSet -> {
        Map<String, Document> result = new HashMap<>();
        while (resultSet.next()) {
          var key = resultSet.getString(TABLE_COLUMN_KEY);
          var document = DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL));
          result.put(key, document);
        }

        return result.isEmpty() ? null : result;
      }, null, chunkSize, beginIndex);
  }

  @Override
  public void close() {
  }
}
