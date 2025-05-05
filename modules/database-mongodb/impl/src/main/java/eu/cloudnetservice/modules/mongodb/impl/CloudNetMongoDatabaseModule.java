/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.mongodb.impl;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.mongodb.config.MongoDBConnectionConfig;
import eu.cloudnetservice.node.impl.database.NodeDatabaseProvider;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class CloudNetMongoDatabaseModule extends DriverModule {

  private MongoDBConnectionConfig config;

  @ModuleTask(order = 126, lifecycle = ModuleLifeCycle.LOADED)
  public void loadConfig() {
    this.config = this.readConfig(MongoDBConnectionConfig.class, MongoDBConnectionConfig::new, DocumentFactory.json());
  }

  @ModuleTask(order = 125, lifecycle = ModuleLifeCycle.LOADED)
  public void registerDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    serviceRegistry.registerProvider(
      NodeDatabaseProvider.class,
      this.config.databaseServiceName(),
      new MongoDBDatabaseProvider(this.config));
  }

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STOPPED)
  public void unregisterDatabaseProvider(@NonNull ServiceRegistry serviceRegistry) {
    var registration = serviceRegistry.registration(NodeDatabaseProvider.class, this.config.databaseServiceName());
    if (registration != null) {
      registration.unregister();
    }
  }
}
