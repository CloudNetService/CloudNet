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

package eu.cloudnetservice.modules.bridge.node.listener;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.modules.bridge.BridgeServiceProperties;
import eu.cloudnetservice.modules.bridge.node.player.NodePlayerManager;
import lombok.NonNull;

public final class BridgeLocalProxyPlayerDisconnectListener {

  private final NodePlayerManager playerManager;

  public BridgeLocalProxyPlayerDisconnectListener(@NonNull NodePlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  @EventListener
  public void handleServiceUpdate(@NonNull CloudServiceUpdateEvent event) {
    var info = event.serviceInfo();
    if (info.serviceId().nodeUniqueId().equals(CloudNetDriver.instance().componentName())
      && ServiceEnvironmentType.minecraftProxy(info.serviceId().environment())) {
      // get all the players which are connected to the proxy
      var players = info.property(BridgeServiceProperties.PLAYERS).orElse(null);
      if (players == null) {
        // no player property there yet, skip the check
        return;
      }
      // test if any player has the login service but is not connected to it
      for (var value : this.playerManager.players().values()) {
        if (value.loginService().serviceId().uniqueId().equals(info.serviceId().uniqueId())) {
          // the player is on the service
          var match = Iterables.tryFind(
            players,
            player -> player.uniqueId().equals(value.uniqueId())
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
  public void handle(@NonNull CloudServicePostLifecycleEvent event) {
    if (event.newLifeCycle() == ServiceLifeCycle.STOPPED || event.newLifeCycle() == ServiceLifeCycle.DELETED) {
      this.handleCloudServiceRemove(event.serviceInfo());
    }
  }

  private void handleCloudServiceRemove(@NonNull ServiceInfoSnapshot snapshot) {
    if (ServiceEnvironmentType.minecraftProxy(snapshot.serviceId().environment())) {
      // test if any player has the stopped service as the login service
      for (var value : this.playerManager.players().values()) {
        if (value.loginService().serviceId().uniqueId().equals(snapshot.serviceId().uniqueId())) {
          // the player was connected to that proxy, log him out now
          this.playerManager.logoutPlayer(value);
        }
      }
    }
  }
}
