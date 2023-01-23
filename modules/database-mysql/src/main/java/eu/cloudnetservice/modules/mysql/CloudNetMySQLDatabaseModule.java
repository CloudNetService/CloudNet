/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.mysql;

import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.mysql.config.MySQLConfiguration;
import eu.cloudnetservice.modules.mysql.config.MySQLConnectionEndpoint;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.NonNull;

@Singleton
public final class CloudNetMySQLDatabaseModule extends DriverModule {

  private volatile MySQLConfiguration configuration;

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    var config = this.readConfig();
    if (config.contains("addresses")) {
      // convert all entries
      this.writeConfig(JsonDocument.newDocument(new MySQLConfiguration(
        config.getString("username"),
        config.getString("password"),
        config.getString("database"),
        config.get("addresses", TypeToken.getParameterized(List.class, MySQLConnectionEndpoint.class).getType())
      )));
    }
  }

  @ModuleTask(order = 125, lifecycle = ModuleLifeCycle.LOADED)
  public void registerDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    this.configuration = this.readConfig(MySQLConfiguration.class, () -> new MySQLConfiguration(
      "root",
      "123456",
      "mysql",
      List.of(new MySQLConnectionEndpoint(false, "cloudnet", new HostAndPort("127.0.0.1", 3306)))
    ));

    serviceRegistry.registerProvider(
      NodeDatabaseProvider.class,
      this.configuration.databaseServiceName(),
      new MySQLDatabaseProvider(this.configuration, null));
  }

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STOPPED)
  public void unregisterDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    serviceRegistry.unregisterProvider(NodeDatabaseProvider.class, this.configuration.databaseServiceName());
  }
}
