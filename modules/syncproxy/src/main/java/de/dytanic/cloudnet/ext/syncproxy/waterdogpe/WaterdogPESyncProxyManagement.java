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

package de.dytanic.cloudnet.ext.syncproxy.waterdogpe;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import java.util.List;

public class WaterdogPESyncProxyManagement extends AbstractSyncProxyManagement {

  public WaterdogPESyncProxyManagement() {
    this.initialize();
  }

  @Override
  protected void schedule(Runnable runnable, long millis) {
    ProxyServer.getInstance().getScheduler().scheduleDelayed(runnable, (int) millis / 50, true);
  }

  @Override
  public void updateTabList() {
  }

  @Override
  protected void checkWhitelist() {
    SyncProxyProxyLoginConfiguration configuration = super.getLoginConfiguration();
    if (configuration != null && configuration.isMaintenance()) {
      List<String> whitelist = configuration.getWhitelist();
      for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers().values()) {
        if (!this.isWhitelisted(player, whitelist)) {
          player.disconnect(
            super.getSyncProxyConfiguration().getMessages().get("player-login-not-whitelisted").replace('&', '§'));
        }
      }
    }
  }

  @Override
  protected void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
    if (super.syncProxyConfiguration != null && super.syncProxyConfiguration.showIngameServicesStartStopMessages()) {
      String message = super.getServiceStateChangeMessage(key, serviceInfoSnapshot).replace('&', '§');
      for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers().values()) {
        if (player.hasPermission("cloudnet.syncproxy.notify")) {
          player.sendMessage(message);
        }
      }
    }
  }

  public boolean isWhitelisted(ProxiedPlayer player, List<String> whitelist) {
    if (whitelist != null
      && (whitelist.contains(player.getName()) || whitelist.contains(player.getUniqueId().toString()))) {
      return true;
    }

    return player.hasPermission("cloudnet.syncproxy.maintenance");
  }
}
