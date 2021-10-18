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

package eu.cloudnetservice.cloudnet.ext.labymod.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.EventPriority;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;

public class LabyModListener {

  private final AbstractLabyModManagement labyModManagement;

  public LabyModListener(AbstractLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
  }

  @EventListener(priority = EventPriority.HIGHEST)
  public void handle(CloudServiceInfoUpdateEvent event) {
    ServiceInfoSnapshot newServiceInfoSnapshot = event.getServiceInfo();
    ServiceInfoSnapshot oldServiceInfoSnapshot = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(newServiceInfoSnapshot.getServiceId().getName());
    if (oldServiceInfoSnapshot == null) {
      return;
    }

    if (LabyModUtils.canSpectate(newServiceInfoSnapshot) &&
      !oldServiceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false)) {
      newServiceInfoSnapshot.getProperty(BridgeServiceProperty.PLAYERS).ifPresent(players -> {
        for (ServicePlayer player : players) {
          ICloudPlayer cloudPlayer = this.labyModManagement.getPlayerManager().getOnlinePlayer(player.getUniqueId());
          if (cloudPlayer != null &&
            cloudPlayer.getLoginService().getServiceId().getUniqueId()
              .equals(Wrapper.getInstance().getServiceId().getUniqueId()) &&
            LabyModUtils.getLabyModOptions(cloudPlayer) != null) {
            this.labyModManagement.sendServerUpdate(player.getUniqueId(), cloudPlayer, newServiceInfoSnapshot);
          }
        }
      });
    }
  }

}
