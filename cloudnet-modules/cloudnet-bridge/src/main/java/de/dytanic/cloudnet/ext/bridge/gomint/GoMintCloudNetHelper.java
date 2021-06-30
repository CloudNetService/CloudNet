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

package de.dytanic.cloudnet.ext.bridge.gomint;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.WorldInfo;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.math.Location;
import io.gomint.server.GoMintServer;
import io.gomint.server.network.Protocol;
import io.gomint.world.Gamerule;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class GoMintCloudNetHelper extends BridgeServerHelper {

  private GoMintCloudNetHelper() {
    throw new UnsupportedOperationException();
  }

  public static void init() {
    BridgeServerHelper.setMotd(GoMint.instance().motd());
    BridgeServerHelper.setState("LOBBY");
    BridgeServerHelper.setMaxPlayers(GoMint.instance().maxPlayerCount());
  }

  public static GoMintServer getGoMintServer() {
    return (GoMintServer) GoMint.instance();
  }

  public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
    serviceInfoSnapshot.getProperties()
      .append("Online", BridgeHelper.isOnline())
      .append("Version", Protocol.MINECRAFT_PE_NETWORK_VERSION)
      .append("GoMint-Version", GoMint.instance().version())
      .append("Max-Players", BridgeServerHelper.getMaxPlayers())
      .append("Motd", BridgeServerHelper.getMotd())
      .append("Extra", BridgeServerHelper.getExtra())
      .append("State", BridgeServerHelper.getState())
      .append("TPS", GoMint.instance().tps())
      .append("Online-Count", GoMint.instance().onlinePlayers().size())
      .append("Players", GoMint.instance().onlinePlayers().stream().map(player -> {
        Location location = player.location();

        return new GoMintCloudNetPlayerInfo(
          player.health(),
          player.maxHealth(),
          player.saturation(),
          player.level(),
          player.ping(),
          player.locale(),
          new WorldPosition(
            location.x(),
            location.y(),
            location.z(),
            location.yaw(),
            location.pitch(),
            location.world().name()
          ),
          new HostAndPort(player.address()),
          player.uuid(),
          player.online(),
          player.name(),
          player.deviceInfo().deviceName(),
          player.xboxID(),
          player.gamemode().name()
        );
      }).collect(Collectors.toList()))
      .append("Worlds", GoMint.instance().worlds().stream().map(world -> {
        Map<String, String> gameRules = new HashMap<>();

        for (Field field : Gamerule.class.getFields()) {
          if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) &&
            Modifier.isPublic(field.getModifiers()) && Gamerule.class.isAssignableFrom(field.getType())) {
            try {
              field.setAccessible(true);
              Gamerule<?> gameRule = (Gamerule<?>) field.get(null);
              gameRules.put(gameRule.name(), String.valueOf(world.gamerule(gameRule)));

            } catch (IllegalAccessException exception) {
              exception.printStackTrace();
            }
          }
        }

        return new WorldInfo(
          new UUID(0, 0),
          world.name(),
          world.difficulty().name(),
          gameRules
        );
      }).collect(Collectors.toList()));
  }

  public static NetworkConnectionInfo createNetworkConnectionInfo(EntityPlayer player) {
    return BridgeHelper.createNetworkConnectionInfo(
      player.uuid(),
      player.name(),
      Protocol.MINECRAFT_PE_PROTOCOL_VERSION,
      new HostAndPort(player.address()),
      new HostAndPort(getGoMintServer().serverConfig().listener().ip(), GoMint.instance().port()),
      getGoMintServer().encryptionKeyFactory().isKeyGiven(),
      false,
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

  public static NetworkPlayerServerInfo createNetworkPlayerServerInfo(EntityPlayer player, boolean login) {
    WorldPosition worldPosition;

    if (login) {
      worldPosition = new WorldPosition(-1, -1, -1, -1, -1, "world");
    } else {
      Location location = player.location();

      worldPosition = new WorldPosition(
        location.x(),
        location.y(),
        location.z(),
        location.yaw(),
        location.pitch(),
        location.world().name()
      );
    }

    return new NetworkPlayerServerInfo(
      player.uuid(),
      player.name(),
      player.xboxID(),
      player.health(),
      player.maxHealth(),
      player.saturation(),
      player.level(),
      worldPosition,
      new HostAndPort(player.address()),
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }
}
