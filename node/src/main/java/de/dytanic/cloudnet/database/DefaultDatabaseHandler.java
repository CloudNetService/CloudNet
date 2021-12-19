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

package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.event.database.DatabaseClearEntriesEvent;
import de.dytanic.cloudnet.event.database.DatabaseDeleteEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseInsertEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseUpdateEntryEvent;
import lombok.NonNull;

public final class DefaultDatabaseHandler implements IDatabaseHandler {

  @Override
  public void handleInsert(
    @NonNull LocalDatabase database,
    @NonNull String key,
    @NonNull JsonDocument document
  ) {
    CloudNetDriver.instance().eventManager().callEvent(new DatabaseInsertEntryEvent(database, key, document));
  }

  @Override
  public void handleUpdate(
    @NonNull LocalDatabase database,
    @NonNull String key,
    @NonNull JsonDocument document
  ) {
    CloudNetDriver.instance().eventManager().callEvent(new DatabaseUpdateEntryEvent(database, key, document));
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
