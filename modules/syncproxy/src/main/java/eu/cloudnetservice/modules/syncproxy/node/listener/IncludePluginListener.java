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

package eu.cloudnetservice.modules.syncproxy.node.listener;

import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.util.DefaultModuleHelper;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServicePreProcessStartEvent;
import eu.cloudnetservice.modules.syncproxy.node.NodeSyncProxyManagement;
import lombok.NonNull;

public final class IncludePluginListener {

  private final NodeSyncProxyManagement management;

  public IncludePluginListener(@NonNull NodeSyncProxyManagement management) {
    this.management = management;
  }

  @EventListener
  public void handleLifecycleUpdate(@NonNull CloudServicePreProcessStartEvent event) {
    var service = event.service();
    if (!ServiceEnvironmentType.minecraftProxy(service.serviceId().environment())) {
      return;
    }

    var syncProxyConfiguration = this.management.configuration();
    var groupEntryExists = syncProxyConfiguration.loginConfigurations().stream()
      .anyMatch(config -> service.serviceConfiguration().groups().contains(config.targetGroup()))
      || syncProxyConfiguration.tabListConfigurations().stream()
      .anyMatch(config -> service.serviceConfiguration().groups().contains(config.targetGroup()));

    if (groupEntryExists) {
      var pluginsFolder = event.service().directory().resolve("plugins");
      FileUtil.createDirectory(pluginsFolder);

      var targetFile = pluginsFolder.resolve("cloudnet-syncproxy.jar");
      FileUtil.delete(targetFile);

      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, targetFile)) {
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          IncludePluginListener.class,
          event.service().serviceId().environment(),
          targetFile
        );
      }
    }
  }
}
