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

package eu.cloudnetservice.modules.cloudperms.bukkit;

import eu.cloudnetservice.driver.permission.PermissionCheckResult;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class BukkitCloudPermissionsPermissible extends PermissibleBase {

  private final Player player;
  private final Collection<String> assignedServiceGroups;
  private final PermissionManagement permissionManagement;

  public BukkitCloudPermissionsPermissible(
    @NonNull Player player,
    @NonNull PermissionManagement permissionsManagement,
    @NonNull WrapperConfiguration wrapperConfiguration
  ) {
    super(player);

    this.player = player;
    this.permissionManagement = permissionsManagement;
    this.assignedServiceGroups = wrapperConfiguration.serviceConfiguration().groups();
  }

  private @NonNull Set<Permission> defaultPermissions() {
    return this.player.getServer().getPluginManager().getDefaultPermissions(false);
  }

  @Override
  public @NonNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
    Set<PermissionAttachmentInfo> infos = new HashSet<>();

    var user = this.permissionManagement.user(this.player.getUniqueId());
    if (user != null) {
      for (var group : this.assignedServiceGroups) {
        this.permissionManagement
          .allGroupPermissions(user, group)
          .forEach(permission -> {
            var bukkit = this.player.getServer().getPluginManager().getPermission(permission.name());
            if (bukkit != null) {
              this.forEachChildren(
                bukkit,
                (name, value) -> infos.add(new PermissionAttachmentInfo(this, name, null, value)));
            } else {
              infos.add(new PermissionAttachmentInfo(this, permission.name(), null, permission.potency() >= 0));
            }
          });
      }

      for (var defaultPermission : this.defaultPermissions()) {
        this.forEachChildren(
          defaultPermission,
          (name, value) -> infos.add(new PermissionAttachmentInfo(this, name, null, value)));
      }
    }

    return infos;
  }

  @Override
  public boolean isPermissionSet(@NonNull String name) {
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
  public boolean hasPermission(@NonNull String inName) {
    try {
      var user = this.permissionManagement.user(this.player.getUniqueId());
      if (user == null) {
        return false;
      }

      for (var permission : this.defaultPermissions()) {
        if (permission.getName().equalsIgnoreCase(inName)) {
          // default permissions are always active if not explicitly forbidden
          var result = this.permissionManagement.permissionResult(
            user,
            eu.cloudnetservice.driver.permission.Permission.of(inName));
          return result == PermissionCheckResult.DENIED || result.asBoolean();
        }
      }

      var result = this.permissionManagement.permissionResult(
        user,
        eu.cloudnetservice.driver.permission.Permission.of(inName));
      if (result != PermissionCheckResult.DENIED) {
        return result.asBoolean();
      }

      return this.testParents(
        inName,
        perm -> this.permissionManagement.permissionResult(
          user,
          eu.cloudnetservice.driver.permission.Permission.of(perm.getName())));
    } catch (Exception ex) {
      this.player.getServer().getLogger().log(Level.SEVERE, "Exception while checking permissions", ex);
      return false;
    }
  }

  private boolean testParents(String inName, Function<Permission, PermissionCheckResult> parentAcceptor) {
    for (var parent : this.player.getServer().getPluginManager().getPermissions()) {
      var result = this.testParents(inName, parent, null, parentAcceptor);
      if (result != PermissionCheckResult.DENIED) {
        return result.asBoolean();
      }
    }
    return false;
  }

  private PermissionCheckResult testParents(String inName, Permission parent, Permission lastParent,
    Function<Permission, PermissionCheckResult> parentAcceptor) {
    for (var entry : parent.getChildren().entrySet()) {
      if (entry.getKey().equalsIgnoreCase(inName)) {
        PermissionCheckResult result;
        result = parentAcceptor.apply(Objects.requireNonNullElse(lastParent, parent));

        if (result != PermissionCheckResult.DENIED) {
          return PermissionCheckResult.fromBoolean(entry.getValue());
        }
        continue;
      }

      var child = this.player.getServer().getPluginManager().getPermission(entry.getKey());
      if (child != null) {
        var result = this.testParents(inName, child, parent, parentAcceptor);
        if (result != PermissionCheckResult.DENIED) {
          return result;
        }
      }
    }

    return PermissionCheckResult.DENIED;
  }

  private void forEachChildren(Permission permission, BiConsumer<String, Boolean> permissionAcceptor) {
    permissionAcceptor.accept(permission.getName(), true);
    for (var entry : permission.getChildren().entrySet()) {
      permissionAcceptor.accept(entry.getKey(), entry.getValue());
    }
  }

  public Player player() {
    return this.player;
  }
}
