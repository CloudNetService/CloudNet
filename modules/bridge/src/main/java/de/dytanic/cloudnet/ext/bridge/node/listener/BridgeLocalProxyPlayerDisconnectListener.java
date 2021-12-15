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

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.event.service.CloudServicePostLifecycleEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import org.jetbrains.annotations.NotNull;

public final class BridgeLocalProxyPlayerDisconnectListener {

  private final NodePlayerManager playerManager;

  public BridgeLocalProxyPlayerDisconnectListener(@NotNull NodePlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  @EventListener
  public void handleServiceUpdate(@NotNull CloudServiceUpdateEvent event) {
    var info = event.getServiceInfo();
    if (info.getServiceId().getNodeUniqueId().equals(CloudNetDriver.getInstance().getComponentName())
      && ServiceEnvironmentType.isMinecraftProxy(info.getServiceId().getEnvironment())) {
      // get all the players which are connected to the proxy
      var players = info.getProperty(BridgeServiceProperties.PLAYERS).orElse(null);
      if (players == null) {
        // no player property there yet, skip the check
        return;
      }
      // test if any player has the login service but is not connected to it
      for (var value : this.playerManager.getOnlinePlayers().values()) {
        if (value.getLoginService().serviceId().getUniqueId().equals(info.getServiceId().getUniqueId())) {
          // the player is on the service
          var match = Iterables.tryFind(
            players,
            player -> player.getUniqueId().equals(value.getUniqueId())
          ).orNull();
          // the player is not connected to the service, check if we already saw that in the last 10 seconds
          if (match == null) {
            // the player was added already to the set, log him out now
            this.playerManager.logoutPlayer(value);
          }
        }
      }
    }
  }

  @EventListener
  public void handle(@NotNull CloudServicePostLifecycleEvent event) {
    if (event.getNewLifeCycle() == ServiceLifeCycle.STOPPED) {
      this.handleCloudServiceRemove(event.getServiceInfo());
    }
  }

  private void handleCloudServiceRemove(@NotNull ServiceInfoSnapshot snapshot) {
    if (ServiceEnvironmentType.isMinecraftProxy(snapshot.getServiceId().getEnvironment())) {
      // test if any player has the stopped service as the login service
      for (var value : this.playerManager.getOnlinePlayers().values()) {
        if (value.getLoginService().serviceId().getUniqueId().equals(snapshot.getServiceId().getUniqueId())) {
          // the player was connected to that proxy, log him out now
          this.playerManager.logoutPlayer(value);
        }
      }
    }
  }
}
