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
import eu.cloudnetservice.modules.sql.config.SQLConfigurationEntry;
import eu.cloudnetservice.modules.sql.impl.table.MariaDBTableCreator;
import eu.cloudnetservice.modules.sql.impl.table.MySQLTableCreator;
import eu.cloudnetservice.modules.sql.impl.table.PostgreSQLTableCreator;
import eu.cloudnetservice.modules.sql.impl.table.SQLiteTableCreator;
import eu.cloudnetservice.modules.sql.impl.table.TableCreator;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;
import org.jooq.SQLDialect;

public enum JooqDatabaseType {
  MYSQL(
    "com.mysql.cj.jdbc.Driver",
    SQLDialect.MYSQL,
    true,
    new MySQLTableCreator()
  ),
  MARIADB(
    "org.mariadb.jdbc.Driver",
    SQLDialect.MARIADB,
    true,
    new MariaDBTableCreator()
  ),
  POSTGRESQL(
    "org.postgresql.Driver",
    SQLDialect.POSTGRES,
    true,
    new PostgreSQLTableCreator()
  ),
  SQLITE(
    "org.sqlite.JDBC",
    SQLDialect.SQLITE,
    false,
    new SQLiteTableCreator()
  );

  private final String driverClassName;
  private final SQLDialect jooqDialect;
  private final boolean synced;
  private final TableCreator tableCreator;

  JooqDatabaseType(
    @NonNull String driverClassName,
    @NonNull SQLDialect jooqDialect,
    boolean synced,
    @NonNull TableCreator tableCreator
  ) {
    this.driverClassName = driverClassName;
    this.jooqDialect = jooqDialect;
    this.synced = synced;
    this.tableCreator = tableCreator;
  }

  public static @NonNull JooqDatabaseType fromDatabaseType(@UnknownNullability DatabaseType databaseType) {
    return switch (databaseType) {
      case MYSQL -> MYSQL;
      case MARIADB -> MARIADB;
      case POSTGRES -> POSTGRESQL;
      case SQLITE -> SQLITE;
    };
  }

  public @NonNull String driverClassName() {
    return this.driverClassName;
  }

  public @NonNull SQLDialect jooqDialect() {
    return this.jooqDialect;
  }

  public boolean synced() {
    return this.synced;
  }

  public @NonNull JooqProvider createProvider(@NonNull SQLConfigurationEntry config) {
    return new JooqProvider(this.tableCreator, this, config);
  }
}
