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

package eu.cloudnetservice.cloudnet.ext.labymod.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;

public class VelocityLabyModListener {

  private final AbstractLabyModManagement labyModManagement;

  public VelocityLabyModListener(AbstractLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
  }

  @Subscribe
  public void handleServerConnected(ServerConnectedEvent event) {
    this.labyModManagement
      .sendServerUpdate(event.getPlayer().getUniqueId(), event.getServer().getServerInfo().getName());
  }

  @Subscribe
  public void handlePluginMessage(PluginMessageEvent event) {
    LabyModConfiguration configuration = LabyModUtils.getConfiguration();
    if (configuration == null || !configuration.isEnabled() || !event.getIdentifier().getId()
      .equals(LabyModConstants.LMC_CHANNEL_NAME)) {
      return;
    }

    if (!(event.getSource() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getSource();

    this.labyModManagement.handleChannelMessage(player.getUniqueId(), event.getData());
  }

}
