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

package de.dytanic.cloudnet.ext.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.Permission;
import cn.nukkit.permission.PermissionAttachmentInfo;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class NukkitCloudNetCloudPermissionsPermissible extends PermissibleBase {

  private final Player player;
  private final IPermissionManagement permissionsManagement;

  public NukkitCloudNetCloudPermissionsPermissible(Player player, IPermissionManagement permissionsManagement) {
    super(player);

    this.player = player;
    this.permissionsManagement = permissionsManagement;
  }

  @Override
  public Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
    Map<String, PermissionAttachmentInfo> infos = new HashMap<>();
    IPermissionUser permissionUser = this.permissionsManagement.getUser(this.player.getUniqueId());
    if (permissionUser == null) {
      return infos;
    }

    for (String group : Wrapper.getInstance().getServiceConfiguration().getGroups()) {
      infos.putAll(
        this.permissionsManagement.getAllPermissions(permissionUser, group)
          .stream()
          .map(
            permission -> new PermissionAttachmentInfo(this, permission.getName(), null, permission.getPotency() >= 0))
          .collect(Collectors.toMap(PermissionAttachmentInfo::getPermission, o -> o))
      );
    }

    return infos;
  }

  @Override
  public boolean isPermissionSet(String name) {
    return this.hasPermission(name);
  }

  @Override
  public boolean isPermissionSet(Permission perm) {
    return this.isPermissionSet(perm.getName());
  }

  @Override
  public boolean hasPermission(Permission perm) {
    return this.hasPermission(perm.getName());
  }

  @Override
  public boolean hasPermission(String inName) {
    if (inName == null) {
      return false;
    }

    IPermissionUser permissionUser = this.permissionsManagement.getUser(this.player.getUniqueId());
    return permissionUser != null && this.permissionsManagement.hasPermission(permissionUser, inName);
  }

  public Player getPlayer() {
    return this.player;
  }
}
