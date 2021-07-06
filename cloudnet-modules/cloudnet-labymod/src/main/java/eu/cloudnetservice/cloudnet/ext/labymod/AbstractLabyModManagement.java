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

package eu.cloudnetservice.cloudnet.ext.labymod;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import eu.cloudnetservice.cloudnet.ext.labymod.listener.LabyModListener;
import eu.cloudnetservice.cloudnet.ext.labymod.player.LabyModPlayerOptions;
import java.util.UUID;

public abstract class AbstractLabyModManagement {

  private final IPlayerManager playerManager;

  public AbstractLabyModManagement(IPlayerManager playerManager) {
    this.playerManager = playerManager;
    CloudNetDriver.getInstance().getEventManager().registerListener(new LabyModListener(this));
  }

  public IPlayerManager getPlayerManager() {
    return this.playerManager;
  }

  protected abstract void connectPlayer(UUID playerId, String target);

  protected abstract void sendData(UUID playerId, byte[] data);

  public void sendPermissions(UUID uniqueId) {
    if (!LabyModUtils.getConfiguration().getPermissionConfig().isEnabled()) {
      return;
    }

    final byte[] permissionBytes = LabyModChannelUtils.getLMCMessageContents(
      "PERMISSIONS",
      JsonDocument.newDocument(LabyModUtils.getConfiguration().getPermissionConfig().getLabyModPermissions())
    );

    this.sendData(uniqueId, permissionBytes);
  }

  public void sendBanner(UUID uniqueId) {
    if (!LabyModUtils.getConfiguration().getBannerConfig().isEnabled()) {
      return;
    }

    final byte[] bannerBytes = LabyModChannelUtils.getLMCMessageContents(
      "server_banner",
      JsonDocument.newDocument("url", LabyModUtils.getConfiguration().getBannerConfig().getBannerUrl())
    );

    this.sendData(uniqueId, bannerBytes);
  }

  public void connectTo(UUID player, ICloudPlayer target) {
    ServiceInfoSnapshot connectedService = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(target.getConnectedService().getServerName());

    if (connectedService == null) {
      this.playerManager.updateOnlinePlayer(target);
      return;
    }

    byte[] discordRPCData = LabyModUtils.getDiscordRPCGameInfoUpdateMessageContents(target, connectedService);
    if (discordRPCData != null) {
      target.getPlayerExecutor().sendPluginMessage(LabyModConstants.LMC_CHANNEL_NAME, discordRPCData);
    }

    this.connectPlayer(player, connectedService.getName());
  }

  public void sendServerUpdate(UUID playerId, ICloudPlayer cloudPlayer, ServiceInfoSnapshot serviceInfoSnapshot) {
    byte[] gameModeData = LabyModUtils.getShowGameModeMessageContents(serviceInfoSnapshot);
    byte[] discordRPCData = LabyModUtils.getDiscordRPCGameInfoUpdateMessageContents(cloudPlayer, serviceInfoSnapshot);
    if (gameModeData != null) {
      this.sendData(playerId, gameModeData);
    }
    if (discordRPCData != null) {
      this.sendData(playerId, discordRPCData);
    }
  }

  public void sendServerUpdate(UUID playerId, String server) {
    ICloudPlayer cloudPlayer = this.playerManager.getOnlinePlayer(playerId);
    if (cloudPlayer != null && LabyModUtils.getLabyModOptions(cloudPlayer) != null) {
      ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper.getCachedServiceInfoSnapshot(server);
      this.sendServerUpdate(playerId, cloudPlayer, serviceInfoSnapshot);
    }
  }

  public void handleChannelMessage(UUID playerId, byte[] data) {
    Pair<String, JsonDocument> pair = LabyModChannelUtils.readLMCMessageContents(data);
    String messageKey = pair.getFirst();
    JsonDocument messageContents = pair.getSecond();

    ICloudPlayer cloudPlayer = this.playerManager.getOnlinePlayer(playerId);
    if (cloudPlayer == null) {
      return;
    }

    if (messageKey.equals("INFO")) {
      this.handleInfo(cloudPlayer, messageContents);
    } else if (messageKey.equals("discord_rpc")) {
      this.handleDiscordRPC(cloudPlayer, messageContents);
    }
  }

  private void handleInfo(ICloudPlayer cloudPlayer, JsonDocument messageContents) {
    this.sendBanner(cloudPlayer.getUniqueId());
    this.sendPermissions(cloudPlayer.getUniqueId());

    if (LabyModUtils.getLabyModOptions(cloudPlayer) != null) {
      return;
    }

    LabyModPlayerOptions labyModOptions = messageContents.toInstanceOf(LabyModPlayerOptions.class);
    if (!labyModOptions.isValid()) {
      return;
    }
    labyModOptions.setCreationTime(System.currentTimeMillis());
    LabyModUtils.setLabyModOptions(cloudPlayer, labyModOptions);
    this.playerManager.updateOnlinePlayer(cloudPlayer);

    if (cloudPlayer.getConnectedService() != null) {
      ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
        .getCachedServiceInfoSnapshot(cloudPlayer.getConnectedService().getServerName());
      this.sendServerUpdate(cloudPlayer.getUniqueId(), cloudPlayer, serviceInfoSnapshot);
    }
  }

  private void handleDiscordRPC(ICloudPlayer cloudPlayer, JsonDocument messageContents) {
    if (messageContents.contains("joinSecret")) {
      UUID joinSecret = messageContents.get("joinSecret", UUID.class);
      if (joinSecret == null) {
        return;
      }

      LabyModUtils.getPlayerByJoinSecret(joinSecret).onComplete(secretOwner -> {
        if (secretOwner == null || secretOwner.getConnectedService() == null) {
          return;
        }
        LabyModPlayerOptions options = LabyModUtils.getLabyModOptions(secretOwner);
        if (options == null || (options.getLastJoinSecretRedeem() != -1 &&
          options.getLastJoinSecretRedeem() + 1000 > System.currentTimeMillis())) {
          return;
        }
        options.setLastJoinSecretRedeem(System.currentTimeMillis());

        this.connectTo(cloudPlayer.getUniqueId(), secretOwner);
      });
    }

    if (messageContents.contains("spectateSecret")) {
      UUID spectateSecret = messageContents.get("spectateSecret", UUID.class);
      if (spectateSecret == null) {
        return;
      }

      LabyModUtils.getPlayerBySpectateSecret(spectateSecret).onComplete(secretOwner -> {
        if (secretOwner == null || secretOwner.getConnectedService() == null) {
          return;
        }
        LabyModPlayerOptions options = LabyModUtils.getLabyModOptions(secretOwner);
        if (options == null || (options.getLastSpectateSecretRedeem() != -1 &&
          options.getLastSpectateSecretRedeem() + 1000 > System.currentTimeMillis())) {
          return;
        }
        options.setLastSpectateSecretRedeem(System.currentTimeMillis());

        this.connectTo(cloudPlayer.getUniqueId(), secretOwner);
      });
    }
  }

}
