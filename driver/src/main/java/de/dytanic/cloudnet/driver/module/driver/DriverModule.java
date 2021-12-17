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
    return JsonDocument.newDocument(this.configPath());
  }

  public void writeConfig(@NotNull JsonDocument config) {
    config.write(this.configPath());
  }

  protected @NotNull Path configPath() {
    return this.moduleWrapper().dataDirectory().resolve("config.json");
  }

  public final @NotNull IEventManager registerListener(Object @NotNull ... listener) {
    return this.eventManager().registerListeners(listener);
  }

  public final @NotNull IServicesRegistry serviceRegistry() {
    return this.driver().servicesRegistry();
  }

  public final @NotNull IEventManager eventManager() {
    return this.driver().eventManager();
  }

  public final @NotNull RPCProviderFactory rpcFactory() {
    return this.driver().rpcProviderFactory();
  }

  @Contract(pure = true)
  public final @NotNull CloudNetDriver driver() {
    return CloudNetDriver.instance();
  }
}
