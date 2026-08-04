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

package eu.cloudnetservice.modules.postgres.impl;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.postgres.config.PostgresConfiguration;
import eu.cloudnetservice.modules.postgres.config.PostgresConnectionEndpoint;
import eu.cloudnetservice.node.impl.database.NodeDatabaseProvider;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.NonNull;

@Singleton
public final class CloudNetPostgresDatabaseModule extends DriverModule {

  private volatile PostgresConfiguration configuration;

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    var config = this.readConfig(DocumentFactory.json());
    if (config.contains("addresses")) {
      this.writeConfig(Document.newJsonDocument().appendTree(new PostgresConfiguration(
        config.getString("username"),
        config.getString("password"),
        config.getString("database"),
        config.readObject("addresses", TypeFactory.parameterizedClass(List.class, PostgresConnectionEndpoint.class))
      )));
    }
  }

  @ModuleTask(order = 125, lifecycle = ModuleLifeCycle.LOADED)
  public void registerDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    this.configuration = this.readConfig(
      PostgresConfiguration.class,
      () -> new PostgresConfiguration(
        "postgres",
        "postgres",
        "postgres",
        List.of(new PostgresConnectionEndpoint("cloudnet", new HostAndPort("127.0.0.1", 5432)))),
      DocumentFactory.json());

    serviceRegistry.registerProvider(
      NodeDatabaseProvider.class,
      this.configuration.databaseServiceName(),
      new PostgresDatabaseProvider(this.configuration));
  }

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STOPPED)
  public void unregisterDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    var service = serviceRegistry.registration(NodeDatabaseProvider.class, this.configuration.databaseServiceName());
    if (service != null) {
      service.unregister();
    }
  }
}
