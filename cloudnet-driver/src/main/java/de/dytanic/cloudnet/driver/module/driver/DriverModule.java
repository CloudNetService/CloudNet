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
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.module.DefaultModule;
import java.nio.file.Files;
import java.nio.file.Path;

public class DriverModule extends DefaultModule {

  protected JsonDocument config;

  public final JsonDocument getConfig() {
    if (this.config == null) {
      this.config = this.reloadConfig0();
    }

    return this.config;
  }

  public JsonDocument reloadConfig() {
    return this.config = this.reloadConfig0();
  }

  public final DriverModule saveConfig() {
    if (this.config != null) {
      this.config.write(this.getModuleWrapper().getDataDirectory().resolve("config.json"));
    }

    return this;
  }

  private JsonDocument reloadConfig0() {
    FileUtils.createDirectoryReported(this.getModuleWrapper().getDataDirectory());

    Path configuration = this.getModuleWrapper().getDataDirectory().resolve("config.json");
    if (Files.notExists(configuration)) {
      JsonDocument.EMPTY.write(configuration);
    }

    return JsonDocument.newDocument(configuration);
  }

  public final ILogger log(LogLevel level, String message) {
    return this.getLogger().log(level, message);
  }

  public final IEventManager registerListener(Object listener) {
    return this.getEventManager().registerListener(listener);
  }

  public final IEventManager registerListeners(Object... listeners) {
    return this.getEventManager().registerListeners(listeners);
  }

  public final IServicesRegistry getRegistry() {
    return this.getDriver().getServicesRegistry();
  }

  public final ILogger getLogger() {
    return this.getDriver().getLogger();
  }

  public final IEventManager getEventManager() {
    return this.getDriver().getEventManager();
  }

  public final CloudNetDriver getDriver() {
    return CloudNetDriver.getInstance();
  }
}
