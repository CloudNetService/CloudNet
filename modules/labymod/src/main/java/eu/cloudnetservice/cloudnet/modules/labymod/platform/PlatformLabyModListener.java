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

package eu.cloudnetservice.cloudnet.modules.labymod.platform;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.modules.labymod.LabyModManagement;
import eu.cloudnetservice.cloudnet.modules.labymod.config.LabyModConfiguration;
import org.jetbrains.annotations.NotNull;

public class PlatformLabyModListener {

  private final PlatformLabyModManagement labyModManagement;
  private final PlatformBridgeManagement<?, ?> bridgeManagement;

  public PlatformLabyModListener(@NotNull PlatformLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
    this.bridgeManagement = Wrapper.getInstance().servicesRegistry().firstService(PlatformBridgeManagement.class);
  }

  @EventListener
  public void handlePlayerServerSwitch(@NotNull BridgeProxyPlayerServerSwitchEvent event) {
    this.bridgeManagement.getCachedService(event.getTarget().uniqueId()).ifPresent(service -> {
      // let the management handle the new server
      this.labyModManagement.handleServerUpdate(event.getCloudPlayer(), service);
    });
  }

  @EventListener
  public void handleConfigUpdate(@NotNull ChannelMessageReceiveEvent event) {
    // handle incoming channel messages on the labymod channel
    if (event.channel().equals(LabyModManagement.LABYMOD_MODULE_CHANNEL)
      && LabyModManagement.LABYMOD_UPDATE_CONFIG.equals(event.message())) {
      // update the configuration locally
      this.labyModManagement.setConfigurationSilently(event.content().readObject(LabyModConfiguration.class));
    }
  }
}
