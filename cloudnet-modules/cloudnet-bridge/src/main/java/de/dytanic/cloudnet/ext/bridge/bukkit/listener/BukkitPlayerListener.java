package de.dytanic.cloudnet.ext.bridge.bukkit.listener;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class BukkitPlayerListener implements Listener {

  private final Collection<UUID> accessUniqueIds = Iterables
      .newCopyOnWriteArrayList();

  private final Collection<String> accessNames = Iterables
      .newCopyOnWriteArrayList();

  private final BukkitCloudNetBridgePlugin plugin;

  @EventHandler
  public void handle(BukkitBridgeProxyPlayerServerConnectRequestEvent event) {
    BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider
        .load();

    if (Bukkit.getOnlineMode()) {
      return;
    }

    if (bridgeConfiguration != null
        && bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() != null) {
      for (String group : bridgeConfiguration
          .getExcludedOnlyProxyWalkableGroups()) {
        if (Iterables.contains(group,
            Wrapper.getInstance().getServiceConfiguration().getGroups())) {
          return;
        }
      }
    }

    if (event.getNetworkConnectionInfo().getUniqueId() != null) {

      UUID uniqueId = event.getNetworkConnectionInfo().getUniqueId();

      accessUniqueIds.add(uniqueId);
      Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
        @Override
        public void run() {
          accessUniqueIds.remove(uniqueId);
        }
      }, 40);
      return;
    }

    if (event.getNetworkConnectionInfo().getName() != null) {
      String name = event.getNetworkConnectionInfo().getName();

      accessNames.add(name);
      Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
        @Override
        public void run() {
          accessNames.remove(name);
        }
      }, 40);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handle(PlayerLoginEvent event) {
    BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider
        .load();

    boolean onlyProxyProtection = true;

    if (Bukkit.getOnlineMode()) {
      onlyProxyProtection = false;
    }

    if (onlyProxyProtection && bridgeConfiguration != null
        && bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() != null) {
      for (String group : bridgeConfiguration
          .getExcludedOnlyProxyWalkableGroups()) {
        if (Iterables.contains(group,
            Wrapper.getInstance().getServiceConfiguration().getGroups())) {
          onlyProxyProtection = false;
          break;
        }
      }
    }

    if (onlyProxyProtection) {
      UUID uniqueId = event.getPlayer().getUniqueId();
      boolean checkName = true;

      if (uniqueId != null) {
        if (!accessUniqueIds.contains(uniqueId)) {
          event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
          event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
              bridgeConfiguration.getMessages()
                  .get("server-join-cancel-because-only-proxy")));
          return;

        } else {
          accessUniqueIds.remove(uniqueId);
        }

        checkName = false;
      }

      if (checkName) {
        String name = event.getPlayer().getName();

        if (name != null) {
          if (!accessNames.contains(name)) {
            event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
            event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
                bridgeConfiguration.getMessages()
                    .get("server-join-cancel-because-only-proxy")));
            return;

          } else {
            accessNames.remove(name);
          }
        }
      }
    }

    BridgeHelper.sendChannelMessageServerLoginRequest(
        BukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        BukkitCloudNetHelper
            .createNetworkPlayerServerInfo(event.getPlayer(), true)
    );
  }

  @EventHandler
  public void handle(PlayerJoinEvent event) {
    BridgeHelper.sendChannelMessageServerLoginSuccess(
        BukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        BukkitCloudNetHelper
            .createNetworkPlayerServerInfo(event.getPlayer(), false));

    BridgeHelper.updateServiceInfo();
  }

  @EventHandler
  public void handle(PlayerQuitEvent event) {
    BridgeHelper.sendChannelMessageServerDisconnect(
        BukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        BukkitCloudNetHelper
            .createNetworkPlayerServerInfo(event.getPlayer(), false));

    Wrapper.getInstance().runTask(new Runnable() {
      @Override
      public void run() {
        BridgeHelper.updateServiceInfo();
      }
    });
  }

  /*= ---------------------------------------------------------------------------------------------------- =*/
}