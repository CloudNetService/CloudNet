/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.mongodb;

import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.node.database.AbstractDatabaseProvider;
import eu.cloudnetservice.modules.mongodb.config.MongoDBConnectionConfig;

public class CloudNetMongoDatabaseModule extends DriverModule {

  private MongoDBConnectionConfig config;

  @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
  public void loadConfig() {
    this.config = this.readConfig(MongoDBConnectionConfig.class, MongoDBConnectionConfig::new);
  }

  @ModuleTask(order = 125, event = ModuleLifeCycle.LOADED)
  public void registerDatabaseProvider() {
    this.serviceRegistry().registerProvider(
      AbstractDatabaseProvider.class,
      this.config.databaseServiceName(),
      new MongoDBDatabaseProvider(this.config));
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STOPPED)
  public void unregisterDatabaseProvider() {
    this.serviceRegistry().unregisterProvider(AbstractDatabaseProvider.class, this.config.databaseServiceName());
  }
}
