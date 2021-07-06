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

package de.dytanic.cloudnet.ext.bridge.sponge;

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
import java.util.Optional;
import java.util.stream.Collectors;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.ExperienceHolderData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public final class SpongeCloudNetHelper extends BridgeServerHelper {

  private SpongeCloudNetHelper() {
    throw new UnsupportedOperationException();
  }

  public static void init() {
    BridgeServerHelper.setMotd(Sponge.getServer().getMotd().toPlain());
    BridgeServerHelper.setState("LOBBY");
    BridgeServerHelper.setMaxPlayers(Sponge.getServer().getMaxPlayers());
  }

  public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(serviceInfoSnapshot);

    serviceInfoSnapshot.getProperties()
      .append("Online", BridgeHelper.isOnline())
      .append("Version", Sponge.getPlatform().getMinecraftVersion().getName())
      .append("Sponge-Version", Sponge.getPlatform().getContainer(Platform.Component.API).getVersion().orElse(""))
      .append("Online-Count", Sponge.getServer().getOnlinePlayers().size())
      .append("Max-Players", BridgeServerHelper.getMaxPlayers())
      .append("Motd", BridgeServerHelper.getMotd())
      .append("Extra", BridgeServerHelper.getExtra())
      .append("State", BridgeServerHelper.getState())
      .append("Outgoing-Channels", Sponge.getChannelRegistrar().getRegisteredChannels(Platform.Type.SERVER))
      .append("Incoming-Channels", Sponge.getChannelRegistrar().getRegisteredChannels(Platform.Type.CLIENT))
      .append("Online-Mode", Sponge.getServer().getOnlineMode())
      .append("Whitelist-Enabled", Sponge.getServer().hasWhitelist())
      .append("Players", Sponge.getServer().getOnlinePlayers().stream().map(player -> {
        Location<World> location = player.getLocation();

        Optional<ExperienceHolderData> holderData = player.get(ExperienceHolderData.class);

        return new SpongeCloudNetPlayerInfo(
          player.getUniqueId(),
          player.getName(),
          player.getConnection().getLatency(),
          player.getHealthData().health().get(),
          player.getHealthData().maxHealth().get(),
          player.saturation().get(),
          holderData.map(experienceHolderData -> experienceHolderData.level().get()).orElse(0),
          new WorldPosition(
            location.getX(),
            location.getY(),
            location.getZ(),
            0F,
            0F,
            player.getWorld().getName()
          ),
          new HostAndPort(player.getConnection().getAddress())
        );
      }).collect(Collectors.toList()))
      .append("Plugins", Sponge.getGame().getPluginManager().getPlugins().stream().map(pluginContainer -> {
        PluginInfo pluginInfo = new PluginInfo(pluginContainer.getId(),
          pluginContainer.getVersion().isPresent() ? pluginContainer.getVersion().get() : null);

        pluginInfo.getProperties()
          .append("name", pluginContainer.getName())
          .append("authors", pluginContainer.getAuthors())
          .append("url", pluginContainer.getUrl().isPresent() ? pluginContainer.getUrl().get() : null)
          .append("description",
            pluginContainer.getDescription().isPresent() ? pluginContainer.getDescription().get() : null);

        return pluginInfo;
      }).collect(Collectors.toList()))
      .append("Worlds", Sponge.getServer().getWorlds().stream()
        .map(world -> new WorldInfo(world.getUniqueId(), world.getName(), world.getDifficulty().getName(),
          world.getGameRules()))
        .collect(Collectors.toList()));
  }

  public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
    boolean onlineMode = Sponge.getServer().getOnlineMode();

    return BridgeHelper.createNetworkConnectionInfo(
      player.getUniqueId(),
      player.getName(),
      -1,
      new HostAndPort(player.getConnection().getAddress()),
      new HostAndPort(Sponge.getServer().getBoundAddress().orElse(null)),
      onlineMode,
      false,
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

  public static NetworkPlayerServerInfo createNetworkPlayerServerInfo(Player player, boolean login) {
    WorldPosition worldPosition;

    if (login) {
      worldPosition = new WorldPosition(-1, -1, -1, -1, -1, "world");
    } else {
      Location<?> location = player.getLocation();
      worldPosition = new WorldPosition(
        location.getX(),
        location.getY(),
        location.getZ(),
        0F,
        0F,
        player.getWorld().getName()
      );
    }
    Optional<ExperienceHolderData> holderData = player.get(ExperienceHolderData.class);

    return new NetworkPlayerServerInfo(
      player.getUniqueId(),
      player.getName(),
      null,
      player.getHealthData().health().get(),
      player.getHealthData().maxHealth().get(),
      player.saturation().get(),
      holderData.map(experienceHolderData -> experienceHolderData.level().get()).orElse(0),
      worldPosition,
      new HostAndPort(player.getConnection().getAddress()),
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

}
