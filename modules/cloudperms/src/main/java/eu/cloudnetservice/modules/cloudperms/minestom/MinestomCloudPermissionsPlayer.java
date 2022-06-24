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

import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.UUID;
import lombok.NonNull;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.permission.PermissionVerifier;
import org.jetbrains.annotations.Nullable;

public class MinestomCloudPermissionsPlayer extends Player {

  public MinestomCloudPermissionsPlayer(
    @NonNull UUID uuid,
    @NonNull String username,
    @NonNull PlayerConnection playerConnection
  ) {
    super(uuid, username, playerConnection);
  }

  @Override
  public boolean hasPermission(@NonNull net.minestom.server.permission.Permission permission) {
    return this.hasPermission(permission.getPermissionName());
  }

  @Override
  public boolean hasPermission(@NonNull String permissionName) {
    var management = Wrapper.instance().permissionManagement();
    var user = management.user(this.uuid);
    return user != null && management.hasPermission(user, Permission.of(permissionName));
  }

  @Override
  public boolean hasPermission(@NonNull String permissionName, @Nullable PermissionVerifier permissionVerifier) {
    return this.hasPermission(permissionName);
  }
}
