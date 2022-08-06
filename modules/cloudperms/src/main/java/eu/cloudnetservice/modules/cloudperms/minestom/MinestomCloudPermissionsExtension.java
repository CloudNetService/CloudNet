/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.minestom;

import com.google.common.util.concurrent.MoreExecutors;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.cloudperms.PermissionsUpdateListener;
import eu.cloudnetservice.modules.cloudperms.minestom.listener.MinestomCloudPermissionsPlayerListener;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.extensions.Extension;

public final class MinestomCloudPermissionsExtension extends Extension {

  @Override
  public void initialize() {
    // provide an own player provider to support cloud permissions
    MinecraftServer.getConnectionManager().setPlayerProvider(MinestomCloudPermissionsPlayer::new);

    // listen to any permission updates and update the command tree
    CloudNetDriver.instance().eventManager().registerListener(new PermissionsUpdateListener<>(
      MoreExecutors.directExecutor(),
      Player::refreshCommands,
      Player::getUuid,
      uniqueId -> {
        var player = MinecraftServer.getConnectionManager().getPlayer(uniqueId);
        // only provide real players
        return player instanceof FakePlayer ? null : player;
      },
      () -> MinecraftServer.getConnectionManager().getOnlinePlayers()
        .stream()
        .filter(player -> !(player instanceof FakePlayer))
        .toList()));
    // handle player login and disconnects
    new MinestomCloudPermissionsPlayerListener();
  }

  @Override
  public void terminate() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
