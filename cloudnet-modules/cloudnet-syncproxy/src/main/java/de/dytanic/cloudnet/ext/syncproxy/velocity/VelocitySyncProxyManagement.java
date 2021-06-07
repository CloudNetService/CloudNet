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

package de.dytanic.cloudnet.ext.syncproxy.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyTabList;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class VelocitySyncProxyManagement extends AbstractSyncProxyManagement {

  private final ProxyServer proxyServer;

  private final Object plugin;

  public VelocitySyncProxyManagement(ProxyServer proxyServer, Object plugin) {
    this.proxyServer = proxyServer;
    this.plugin = plugin;

    this.initialize();
  }

  @Override
  protected void schedule(Runnable runnable, long millis) {
    this.proxyServer.getScheduler().buildTask(this.plugin, runnable).delay(millis, TimeUnit.MILLISECONDS).schedule();
  }

  @Override
  public void updateTabList() {
    if (super.tabListEntryIndex.get() == -1) {
      return;
    }

    this.proxyServer.getAllPlayers().forEach(this::updateTabList);
  }

  public void updateTabList(Player player) {
    if (super.tabListEntryIndex.get() == -1) {
      return;
    }

    player.sendPlayerListHeaderAndFooter(
      LegacyComponentSerializer.legacySection()
        .deserialize(super.tabListHeader != null ? this.replaceTabListItem(player, super.tabListHeader) : ""),
      LegacyComponentSerializer.legacySection()
        .deserialize(super.tabListFooter != null ? this.replaceTabListItem(player, super.tabListFooter) : "")
    );
  }

  private String replaceTabListItem(Player player, String input) {
    String taskName = player.getCurrentServer()
      .map(
        serverConnection -> BridgeProxyHelper.getCachedServiceInfoSnapshot(serverConnection.getServerInfo().getName()))
      .map(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName())
      .orElse("");
    String serverName = player.getCurrentServer().map(serverConnection -> serverConnection.getServerInfo().getName())
      .orElse("");
    input = input
      .replace("%server%", serverName)
      .replace("%task%", taskName)
      .replace("%online_players%", String.valueOf(
        super.loginConfiguration != null ? super.getSyncProxyOnlineCount() : this.proxyServer.getPlayerCount()))
      .replace("%max_players%", String.valueOf(
        super.loginConfiguration != null ? super.loginConfiguration.getMaxPlayers()
          : this.proxyServer.getConfiguration().getShowMaxPlayers()))
      .replace("%name%", player.getUsername())
      .replace("%ping%", String.valueOf(player.getPing()));

    return SyncProxyTabList.replaceTabListItem(input, player.getUniqueId());
  }


  @Override
  protected void checkWhitelist() {
    if (super.loginConfiguration != null) {
      for (Player player : this.proxyServer.getAllPlayers()) {
        if (super.loginConfiguration.isMaintenance()
          && super.loginConfiguration.getWhitelist() != null
          && !super.loginConfiguration.getWhitelist().contains(player.getUsername())
          && !super.loginConfiguration.getWhitelist().contains(player.getUniqueId().toString())
          && !player.hasPermission("cloudnet.syncproxy.maintenance")) {
          player.disconnect(LegacyComponentSerializer.legacySection().deserialize(
            this.replaceColorChar(super.syncProxyConfiguration.getMessages().get("player-login-not-whitelisted"))
          ));
        }
      }
    }
  }

  @Override
  public void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
    if (super.syncProxyConfiguration != null && super.syncProxyConfiguration.showIngameServicesStartStopMessages()) {
      String message = this.replaceColorChar(super.getServiceStateChangeMessage(key, serviceInfoSnapshot));

      for (Player player : this.proxyServer.getAllPlayers()) {
        if (player.hasPermission("cloudnet.syncproxy.notify")) {
          player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
        }
      }
    }
  }

  private String replaceColorChar(String input) {
    char[] translate = input.toCharArray();
    for (int i = 0; i < translate.length - 1; i++) {
      if (translate[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(translate[i + 1]) > -1) {
        translate[i] = '§';
      }
    }

    return new String(translate);
  }
}
