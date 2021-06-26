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

package de.dytanic.cloudnet.ext.bridge.bukkit.listener;

import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.OnlyProxyProtection;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BukkitPlayerListener implements Listener {

  private final BukkitCloudNetBridgePlugin plugin;

  private final OnlyProxyProtection onlyProxyProtection;

  public BukkitPlayerListener(BukkitCloudNetBridgePlugin plugin) {
    this.plugin = plugin;

    this.onlyProxyProtection = new OnlyProxyProtection(Bukkit.getOnlineMode());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handle(PlayerLoginEvent event) {
    Player player = event.getPlayer();
    BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

    if (this.onlyProxyProtection.shouldDisallowPlayer(event.getRealAddress().getHostAddress())) {
      event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
      event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
        bridgeConfiguration.getMessages().get("server-join-cancel-because-only-proxy")));
      return;
    }

    String currentTaskName = Wrapper.getInstance().getServiceId().getTaskName();
    ServiceTask serviceTask = Wrapper.getInstance().getServiceTaskProvider().getServiceTask(currentTaskName);

    if (serviceTask != null) {
      String requiredPermission = serviceTask.getProperties().getString("requiredPermission");
      if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
        event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
        event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
          bridgeConfiguration.getMessages().get("server-join-cancel-because-permission")));
        return;
      }

      if (serviceTask.isMaintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
        event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
        event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
          bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance")));
        return;
      }
    }

    BridgeHelper.sendChannelMessageServerLoginRequest(BukkitCloudNetHelper.createNetworkConnectionInfo(player),
      BukkitCloudNetHelper.createNetworkPlayerServerInfo(player, true)
    );
  }

  @EventHandler
  public void handle(PlayerJoinEvent event) {
    BridgeHelper
      .sendChannelMessageServerLoginSuccess(BukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        BukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

    BridgeHelper.updateServiceInfo();
  }

  @EventHandler
  public void handle(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    Bukkit.getScheduler().runTask(this.plugin, () -> {
      BridgeHelper.sendChannelMessageServerDisconnect(BukkitCloudNetHelper.createNetworkConnectionInfo(player),
        BukkitCloudNetHelper.createNetworkPlayerServerInfo(player, false));

      BridgeHelper.updateServiceInfo();
    });
  }

}
