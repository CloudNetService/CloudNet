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

package eu.cloudnetservice.modules.cloudperms.node;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionGroup;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.cloudnet.node.module.listener.PluginIncludeListener;
import eu.cloudnetservice.modules.cloudperms.node.config.CloudPermissionConfig;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;

public final class CloudNetCloudPermissionsModule extends DriverModule {

  private volatile CloudPermissionConfig permissionsConfig;

  @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
  public void init() {
    CloudNet.instance().dataSyncRegistry().registerHandler(DataSyncHandler.<CloudPermissionConfig>builder()
      .key("cloudperms-config")
      .convertObject(CloudPermissionConfig.class)
      .currentGetter($ -> this.permissionsConfig)
      .singletonCollector(() -> this.permissionsConfig)
      .nameExtractor(cloudPermissionsConfig -> "Permission Config")
      .writer(config -> this.writeConfig(JsonDocument.newDocument(config)))
      .build());
    CloudNet.instance().dataSyncRegistry().registerHandler(DataSyncHandler.<PermissionGroup>builder()
      .alwaysForce()
      .key("cloudperms-groups")
      .nameExtractor(PermissionGroup::name)
      .convertObject(PermissionGroup.class)
      .dataCollector(() -> CloudNet.instance().permissionManagement().groups())
      .writer(group -> CloudNet.instance().permissionManagement().addGroupSilently(group))
      .currentGetter(group -> CloudNet.instance().permissionManagement().group(group.name()))
      .build());
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
  public void initConfig() {
    this.permissionsConfig = this.readConfig(
      CloudPermissionConfig.class,
      () -> new CloudPermissionConfig(true, List.of()));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void reload() {
    this.initConfig();
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this.registerListener(new PluginIncludeListener(
      "cloudnet-cloudperms",
      CloudNetCloudPermissionsModule.class,
      service -> this.permissionsConfig.enabled()
        && Collections.disjoint(this.permissionsConfig.excludedGroups(), service.serviceConfiguration().groups())));
  }

  public @NonNull CloudPermissionConfig permissionsConfig() {
    return this.permissionsConfig;
  }
}
