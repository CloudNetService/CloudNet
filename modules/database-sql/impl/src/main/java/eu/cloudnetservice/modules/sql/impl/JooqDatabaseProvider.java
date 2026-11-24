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
import eu.cloudnetservice.modules.sql.config.SQLConfigurationEntry;
import eu.cloudnetservice.modules.sql.impl.table.TableCreator;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.impl.database.AbstractNodeDatabaseProvider;
import java.util.Collection;
import java.util.Properties;
import lombok.NonNull;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;

public class JooqDatabaseProvider extends AbstractNodeDatabaseProvider {

  protected final TableCreator tableCreator;
  protected final JooqDatabaseType databaseType;
  protected final SQLConfigurationEntry config;

  protected DSLContext dslContext;
  protected HikariDataSource dataSource;

  protected JooqDatabaseProvider(
    @NonNull TableCreator tableCreator,
    @NonNull JooqDatabaseType databaseType,
    @NonNull SQLConfigurationEntry config
  ) {
    super(DEFAULT_REMOVAL_LISTENER);

    this.tableCreator = tableCreator;
    this.databaseType = databaseType;
    this.config = config;
  }

  @Override
  public boolean init() {
    var hikariConfig = this.createHikariConfig();
    this.dataSource = new HikariDataSource(hikariConfig);
    this.dslContext = DSL.using(this.dataSource, this.databaseType.jooqDialect());
    return true;
  }

  protected @NonNull HikariConfig createHikariConfig() {
    var properties = new Properties();
    properties.putAll(this.config.hikariProperties());
    properties.putIfAbsent("minimumIdle", "1");
    properties.putIfAbsent("maximumPoolSize", "15");
    properties.putIfAbsent("connectionTimeout", "5000");
    properties.putIfAbsent("validationTimeout", "5000");

    var hikariConfig = new HikariConfig(properties);
    hikariConfig.setUsername(this.config.username());
    hikariConfig.setPassword(this.config.password());
    hikariConfig.setJdbcUrl(this.config.buildConnectionUri());
    hikariConfig.setDriverClassName(this.databaseType.driverClassName());

    return hikariConfig;
  }

  @Override
  public boolean synced() {
    return this.databaseType.synced();
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    return this.databaseCache.get(name, _ -> {
      this.tableCreator.createTable(
        this.dslContext,
        name,
        JooqDatabase.KEY_FIELD,
        JooqDatabase.DOCUMENT_FIELD);
      return new JooqDatabase(name, this, this.dslContext);
    });
  }

  @Override
  public boolean containsDatabase(@NonNull String name) {
    return this.databaseNames().stream().anyMatch(dbName -> dbName.equalsIgnoreCase(name));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    this.databaseCache.invalidate(name);
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
