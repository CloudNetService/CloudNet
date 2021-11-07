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

package de.dytanic.cloudnet.driver.module.driver;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.module.DefaultModule;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import java.nio.file.Path;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class DriverModule extends DefaultModule {

  public @NotNull JsonDocument readConfig() {
    return JsonDocument.newDocument(this.getConfigPath());
  }

  public void writeConfig(@NotNull JsonDocument config) {
    config.write(this.getConfigPath());
  }

  protected @NotNull Path getConfigPath() {
    return this.getModuleWrapper().getDataDirectory().resolve("config.json");
  }

  public final @NotNull IEventManager registerListener(Object @NotNull ... listener) {
    return this.getEventManager().registerListener(listener);
  }

  public final @NotNull IServicesRegistry getServiceRegistry() {
    return this.getDriver().getServicesRegistry();
  }

  public final @NotNull IEventManager getEventManager() {
    return this.getDriver().getEventManager();
  }

  public final @NotNull RPCProviderFactory getRPCFactory() {
    return this.getDriver().getRPCProviderFactory();
  }

  @Contract(pure = true)
  public final @NotNull CloudNetDriver getDriver() {
    return CloudNetDriver.getInstance();
  }
}
