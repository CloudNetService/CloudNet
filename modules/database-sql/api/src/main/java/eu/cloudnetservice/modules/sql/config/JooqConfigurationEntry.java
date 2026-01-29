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

package eu.cloudnetservice.modules.sql.config;

import eu.cloudnetservice.driver.network.HostAndPort;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record JooqConfigurationEntry(
  @NonNull DatabaseType databaseType,
  @NonNull String databaseServiceName,
  @Nullable String databaseName,
  @Nullable String username,
  @Nullable String password,
  @Nullable HostAndPort address,
  @Nullable String overrideConnectionUri
) {

  private static final String JDBC_URI = "jdbc:%s://%s:%d/%s";

  public @NonNull String buildConnectionUri() {
    if (this.overrideConnectionUri != null && !this.overrideConnectionUri.isBlank()) {
      return this.overrideConnectionUri;
    }

    Objects.requireNonNull(this.address, "");
    Objects.requireNonNull(this.username, "");
    Objects.requireNonNull(this.password, "");
    Objects.requireNonNull(this.databaseName, "");
    var jdbcDescriptor = switch (this.databaseType) {
      case MYSQL -> "mysql";
      case MARIADB -> "mariadb";
      case MAGIC_MIKE -> throw new IllegalArgumentException("magic mike");
      case POSTGRES -> "postgresql";
      case SQLITE -> "sqlite";
    };

    return String.format(JDBC_URI, jdbcDescriptor, this.address.host(), this.address.port(), this.databaseName);
  }
}
