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

package de.dytanic.cloudnet.ext.signs.nukkit.listener;

import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.level.Location;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.nukkit.NukkitSignManagement;
import de.dytanic.cloudnet.ext.signs.nukkit.event.NukkitCloudSignInteractEvent;

public class NukkitSignInteractionListener implements Listener {

  private final NukkitSignManagement nukkitSignManagement;

  public NukkitSignInteractionListener(NukkitSignManagement nukkitSignManagement) {
    this.nukkitSignManagement = nukkitSignManagement;
  }

  @EventHandler
  public void handleInteract(PlayerInteractEvent event) {
    SignConfigurationEntry entry = this.nukkitSignManagement.getOwnSignConfigurationEntry();

    if (entry != null) {
      if ((event.getAction().equals(PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)) &&
        event.getBlock() != null &&
        event.getBlock().getLevel().getBlockEntity(event.getBlock().getLocation()) instanceof BlockEntitySign) {
        for (Sign sign : this.nukkitSignManagement.getSigns()) {
          Location location = this.nukkitSignManagement.toLocation(sign.getWorldPosition());

          if (location == null || !location.equals(event.getBlock().getLocation())) {
            continue;
          }

          String targetServer = sign.getServiceInfoSnapshot() == null ? null : sign.getServiceInfoSnapshot().getName();

          NukkitCloudSignInteractEvent signInteractEvent = new NukkitCloudSignInteractEvent(event.getPlayer(), sign,
            targetServer);
          Server.getInstance().getPluginManager().callEvent(signInteractEvent);

          if (!signInteractEvent.isCancelled() && signInteractEvent.getTargetServer() != null) {
            CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class)
              .getPlayerExecutor(event.getPlayer().getUniqueId()).connect(signInteractEvent.getTargetServer());

            String serverConnectMessage = SignConfigurationProvider.load().getMessages()
              .get("server-connecting-message");

            if (serverConnectMessage != null) {
              event.getPlayer().sendMessage(
                serverConnectMessage
                  .replace("%server%", sign.getServiceInfoSnapshot().getServiceId().getName())
                  .replace('&', '§')
              );
            }

          }

          return;
        }
      }
    }
  }


}
