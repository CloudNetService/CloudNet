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

package eu.cloudnetservice.modules.syncproxy.impl.node.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.modules.syncproxy.SyncProxyConfigurationUpdateEvent;
import eu.cloudnetservice.modules.syncproxy.SyncProxyConstants;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.modules.syncproxy.impl.node.NodeSyncProxyManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class NodeSyncProxyChannelMessageListener {

  private final NodeSyncProxyManagement management;
  private final EventManager eventManager;

  @Inject
  public NodeSyncProxyChannelMessageListener(
    @NonNull NodeSyncProxyManagement management,
    @NonNull EventManager eventManager
  ) {
    this.management = management;
    this.eventManager = eventManager;
  }

  @EventListener
  public void handleConfigUpdate(@NonNull ChannelMessageReceiveEvent event) {
    if (!event.channel().equals(SyncProxyConstants.SYNC_PROXY_CHANNEL)) {
      return;
    }

    if (SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIG.equals(event.message())) {
      // read the configuration from the databuf
      var configuration = event.content().readObject(SyncProxyConfiguration.class);
      // write the configuration silently to the file
      this.management.configurationSilently(configuration);
      // call the local event for the update
      this.eventManager.callEvent(new SyncProxyConfigurationUpdateEvent(configuration));
    } else if (SyncProxyConstants.SYNC_PROXY_CONFIG_REQUEST.equals(event.message())) {
      var configuration = this.management.configuration();
      // respond with the currently loaded configuration
      event.binaryResponse(DataBuf.empty().writeObject(configuration));
    }
  }
}
