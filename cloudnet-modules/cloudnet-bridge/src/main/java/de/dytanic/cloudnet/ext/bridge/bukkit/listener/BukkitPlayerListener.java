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
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitServiceTaskAddEvent;
import de.dytanic.cloudnet.ext.bridge.server.OnlyProxyProtection;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Optional;
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

  private ServiceTask serviceTask;

  public BukkitPlayerListener(BukkitCloudNetBridgePlugin plugin) {
    this.plugin = plugin;

    this.onlyProxyProtection = new OnlyProxyProtection(Bukkit.getOnlineMode());

    String currentTaskName = Wrapper.getInstance().getServiceId().getTaskName();
    this.serviceTask = Wrapper.getInstance().getServiceTaskProvider().getServiceTask(currentTaskName);
  }

  private Optional<String> getPlayerKickMessage(Player player) {
    if (this.serviceTask == null) {
      return Optional.empty();
    }

    BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

    if (this.serviceTask.isMaintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
      return Optional.of(ChatColor.translateAlternateColorCodes('&',
        bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance")));
    } else {
      String requiredPermission = this.serviceTask.getProperties().getString("requiredPermission");
      if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
        return Optional.of(ChatColor.translateAlternateColorCodes('&',
          bridgeConfiguration.getMessages().get("server-join-cancel-because-permission")));
      }
    }

    return Optional.empty();
  }

  @EventHandler
  public void handle(BukkitServiceTaskAddEvent event) {
    ServiceTask task = event.getTask();

    if (this.serviceTask != null && this.serviceTask.getName().equals(task.getName())) {
      this.serviceTask = task;

      Bukkit.getOnlinePlayers().forEach(player -> this.getPlayerKickMessage(player).ifPresent(player::kickPlayer));
    }
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

    Optional<String> kickMessageOptional = this.getPlayerKickMessage(player);

    if (kickMessageOptional.isPresent()) {
      String kickMessage = kickMessageOptional.get();

      event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
      event.setKickMessage(kickMessage);
      return;
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
