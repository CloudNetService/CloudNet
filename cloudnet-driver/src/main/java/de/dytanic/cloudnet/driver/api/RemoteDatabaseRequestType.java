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

package de.dytanic.cloudnet.driver.api;

public enum RemoteDatabaseRequestType {

  CONTAINS_DATABASE(false),
  DELETE_DATABASE(false),
  GET_DATABASES(false),

  DATABASE_INSERT(true),
  DATABASE_UPDATE(true),
  DATABASE_CONTAINS(true),
  DATABASE_DELETE(true),
  DATABASE_GET_BY_KEY(true),
  DATABASE_GET_BY_FIELD(true),
  DATABASE_GET_BY_FILTERS(true),
  DATABASE_KEYS(true),
  DATABASE_DOCUMENTS(true),
  DATABASE_ENTRIES(true),
  DATABASE_CLEAR(true),
  DATABASE_CLOSE(true),
  DATABASE_COUNT_DOCUMENTS(true);

  private final boolean databaseSpecific;

  RemoteDatabaseRequestType(boolean databaseSpecific) {
    this.databaseSpecific = databaseSpecific;
  }

  public boolean isDatabaseSpecific() {
    return this.databaseSpecific;
  }
}
