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

package eu.cloudnetservice.cloudnet.ext.labymod.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.node.CloudNetLabyModModule;
import java.util.UUID;

public class LabyModCustomChannelMessageListener {

  private final CloudNetLabyModModule module;

  private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry()
    .getFirstService(IPlayerManager.class);

  public LabyModCustomChannelMessageListener(CloudNetLabyModModule module) {
    this.module = module;
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equals(LabyModConstants.CLOUDNET_CHANNEL_NAME) || event.getMessage() == null || !event
      .isQuery()) {
      return;
    }

    switch (event.getMessage()) {
      case LabyModConstants.GET_CONFIGURATION:
        event.setJsonResponse(JsonDocument.newDocument("labyModConfig", this.module.getConfiguration()));
        break;

      case LabyModConstants.GET_PLAYER_JOIN_SECRET:
        UUID joinSecret = event.getBuffer().readUUID();
        event.createBinaryResponse().writeOptionalObject(this.getPlayerByJoinSecret(joinSecret));
        break;

      case LabyModConstants.GET_PLAYER_SPECTATE_SECRET:
        UUID spectateSecret = event.getBuffer().readUUID();
        event.createBinaryResponse().writeOptionalObject(this.getPlayerBySpectateSecret(spectateSecret));
        break;
      default:
        break;
    }
  }

  private ICloudPlayer getPlayerByJoinSecret(UUID joinSecret) {
    return this.playerManager.onlinePlayers().asPlayers()
      .stream()
      .filter(o -> LabyModUtils.getLabyModOptions(o) != null)
      .filter(o -> LabyModUtils.getLabyModOptions(o).getJoinSecret() != null)
      .filter(o -> LabyModUtils.getLabyModOptions(o).getJoinSecret().equals(joinSecret))
      .findFirst()
      .orElse(null);
  }

  private ICloudPlayer getPlayerBySpectateSecret(UUID spectateSecret) {
    return this.playerManager.onlinePlayers().asPlayers()
      .stream()
      .filter(o -> LabyModUtils.getLabyModOptions(o) != null)
      .filter(o -> LabyModUtils.getLabyModOptions(o).getSpectateSecret() != null)
      .filter(o -> LabyModUtils.getLabyModOptions(o).getSpectateSecret().equals(spectateSecret))
      .findFirst()
      .orElse(null);
  }

}
