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

package de.dytanic.cloudnet.ext.database.mysql;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.sql.SQLDatabase;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.jetbrains.annotations.Nullable;

public final class MySQLDatabase extends SQLDatabase {

  public MySQLDatabase(MySQLDatabaseProvider databaseProvider, String name, ExecutorService executorService) {
    super(databaseProvider, name, executorService);
  }

  public MySQLDatabase(
    SQLDatabaseProvider databaseProvider,
    String name,
    long cacheRemovalDelay,
    ExecutorService executorService
  ) {
    super(databaseProvider, name, cacheRemovalDelay, executorService);
  }

  @Override
  public boolean isSynced() {
    return true;
  }

  @Override
  public @Nullable Map<String, JsonDocument> readChunk(long beginIndex, int chunkSize) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT * FROM `%s` WHERE ROW_NUMBER() BETWEEN ? AND ?;", this.name),
      resultSet -> {
        Map<String, JsonDocument> result = new HashMap<>();
        while (resultSet.next()) {
          String key = resultSet.getString(TABLE_COLUMN_KEY);
          JsonDocument document = JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE));
          result.put(key, document);
        }

        return result.isEmpty() ? null : result;
      },
      beginIndex, beginIndex + chunkSize
    );
  }
}
