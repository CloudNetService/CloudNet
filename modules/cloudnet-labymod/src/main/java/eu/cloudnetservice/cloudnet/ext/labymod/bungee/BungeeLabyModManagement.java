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

package eu.cloudnetservice.cloudnet.ext.labymod.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeLabyModManagement extends AbstractLabyModManagement {

  private final ProxyServer proxyServer;

  public BungeeLabyModManagement(ProxyServer proxyServer) {
    super(CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class));
    this.proxyServer = proxyServer;
  }

  @Override
  protected void connectPlayer(UUID playerId, String target) {
    ProxiedPlayer player = this.proxyServer.getPlayer(playerId);
    player.connect(ProxyServer.getInstance().getServerInfo(target));
  }

  @Override
  protected void sendData(UUID playerId, byte[] data) {
    ProxiedPlayer player = this.proxyServer.getPlayer(playerId);
    player.sendData(LabyModConstants.LMC_CHANNEL_NAME, data);
  }
}
