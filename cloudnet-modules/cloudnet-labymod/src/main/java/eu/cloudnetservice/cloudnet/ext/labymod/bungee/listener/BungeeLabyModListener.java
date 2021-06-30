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

package eu.cloudnetservice.cloudnet.ext.labymod.bungee.listener;

import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeLabyModListener implements Listener {

  private final AbstractLabyModManagement labyModManagement;

  public BungeeLabyModListener(AbstractLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
  }

  @EventHandler
  public void handle(ServerConnectedEvent event) {
    this.labyModManagement.sendServerUpdate(event.getPlayer().getUniqueId(), event.getServer().getInfo().getName());
  }

  @EventHandler
  public void handle(PluginMessageEvent event) {
    LabyModConfiguration configuration = LabyModUtils.getConfiguration();
    if (configuration == null || !configuration.isEnabled() || !event.getTag()
      .equals(LabyModConstants.LMC_CHANNEL_NAME)) {
      return;
    }

    if (!(event.getSender() instanceof ProxiedPlayer)) {
      return;
    }

    ProxiedPlayer player = (ProxiedPlayer) event.getSender();

    this.labyModManagement.handleChannelMessage(player.getUniqueId(), event.getData());
  }

}
