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

package de.dytanic.cloudnet.ext.bridge.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.GameRules;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.WorldInfo;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class NukkitCloudNetHelper extends BridgeServerHelper {

  private NukkitCloudNetHelper() {
    throw new UnsupportedOperationException();
  }

  public static void init() {
    BridgeServerHelper.setMotd(Server.getInstance().getMotd());
    BridgeServerHelper.setState("LOBBY");
    BridgeServerHelper.setMaxPlayers(Server.getInstance().getMaxPlayers());
  }

  public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
    serviceInfoSnapshot.getProperties()
      .append("Online", BridgeHelper.isOnline())
      .append("Version", Server.getInstance().getVersion())
      .append("Codename", Server.getInstance().getCodename())
      .append("Nukkit-Version", Server.getInstance().getApiVersion())
      .append("Online-Count", Server.getInstance().getOnlinePlayers().size())
      .append("Max-Players", BridgeServerHelper.getMaxPlayers())
      .append("Motd", BridgeServerHelper.getMotd())
      .append("Extra", BridgeServerHelper.getExtra())
      .append("State", BridgeServerHelper.getState())
      .append("Allow-Nether", Server.getInstance().isNetherAllowed())
      .append("Allow-Flight", Server.getInstance().getAllowFlight())
      .append("Players",
        Server.getInstance().getOnlinePlayers().values().stream().map(player -> new NukkitCloudNetPlayerInfo(
          player.getHealth(),
          player.getMaxHealth(),
          player.getFoodData().getLevel(),
          player.getExperienceLevel(),
          player.getPing(),
          new WorldPosition(
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYaw(),
            player.getPitch(),
            player.getLevel().getName()
          ),
          new HostAndPort(player.getAddress(), player.getPort()),
          player.getUniqueId(),
          player.getName()
        )).collect(Collectors.toList()))
      .append("Plugins", Server.getInstance().getPluginManager().getPlugins().values().stream().map(plugin -> {
        PluginInfo pluginInfo = new PluginInfo(plugin.getName(), plugin.getDescription().getVersion());

        pluginInfo.getProperties()
          .append("authors", plugin.getDescription().getAuthors())
          .append("dependencies", plugin.getDescription().getDepend())
          .append("load-before", plugin.getDescription().getLoadBefore())
          .append("description", plugin.getDescription().getDescription())
          .append("commands", plugin.getDescription().getCommands())
          .append("soft-dependencies", plugin.getDescription().getSoftDepend())
          .append("website", plugin.getDescription().getWebsite())
          .append("main-class", plugin.getClass().getName())
          .append("prefix", plugin.getDescription().getPrefix())
        ;

        return pluginInfo;
      }).collect(Collectors.toList()))
      .append("Worlds", Server.getInstance().getLevels().values().stream().map(level -> {
        Map<String, String> gameRules = new HashMap<>();

        for (GameRule gameRule : level.getGameRules().getRules()) {
          GameRules.Value<?> type = level.getGameRules().getGameRules().get(gameRule);

          switch (type.getType()) {
            case FLOAT:
              gameRules.put(gameRule.getName(), String.valueOf(level.getGameRules().getFloat(gameRule)));
              break;
            case BOOLEAN:
              gameRules.put(gameRule.getName(), String.valueOf(level.getGameRules().getBoolean(gameRule)));
              break;
            case INTEGER:
              gameRules.put(gameRule.getName(), String.valueOf(level.getGameRules().getInteger(gameRule)));
              break;
            default:
              gameRules.put(gameRule.getName(), String.valueOf(level.getGameRules().getString(gameRule)));
              break;
          }
        }

        return new WorldInfo(null, level.getName(), getDifficultyToString(Server.getInstance().getDifficulty()),
          gameRules);
      }).collect(Collectors.toList()));
  }

  public static String getDifficultyToString(int value) {
    switch (value) {
      case 1:
        return "easy";
      case 2:
        return "normal";
      case 3:
        return "hard";
      default:
        return "peaceful";
    }
  }

  public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
    return BridgeHelper.createNetworkConnectionInfo(
      player.getUniqueId(),
      player.getName(),
      -1,
      new HostAndPort(player.getAddress(), player.getPort()),
      new HostAndPort("0.0.0.0", Server.getInstance().getPort()),
      true,
      false,
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

  public static NetworkPlayerServerInfo createNetworkPlayerServerInfo(Player player, boolean login) {
    WorldPosition worldPosition;

    if (login) {
      worldPosition = new WorldPosition(-1, -1, -1, -1, -1, "world");
    } else {
      worldPosition = new WorldPosition(
        player.getX(),
        player.getY(),
        player.getZ(),
        player.getYaw(),
        player.getPitch(),
        player.getLevel().getName()
      );
    }

    return new NetworkPlayerServerInfo(
      player.getUniqueId(),
      player.getName(),
      null,
      player.getHealth(),
      player.getMaxHealth(),
      player.getFoodData() == null ? -1 : player.getFoodData().getLevel(),
      player.getExperienceLevel(),
      worldPosition,
      new HostAndPort(player.getAddress(), player.getPort()),
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

}
