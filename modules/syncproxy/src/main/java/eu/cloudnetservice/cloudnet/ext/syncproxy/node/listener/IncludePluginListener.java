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

package eu.cloudnetservice.cloudnet.ext.syncproxy.node.listener;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreLifecycleEvent;
import de.dytanic.cloudnet.service.ICloudService;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.cloudnet.ext.syncproxy.node.NodeSyncProxyManagement;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public final class IncludePluginListener {

  private final NodeSyncProxyManagement management;

  public IncludePluginListener(@NotNull NodeSyncProxyManagement management) {
    this.management = management;
  }

  @EventListener
  public void handleLifecycleUpdate(@NotNull CloudServicePreLifecycleEvent event) {
    if (event.getTargetLifecycle() != ServiceLifeCycle.RUNNING) {
      return;
    }

    ICloudService service = event.getService();
    if (!service.getServiceId().getEnvironment().isMinecraftProxy()) {
      return;
    }

    SyncProxyConfiguration syncProxyConfiguration = this.management.getConfiguration();
    boolean groupEntryExists = syncProxyConfiguration.getLoginConfigurations().stream()
      .anyMatch(loginConfiguration -> service.getServiceConfiguration().getGroups()
        .contains(loginConfiguration.getTargetGroup()))
      || syncProxyConfiguration.getTabListConfigurations().stream()
      .anyMatch(tabListConfiguration -> service.getServiceConfiguration().getGroups()
        .contains(tabListConfiguration.getTargetGroup()));

    if (groupEntryExists) {
      Path pluginsFolder = event.getService().getDirectory().resolve("plugins");
      FileUtils.createDirectory(pluginsFolder);

      Path targetFile = pluginsFolder.resolve("cloudnet-syncproxy.jar");
      FileUtils.delete(targetFile);

      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, targetFile)) {
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          IncludePluginListener.class,
          event.getService().getServiceConfiguration().getProcessConfig().getEnvironment(),
          targetFile
        );
      }
    }
  }
}
