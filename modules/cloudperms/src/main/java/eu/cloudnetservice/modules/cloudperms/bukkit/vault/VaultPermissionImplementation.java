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

package eu.cloudnetservice.modules.cloudperms.bukkit.vault;

import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import java.util.Optional;
import lombok.NonNull;
import net.milkbowl.vault.permission.Permission;

public class VaultPermissionImplementation extends Permission {

  private final PermissionManagement permissionManagement;

  public VaultPermissionImplementation(@NonNull PermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  private @NonNull Optional<PermissionUser> permissionUserByName(@NonNull String name) {
    return Optional.ofNullable(this.permissionManagement.firstUser(name));
  }

  private @NonNull Optional<PermissionGroup> permissionGroupByName(@NonNull String name) {
    return Optional.ofNullable(this.permissionManagement.group(name));
  }

  @Override
  public String getName() {
    return "CloudNet-CloudPerms";
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean hasSuperPermsCompat() {
    return true;
  }

  @Override
  public boolean playerHas(String world, String player, String permission) {
    return this.permissionUserByName(player).map(user -> user.hasPermission(permission).asBoolean()).orElse(false);
  }

  @Override
  public boolean playerAdd(String world, String player, String permission) {
    return this.permissionUserByName(player)
      .map(user -> {
        user.addPermission(permission);
        this.permissionManagement.updateUser(user);
        return true;
      }).orElse(false);
  }

  @Override
  public boolean playerRemove(String world, String player, String permission) {
    return this.permissionUserByName(player)
      .map(user -> {
        if (user.removePermission(permission)) {
          this.permissionManagement.updateUser(user);
          return true;
        } else {
          return false;
        }
      }).orElse(false);
  }

  @Override
  public boolean groupHas(String world, String group, String permission) {
    return this.permissionGroupByName(group)
      .map(permissionGroup -> permissionGroup.hasPermission(permission).asBoolean())
      .orElse(false);
  }

  @Override
  public boolean groupAdd(String world, String group, String permission) {
    return this.permissionGroupByName(group)
      .map(permissionGroup -> {
        permissionGroup.addPermission(permission);
        this.permissionManagement.updateGroup(permissionGroup);
        return true;
      }).orElse(false);
  }

  @Override
  public boolean groupRemove(String world, String group, String permission) {
    return this.permissionGroupByName(group)
      .map(permissionGroup -> {
        if (permissionGroup.removePermission(permission)) {
          this.permissionManagement.updateGroup(permissionGroup);
          return true;
        } else {
          return false;
        }
      }).orElse(false);
  }

  @Override
  public boolean playerInGroup(String world, String player, String group) {
    return this.permissionUserByName(player).map(user -> user.inGroup(group)).orElse(false);
  }

  @Override
  public boolean playerAddGroup(String world, String player, String group) {
    return this.permissionUserByName(player)
      .map(user -> {
        this.permissionManagement.updateUser(user.addGroup(group));
        return true;
      }).orElse(false);
  }

  @Override
  public boolean playerRemoveGroup(String world, String player, String group) {
    return this.permissionUserByName(player)
      .map(user -> {
        if (user.removeGroup(group)) {
          this.permissionManagement.updateUser(user);
          return true;
        } else {
          return false;
        }
      }).orElse(false);
  }

  @Override
  public String[] getPlayerGroups(String world, String player) {
    return this.permissionUserByName(player)
      .map(user -> user.groupNames().toArray(String[]::new))
      .orElse(new String[0]);
  }

  @Override
  public String getPrimaryGroup(String world, String player) {
    return this.permissionUserByName(player)
      .map(this.permissionManagement::highestPermissionGroup)
      .map(PermissionGroup::name)
      .orElse(null);
  }

  @Override
  public String[] getGroups() {
    return this.permissionManagement.groups().stream().map(Named::name).toArray(String[]::new);
  }

  @Override
  public boolean hasGroupSupport() {
    return true;
  }
}
