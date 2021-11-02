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
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.task.LocalServiceTaskAddEvent;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationHelper;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyTabListConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;

public class SyncProxyDefaultConfigurationListener {

  private final CloudNetSyncProxyModule syncProxyModule;

  public SyncProxyDefaultConfigurationListener(CloudNetSyncProxyModule syncProxyModule) {
    this.syncProxyModule = syncProxyModule;
  }

  @EventListener
  public void handleQuery(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equalsIgnoreCase(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME) || !event.isQuery()) {
      return;
    }

    if (SyncProxyConstants.SYNC_PROXY_GET_CONFIGURATION.equals(event.getMessage())) {
      event.setBinaryResponse(
        DataBuf.empty().writeObject(this.syncProxyModule.getSyncProxyConfiguration()));
    } else if (SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION.equals(event.getMessage())) {
      SyncProxyConfiguration configuration = event.getContent().readObject(SyncProxyConfiguration.class);
      if (configuration == null) {
        return;
      }

      this.syncProxyModule.setSyncProxyConfiguration(configuration);
      SyncProxyConfigurationHelper.write(configuration, this.syncProxyModule.getConfigurationFilePath());
    }
  }

  @EventListener
  public void handleTaskAdd(LocalServiceTaskAddEvent event) {
    ServiceTask task = event.getTask();

    if (!task.getProcessConfiguration().getEnvironment().isMinecraftJavaProxy() &&
      !task.getProcessConfiguration().getEnvironment().isMinecraftBedrockProxy()) {
      return;
    }

    SyncProxyConfiguration configuration = this.syncProxyModule.getSyncProxyConfiguration();
    boolean modified = false;

    if (configuration.getLoginConfigurations().stream()
      .noneMatch(loginConfiguration -> loginConfiguration.getTargetGroup().equals(task.getName()))) {
      configuration.getLoginConfigurations()
        .add(SyncProxyLoginConfiguration.createDefault(task.getName()));
      modified = true;
    }

    if (configuration.getTabListConfigurations().stream()
      .noneMatch(tabListConfiguration -> tabListConfiguration.getTargetGroup().equals(task.getName()))) {
      configuration.getTabListConfigurations()
        .add(SyncProxyTabListConfiguration.createDefault(task.getName()));
      modified = true;
    }

    if (modified) {
      SyncProxyConfigurationHelper
        .write(configuration, this.syncProxyModule.getConfigurationFilePath());
    }
  }
}
