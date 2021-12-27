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

package eu.cloudnetservice.cloudnet.modules.labymod.platform;

import static de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties.IS_IN_GAME;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.PlayerManager;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.modules.labymod.LabyModManagement;
import eu.cloudnetservice.cloudnet.modules.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.cloudnet.modules.labymod.config.LabyModPlayerOptions;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class PlatformLabyModManagement implements LabyModManagement {

  private final RPCSender rpcSender;
  private final PlayerManager playerManager;
  private final PlatformBridgeManagement<?, ?> bridgeManagement;
  private LabyModConfiguration configuration;

  public PlatformLabyModManagement() {
    this.rpcSender = Wrapper.instance().rpcProviderFactory().providerForClass(
      Wrapper.instance().networkClient(),
      LabyModManagement.class);
    this.playerManager = Wrapper.instance().servicesRegistry().firstService(PlayerManager.class);
    this.bridgeManagement = Wrapper.instance().servicesRegistry().firstService(PlatformBridgeManagement.class);
    this.setConfigurationSilently(this.rpcSender.invokeMethod("configuration").fireSync());
  }

  @Override
  public @NonNull LabyModConfiguration configuration() {
    return this.configuration;
  }

  @Override
  public void configuration(@NonNull LabyModConfiguration configuration) {
    this.rpcSender.invokeMethod("configuration", configuration).fireSync();
  }

  public void setConfigurationSilently(@NonNull LabyModConfiguration configuration) {
    this.configuration = configuration;
  }

  public void handleServerUpdate(@NonNull CloudPlayer cloudPlayer, @NonNull ServiceInfoSnapshot snapshot) {
    // construct the new discord rpc as the player switched the server
    var discordRPC = this.constructRPCInfo(cloudPlayer, snapshot);
    if (discordRPC != null) {
      // send the updated discord rpc to the player
      this.sendPluginMessage(cloudPlayer, discordRPC);
    }

    var serviceDisplay = this.configuration.gameModeSwitchMessages();
    var playingServer = serviceDisplay.getDisplay(snapshot);
    // if the display is null we can't send anything
    if (playingServer != null) {
      var labyModResponse = JsonDocument.newDocument("show_gamemode", true)
        .append("gamemode_name", playingServer);
      // create a databuf with all information for the gamemode message
      DataBuf gameMode = DataBuf.empty().writeString("server_gamemode").writeString(labyModResponse.toString());
      // send the updated gamemode to the player
      this.sendPluginMessage(cloudPlayer, gameMode);
    }
  }

  public void handleIncomingClientMessage(@NonNull UUID playerId, @Nullable String server, byte @NonNull [] bytes) {
    var dataBuf = DataBufFactory.defaultFactory().createOf(bytes);
    var dataKey = dataBuf.readString();
    var jsonData = JsonDocument.fromJsonString(dataBuf.readString());

    var cloudPlayer = this.playerManager.onlinePlayer(playerId);

    if (cloudPlayer == null) {
      return;
    }

    switch (dataKey) {
      case "INFO" -> this.handleInformationPublish(cloudPlayer, server, jsonData);
      case "discord_rpc" -> this.handleDiscordRPC(cloudPlayer, jsonData);
      default -> {
      }
    }
  }

  protected void handleInformationPublish(
    @NonNull CloudPlayer cloudPlayer,
    @Nullable String server,
    @NonNull JsonDocument jsonData
  ) {
    // check if we need to send the labymod permissions to the player
    var permissions = this.configuration.permissions();
    if (permissions.enabled()) {
      // convert the map into a json document because of the stupidus api
      var jsonWrappedPermissions = JsonDocument.newDocument(permissions.permissions());
      DataBuf permissionData = DataBuf.empty()
        .writeString("PERMISSIONS")
        .writeString(jsonWrappedPermissions.toString());
      // send the permission data to the player using plugin messages
      this.sendPluginMessage(cloudPlayer, permissionData);
    }
    // check if we need to send a banner to the player
    var banner = this.configuration.banner();
    if (banner.enabled()) {
      var jsonWrappedBanner = JsonDocument.newDocument("url", banner.bannerUrl());
      DataBuf bannerData = DataBuf.empty().writeString("server_banner").writeString(jsonWrappedBanner.toString());
      // send the banner data to the player using plugin messages
      this.sendPluginMessage(cloudPlayer, bannerData);
    }
    // only create and send the labymod player options if the player does not have any
    if (cloudPlayer.onlineProperties().contains("labyModOptions")) {
      return;
    }
    // update the creation time of the player options
    var playerOptions = LabyModPlayerOptions.builder(jsonData.toInstanceOf(LabyModPlayerOptions.class))
      .creationTime(System.currentTimeMillis())
      .build();
    this.updatePlayerOptions(cloudPlayer, playerOptions);

    if (server != null) {
      // retrieve the service the player is connected to
      this.bridgeManagement.cachedService(snapshot -> snapshot.name().equals(server)).ifPresent(service -> {
        // construct the new discord rpc for the login server
        var discordRPC = this.constructRPCInfo(cloudPlayer, service);
        if (discordRPC != null) {
          // send the updated discord rpc to the player
          this.sendPluginMessage(cloudPlayer, discordRPC);
        }
      });
    }
  }

  protected void handleDiscordRPC(@NonNull CloudPlayer cloudPlayer, @NonNull JsonDocument jsonData) {
    var joinSecret = jsonData.get("joinSecret", UUID.class);
    if (joinSecret != null) {
      this.getPlayerByJoinSecret(joinSecret).onComplete(player -> {
        if (player != null && player.connectedService() != null) {
          var playerOptions = this.parsePlayerOptions(player);
          // check if the player is using labymod and has its options
          if (playerOptions == null) {
            return;
          }
          var lastRedeem = playerOptions.lastJoinSecretRedeem();
          // only allow one request per second
          if (lastRedeem != -1 && lastRedeem + 1000 > System.currentTimeMillis()) {
            return;
          }

          this.updatePlayerOptions(player, LabyModPlayerOptions.builder(playerOptions)
            .joinRedeemTime(System.currentTimeMillis())
            .build());

          this.connectPlayer(cloudPlayer, player);
        }
      });
    } else {
      var spectateSecret = jsonData.get("spectateSecret", UUID.class);
      this.getPlayerBySpectateSecret(spectateSecret).onComplete(player -> {
        if (player != null && player.connectedService() != null) {
          var playerOptions = this.parsePlayerOptions(player);
          // check if the player is using labymod and has its options
          if (playerOptions == null) {
            return;
          }
          var lastRedeem = playerOptions.lastSpectateSecretRedeem();
          // only allow one request per second
          if (lastRedeem != -1 && lastRedeem + 1000 > System.currentTimeMillis()) {
            return;
          }

          this.updatePlayerOptions(player, LabyModPlayerOptions.builder(playerOptions)
            .spectateRedeemTime(System.currentTimeMillis())
            .build());

          this.connectPlayer(cloudPlayer, player);
        }
      });
    }
  }

  protected @Nullable DataBuf constructRPCInfo(
    @NonNull CloudPlayer cloudPlayer,
    @NonNull ServiceInfoSnapshot snapshot
  ) {
    var playingService = this.configuration.discordRPC().getDisplay(snapshot);
    // only create a rpc information if it's enabled and configured in the config
    if (playingService == null) {
      return null;
    }
    var playerOptions = this.parsePlayerOptions(cloudPlayer);
    // the player has no labymod options, ignore
    if (playerOptions == null) {
      return null;
    }
    var playerOptionsBuilder = LabyModPlayerOptions.builder(playerOptions);

    var joinMatch = this.configuration.discordJoinMatch();
    // used to determine if we need to send the join secret
    var sendJoinSecret = false;
    // check if joining the match is enabled
    if (joinMatch.enabled() && joinMatch.isEnabled(snapshot) && !IS_IN_GAME.read(snapshot).orElse(false)) {
      // create a new join secret
      playerOptionsBuilder.joinSecret(UUID.randomUUID());
      sendJoinSecret = true;
    } else if (playerOptions.joinSecret() != null) {
      // invalidate the existing join secret
      playerOptionsBuilder.joinSecret(null);
    }

    var spectateMatch = this.configuration.discordSpectateMatch();
    // used to determine if we need to send the spectate secret
    var sendSpectateSecret = false;
    // check if spectating a match is allowed
    if (spectateMatch.enabled() && spectateMatch.isEnabled(snapshot) && IS_IN_GAME.read(snapshot).orElse(false)) {
      // create a new spectate secret
      playerOptionsBuilder.spectateSecret(UUID.randomUUID());
      sendSpectateSecret = true;
    } else if (playerOptions.spectateSecret() != null) {
      // invalidate the existing spectate secret
      playerOptionsBuilder.spectateSecret(null);
    }
    var updatedPlayerOptions = playerOptionsBuilder.build();
    // append the updated version of the labymod player options to the cloud player
    this.updatePlayerOptions(cloudPlayer, updatedPlayerOptions);
    // the domain that is used to connect and is displayed in the rpc
    var serverDomain = this.configuration.loginDomain();

    var labyModProtocolResponse = JsonDocument.newDocument()
      .append("hasGame", true)
      .append("game_mode", playingService)
      .append("game_startTime", System.currentTimeMillis())
      .append("game_endTime", 0)
      .append("hasMatchSecret", true)
      .append("matchSecret", cloudPlayer.connectedService().uniqueId() + ":" + serverDomain)
      .append("hasJoinSecret", sendJoinSecret);
    // append join secret if enabled
    if (sendJoinSecret) {
      labyModProtocolResponse.append("joinSecret", updatedPlayerOptions.joinSecret() + ":" + serverDomain);
    }
    labyModProtocolResponse.append("hasSpectateSecret", sendSpectateSecret);
    // append spectate secret if enabled
    if (sendSpectateSecret) {
      labyModProtocolResponse.append("spectateSecret", updatedPlayerOptions.spectateSecret() + ":" + serverDomain);
    }

    return DataBuf.empty().writeString("discord_rpc").writeString(labyModProtocolResponse.toString());
  }

  protected @NonNull Task<@Nullable CloudPlayer> getPlayerByJoinSecret(@NonNull UUID joinSecret) {
    return CompletableTask.supply(() -> {
      for (CloudPlayer player : this.playerManager.onlinePlayers().players()) {
        var playerOptions = this.parsePlayerOptions(player);
        if (playerOptions != null && joinSecret.equals(playerOptions.joinSecret())) {
          return player;
        }
      }

      return null;
    });
  }

  protected @NonNull Task<CloudPlayer> getPlayerBySpectateSecret(@NonNull UUID spectateSecret) {
    return CompletableTask.supply(() -> {
      for (CloudPlayer player : this.playerManager.onlinePlayers().players()) {
        var playerOptions = this.parsePlayerOptions(player);
        if (playerOptions != null && spectateSecret.equals(playerOptions.spectateSecret())) {
          return player;
        }
      }

      return null;
    });
  }

  protected void connectPlayer(@NonNull CloudPlayer player, @NonNull CloudPlayer target) {
    var serviceInfoSnapshot = CloudNetDriver.instance().cloudServiceProvider()
      .service(target.connectedService().uniqueId());
    // check if there is a service to connect to
    if (serviceInfoSnapshot != null) {
      // construct the updated discord rpc
      var discordRPCData = this.constructRPCInfo(player, serviceInfoSnapshot);
      if (discordRPCData != null) {
        this.sendPluginMessage(player, discordRPCData);
      }

      player.playerExecutor().connect(serviceInfoSnapshot.name());
    }
  }

  protected void sendPluginMessage(@NonNull CloudPlayer player, @NonNull DataBuf dataBuf) {
    player.playerExecutor().sendPluginMessage(LABYMOD_CLIENT_CHANNEL, dataBuf.toByteArray());
  }

  protected @Nullable LabyModPlayerOptions parsePlayerOptions(@NonNull CloudPlayer player) {
    return player.onlineProperties().get("labyModOptions", LabyModPlayerOptions.class);
  }

  protected void updatePlayerOptions(@NonNull CloudPlayer cloudPlayer, @NonNull LabyModPlayerOptions playerOptions) {
    cloudPlayer.onlineProperties().append("labyModOptions", playerOptions);
    this.playerManager.updateOnlinePlayer(cloudPlayer);
  }
}
