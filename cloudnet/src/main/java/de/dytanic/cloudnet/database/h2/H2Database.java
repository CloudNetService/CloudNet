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

package de.dytanic.cloudnet.database.h2;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.sql.SQLDatabase;
import java.util.concurrent.ExecutorService;

public final class H2Database extends SQLDatabase {

  public H2Database(H2DatabaseProvider databaseProvider, String name, ExecutorService executorService) {
    super(databaseProvider, name, executorService);
  }

  @Override
  public boolean isSynced() {
    return false;
  }

  @Override
  public boolean insertOrUpdate(String key, JsonDocument document) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(document);

    return this.databaseProvider.executeUpdate(
      String.format("MERGE INTO `%s` (%s, %s) VALUES (?, ?);", this.name, TABLE_COLUMN_KEY, TABLE_COLUMN_VALUE),
      key, document.toString()
    ) != -1;
  }
}
