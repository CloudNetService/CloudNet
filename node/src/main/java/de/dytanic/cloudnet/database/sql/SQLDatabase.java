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

package de.dytanic.cloudnet.database.sql;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabase;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import lombok.NonNull;

public abstract class SQLDatabase extends AbstractDatabase {

  protected static final String TABLE_COLUMN_KEY = "Name";
  protected static final String TABLE_COLUMN_VALUE = "Document";

  protected final String name;
  protected final SQLDatabaseProvider databaseProvider;

  protected final long cacheTimeoutTime;
  protected final ExecutorService executorService;

  public SQLDatabase(
    @NonNull SQLDatabaseProvider databaseProvider,
    @NonNull String name,
    long cacheRemovalDelay,
    @NonNull ExecutorService executorService
  ) {
    super(name, executorService, databaseProvider);

    this.name = name;
    this.databaseProvider = databaseProvider;

    this.executorService = executorService;
    this.cacheTimeoutTime = System.currentTimeMillis() + cacheRemovalDelay;

    databaseProvider.executeUpdate(String.format(
      "CREATE TABLE IF NOT EXISTS `%s` (%s VARCHAR(64) PRIMARY KEY, %s TEXT);",
      name, TABLE_COLUMN_KEY, TABLE_COLUMN_VALUE
    ));
  }

  @Override
  public void close() {
    this.databaseProvider.cachedDatabaseInstances.remove(this.name);
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull JsonDocument document) {
    this.databaseProvider.databaseHandler().handleInsert(this, key, document);
    return this.insertOrUpdate(key, document);
  }

  private boolean insert0(@NonNull String key, @NonNull JsonDocument document) {
    return this.databaseProvider.executeUpdate(
      "INSERT INTO `" + this.name + "` (" + TABLE_COLUMN_KEY + "," + TABLE_COLUMN_VALUE + ") VALUES (?, ?);",
      key, document.toString()
    ) != -1;
  }

  @Override
  public boolean update(@NonNull String key, @NonNull JsonDocument document) {
    this.databaseProvider.databaseHandler().handleUpdate(this, key, document);
    return this.insertOrUpdate(key, document);
  }

  public boolean update0(String key, JsonDocument document) {
    return this.databaseProvider.executeUpdate(
      "UPDATE `" + this.name + "` SET " + TABLE_COLUMN_VALUE + "=? WHERE " + TABLE_COLUMN_KEY + "=?",
      document.toString(), key
    ) != -1;
  }

