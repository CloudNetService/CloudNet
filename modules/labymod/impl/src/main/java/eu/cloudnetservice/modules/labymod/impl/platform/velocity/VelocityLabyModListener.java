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

package eu.cloudnetservice.modules.labymod.impl.platform.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.impl.platform.PlatformLabyModManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class VelocityLabyModListener {

  private final PlatformLabyModManagement labyModManagement;

  @Inject
  public VelocityLabyModListener(@NonNull PlatformLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
  }

  @Subscribe
  public void handlePluginMessage(@NonNull PluginMessageEvent event) {
    var configuration = this.labyModManagement.configuration();
    if (configuration.enabled() && event.getIdentifier().getId().equals(LabyModManagement.LABYMOD_CLIENT_CHANNEL)) {
      if (event.getSource() instanceof Player player) {
        this.labyModManagement.handleIncomingClientMessage(
          player.getUniqueId(),
          player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse(null),
          event.getData());
      }
    }
  }
}
