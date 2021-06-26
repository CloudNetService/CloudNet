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

package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;

abstract class DatabaseEvent extends DriverEvent {

  private final IDatabase database;

  public DatabaseEvent(IDatabase database) {
    this.database = database;
  }

  public IDatabase getDatabase() {
    return this.database;
  }
}