  public boolean insertOrUpdate(String key, JsonDocument document) {
    return this.contains(key) ? this.update0(key, document) : this.insert0(key, document);
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s` WHERE %s = ?", TABLE_COLUMN_KEY, this.name, TABLE_COLUMN_KEY),
      ResultSet::next,
      key
    );
  }

  @Override
  public boolean delete(@NonNull String key) {
    this.databaseProvider.databaseHandler().handleDelete(this, key);
    return this.delete0(key);
  }

  public boolean delete0(String key) {
    return this.databaseProvider.executeUpdate(
      String.format("DELETE FROM `%s` WHERE %s = ?", this.name, TABLE_COLUMN_KEY),
      key
    ) != -1;
  }

  @Override
  public JsonDocument get(String key) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s` WHERE %s = ?", TABLE_COLUMN_VALUE, this.name, TABLE_COLUMN_KEY),
      resultSet -> resultSet.next() ? JsonDocument.fromJsonString(resultSet.getString(TABLE_COLUMN_VALUE)) : null,
      key
    );
  }

  @Override
  public @NonNull List<JsonDocument> get(@NonNull String fieldName, Object fieldValue) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s` WHERE %s LIKE ?", TABLE_COLUMN_VALUE, this.name, TABLE_COLUMN_VALUE),
      resultSet -> {
        List<JsonDocument> jsonDocuments = new ArrayList<>();
        while (resultSet.next()) {
          jsonDocuments.add(JsonDocument.fromJsonString(resultSet.getString(TABLE_COLUMN_VALUE)));
        }

        return jsonDocuments;
      },
      "%\"" + fieldName + "\":" + JsonDocument.GSON.toJson(fieldValue) + "%"
    );
  }

  @Override
  public @NonNull List<JsonDocument> get(@NonNull JsonDocument filters) {
    var stringBuilder = new StringBuilder("SELECT ").append(TABLE_COLUMN_VALUE).append(" FROM `")
      .append(this.name).append('`');

    Collection<String> collection = new ArrayList<>();

    if (filters.size() > 0) {
      stringBuilder.append(" WHERE ");

      var iterator = filters.iterator();
      String item;

      while (iterator.hasNext()) {
        item = iterator.next();

        stringBuilder.append(TABLE_COLUMN_VALUE).append(" LIKE ?");
        collection.add("%\"" + item + "\":" + filters.get(item) + "%");

        if (iterator.hasNext()) {
          stringBuilder.append(" and ");
        }
      }
    }

    return this.databaseProvider.executeQuery(
      stringBuilder.append(";").toString(),
      resultSet -> {
        List<JsonDocument> jsonDocuments = new ArrayList<>();
        while (resultSet.next()) {
          jsonDocuments.add(JsonDocument.fromJsonString(resultSet.getString(TABLE_COLUMN_VALUE)));
        }

        return jsonDocuments;
      },
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
      }
    );
  }

  @Override
  public @NonNull Collection<JsonDocument> documents() {
    return this.databaseProvider.executeQuery(
      String.format("SELECT %s FROM `%s`;", TABLE_COLUMN_VALUE, this.name),
      resultSet -> {
        Collection<JsonDocument> documents = new ArrayList<>();
        while (resultSet.next()) {
          documents.add(JsonDocument.fromJsonString(resultSet.getString(TABLE_COLUMN_VALUE)));
        }

        return documents;
      }
    );
  }

  @Override
  public @NonNull Map<String, JsonDocument> entries() {
    return this.databaseProvider.executeQuery(
      String.format("SELECT * FROM `%s`;", this.name),
      resultSet -> {
        Map<String, JsonDocument> map = new WeakHashMap<>();
        while (resultSet.next()) {
          map.put(resultSet.getString(TABLE_COLUMN_KEY),
            JsonDocument.fromJsonString(resultSet.getString(TABLE_COLUMN_VALUE)));
        }

        return map;
      }
    );
  }

  @Override
  public @NonNull Map<String, JsonDocument> filter(@NonNull BiPredicate<String, JsonDocument> predicate) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT * FROM `%s`;", this.name),
      resultSet -> {
        Map<String, JsonDocument> map = new HashMap<>();
        while (resultSet.next()) {
          var key = resultSet.getString(TABLE_COLUMN_KEY);
          var document = JsonDocument.fromJsonString(resultSet.getString(TABLE_COLUMN_VALUE));

          if (predicate.test(key, document)) {
            map.put(key, document);
          }
        }

        return map;
      }
    );
  }

  @Override
  public void iterate(@NonNull BiConsumer<String, JsonDocument> consumer) {
    this.databaseProvider.executeQuery(
      String.format("SELECT * FROM `%s`;", this.name),
      resultSet -> {
        while (resultSet.next()) {
          var key = resultSet.getString(TABLE_COLUMN_KEY);
          var document = JsonDocument.fromJsonString(resultSet.getString(TABLE_COLUMN_VALUE));
          consumer.accept(key, document);
        }

        return null;
      }
    );
  }

  @Override
  public void clear() {
    this.databaseProvider.databaseHandler().handleClear(this);
    this.databaseProvider.executeUpdate(String.format("TRUNCATE TABLE `%s`", this.name));
  }

  @Override
  public long documentCount() {
    return this.databaseProvider.executeQuery("SELECT COUNT(*) FROM `" + this.name + "`;", resultSet -> {
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
      return -1L;
    });
  }
}
