/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.labymod.impl.platform.bungeecord;

import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.impl.platform.PlatformLabyModManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@Singleton
public final class BungeeCordLabyModListener implements Listener {

  private final PlatformLabyModManagement labyModManagement;

  @Inject
  public BungeeCordLabyModListener(@NonNull PlatformLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
  }

  @EventHandler
  public void handlePluginMessage(@NonNull PluginMessageEvent event) {
    var configuration = this.labyModManagement.configuration();
    if (configuration.enabled() && event.getTag().equals(LabyModManagement.LABYMOD_CLIENT_CHANNEL)) {
      if (event.getSender() instanceof ProxiedPlayer player) {
        this.labyModManagement.handleIncomingClientMessage(
          player.getUniqueId(),
          player.getServer() == null ? null : player.getServer().getInfo().getName(),
          event.getData());
      }
    }
  }
}
