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

package eu.cloudnetservice.cloudnet.ext.labymod.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import java.util.UUID;

public class VelocityLabyModManagement extends AbstractLabyModManagement {

  private final ProxyServer proxyServer;
  private final ChannelIdentifier channelIdentifier;

  public VelocityLabyModManagement(ProxyServer proxyServer, ChannelIdentifier channelIdentifier) {
    super(CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class));
    this.proxyServer = proxyServer;
    this.channelIdentifier = channelIdentifier;
  }

  @Override
  protected void connectPlayer(UUID playerId, String target) {
    this.proxyServer.getPlayer(playerId).ifPresent(player ->
      this.proxyServer.getServer(target).ifPresent(registeredServer ->
        player.createConnectionRequest(registeredServer).connect()
      )
    );
  }

  @Override
  protected void sendData(UUID playerId, byte[] data) {
    this.proxyServer.getPlayer(playerId).ifPresent(player -> player.sendPluginMessage(this.channelIdentifier, data));
  }
}
