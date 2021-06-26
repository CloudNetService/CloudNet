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

package de.dytanic.cloudnet.ext.bridge.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.WorldInfo;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class BukkitCloudNetHelper extends BridgeServerHelper {

  private BukkitCloudNetHelper() {
    throw new UnsupportedOperationException();
  }

  public static void init() {
    BridgeServerHelper.setMotd(Bukkit.getMotd());
    BridgeServerHelper.setState("LOBBY");
    BridgeServerHelper.setMaxPlayers(Bukkit.getMaxPlayers());
  }

  public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(serviceInfoSnapshot);

    Collection<BukkitCloudNetPlayerInfo> players = new ArrayList<>();
    Bukkit.getOnlinePlayers().forEach(player -> {
      Location location = player.getLocation();

      players.add(new BukkitCloudNetPlayerInfo(
        player.getUniqueId(),
        player.getName(),
        player.getHealth(),
        player.getMaxHealth(),
        player.getFoodLevel(),
        player.getLevel(),
        new WorldPosition(
          location.getX(),
          location.getY(),
          location.getZ(),
          location.getYaw(),
          location.getPitch(),
          location.getWorld().getName()
        ),
        new HostAndPort(player.getAddress())
      ));
    });

    serviceInfoSnapshot.getProperties()
      .append("Online", BridgeHelper.isOnline())
      .append("Version", Bukkit.getVersion())
      .append("Bukkit-Version", Bukkit.getBukkitVersion())
      .append("Online-Count", Bukkit.getOnlinePlayers().size())
      .append("Max-Players", BridgeServerHelper.getMaxPlayers())
      .append("Motd", BridgeServerHelper.getMotd())
      .append("Extra", BridgeServerHelper.getExtra())
      .append("State", BridgeServerHelper.getState())
      .append("Outgoing-Channels", Bukkit.getMessenger().getOutgoingChannels())
      .append("Incoming-Channels", Bukkit.getMessenger().getIncomingChannels())
      .append("Online-Mode", Bukkit.getOnlineMode())
      .append("Whitelist-Enabled", Bukkit.hasWhitelist())
      .append("Whitelist",
        Bukkit.getWhitelistedPlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList()))
      .append("Allow-Nether", Bukkit.getAllowNether())
      .append("Allow-End", Bukkit.getAllowEnd())
      .append("Players", players)
      .append("Plugins", Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(plugin -> {
        PluginInfo pluginInfo = new PluginInfo(plugin.getName(), plugin.getDescription().getVersion());

        pluginInfo.getProperties()
          .append("authors", plugin.getDescription().getAuthors())
          .append("dependencies", plugin.getDescription().getDepend())
          .append("load-before", plugin.getDescription().getLoadBefore())
          .append("description", plugin.getDescription().getDescription())
          .append("commands", plugin.getDescription().getCommands())
          .append("soft-dependencies", plugin.getDescription().getSoftDepend())
          .append("website", plugin.getDescription().getWebsite())
          .append("main-class", plugin.getDescription().getMain())
          .append("prefix", plugin.getDescription().getPrefix())
        ;

        return pluginInfo;
      }).collect(Collectors.toList()))
      .append("Worlds", Bukkit.getWorlds().stream().map(world -> {
        Map<String, String> gameRules = new HashMap<>();

        for (String entry : world.getGameRules()) {
          gameRules.put(entry, world.getGameRuleValue(entry));
        }

        return new WorldInfo(world.getUID(), world.getName(), world.getDifficulty().name(), gameRules);
      }).collect(Collectors.toList()));
  }

  public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
    return BridgeHelper.createNetworkConnectionInfo(
      player.getUniqueId(),
      player.getName(),
      -1,
      new HostAndPort(player.getAddress()),
      new HostAndPort("0.0.0.0", Bukkit.getServer().getPort()),
      Bukkit.getServer().getOnlineMode(),
      false,
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

  public static NetworkPlayerServerInfo createNetworkPlayerServerInfo(Player player, boolean login) {
    WorldPosition worldPosition;

    if (login) {
      worldPosition = new WorldPosition(-1, -1, -1, -1, -1, "world");
    } else {
      Location location = player.getLocation();
      worldPosition = new WorldPosition(
        location.getX(),
        location.getY(),
        location.getZ(),
        location.getYaw(),
        location.getPitch(),
        location.getWorld().getName()
      );
    }

    return new NetworkPlayerServerInfo(
      player.getUniqueId(),
      player.getName(),
      null,
      player.getHealth(),
      player.getMaxHealth(),
      player.getSaturation(),
      player.getLevel(),
      worldPosition,
      new HostAndPort(player.getAddress()),
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

}
