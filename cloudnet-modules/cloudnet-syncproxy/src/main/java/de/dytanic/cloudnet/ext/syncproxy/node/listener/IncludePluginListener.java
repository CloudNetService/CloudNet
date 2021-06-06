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

package de.dytanic.cloudnet.ext.syncproxy.node.listener;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;
import de.dytanic.cloudnet.service.ICloudService;
import java.nio.file.Path;

public final class IncludePluginListener {

  @EventListener
  public void handle(CloudServicePreStartEvent event) {
    ICloudService service = event.getCloudService();
    if (!service.getServiceId().getEnvironment().isMinecraftProxy()) {
      return;
    }

    SyncProxyConfiguration syncProxyConfiguration = CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration();
    boolean groupEntryExists = syncProxyConfiguration.getLoginConfigurations().stream()
      .anyMatch(loginConfiguration -> service.getGroups().contains(loginConfiguration.getTargetGroup()))
      || syncProxyConfiguration.getTabListConfigurations().stream()
      .anyMatch(tabListConfiguration -> service.getGroups().contains(tabListConfiguration.getTargetGroup()));

    if (groupEntryExists) {
      Path pluginsFolder = event.getCloudService().getDirectoryPath().resolve("plugins");
      FileUtils.createDirectoryReported(pluginsFolder);

      Path targetFile = pluginsFolder.resolve("cloudnet-syncproxy.jar");
      FileUtils.deleteFileReported(targetFile);

      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, targetFile)) {
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          IncludePluginListener.class,
          event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(),
          targetFile
        );
      }
    }
  }
}
