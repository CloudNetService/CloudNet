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

package eu.cloudnetservice.node.impl.database.h2;

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
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class H2Database extends SQLDatabase {

  public H2Database(@NonNull SQLDatabaseProvider provider, @NonNull String name) {
    super(provider, name);

    // create the table
    provider.executeUpdate(String.format(
      "CREATE TABLE IF NOT EXISTS `%s` (%s VARCHAR(64) PRIMARY KEY, %s TEXT);",
      name,
      TABLE_COLUMN_KEY,
      TABLE_COLUMN_VAL));
  }

  @Override
  public void close() {
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull Document document) {
    return this.insertOrUpdate(key, document);
  }

  private boolean insert0(@NonNull String key, @NonNull Document document) {
    return this.databaseProvider.executeUpdate(
      "INSERT INTO `" + this.name + "` (" + TABLE_COLUMN_KEY + "," + TABLE_COLUMN_VAL + ") VALUES (?, ?);",
      key, this.serializeDocumentToJsonString(document)
    ) != -1;
  }

  public boolean update0(String key, Document document) {
    return this.databaseProvider.executeUpdate(
      "UPDATE `" + this.name + "` SET " + TABLE_COLUMN_VAL + "=? WHERE " + TABLE_COLUMN_KEY + "=?",
      this.serializeDocumentToJsonString(document), key
    ) != -1;
  }

  public boolean insertOrUpdate(String key, Document document) {
    return this.contains(key) ? this.update0(key, document) : this.insert0(key, document);
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s` WHERE %s = ?", TABLE_COLUMN_KEY, this.name, TABLE_COLUMN_KEY),
      ResultSet::next,
      false,
      key);
  }

  @Override
  public boolean delete(@NonNull String key) {
    return this.delete0(key);
  }

  public boolean delete0(String key) {
    return this.databaseProvider.executeUpdate(
      String.format("DELETE FROM `%s` WHERE %s = ?", this.name, TABLE_COLUMN_KEY),
      key
    ) > 0;
  }

  @Override
  public @Nullable Document get(@NonNull String key) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s` WHERE %s = ?", TABLE_COLUMN_VAL, this.name, TABLE_COLUMN_KEY),
      resultSet -> resultSet.next() ? DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)) : null,
      null,
      key
    );
  }

  @Override
  public @NonNull List<Document> find(@NonNull String fieldName, String fieldValue) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s` WHERE %s LIKE ? ESCAPE '$'", TABLE_COLUMN_VAL, this.name, TABLE_COLUMN_VAL),
      resultSet -> {
        List<Document> jsonDocuments = new ArrayList<>();
        while (resultSet.next()) {
          jsonDocuments.add(DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }

        return jsonDocuments;
      },
      List.of(),
      "%\"" + fieldName + "\":" + Objects.toString(fieldValue).replaceAll("([_%])", "\\$$1") + "%");
  }

  @Override
  public @NonNull List<Document> find(@NonNull Map<String, String> filters) {
    var stringBuilder = new StringBuilder("SELECT ")
      .append(TABLE_COLUMN_VAL).append(" FROM `")
      .append(this.name).append('`');

    Collection<String> collection = new ArrayList<>();
    if (filters.size() > 0) {
      stringBuilder.append(" WHERE ");

      var iterator = filters.entrySet().iterator();

      while (iterator.hasNext()) {
        var entry = iterator.next();

        stringBuilder.append(TABLE_COLUMN_VAL).append(" LIKE ? ESCAPE '$'");
        collection.add("%\"" + entry.getKey() + "\":\"" + entry.getValue().replaceAll("([_%])", "\\$$1") + "\"%");

        if (iterator.hasNext()) {
          stringBuilder.append(" and ");
        }
      }
    }

    return this.databaseProvider.executeQuery(
      stringBuilder.append(";").toString(),
      resultSet -> {
        List<Document> jsonDocuments = new ArrayList<>();
        while (resultSet.next()) {
          jsonDocuments.add(DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }

        return jsonDocuments;
      },
      List.of(),
      collection.toArray()
    );
  }

  @Override
  public @NonNull Collection<String> keys() {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s`;", TABLE_COLUMN_KEY, this.name),
      resultSet -> {
        Collection<String> keys = new ArrayList<>();
        while (resultSet.next()) {
          keys.add(resultSet.getString(TABLE_COLUMN_KEY));
        }

        return keys;
      }, Set.of());
  }

  @Override
  public @NonNull Collection<Document> documents() {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s`;", TABLE_COLUMN_VAL, this.name),
      resultSet -> {
        Collection<Document> documents = new ArrayList<>();
        while (resultSet.next()) {
          documents.add(DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }

        return documents;
      }, Set.of());
  }

  @Override
  public @NonNull Map<String, Document> entries() {
    return this.databaseProvider.executeQuery(
      String.format("SELECT * FROM `%s`;", this.name),
      resultSet -> {
        Map<String, Document> map = new WeakHashMap<>();
        while (resultSet.next()) {
          map.put(
            resultSet.getString(TABLE_COLUMN_KEY),
            DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL)));
        }

        return map;
      }, Map.of());
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
  public void clear() {
    this.databaseProvider.executeUpdate(String.format("TRUNCATE TABLE `%s`", this.name));
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
    return false;
  }

  @Override
  public @Nullable Map<String, Document> readChunk(long beginIndex, int chunkSize) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT * FROM `%s` ORDER BY `%s` OFFSET ? LIMIT ?;", this.name, TABLE_COLUMN_KEY),
      resultSet -> {
        Map<String, Document> result = new HashMap<>();
        while (resultSet.next()) {
          var key = resultSet.getString(TABLE_COLUMN_KEY);
          var document = DocumentFactory.json().parse(resultSet.getString(TABLE_COLUMN_VAL));
          result.put(key, document);
        }

        return result.isEmpty() ? null : result;
      },
      null,
      beginIndex, chunkSize
    );
  }
}
