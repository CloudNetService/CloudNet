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

package eu.cloudnetservice.modules.sql.impl.table;

import eu.cloudnetservice.driver.document.Document;
import lombok.NonNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class MariaDBTableCreator implements TableCreator {

  @Override
  public void createTable(
    @NonNull DSLContext dslContext,
    @NonNull String name,
    @NonNull Field<String> keyField,
    @NonNull Field<Document> documentField
  ) {
    dslContext.createTableIfNotExists(DSL.name(name))
      .column(keyField, SQLDataType.VARCHAR(512)
        .notNull()
        .collation(DSL.collation("utf8mb4_bin")))
      .column(documentField, SQLDataType.JSONB.notNull())
      .primaryKey(keyField)
      .execute();
  }
}
