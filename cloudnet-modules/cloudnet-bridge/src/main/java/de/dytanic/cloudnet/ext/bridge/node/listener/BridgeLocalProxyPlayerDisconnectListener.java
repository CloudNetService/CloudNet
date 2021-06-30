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
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.event.service.CloudServicePostStopEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public final class BridgeLocalProxyPlayerDisconnectListener {

  private final NodePlayerManager playerManager;
  private final Map<UUID, Long> invisibilityTester;

  public BridgeLocalProxyPlayerDisconnectListener(NodePlayerManager playerManager) {
    this.playerManager = playerManager;
    this.invisibilityTester = new ConcurrentHashMap<>();
  }

  @EventListener
  public void handleServiceUpdate(CloudServiceInfoUpdateEvent event) {
    ServiceInfoSnapshot info = event.getServiceInfo();
    if (info.getServiceId().getNodeUniqueId().equals(CloudNetDriver.getInstance().getComponentName())
      && info.getServiceId().getEnvironment().isMinecraftProxy()) {
      // get all the players which are connected to the proxy
      Collection<ServicePlayer> players = info.getProperty(BridgeServiceProperty.PLAYERS).orElse(null);
      if (players == null) {
        // no player property there yet, skip the check
        return;
      }
      // test if any player has the login service but is not connected to it
      for (CloudPlayer value : this.playerManager.getOnlineCloudPlayers().values()) {
        if (value.getLoginService().getServiceId().getUniqueId().equals(info.getServiceId().getUniqueId())) {
          // the player is on the service
          ServicePlayer match = Iterables.tryFind(
            players,
            player -> player.getUniqueId().equals(value.getUniqueId())
          ).orNull();
          // check if we already had the player
          long timeout = System.currentTimeMillis() + 10_000;
          Long time = this.invisibilityTester.put(value.getUniqueId(), timeout);
          // check if the previous one expired
          if (time != null && System.currentTimeMillis() >= time) {
            time = null;
          }
          // the player is not connected to the service, check if we already saw that in the last 10 seconds
          if (match == null && time != null) {
            // the player was added already to the set, log him out now
            this.playerManager.logoutPlayer(value);
            // free the tester for him
            this.invisibilityTester.remove(value.getUniqueId());
          }
        }
      }
    }
  }

  @EventListener
  public void handle(CloudServiceStopEvent event) {
    this.handleCloudServiceRemove(event.getServiceInfo());
  }

  @EventListener
  public void handle(CloudServicePostStopEvent event) {
    this.handleCloudServiceRemove(event.getCloudService().getServiceInfoSnapshot());
  }

  @EventListener
  public void handle(CloudServiceUnregisterEvent event) {
    this.handleCloudServiceRemove(event.getServiceInfo());
  }

  private void handleCloudServiceRemove(@NotNull ServiceInfoSnapshot snapshot) {
    if (snapshot.getServiceId().getEnvironment().isMinecraftProxy()) {
      // test if any player has the stopped service as the login service
      for (CloudPlayer value : this.playerManager.getOnlineCloudPlayers().values()) {
        if (value.getLoginService().getServiceId().getUniqueId().equals(snapshot.getServiceId().getUniqueId())) {
          // the player was connected to that proxy, log him out now
          this.playerManager.logoutPlayer(value);
        }
      }
    }
  }
}
