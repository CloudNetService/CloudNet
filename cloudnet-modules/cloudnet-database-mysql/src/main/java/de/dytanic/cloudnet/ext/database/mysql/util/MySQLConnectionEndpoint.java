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

package de.dytanic.cloudnet.ext.database.mysql.util;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class MySQLConnectionEndpoint {

  private final boolean useSsl;

  private final String database;

  private final HostAndPort address;

  public MySQLConnectionEndpoint(boolean useSsl, String database, HostAndPort address) {
    this.useSsl = useSsl;
    this.database = database;
    this.address = address;
  }

  public boolean isUseSsl() {
    return this.useSsl;
  }

  public String getDatabase() {
    return this.database;
  }

  public HostAndPort getAddress() {
    return this.address;
  }

}
