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

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.mysql.config.DatabaseType;
import eu.cloudnetservice.modules.mysql.config.JooqConfigurationEntry;
import eu.cloudnetservice.modules.mysql.config.SQLModuleConfiguration;
import eu.cloudnetservice.node.impl.database.NodeDatabaseProvider;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.NonNull;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

@Singleton
public final class CloudNetMySQLDatabaseModule extends DriverModule {

  private volatile SQLModuleConfiguration configuration;

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    /*var config = this.readConfig(DocumentFactory.json());
    if (config.contains("addresses")) {
      // convert all entries
      this.writeConfig(Document.newJsonDocument().appendTree(new MySQLConfiguration(
        config.getString("username"),
        config.getString("password"),
        config.getString("database"),
        config.readObject("addresses", TypeFactory.parameterizedClass(List.class, MySQLConnectionEndpoint.class))
      )));
    }*/
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
}
