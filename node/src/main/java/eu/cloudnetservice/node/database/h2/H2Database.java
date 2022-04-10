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

package eu.cloudnetservice.node.database.h2;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.node.database.sql.SQLDatabase;
import eu.cloudnetservice.node.database.sql.SQLDatabaseProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class H2Database extends SQLDatabase {

  public H2Database(@NonNull SQLDatabaseProvider provider, @NonNull String name, @NonNull ExecutorService executor) {
    super(provider, name, executor);
  }

  @Override
  public boolean synced() {
    return false;
  }

  @Override
  public @Nullable Map<String, JsonDocument> readChunk(long beginIndex, int chunkSize) {
    return this.databaseProvider.executeQuery(
      String.format("SELECT * FROM `%s` ORDER BY `%s` OFFSET ? LIMIT ?;", this.name, TABLE_COLUMN_KEY),
      resultSet -> {
        Map<String, JsonDocument> result = new HashMap<>();
        while (resultSet.next()) {
          var key = resultSet.getString(TABLE_COLUMN_KEY);
          var document = JsonDocument.fromJsonString(resultSet.getString(TABLE_COLUMN_VAL));
          result.put(key, document);
        }

        return result.isEmpty() ? null : result;
      },
      null,
      beginIndex, chunkSize
    );
  }
}
