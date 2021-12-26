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

package de.dytanic.cloudnet.ext.cloudperms.bukkit.vault;

import de.dytanic.cloudnet.common.Nameable;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUserGroupInfo;
import java.util.Optional;
import net.milkbowl.vault.permission.Permission;

public class VaultPermissionImplementation extends Permission {

  private final PermissionManagement permissionManagement;

  public VaultPermissionImplementation(PermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  private Optional<PermissionUser> permissionUserByName(String name) {
    return this.permissionManagement.usersByName(name).stream().findFirst();
  }

  private Optional<PermissionGroup> permissionGroupByName(String name) {
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
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.isPresent() && optionalPermissionUser.get().hasPermission(permission).asBoolean();
  }

  @Override
  public boolean playerAdd(String world, String player, String permission) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser -> {
      var success = permissionUser.addPermission(permission);
      this.permissionManagement.updateUser(permissionUser);

      return success;
    }).orElse(false);
  }

  @Override
  public boolean playerRemove(String world, String player, String permission) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser -> {
      var success = permissionUser.removePermission(permission);
      this.permissionManagement.updateUser(permissionUser);

      return success;
    }).orElse(false);
  }

  @Override
  public boolean groupHas(String world, String group, String permission) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.isPresent() && optionalPermissionGroup.get().hasPermission(permission).asBoolean();
  }

  @Override
  public boolean groupAdd(String world, String group, String permission) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(permissionGroup -> {
      var success = permissionGroup.addPermission(permission);
      this.permissionManagement.updateGroup(permissionGroup);

      return success;
    }).orElse(false);
  }

  @Override
  public boolean groupRemove(String world, String group, String permission) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(permissionGroup -> {
      var success = permissionGroup.removePermission(permission);
      this.permissionManagement.updateGroup(permissionGroup);

      return success;
    }).orElse(false);
  }

  @Override
  public boolean playerInGroup(String world, String player, String group) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.isPresent() && optionalPermissionUser.get().inGroup(group);
  }

  @Override
  public boolean playerAddGroup(String world, String player, String group) {
    var optionalPermissionUser = this.permissionUserByName(player);

    if (optionalPermissionUser.isPresent()) {
      var permissionUser = optionalPermissionUser.get();

      permissionUser.addGroup(group);
      this.permissionManagement.updateUser(permissionUser);

      return true;
    }

    return false;
  }

  @Override
  public boolean playerRemoveGroup(String world, String player, String group) {
    var optionalPermissionUser = this.permissionUserByName(player);

    if (optionalPermissionUser.isPresent()) {
      var permissionUser = optionalPermissionUser.get();

      permissionUser.removeGroup(group);
      this.permissionManagement.updateUser(permissionUser);

      return true;
    }

    return false;
  }

  @Override
  public String[] getPlayerGroups(String world, String player) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser ->
      permissionUser.groups().stream().map(PermissionUserGroupInfo::group).toArray(String[]::new))
      .orElse(new String[0]);
  }

  @Override
  public String getPrimaryGroup(String world, String player) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser ->
      this.permissionManagement.highestPermissionGroup(permissionUser).name()).orElse(null);
  }

  @Override
  public String[] getGroups() {
    return this.permissionManagement.groups().stream().map(Nameable::name).toArray(String[]::new);
  }

  @Override
  public boolean hasGroupSupport() {
    return true;
  }

}
