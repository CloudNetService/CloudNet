/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.ext.mongodb;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.ext.mongodb.config.MongoDBConnectionConfig;

public class CloudNetMongoDatabaseModule extends DriverModule {

  private MongoDBConnectionConfig config;

  @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
  public void loadConfig() {
    var configuration = this.readConfig();
    this.config = configuration.get("config", MongoDBConnectionConfig.class, new MongoDBConnectionConfig());
    super.writeConfig(JsonDocument.newDocument("config", this.config));
  }

  @ModuleTask(order = 125, event = ModuleLifeCycle.LOADED)
  public void registerDatabaseProvider() {
    this.getServiceRegistry().registerService(AbstractDatabaseProvider.class, this.config.getDatabaseServiceName(),
      new MongoDBDatabaseProvider(this.config));
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STOPPED)
  public void unregisterDatabaseProvider() {
    this.getServiceRegistry().unregisterService(AbstractDatabaseProvider.class, this.config.getDatabaseServiceName());
  }
}
