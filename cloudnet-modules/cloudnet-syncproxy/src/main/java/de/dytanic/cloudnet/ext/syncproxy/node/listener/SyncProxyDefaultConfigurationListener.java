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

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationWriterAndReader;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;

public class SyncProxyDefaultConfigurationListener {

  @EventListener
  public void handleTaskAdd(ServiceTaskAddEvent event) {
    ServiceTask task = event.getTask();

    if (!task.getProcessConfiguration().getEnvironment().isMinecraftJavaProxy() &&
      !task.getProcessConfiguration().getEnvironment().isMinecraftBedrockProxy()) {
      return;
    }

    SyncProxyConfiguration configuration = CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration();
    boolean modified = false;

    if (configuration.getLoginConfigurations().stream()
      .noneMatch(loginConfiguration -> loginConfiguration.getTargetGroup().equals(task.getName()))) {
      configuration.getLoginConfigurations()
        .add(SyncProxyConfigurationWriterAndReader.createDefaultLoginConfiguration(task.getName()));
      modified = true;
    }

    if (configuration.getTabListConfigurations().stream()
      .noneMatch(tabListConfiguration -> tabListConfiguration.getTargetGroup().equals(task.getName()))) {
      configuration.getTabListConfigurations()
        .add(SyncProxyConfigurationWriterAndReader.createDefaultTabListConfiguration(task.getName()));
      modified = true;
    }

    if (modified) {
      SyncProxyConfigurationWriterAndReader
        .write(configuration, CloudNetSyncProxyModule.getInstance().getConfigurationFilePath());
    }

  }

}
