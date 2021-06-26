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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationWriterAndReader;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;

public final class SyncProxyConfigUpdateListener {

  @EventListener
  public void handleQuery(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equalsIgnoreCase(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME) || !event.isQuery()) {
      return;
    }

    if (SyncProxyConstants.SYNC_PROXY_CHANNEL_GET_CONFIGURATION.equals(event.getMessage())) {
      event.setJsonResponse(JsonDocument
        .newDocument("syncProxyConfiguration", CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration()));
    }
  }

  @EventListener
  public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
        /*CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
                SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
                new JsonDocument("syncProxyConfiguration", CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration())
        );*/
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equals(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME) || event.getMessage() == null) {
      return;
    }

    if (SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION.equals(event.getMessage())) {
      SyncProxyConfiguration syncProxyConfiguration = event.getData()
        .get("syncProxyConfiguration", SyncProxyConfiguration.TYPE);

      if (syncProxyConfiguration != null) {
        CloudNetSyncProxyModule.getInstance().setSyncProxyConfiguration(syncProxyConfiguration);
      }

      SyncProxyConfigurationWriterAndReader
        .write(syncProxyConfiguration, CloudNetSyncProxyModule.getInstance().getConfigurationFilePath());
    }
  }
}
