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

package eu.cloudnetservice.modules.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.Permission;
import cn.nukkit.permission.PermissionAttachmentInfo;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class NukkitCloudPermissionsPermissible extends PermissibleBase {

  private final Player player;
  private final Collection<String> groups;
  private final PermissionManagement permissionsManagement;

  public NukkitCloudPermissionsPermissible(
    @NonNull Player player,
    @NonNull WrapperConfiguration wrapperConfiguration,
    @NonNull PermissionManagement permissionsManagement
  ) {
    super(player);

    this.player = player;
    this.permissionsManagement = permissionsManagement;
    this.groups = wrapperConfiguration.serviceConfiguration().groups();
  }

  @Override
  public Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
    Map<String, PermissionAttachmentInfo> infos = new HashMap<>();
    var permissionUser = this.permissionsManagement.user(this.player.getUniqueId());
    if (permissionUser == null) {
      return infos;
    }

    for (var group : this.groups) {
      infos.putAll(
        this.permissionsManagement.allGroupPermissions(permissionUser, group)
          .stream()
          .map(
            permission -> new PermissionAttachmentInfo(this, permission.name(), null, permission.potency() >= 0))
          .collect(Collectors.toMap(PermissionAttachmentInfo::getPermission, o -> o))
      );
    }

    return infos;
  }

  @Override
  public boolean isPermissionSet(@Nullable String name) {
    return this.hasPermission(name);
  }

  @Override
  public boolean isPermissionSet(@NonNull Permission perm) {
    return this.isPermissionSet(perm.getName());
  }

  @Override
  public boolean hasPermission(@NonNull Permission perm) {
    return this.hasPermission(perm.getName());
  }

  @Override
  public boolean hasPermission(@Nullable String inName) {
    if (inName == null) {
      return false;
    }

    var permissionUser = this.permissionsManagement.user(this.player.getUniqueId());
    return permissionUser != null && this.permissionsManagement.hasPermission(
      permissionUser,
      eu.cloudnetservice.driver.permission.Permission.of(inName));
  }

  public Player player() {
    return this.player;
  }
}
