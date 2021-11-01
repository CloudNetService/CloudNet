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

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.sync.DataSyncHandler;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.ext.cloudperms.node.config.CloudPermissionConfig;
import de.dytanic.cloudnet.ext.cloudperms.node.config.CloudPermissionConfigHelper;
import de.dytanic.cloudnet.ext.cloudperms.node.listener.IncludePluginListener;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public final class CloudNetCloudPermissionsModule extends DriverModule {

  private static final Type LIST_STRING = TypeToken.getParameterized(List.class, String.class).getType();

  private static CloudNetCloudPermissionsModule instance;

  private CloudPermissionConfig permissionsConfig;

  public static CloudNetCloudPermissionsModule getInstance() {
    return CloudNetCloudPermissionsModule.instance;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
  public void init() {
    instance = this;

    CloudNet.getInstance().getDataSyncRegistry().registerHandler(
      DataSyncHandler.<CloudPermissionConfig>builder()
        .key("cloudperms-config")
        .nameExtractor(cloudPermissionsConfig -> "Permission Config")
        .convertObject(CloudPermissionConfig.class)
        .writer(cloudPermissionConfig -> CloudPermissionConfigHelper.write(cloudPermissionConfig,
          this.moduleWrapper.getDataDirectory().resolve("config.json")))
        .dataCollector(() -> Collections.singleton(this.permissionsConfig))
        .currentGetter($ -> this.permissionsConfig)
        .build());
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void initConfig() {
    this.permissionsConfig = CloudPermissionConfigHelper.read(
      this.moduleWrapper.getDataDirectory().resolve("config.json"));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void reload() {
    this.initConfig();
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this.registerListeners(new IncludePluginListener());
  }

  public List<String> getExcludedGroups() {
    return this.getConfig().get("excludedGroups", LIST_STRING);
  }
}
