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

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUserGroupInfo;
import java.util.Optional;
import net.milkbowl.vault.permission.Permission;

public class VaultPermissionImplementation extends Permission {

  private final IPermissionManagement permissionManagement;

  public VaultPermissionImplementation(IPermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  private Optional<IPermissionUser> permissionUserByName(String name) {
    return this.permissionManagement.getUsers(name).stream().findFirst();
  }

  private Optional<IPermissionGroup> permissionGroupByName(String name) {
    return Optional.ofNullable(this.permissionManagement.getGroup(name));
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
    Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.isPresent() && optionalPermissionUser.get().hasPermission(permission).asBoolean();
  }

  @Override
  public boolean playerAdd(String world, String player, String permission) {
    Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser -> {
      boolean success = permissionUser.addPermission(permission);
      this.permissionManagement.updateUser(permissionUser);

      return success;
    }).orElse(false);
  }

  @Override
  public boolean playerRemove(String world, String player, String permission) {
    Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser -> {
      boolean success = permissionUser.removePermission(permission);
      this.permissionManagement.updateUser(permissionUser);

      return success;
    }).orElse(false);
  }

  @Override
  public boolean groupHas(String world, String group, String permission) {
    Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.isPresent() && optionalPermissionGroup.get().hasPermission(permission).asBoolean();
  }

  @Override
  public boolean groupAdd(String world, String group, String permission) {
    Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(permissionGroup -> {
      boolean success = permissionGroup.addPermission(permission);
      this.permissionManagement.updateGroup(permissionGroup);

      return success;
    }).orElse(false);
  }

  @Override
  public boolean groupRemove(String world, String group, String permission) {
    Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(permissionGroup -> {
      boolean success = permissionGroup.removePermission(permission);
      this.permissionManagement.updateGroup(permissionGroup);

      return success;
    }).orElse(false);
  }

  @Override
  public boolean playerInGroup(String world, String player, String group) {
    Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.isPresent() && optionalPermissionUser.get().inGroup(group);
  }

  @Override
  public boolean playerAddGroup(String world, String player, String group) {
    Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

    if (optionalPermissionUser.isPresent()) {
      IPermissionUser permissionUser = optionalPermissionUser.get();

      permissionUser.addGroup(group);
      this.permissionManagement.updateUser(permissionUser);

      return true;
    }

    return false;
  }

  @Override
  public boolean playerRemoveGroup(String world, String player, String group) {
    Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

    if (optionalPermissionUser.isPresent()) {
      IPermissionUser permissionUser = optionalPermissionUser.get();

      permissionUser.removeGroup(group);
      this.permissionManagement.updateUser(permissionUser);

      return true;
    }

    return false;
  }

  @Override
  public String[] getPlayerGroups(String world, String player) {
    Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser ->
      permissionUser.getGroups().stream().map(PermissionUserGroupInfo::getGroup).toArray(String[]::new))
      .orElse(new String[0]);
  }

  @Override
  public String getPrimaryGroup(String world, String player) {
    Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser ->
      this.permissionManagement.getHighestPermissionGroup(permissionUser).getName()).orElse(null);
  }

  @Override
  public String[] getGroups() {
    return this.permissionManagement.getGroups().stream().map(INameable::getName).toArray(String[]::new);
  }

  @Override
  public boolean hasGroupSupport() {
    return true;
  }

}
