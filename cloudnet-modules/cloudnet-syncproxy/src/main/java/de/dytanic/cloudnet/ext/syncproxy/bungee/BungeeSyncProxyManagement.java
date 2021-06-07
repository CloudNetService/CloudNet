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

package de.dytanic.cloudnet.ext.syncproxy.bungee;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyTabList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeSyncProxyManagement extends AbstractSyncProxyManagement {

  private final Plugin plugin;

  public BungeeSyncProxyManagement(Plugin plugin) {
    this.plugin = plugin;
    this.initialize();
  }

  @Override
  protected void schedule(Runnable runnable, long millis) {
    ProxyServer.getInstance().getScheduler().schedule(this.plugin, runnable, millis, TimeUnit.MILLISECONDS);
  }

  @Override
  public void updateTabList() {
    if (super.tabListEntryIndex.get() == -1) {
      return;
    }

    ProxyServer.getInstance().getPlayers().forEach(this::updateTabList);
  }

  public void updateTabList(ProxiedPlayer proxiedPlayer) {
    if (super.tabListEntryIndex.get() == -1) {
      return;
    }

    proxiedPlayer.setTabHeader(
      TextComponent.fromLegacyText(super.tabListHeader != null ?
        this.replaceTabListItem(proxiedPlayer, ChatColor.translateAlternateColorCodes('&', super.tabListHeader))
        : ""
      ),
      TextComponent.fromLegacyText(super.tabListFooter != null ?
        this.replaceTabListItem(proxiedPlayer, ChatColor.translateAlternateColorCodes('&', super.tabListFooter))
        : ""
      )
    );
  }

  private String replaceTabListItem(ProxiedPlayer proxiedPlayer, String input) {
    String taskName = "";
    if (proxiedPlayer.getServer() != null) {
      ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
        .getCachedServiceInfoSnapshot(proxiedPlayer.getServer().getInfo().getName());
      if (serviceInfoSnapshot != null) {
        taskName = serviceInfoSnapshot.getServiceId().getTaskName();
      }
    }
    input = input
      .replace("%server%", proxiedPlayer.getServer() != null ? proxiedPlayer.getServer().getInfo().getName() : "")
      .replace("%task%", taskName)
      .replace("%online_players%", String.valueOf(super.loginConfiguration != null ? super.getSyncProxyOnlineCount()
        : ProxyServer.getInstance().getOnlineCount()))
      .replace("%max_players%", String.valueOf(
        super.loginConfiguration != null ? super.loginConfiguration.getMaxPlayers()
          : proxiedPlayer.getPendingConnection().getListener().getMaxPlayers()))
      .replace("%name%", proxiedPlayer.getName())
      .replace("%ping%", String.valueOf(proxiedPlayer.getPing()));

    return SyncProxyTabList.replaceTabListItem(input, proxiedPlayer.getUniqueId());
  }

  @Override
  protected void checkWhitelist() {
    SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = super.getLoginConfiguration();

    if (syncProxyProxyLoginConfiguration != null) {
      for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers()) {
        if (syncProxyProxyLoginConfiguration.isMaintenance()
          && syncProxyProxyLoginConfiguration.getWhitelist() != null
          && !syncProxyProxyLoginConfiguration.getWhitelist().contains(proxiedPlayer.getName())) {
          UUID uniqueId = proxiedPlayer.getUniqueId();

          if (syncProxyProxyLoginConfiguration.getWhitelist().contains(uniqueId.toString())) {
            continue;
          }

          if (!proxiedPlayer.hasPermission("cloudnet.syncproxy.maintenance")) {
            proxiedPlayer.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
              super.getSyncProxyConfiguration().getMessages().get("player-login-not-whitelisted")))
            );
          }
        }
      }
    }
  }

  @Override
  public void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
    if (super.syncProxyConfiguration != null && super.syncProxyConfiguration.showIngameServicesStartStopMessages()) {
      String message = ChatColor
        .translateAlternateColorCodes('&', super.getServiceStateChangeMessage(key, serviceInfoSnapshot));

      for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
        if (player.hasPermission("cloudnet.syncproxy.notify")) {
          player.sendMessage(TextComponent.fromLegacyText(message));
        }
      }
    }
  }

}
