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

package eu.cloudnetservice.modules.labymod.platform;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.wrapper.Wrapper;
import lombok.NonNull;

public class PlatformLabyModListener {

  private final PlatformLabyModManagement labyModManagement;
  private final PlatformBridgeManagement<?, ?> bridgeManagement;

  public PlatformLabyModListener(@NonNull PlatformLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
    this.bridgeManagement = Wrapper.instance().serviceRegistry().firstProvider(PlatformBridgeManagement.class);
  }

  @EventListener
  public void handlePlayerServerSwitch(@NonNull BridgeProxyPlayerServerSwitchEvent event) {
    this.bridgeManagement.cachedService(event.target().uniqueId()).ifPresent(service -> {
      // let the management handle the new server
      this.labyModManagement.handleServerUpdate(event.cloudPlayer(), service);
    });
  }

  @EventListener
  public void handleConfigUpdate(@NonNull ChannelMessageReceiveEvent event) {
    // handle incoming channel messages on the labymod channel
    if (event.channel().equals(LabyModManagement.LABYMOD_MODULE_CHANNEL)
      && LabyModManagement.LABYMOD_UPDATE_CONFIG.equals(event.message())) {
      // update the configuration locally
      this.labyModManagement.setConfigurationSilently(event.content().readObject(LabyModConfiguration.class));
    }
  }
}
