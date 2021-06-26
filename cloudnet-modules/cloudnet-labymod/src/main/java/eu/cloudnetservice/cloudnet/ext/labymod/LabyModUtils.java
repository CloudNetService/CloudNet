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

import static eu.cloudnetservice.cloudnet.ext.labymod.LabyModChannelUtils.getLMCMessageContents;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.labymod.config.DiscordJoinMatchConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.cloudnet.ext.labymod.config.ServiceDisplay;
import eu.cloudnetservice.cloudnet.ext.labymod.player.LabyModPlayerOptions;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class LabyModUtils {

  private static LabyModConfiguration cachedConfiguration;

  private LabyModUtils() {
    throw new UnsupportedOperationException();
  }

  public static LabyModConfiguration getConfiguration() {
    if (cachedConfiguration == null) {
      ChannelMessage response = ChannelMessage.builder()
        .channel(LabyModConstants.CLOUDNET_CHANNEL_NAME)
        .message(LabyModConstants.GET_CONFIGURATION)
        .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
        .build()
        .sendSingleQuery();
      if (response != null) {
        cachedConfiguration = response.getJson().get("labyModConfig", LabyModConfiguration.class);
      }
    }

    return cachedConfiguration;
  }

  public static void setLabyModOptions(ICloudPlayer cloudPlayer, LabyModPlayerOptions options) {
    cloudPlayer.getOnlineProperties().append("labyModOptions", options);
  }

  public static LabyModPlayerOptions getLabyModOptions(ICloudPlayer cloudPlayer) {
    return cloudPlayer.getOnlineProperties().get("labyModOptions", LabyModPlayerOptions.class);
  }

  @NotNull
  public static ITask<ICloudPlayer> getPlayerByJoinSecret(UUID joinSecret) {
    return ChannelMessage.builder()
      .channel(LabyModConstants.CLOUDNET_CHANNEL_NAME)
      .message(LabyModConstants.GET_PLAYER_JOIN_SECRET)
      .buffer(ProtocolBuffer.create().writeUUID(joinSecret))
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQueryAsync()
      .map(message -> message.getBuffer().readOptionalObject(CloudPlayer.class));
  }

  @NotNull
  public static ITask<ICloudPlayer> getPlayerBySpectateSecret(UUID spectateSecret) {
    return ChannelMessage.builder()
      .channel(LabyModConstants.CLOUDNET_CHANNEL_NAME)
      .message(LabyModConstants.GET_PLAYER_SPECTATE_SECRET)
      .buffer(ProtocolBuffer.create().writeUUID(spectateSecret))
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQueryAsync()
      .map(message -> message.getBuffer().readOptionalObject(CloudPlayer.class));
  }

  private static String getDisplay(ServiceInfoSnapshot serviceInfoSnapshot, ServiceDisplay serviceDisplay) {
    if (serviceDisplay == null || !serviceDisplay.isEnabled()) {
      return null;
    }

    return serviceDisplay.getDisplay(serviceInfoSnapshot);
  }

  public static byte[] getShowGameModeMessageContents(ServiceInfoSnapshot serviceInfoSnapshot) {
    String display = getDisplay(serviceInfoSnapshot, getConfiguration().getGameModeSwitchMessages());
    if (display == null) {
      return null;
    }

    JsonDocument document = JsonDocument.newDocument();
    document.append("show_gamemode", true).append("gamemode_name", display);

    return getLMCMessageContents("server_gamemode", document);
  }

  public static boolean canSpectate(ServiceInfoSnapshot serviceInfoSnapshot) {
    return getConfiguration().isDiscordSpectateEnabled() &&
      !isExcluded(getConfiguration().getExcludedSpectateGroups(), serviceInfoSnapshot.getConfiguration().getGroups()) &&
      serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false);
  }

  public static byte[] getDiscordRPCGameInfoUpdateMessageContents(ICloudPlayer cloudPlayer,
    ServiceInfoSnapshot serviceInfoSnapshot) {
    String display = getDisplay(serviceInfoSnapshot, getConfiguration().getDiscordRPC());
    if (display == null) {
      return null;
    }

    DiscordJoinMatchConfig joinMatchConfig = getConfiguration().getDiscordJoinMatch();
    boolean joinSecret = false;
    boolean spectateSecret = false;

    boolean modified = false;

    LabyModPlayerOptions options = getLabyModOptions(cloudPlayer);
    if (options == null) {
      return null;
    }

    if (joinMatchConfig != null && joinMatchConfig.isEnabled() && !joinMatchConfig.isExcluded(serviceInfoSnapshot)) {
      options.createNewJoinSecret();

      joinSecret = true;
      modified = true;
    } else if (options.getJoinSecret() != null) {
      options.removeJoinSecret();

      modified = true;
    }

    if (canSpectate(serviceInfoSnapshot)) {
      options.createNewSpectateSecret();

      spectateSecret = true;
      modified = true;
    } else if (options.getSpectateSecret() != null) {
      options.removeSpectateSecret();
      modified = true;
    }

    if (modified) {
      setLabyModOptions(cloudPlayer, options);
      CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class)
        .updateOnlinePlayer(cloudPlayer);
    }

    JsonDocument document = JsonDocument.newDocument();
    document.append("hasGame", true);

    document.append("game_mode", display)
      .append("game_startTime", 0)
      .append("game_endTime", 0);

    String domain = getConfiguration().getLoginDomain();

    document.append("hasJoinSecret", joinSecret);
    if (joinSecret) {
      document.append("joinSecret", options.getJoinSecret() + ":" + domain);
    }
    document.append("hasMatchSecret", cloudPlayer.getConnectedService() != null);
    if (cloudPlayer.getConnectedService() != null) {
      document.append("matchSecret", cloudPlayer.getConnectedService().getUniqueId() + ":" + domain);
    }
    document.append("hasSpectateSecret", spectateSecret);
    if (spectateSecret) {
      document.append("spectateSecret", options.getSpectateSecret() + ":" + domain);
    }

    return getLMCMessageContents("discord_rpc", document);
  }

  public static boolean isExcluded(Collection<String> excludedGroups, String[] serviceGroups) {
    for (String excludedGroup : excludedGroups) {
      if (Arrays.asList(serviceGroups).contains(excludedGroup)) {
        return true;
      }
    }
    return false;
  }

}
