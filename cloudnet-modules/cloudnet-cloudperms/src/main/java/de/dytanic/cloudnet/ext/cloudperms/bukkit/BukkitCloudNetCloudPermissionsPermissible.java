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

package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionCheckResult;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

public final class BukkitCloudNetCloudPermissionsPermissible extends PermissibleBase {

  private final Player player;
  private final IPermissionManagement permissionsManagement;

  public BukkitCloudNetCloudPermissionsPermissible(Player player, IPermissionManagement permissionsManagement) {
    super(player);

    this.player = player;
    this.permissionsManagement = permissionsManagement;
  }

  private Set<Permission> getDefaultPermissions() {
    return this.player.getServer().getPluginManager().getDefaultPermissions(false);
  }

  @Override
  public Set<PermissionAttachmentInfo> getEffectivePermissions() {
    Set<PermissionAttachmentInfo> infos = new HashSet<>();

    IPermissionUser permissionUser = CloudNetDriver.getInstance().getPermissionManagement()
      .getUser(this.player.getUniqueId());
    if (permissionUser != null) {
      for (String group : Wrapper.getInstance().getServiceConfiguration().getGroups()) {
        CloudNetDriver.getInstance()
          .getPermissionManagement()
          .getAllPermissions(permissionUser, group)
          .forEach(permission -> {
            Permission bukkitPermission = this.player.getServer().getPluginManager()
              .getPermission(permission.getName());
            if (bukkitPermission != null) {
              this.forEachChildren(bukkitPermission,
                (name, value) -> infos.add(new PermissionAttachmentInfo(this, name, null, value)));
            } else {
              infos.add(new PermissionAttachmentInfo(this, permission.getName(), null, permission.getPotency() >= 0));
            }
          });
      }

      for (Permission defaultPermission : this.getDefaultPermissions()) {
        this.forEachChildren(defaultPermission,
          (name, value) -> infos.add(new PermissionAttachmentInfo(this, name, null, value)));
      }
    }

    return infos;
  }

  @Override
  public boolean isPermissionSet(@NotNull String name) {
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
  public boolean hasPermission(@NotNull String inName) {
    try {
      IPermissionUser permissionUser = CloudNetDriver.getInstance().getPermissionManagement()
        .getUser(this.player.getUniqueId());
      if (permissionUser == null) {
        return false;
      }

      for (Permission permission : this.getDefaultPermissions()) {
        if (permission.getName().equalsIgnoreCase(inName)) {
          // default permissions are always active if not explicitly forbidden
          PermissionCheckResult result = this.permissionsManagement.getPermissionResult(permissionUser, inName);
          return result == PermissionCheckResult.DENIED || result.asBoolean();
        }
      }

      PermissionCheckResult result = this.permissionsManagement.getPermissionResult(permissionUser, inName);
      if (result != PermissionCheckResult.DENIED) {
        return result.asBoolean();
      }

      return this
        .testParents(inName, perm -> this.permissionsManagement.getPermissionResult(permissionUser, perm.getName()));
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  private boolean testParents(String inName, Function<Permission, PermissionCheckResult> parentAcceptor) {
    for (Permission parent : this.player.getServer().getPluginManager().getPermissions()) {
      PermissionCheckResult result = this.testParents(inName, parent, null, parentAcceptor);
      if (result != PermissionCheckResult.DENIED) {
        return result.asBoolean();
      }
    }
    return false;
  }

  private PermissionCheckResult testParents(String inName, Permission parent, Permission lastParent,
    Function<Permission, PermissionCheckResult> parentAcceptor) {
    for (Map.Entry<String, Boolean> entry : parent.getChildren().entrySet()) {
      if (entry.getKey().equalsIgnoreCase(inName)) {
        PermissionCheckResult result;
        if (lastParent != null) {
          result = parentAcceptor.apply(lastParent);
        } else {
          result = parentAcceptor.apply(parent);
        }

        if (result != PermissionCheckResult.DENIED) {
          return PermissionCheckResult.fromBoolean(entry.getValue());
        }
        continue;
      }

      Permission child = this.player.getServer().getPluginManager().getPermission(entry.getKey());
      if (child != null) {
        PermissionCheckResult result = this.testParents(inName, child, parent, parentAcceptor);
        if (result != PermissionCheckResult.DENIED) {
          return result;
        }
      }
    }

    return PermissionCheckResult.DENIED;
  }

  private void forEachChildren(Permission permission, BiConsumer<String, Boolean> permissionAcceptor) {
    permissionAcceptor.accept(permission.getName(), true);
    for (Map.Entry<String, Boolean> entry : permission.getChildren().entrySet()) {
      permissionAcceptor.accept(entry.getKey(), entry.getValue());
    }
  }

  public Player getPlayer() {
    return this.player;
  }
}
