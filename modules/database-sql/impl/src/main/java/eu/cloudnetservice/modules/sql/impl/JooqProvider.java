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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.cloudnetservice.modules.sql.config.JooqConfigurationEntry;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.impl.database.AbstractNodeDatabaseProvider;
import java.util.Collection;
import lombok.NonNull;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;

public class JooqProvider extends AbstractNodeDatabaseProvider {

  protected final TableCreator tableCreator;
  protected final JooqConfigurationEntry config;

  protected DSLContext dslContext;
  protected HikariDataSource dataSource;

  protected JooqProvider(@NonNull TableCreator tableCreator, @NonNull JooqConfigurationEntry config) {
    super(DEFAULT_REMOVAL_LISTENER);

    this.tableCreator = tableCreator;
    this.config = config;
  }

  @Override
  public boolean init() {
    var hikariConfig = new HikariConfig();
    var endpoint = this.config.buildConnectionUri();

    var databaseType = JooqDatabaseType.fromDatabaseType(this.config.databaseType());

    hikariConfig.setJdbcUrl(endpoint);
    hikariConfig.setDriverClassName(databaseType.driverClassName());
    hikariConfig.setUsername(this.config.username());
    hikariConfig.setPassword(this.config.password());

    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
    hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
    hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
    hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
    hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
    hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
    hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

    hikariConfig.setMinimumIdle(2);
    hikariConfig.setMaximumPoolSize(100);
    hikariConfig.setConnectionTimeout(10_000);
    hikariConfig.setValidationTimeout(10_000);

    this.dataSource = new HikariDataSource(hikariConfig);
    this.dslContext = DSL.using(this.dataSource, databaseType.jooqDialect());
    return true;
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    this.tableCreator.createTable(this.dslContext, name);
    return new JooqDatabase(name, this, this.dslContext);
  }

  @Override
  public boolean containsDatabase(@NonNull String name) {
    return this.databaseNames().stream().anyMatch(dbName -> dbName.equalsIgnoreCase(name));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    return this.dslContext.dropTableIfExists(name).execute() != -1;
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    return this.dslContext.meta()
      .getTables()
      .stream()
      .filter(table -> table.getTableType() == TableOptions.TableType.TABLE)
      .filter(table -> table.field(JooqDatabase.KEY_FIELD_NAME) != null)
      .filter(table -> table.field(JooqDatabase.DOCUMENT_FIELD_NAME) != null)
      .map(Table::getName)
      .toList();
  }

  @Override
  public @NonNull String name() {
    return this.config.databaseServiceName();
  }

  @Override
  public void close() throws Exception {
    super.close();
    this.dataSource.close();
  }
}
