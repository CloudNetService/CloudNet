/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.labymod.platform;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.modules.labymod.config.LabyModPlayerOptions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class PlatformLabyModManagement implements LabyModManagement {

  private final RPCSender rpcSender;
  private final PlayerManager playerManager;
  private final CloudServiceProvider cloudServiceProvider;
  private final PlatformBridgeManagement<?, ?> bridgeManagement;

  private LabyModConfiguration configuration;

  @Inject
  public PlatformLabyModManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull NetworkClient networkClient,
    @NonNull CloudServiceProvider cloudServiceProvider
  ) {
    this.cloudServiceProvider = cloudServiceProvider;
    this.rpcSender = rpcFactory.providerForClass(networkClient, LabyModManagement.class);
    this.playerManager = ServiceRegistry.first(PlayerManager.class);
    this.bridgeManagement = ServiceRegistry.first(PlatformBridgeManagement.class);
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
    var playingServer = serviceDisplay.display(snapshot, cloudPlayer);

    // if the display is null we can't send anything
    if (playingServer != null) {
      var labyModResponse = Document.newJsonDocument()
        .append("show_gamemode", true)
        .append("gamemode_name", playingServer);

      // create & send a databuf with all information for the gamemode message
      var gameMode = DataBuf.empty().writeString("server_gamemode").writeString(labyModResponse.toString());
      this.sendPluginMessage(cloudPlayer, gameMode);
    }
  }

  public void handleIncomingClientMessage(@NonNull UUID playerId, @Nullable String server, byte[] bytes) {
    // get the player that send the message, if possible
    var cloudPlayer = this.playerManager.onlinePlayer(playerId);
    if (cloudPlayer == null) {
      return;
    }

    var dataBuf = DataBufFactory.defaultFactory().fromBytes(bytes);

    // each LabyMod data packet consists of 2 string values: the message key and the message value, encoded as json
    // See https://docs.labymod.net/pages/server/protocol/onjoin/ for an example
    // in normal cases the key and value have a maximum length of Short.MAX_VALUE (=32767), but we don't care here
    var dataKey = dataBuf.readString();
    var jsonData = DocumentFactory.json().parse(dataBuf.readString());

    // call the corresponding message handler, if there is one
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
    @NonNull Document jsonData
  ) {
    // check if we need to send the labymod permissions to the player
    var permissions = this.configuration.permissions();
    if (permissions.enabled()) {
      // convert the map into a json document because of the stupidus api
      var jsonWrappedPermissions = Document.newJsonDocument().appendTree(permissions.permissions());
      DataBuf permissionData = DataBuf.empty()
        .writeString("PERMISSIONS")
        .writeString(jsonWrappedPermissions.serializeToString());

      // send the permission data to the player using plugin messages
      this.sendPluginMessage(cloudPlayer, permissionData);
    }

    // check if we need to send a banner to the player
    var banner = this.configuration.banner();
    if (banner.enabled()) {
      var jsonWrappedBanner = Document.newJsonDocument().append("url", banner.bannerUrl());
      var bannerData = DataBuf.empty().writeString("server_banner").writeString(jsonWrappedBanner.toString());

      // send the banner data to the player using plugin messages
      this.sendPluginMessage(cloudPlayer, bannerData);
    }

    // append the LabyMod player options to the player object unless we already did so before
    if (!cloudPlayer.onlineProperties().contains("labyModOptions")) {
      var playerOptions = LabyModPlayerOptions.builder(jsonData.toInstanceOf(LabyModPlayerOptions.class))
        .creationTime(System.currentTimeMillis())
        .build();
      this.updatePlayerOptions(cloudPlayer, playerOptions);
    }

    if (server != null) {
      // retrieve the service the player is connected to
      this.bridgeManagement.cachedService(snapshot -> snapshot.name().equals(server)).ifPresent(service -> {
        // construct the new discord rpc for the login server
        var discordRPC = this.constructRPCInfo(cloudPlayer, service);
        if (discordRPC != null) {
          this.sendPluginMessage(cloudPlayer, discordRPC);
        }
      });
    }
  }

  protected void handleDiscordRPC(@NonNull CloudPlayer cloudPlayer, @NonNull Document jsonData) {
    // check if the join secret was supplied
    var joinSecret = jsonData.readObject("joinSecret", UUID.class);
    if (joinSecret != null) {
      this.playerByJoinSecret(joinSecret).thenAccept(player -> {
        if (player != null && player.connectedService() != null) {
          // check if the player is using LabyMod and has its options
          var playerOptions = this.parsePlayerOptions(player);
          if (playerOptions == null) {
            return;
          }

          // only allow one request per second
          var lastRedeem = playerOptions.lastJoinSecretRedeem();
          if (lastRedeem != -1 && lastRedeem + 1000 > System.currentTimeMillis()) {
            return;
          }

          this.updatePlayerOptions(player, LabyModPlayerOptions.builder(playerOptions)
            .joinRedeemTime(System.currentTimeMillis())
            .build());
          this.connectPlayer(cloudPlayer, player);
        }
      });
      return;
    }

    // check if the spectate secret was supplied
    var spectateSecret = jsonData.readObject("spectateSecret", UUID.class);
    if (spectateSecret != null) {
      this.playerBySpectateSecret(spectateSecret).thenAccept(player -> {
        if (player != null && player.connectedService() != null) {
          // check if the player is using LabyMod and has its options
          var playerOptions = this.parsePlayerOptions(player);
          if (playerOptions == null) {
            return;
          }

          // only allow one request per second
          var lastRedeem = playerOptions.lastSpectateSecretRedeem();
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
    // only create a rpc information if it's enabled and configured in the config
    var playingService = this.configuration.discordRPC().display(snapshot, cloudPlayer);
    if (playingService == null) {
      return null;
    }

    // the player has no LabyMod options, ignore
    var playerOptions = this.parsePlayerOptions(cloudPlayer);
    if (playerOptions == null) {
      return null;
    }

    var serverDomain = this.configuration.loginDomain();
    var joinMatch = this.configuration.discordJoinMatch();
    var spectateMatch = this.configuration.discordSpectateMatch();
    var playerOptionsBuilder = LabyModPlayerOptions.builder(playerOptions);

    // check if joining the match is enabled
    var sendJoinSecret = false;
    if (joinMatch.enabled() && joinMatch.enabled(snapshot) && !BridgeServiceHelper.inGameService(snapshot)) {
      // create a new join secret
      playerOptionsBuilder.joinSecret(UUID.randomUUID());
      sendJoinSecret = true;
    } else if (playerOptions.joinSecret() != null) {
      // invalidate the existing join secret
      playerOptionsBuilder.joinSecret(null);
    }

    // check if spectating a match is allowed
    var sendSpectateSecret = false;
    if (spectateMatch.enabled() && spectateMatch.enabled(snapshot) && BridgeServiceHelper.inGameService(snapshot)) {
      // create a new spectate secret
      playerOptionsBuilder.spectateSecret(UUID.randomUUID());
      sendSpectateSecret = true;
    } else if (playerOptions.spectateSecret() != null) {
      // invalidate the existing spectate secret
      playerOptionsBuilder.spectateSecret(null);
    }

    // append the updated version of the LabyMod player options to the cloud player
    var updatedPlayerOptions = playerOptionsBuilder.build();
    this.updatePlayerOptions(cloudPlayer, updatedPlayerOptions);

    // build the response to the message
    var labyModProtocolResponse = Document.newJsonDocument()
      .append("hasGame", true)
      .append("game_mode", playingService)
      .append("game_startTime", System.currentTimeMillis())
      .append("game_endTime", 0)
      .append("hasMatchSecret", true)
      .append("matchSecret", cloudPlayer.connectedService().uniqueId() + ":" + serverDomain);

    // append the join secret information
    labyModProtocolResponse.append("hasJoinSecret", sendJoinSecret);
    if (sendJoinSecret) {
      labyModProtocolResponse.append("joinSecret", updatedPlayerOptions.joinSecret() + ":" + serverDomain);
    }

    // append the spectate secret information
    labyModProtocolResponse.append("hasSpectateSecret", sendSpectateSecret);
    if (sendSpectateSecret) {
      labyModProtocolResponse.append("spectateSecret", updatedPlayerOptions.spectateSecret() + ":" + serverDomain);
    }

    // compile the message
    return DataBuf.empty().writeString("discord_rpc").writeString(labyModProtocolResponse.toString());
  }

  protected @NonNull Task<@Nullable CloudPlayer> playerByJoinSecret(@NonNull UUID joinSecret) {
    return Task.supply(() -> {
      var players = this.playerManager.onlinePlayers().players();
      return players.stream().filter(player -> {
        // get the player options if present
        var playerOptions = this.parsePlayerOptions(player);
        return playerOptions != null && joinSecret.equals(playerOptions.joinSecret());
      }).findFirst().orElse(null);
    });
  }

  protected @NonNull Task<CloudPlayer> playerBySpectateSecret(@NonNull UUID spectateSecret) {
    return Task.supply(() -> {
      var players = this.playerManager.onlinePlayers().players();
      return players.stream().filter(player -> {
        // get the player options if present
        var playerOptions = this.parsePlayerOptions(player);
        return playerOptions != null && spectateSecret.equals(playerOptions.spectateSecret());
      }).findFirst().orElse(null);
    });
  }

  protected void connectPlayer(@NonNull CloudPlayer player, @NonNull CloudPlayer target) {
    // check if there is a service to connect to
    var serviceInfoSnapshot = this.cloudServiceProvider.service(target.connectedService().uniqueId());
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
    return player.onlineProperties().readObject("labyModOptions", LabyModPlayerOptions.class);
  }

  protected void updatePlayerOptions(@NonNull CloudPlayer cloudPlayer, @NonNull LabyModPlayerOptions playerOptions) {
    cloudPlayer.onlineProperties(cloudPlayer.onlineProperties().mutableCopy().append("labyModOptions", playerOptions));
    this.playerManager.updateOnlinePlayer(cloudPlayer);
  }
}
