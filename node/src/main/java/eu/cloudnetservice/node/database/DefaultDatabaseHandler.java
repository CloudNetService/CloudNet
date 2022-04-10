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

package eu.cloudnetservice.node.database;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.node.event.database.DatabaseClearEntriesEvent;
import eu.cloudnetservice.node.event.database.DatabaseDeleteEntryEvent;
import eu.cloudnetservice.node.event.database.DatabaseInsertEntryEvent;
import lombok.NonNull;

public final class DefaultDatabaseHandler implements DatabaseHandler {

  @Override
  public void handleInsert(
    @NonNull LocalDatabase database,
    @NonNull String key,
    @NonNull JsonDocument document
  ) {
    CloudNetDriver.instance().eventManager().callEvent(new DatabaseInsertEntryEvent(database, key, document));
  }

  @Override
  public void handleDelete(@NonNull LocalDatabase database, @NonNull String key) {
    CloudNetDriver.instance().eventManager().callEvent(new DatabaseDeleteEntryEvent(database, key));
  }

  @Override
  public void handleClear(@NonNull LocalDatabase database) {
    CloudNetDriver.instance().eventManager().callEvent(new DatabaseClearEntriesEvent(database));
  }
}
