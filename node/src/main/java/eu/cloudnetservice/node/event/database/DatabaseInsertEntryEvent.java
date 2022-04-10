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

package eu.cloudnetservice.node.event.database;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.database.Database;
import lombok.NonNull;

public class DatabaseInsertEntryEvent extends DatabaseEvent {

  private final String key;
  private final JsonDocument document;

  public DatabaseInsertEntryEvent(@NonNull Database database, @NonNull String key, @NonNull JsonDocument document) {
    super(database);

    this.key = key;
    this.document = document;
  }

  public @NonNull String key() {
    return this.key;
  }

  public @NonNull JsonDocument document() {
    return this.document;
  }
}
