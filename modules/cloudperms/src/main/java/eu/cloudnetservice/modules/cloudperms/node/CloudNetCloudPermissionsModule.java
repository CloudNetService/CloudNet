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

package eu.cloudnetservice.modules.cloudperms.node;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.modules.cloudperms.node.config.CloudPermissionConfig;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.module.listener.PluginIncludeListener;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;

@Singleton
public final class CloudNetCloudPermissionsModule extends DriverModule {

  private volatile CloudPermissionConfig permissionsConfig;

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
  public void registerDataSyncHandler(@NonNull DataSyncRegistry dataSyncRegistry) {
    dataSyncRegistry.registerHandler(DataSyncHandler.<CloudPermissionConfig>builder()
      .key("cloudperms-config")
      .convertObject(CloudPermissionConfig.class)
      .currentGetter($ -> this.permissionsConfig)
      .singletonCollector(() -> this.permissionsConfig)
      .nameExtractor(cloudPermissionsConfig -> "Permission Config")
      .writer(config -> this.writeConfig(Document.newJsonDocument().appendTree(config)))
      .build());
  }

  @ModuleTask(order = 126, lifecycle = ModuleLifeCycle.LOADED)
  public void initConfig() {
    this.permissionsConfig = this.readConfig(
      CloudPermissionConfig.class,
      () -> new CloudPermissionConfig(true, List.of()),
      DocumentFactory.json());
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
  public void reload() {
    this.initConfig();
  }

  @ModuleTask(order = 124, lifecycle = ModuleLifeCycle.STARTED)
  public void registerListeners(@NonNull EventManager eventManager, @NonNull ModuleHelper moduleHelper) {
    eventManager.registerListener(new PluginIncludeListener(
      "cloudnet-cloudperms",
      CloudNetCloudPermissionsModule.class,
      moduleHelper,
      service -> this.permissionsConfig.enabled() && Collections.disjoint(
        this.permissionsConfig.excludedGroups(),
        service.serviceConfiguration().groups())));
  }

  public @NonNull CloudPermissionConfig permissionsConfig() {
    return this.permissionsConfig;
  }
}
