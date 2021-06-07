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

package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;

public final class NetworkListenerRegisterListener {

  @EventListener
  public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
        /*event.getNode().sendCustomChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_NETWORK_CHANNEL_CLUSTER_MESSAGE_UPDATE_BRIDGE_CONFIGURATION_LISTENER,
                new JsonDocument("bridgeConfiguration", CloudNetBridgeModule.getInstance().getBridgeConfiguration())
        );*/
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL) || !event
      .isQuery()) {
      return;
    }

    if (BridgeConstants.BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION.equals(event.getMessage())) {
      BridgeConfiguration configuration = JsonDocument
        .newDocument(CloudNetBridgeModule.getInstance().getModuleWrapper().getDataDirectory().resolve("config.json"))
        .get("config", BridgeConfiguration.TYPE);

      event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
        .json(JsonDocument.newDocument("bridgeConfig", configuration))
        .build());
    }
  }
}
