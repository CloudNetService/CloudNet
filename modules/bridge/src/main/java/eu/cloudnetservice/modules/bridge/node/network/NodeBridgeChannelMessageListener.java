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

package eu.cloudnetservice.modules.bridge.node.network;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.event.BridgeConfigurationUpdateEvent;
import eu.cloudnetservice.modules.bridge.node.NodeBridgeManagement;
import lombok.NonNull;

public final class NodeBridgeChannelMessageListener {

  private final EventManager eventManager;
  private final NodeBridgeManagement management;

  public NodeBridgeChannelMessageListener(
    @NonNull NodeBridgeManagement management,
    @NonNull EventManager eventManager
  ) {
    this.management = management;
    this.eventManager = eventManager;
  }

  @EventListener
  public void handle(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(BridgeManagement.BRIDGE_CHANNEL_NAME)
      && event.message().equals("update_bridge_configuration")) {
      // read the config
      var configuration = event.content().readObject(BridgeConfiguration.class);
      // set the configuration
      this.management.configurationSilently(configuration);
      // call the update event
      this.eventManager.callEvent(new BridgeConfigurationUpdateEvent(configuration));
    }
  }
}
