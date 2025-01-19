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

package eu.cloudnetservice.modules.labymod.impl.platform;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class PlatformLabyModManagement implements LabyModManagement {

  private final RPCSender rpcSender;
  private final PlayerManager playerManager;
  private final PlatformBridgeManagement<?, ?> bridgeManagement;

  private LabyModConfiguration configuration;

  @Inject
  public PlatformLabyModManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull NetworkClient networkClient,
    @NonNull @Service PlayerManager playerManager,
    @NonNull PlatformBridgeManagement<?, ?> bridgeManagement
  ) {
    this.rpcSender = rpcFactory.newRPCSenderBuilder(LabyModManagement.class).targetComponent(networkClient).build();
    this.playerManager = playerManager;
    this.bridgeManagement = bridgeManagement;
    this.setConfigurationSilently(this.rpcSender.invokeMethod("configuration").fireSync());
  }

  @Override
  public @NonNull LabyModConfiguration configuration() {
    return this.configuration;
  }

  @Override
  public void configuration(@NonNull LabyModConfiguration configuration) {
    this.rpcSender.invokeCaller(configuration).fireSync();
  }

  public void setConfigurationSilently(@NonNull LabyModConfiguration configuration) {
    this.configuration = configuration;
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

    // call the corresponding message handler, if there is one
    if (dataKey.equals("INFO")) {
      this.handleInformationPublish(cloudPlayer, server);
    }
  }

  protected void handleInformationPublish(
    @NonNull CloudPlayer cloudPlayer,
    @Nullable String server
  ) {
    // check if we need to send the labymod permissions to the player
    var permissions = this.configuration.permissions();
    if (permissions.enabled()) {
      // convert the map into a json document because of the stupidus api
      var jsonWrappedPermissions = Document.newJsonDocument().appendTree(permissions.permissions());
      var permissionData = DataBuf.empty()
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

    if (server != null) {
      // retrieve the service the player is connected to
      this.bridgeManagement.cachedService(snapshot -> snapshot.name().equals(server)).ifPresent(service -> {
        // construct the new discord rpc for the login server
        var discordRPC = this.constructRPCInfo(service);
        if (discordRPC != null) {
          this.sendPluginMessage(cloudPlayer, discordRPC);
        }
      });
    }
  }

  protected @Nullable DataBuf constructRPCInfo(@NonNull ServiceInfoSnapshot snapshot) {
    // only create a rpc information if it's enabled and configured in the config
    var playingService = this.configuration.discordRPC().display(snapshot);
    if (playingService == null) {
      return null;
    }

    // build the response to the message
    var labyModProtocolResponse = Document.newJsonDocument()
      .append("hasGame", true)
      .append("game_mode", playingService)
      .append("game_startTime", System.currentTimeMillis())
      .append("game_endTime", 0);

    // compile the message
    return DataBuf.empty().writeString("discord_rpc").writeString(labyModProtocolResponse.toString());
  }

  protected void sendPluginMessage(@NonNull CloudPlayer player, @NonNull DataBuf dataBuf) {
    this.playerManager.playerExecutor(player.uniqueId())
      .sendPluginMessage(LABYMOD_CLIENT_CHANNEL, dataBuf.toByteArray());
  }
}
