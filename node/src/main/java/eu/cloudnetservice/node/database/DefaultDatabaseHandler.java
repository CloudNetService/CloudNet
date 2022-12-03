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
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.node.event.database.DatabaseClearEntriesEvent;
import eu.cloudnetservice.node.event.database.DatabaseDeleteEntryEvent;
import eu.cloudnetservice.node.event.database.DatabaseInsertEntryEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class DefaultDatabaseHandler implements DatabaseHandler {

  private final EventManager eventManager;

  @Inject
  public DefaultDatabaseHandler(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  @Override
  public void handleInsert(
    @NonNull LocalDatabase database,
    @NonNull String key,
    @NonNull JsonDocument document
  ) {
    this.eventManager.callEvent(new DatabaseInsertEntryEvent(database, key, document));
  }

  @Override
  public void handleDelete(@NonNull LocalDatabase database, @NonNull String key) {
    this.eventManager.callEvent(new DatabaseDeleteEntryEvent(database, key));
  }

  @Override
  public void handleClear(@NonNull LocalDatabase database) {
    this.eventManager.callEvent(new DatabaseClearEntriesEvent(database));
  }
}
