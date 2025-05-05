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

package eu.cloudnetservice.modules.bridge.impl.node.network;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.event.BridgeConfigurationUpdateEvent;
import eu.cloudnetservice.modules.bridge.impl.node.NodeBridgeManagement;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class NodeBridgeChannelMessageListener {

  @EventListener
  public void handle(
    @NonNull ChannelMessageReceiveEvent event,
    @NonNull EventManager eventManager,
    @NonNull NodeBridgeManagement management
  ) {
    if (event.channel().equals(BridgeManagement.BRIDGE_CHANNEL_NAME)
      && event.message().equals("update_bridge_configuration")) {
      // read the config
      var configuration = event.content().readObject(BridgeConfiguration.class);
      // set the configuration
      management.configurationSilently(configuration);
      // call the update event
      eventManager.callEvent(new BridgeConfigurationUpdateEvent(configuration));
    }
  }
}
