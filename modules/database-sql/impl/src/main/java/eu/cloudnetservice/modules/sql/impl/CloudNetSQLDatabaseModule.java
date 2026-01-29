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
import eu.cloudnetservice.modules.sql.config.JooqConfigurationEntry;
import eu.cloudnetservice.modules.sql.config.SQLModuleConfiguration;
import eu.cloudnetservice.node.impl.database.NodeDatabaseProvider;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

@Singleton
public final class CloudNetSQLDatabaseModule extends DriverModule {

  private volatile SQLModuleConfiguration configuration;

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    var config = this.readConfig(DocumentFactory.json());
    if (!config.contains("overrideConnectionUri")) {
      return;
    }

    var serviceName = config.getString("databaseServiceName");
    var username = config.getString("username");
    var password = config.getString("password");
    if (config.contains("addresses")) {
      List<LegacyConnectionEndpoint> addresses = config.readObject(
        "addresses",
        TypeFactory.parameterizedClass(List.class, LegacyConnectionEndpoint.class));
      var convertedConfig = new JooqConfigurationEntry(
        DatabaseType.MYSQL,
        serviceName,
        config.getString("database"),
        username,
        password,
        addresses.isEmpty() ? new HostAndPort("127.0.0.1", 3306) : addresses.getFirst().address(),
        null);
      this.writeConfig(Document.newJsonDocument().appendTree(convertedConfig));
    } else if (config.contains("endpoints")) {
      List<LegacyConnectionEndpoint> endpoints = config.readObject(
        "endpoints",
        TypeFactory.parameterizedClass(List.class, LegacyConnectionEndpoint.class));
      var endpoint = endpoints.isEmpty()
        ? new LegacyConnectionEndpoint("cloudnet", new HostAndPort("127.0.0.1", 3306))
        : endpoints.getFirst();
      var convertedConfig = new JooqConfigurationEntry(
        DatabaseType.MYSQL,
        serviceName,
        endpoint.database(),
        username,
        password,
        endpoint.address(),
        null);
      this.writeConfig(Document.newJsonDocument().appendTree(convertedConfig));
    }
  }

  @ModuleTask(order = 125, lifecycle = ModuleLifeCycle.LOADED)
  public void registerDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    this.configuration = this.readConfig(
      SQLModuleConfiguration.class,
      () -> new SQLModuleConfiguration(List.of(new JooqConfigurationEntry(
        DatabaseType.MAGIC_MIKE,
        "sql",
        "cloudnet",
        "cloudnet",
        "password",
        new HostAndPort("127.0.0.1", 3306),
        null
      ))),
      DocumentFactory.json());

    for (var entry : this.configuration.entries()) {
      serviceRegistry.registerProvider(
        NodeDatabaseProvider.class,
        entry.databaseServiceName(),
        new JooqProvider(((dslContext, name) -> {
          dslContext.createTableIfNotExists(DSL.name(name))
            .column(JooqDatabase.KEY_FIELD, SQLDataType.VARCHAR(512)
              .notNull()
              .collation(DSL.collation("utf8mb4_bin")))
            .column(JooqDatabase.DOCUMENT_FIELD, SQLDataType.JSONB.notNull())
            .execute();
        }), entry));
    }
  }

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STOPPED)
  public void unregisterDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    for (var entry : this.configuration.entries()) {
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
