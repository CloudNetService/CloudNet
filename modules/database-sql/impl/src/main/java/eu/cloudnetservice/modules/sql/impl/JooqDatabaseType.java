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

package eu.cloudnetservice.modules.sql.impl;

import eu.cloudnetservice.modules.sql.config.DatabaseType;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;
import org.jooq.SQLDialect;

public enum JooqDatabaseType {
  MYSQL("com.mysql.cj.jdbc.Driver", SQLDialect.MYSQL),
  MARIADB("org.mariadb.jdbc.Driver", SQLDialect.MARIADB),
  POSTGRESQL("org.postgresql.Driver", SQLDialect.POSTGRES),
  SQLITE("org.sqlite.JDBC", SQLDialect.SQLITE);

  private final String driverClassName;
  private final SQLDialect jooqDialect;

  JooqDatabaseType(@NonNull String driverClassName, @NonNull SQLDialect jooqDialect) {
    this.driverClassName = driverClassName;
    this.jooqDialect = jooqDialect;
  }

  public static @NonNull JooqDatabaseType fromDatabaseType(@UnknownNullability DatabaseType databaseType) {
    return switch (databaseType) {
      case MYSQL -> MYSQL;
      case MARIADB -> MARIADB;
      case POSTGRES -> POSTGRESQL;
      case SQLITE -> SQLITE;
      case MAGIC_MIKE -> throw new IllegalArgumentException("magic mix");
    };
  }

  public @NonNull String driverClassName() {
    return this.driverClassName;
  }

  public @NonNull SQLDialect jooqDialect() {
    return this.jooqDialect;
  }
}
