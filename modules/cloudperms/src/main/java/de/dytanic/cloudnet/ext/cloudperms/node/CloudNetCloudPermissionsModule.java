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

package de.dytanic.cloudnet.ext.cloudperms.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.sync.DataSyncHandler;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.ext.cloudperms.node.config.CloudPermissionConfig;
import de.dytanic.cloudnet.ext.cloudperms.node.config.CloudPermissionConfigHelper;
import de.dytanic.cloudnet.ext.cloudperms.node.listener.IncludePluginListener;
import org.jetbrains.annotations.NotNull;

public final class CloudNetCloudPermissionsModule extends DriverModule {

  private volatile CloudPermissionConfig permissionsConfig;

  @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
  public void init() {
    CloudNet.getInstance().getDataSyncRegistry().registerHandler(DataSyncHandler.<CloudPermissionConfig>builder()
      .key("cloudperms-config")
      .convertObject(CloudPermissionConfig.class)
      .currentGetter($ -> this.permissionsConfig)
      .singletonCollector(() -> this.permissionsConfig)
      .nameExtractor(cloudPermissionsConfig -> "Permission Config")
      .writer(config -> CloudPermissionConfigHelper.write(config, this.getConfigPath()))
      .build());
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void initConfig() {
    this.permissionsConfig = CloudPermissionConfigHelper.read(this.getConfigPath());
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void reload() {
    this.initConfig();
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this.registerListener(new IncludePluginListener(this));
  }

  public @NotNull CloudPermissionConfig getPermissionsConfig() {
    return this.permissionsConfig;
  }
}
