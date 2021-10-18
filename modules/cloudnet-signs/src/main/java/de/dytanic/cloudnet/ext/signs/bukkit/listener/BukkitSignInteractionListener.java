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

package de.dytanic.cloudnet.ext.signs.bukkit.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.bukkit.BukkitSignManagement;
import de.dytanic.cloudnet.ext.signs.bukkit.event.BukkitCloudSignInteractEvent;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class BukkitSignInteractionListener implements Listener {

  private final BukkitSignManagement bukkitSignManagement;

  public BukkitSignInteractionListener(BukkitSignManagement bukkitSignManagement) {
    this.bukkitSignManagement = bukkitSignManagement;
  }

  @EventHandler
  public void handleInteract(PlayerInteractEvent event) {
    SignConfigurationEntry entry = this.bukkitSignManagement.getOwnSignConfigurationEntry();

    if (entry != null) {
      if ((event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) &&
        event.getClickedBlock() != null &&
        event.getClickedBlock().getState() instanceof org.bukkit.block.Sign) {
        for (Sign sign : this.bukkitSignManagement.getSigns()) {
          Location location = this.bukkitSignManagement.toLocation(sign.getWorldPosition());

          if (location == null || !location.equals(event.getClickedBlock().getLocation())) {
            continue;
          }

          String targetServer = sign.getServiceInfoSnapshot() == null ? null : sign.getServiceInfoSnapshot().getName();

          BukkitCloudSignInteractEvent signInteractEvent = new BukkitCloudSignInteractEvent(event.getPlayer(), sign,
            targetServer);
          Bukkit.getPluginManager().callEvent(signInteractEvent);

          if (!signInteractEvent.isCancelled() && signInteractEvent.getTargetServer() != null) {
            CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class)
              .getPlayerExecutor(event.getPlayer().getUniqueId()).connect(signInteractEvent.getTargetServer());

            String serverConnectMessage = SignConfigurationProvider.load().getMessages()
              .get("server-connecting-message");

            if (serverConnectMessage != null) {
              event.getPlayer().sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                  serverConnectMessage.replace("%server%", sign.getServiceInfoSnapshot().getServiceId().getName())
                )
              );
            }
          }

          return;
        }
      }
    }
  }
}
