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

package eu.cloudnetservice.modules.cloudperms.minestom;

import com.google.common.util.concurrent.MoreExecutors;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ExternalDependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Repository;
import eu.cloudnetservice.modules.cloudperms.PermissionsUpdateListener;
import eu.cloudnetservice.modules.cloudperms.minestom.listener.MinestomCloudPermissionsPlayerListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.network.ConnectionManager;

@Singleton
@PlatformPlugin(
  platform = "minestom",
  name = "CloudNet-CloudPerms",
  authors = "CloudNetService",
  version = "@version@",
  externalDependencies = @ExternalDependency(
    groupId = "com.google.guava",
    artifactId = "guava",
    version = "31.1-jre",
    repository = @Repository(
      id = "central",
      url = "https://repo1.maven.org/maven2/"
    )
  )
)
public final class MinestomCloudPermissionsExtension implements PlatformEntrypoint {

  private final GlobalEventHandler eventHandler;
  private final ConnectionManager connectionManager;

  private final ModuleHelper moduleHelper;
  private final EventManager eventManager;
  private final PermissionManagement permissionManagement;

  @Inject
  public MinestomCloudPermissionsExtension(
    @NonNull GlobalEventHandler eventHandler,
    @NonNull ConnectionManager connectionManager,
    @NonNull ModuleHelper moduleHelper,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.eventHandler = eventHandler;
    this.connectionManager = connectionManager;
    this.moduleHelper = moduleHelper;
    this.eventManager = eventManager;
    this.permissionManagement = permissionManagement;
  }

  @Override
  public void onLoad() {
    // provide an own player provider to support cloud permissions
    this.connectionManager.setPlayerProvider((uuid, username, connection) -> new MinestomCloudPermissionsPlayer(
      uuid,
      username,
      connection,
      this.permissionManagement));

    // listen to any permission updates and update the command tree
    this.eventManager.registerListener(new PermissionsUpdateListener<>(
      MoreExecutors.directExecutor(),
      Player::refreshCommands,
      Player::getUuid,
      uniqueId -> {
        // only provide real players
        var player = this.connectionManager.getPlayer(uniqueId);
        return player instanceof FakePlayer ? null : player;
      },
      this.permissionManagement,
      () -> this.connectionManager.getOnlinePlayers()
        .stream()
        .filter(player -> !(player instanceof FakePlayer))
        .toList()));

    // handle player login and disconnects
    new MinestomCloudPermissionsPlayerListener(this.eventHandler, this.permissionManagement);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
