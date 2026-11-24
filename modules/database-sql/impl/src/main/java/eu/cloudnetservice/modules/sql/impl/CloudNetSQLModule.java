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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.sql.config.DatabaseType;
import eu.cloudnetservice.modules.sql.config.SQLConfigurationEntry;
import eu.cloudnetservice.modules.sql.config.SQLModuleConfiguration;
import eu.cloudnetservice.node.impl.database.NodeDatabaseProvider;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class CloudNetSQLModule extends DriverModule {

  private static final Map<String, String> DEFAULT_HIKARI_PROPERTIES = Map.of(
    "dataSource.cachePrepStmts", "true",
    "dataSource.prepStmtCacheSize", "250",
    "dataSource.maintainTimeStats", "false",
    "dataSource.useServerPrepStmts", "true",
    "dataSource.useLocalSessionState", "true",
    "dataSource.prepStmtCacheSqlLimit", "2048",
    "dataSource.cacheResultSetMetadata", "true",
    "dataSource.cacheServerConfiguration", "true",
    "dataSource.rewriteBatchedStatements", "true");

  static {
    System.setProperty("org.jooq.no-logo", "true");
    System.setProperty("org.jooq.no-tips", "true");
  }

  private volatile SQLModuleConfiguration configuration;

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    var config = this.readConfig(DocumentFactory.json());
    if (config.contains("entries")) {
      return;
    }

    List<LegacyConnectionEndpoint> endpoints = config.readObject(
      "endpoints",
      TypeFactory.parameterizedClass(List.class, LegacyConnectionEndpoint.class));
    var legacyEndpoint = endpoints.isEmpty()
      ? new LegacyConnectionEndpoint("cloudnet", new HostAndPort("127.0.0.1", 3306))
      : endpoints.getFirst();
    var convertedConfig = new SQLConfigurationEntry(
      true,
      DatabaseType.MYSQL,
      config.getString("databaseServiceName"),
      legacyEndpoint.database(),
      config.getString("username"),
      config.getString("password"),
      legacyEndpoint.address(),
      null,
      DEFAULT_HIKARI_PROPERTIES);
    this.writeConfig(Document.newJsonDocument().appendTree(new SQLModuleConfiguration(List.of(convertedConfig))));
  }

  @ModuleTask(order = 125, lifecycle = ModuleLifeCycle.LOADED)
  public void registerDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    this.configuration = this.readConfig(
      SQLModuleConfiguration.class,
      () -> new SQLModuleConfiguration(List.of(new SQLConfigurationEntry(
        false,
        DatabaseType.MYSQL,
        "sql",
        "cloudnet",
        "cloudnet",
        "password",
        new HostAndPort("127.0.0.1", 3306),
        null,
        DEFAULT_HIKARI_PROPERTIES
      ))),
      DocumentFactory.json());

    for (var entry : this.configuration.entries()) {
      if (!entry.enabled()) {
        continue;
      }

      var databaseType = JooqDatabaseType.fromDatabaseType(entry.databaseType());
      serviceRegistry.registerProvider(
        NodeDatabaseProvider.class,
        entry.databaseServiceName(),
        databaseType.createProvider(entry));
    }
  }

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STOPPED)
  public void unregisterDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    if (this.configuration == null) {
      return;
    }

    for (var entry : this.configuration.entries()) {
      if (!entry.enabled()) {
        continue;
      }

      var service = serviceRegistry.registration(NodeDatabaseProvider.class, entry.databaseServiceName());
      if (service != null) {
        service.unregister();
      }
    }
  }

  @Deprecated
  record LegacyConnectionEndpoint(@Nullable String database, @NonNull HostAndPort address) {

  }
}
