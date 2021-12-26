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

import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.util.Optional;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

public class VaultChatImplementation extends Chat {

  private final PermissionManagement permissionManagement;

  public VaultChatImplementation(Permission permission, PermissionManagement permissionManagement) {
    super(permission);
    this.permissionManagement = permissionManagement;
  }

  private Optional<String> userPermissionGroupName(String username) {
    var optionalPermissionUser = this.permissionManagement.usersByName(username).stream()
      .findFirst();

    return optionalPermissionUser
      .map(permissionUser -> this.permissionManagement.highestPermissionGroup(permissionUser).name());
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
  public String getPlayerPrefix(String world, String player) {
    var optionalGroupName = this.userPermissionGroupName(player);

    return optionalGroupName.map(groupName -> this.getGroupPrefix(world, groupName)).orElse(null);
  }

  @Override
  public void setPlayerPrefix(String world, String player, String prefix) {
    var optionalGroupName = this.userPermissionGroupName(player);

    optionalGroupName.ifPresent(groupName -> this.setGroupPrefix(world, groupName, prefix));
  }

  @Override
  public String getPlayerSuffix(String world, String player) {
    var optionalGroupName = this.userPermissionGroupName(player);

    return optionalGroupName.map(groupName -> this.getGroupSuffix(world, groupName)).orElse(null);
  }

  @Override
  public void setPlayerSuffix(String world, String player, String suffix) {
    var optionalGroupName = this.userPermissionGroupName(player);

    optionalGroupName.ifPresent(groupName -> this.setGroupSuffix(world, groupName, suffix));
  }

  @Override
  public String getGroupPrefix(String world, String group) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(PermissionGroup::display).orElse(null);
  }

  @Override
  public void setGroupPrefix(String world, String group, String prefix) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    optionalPermissionGroup.ifPresent(permissionGroup -> {
      permissionGroup.display(prefix);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

  @Override
  public String getGroupSuffix(String world, String group) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(PermissionGroup::suffix).orElse(null);
  }

  @Override
  public void setGroupSuffix(String world, String group, String suffix) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    optionalPermissionGroup.ifPresent(permissionGroup -> {
      permissionGroup.suffix(suffix);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

  @Override
  public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser -> permissionUser.properties().getInt(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setPlayerInfoInteger(String world, String player, String node, int value) {
    var optionalPermissionUser = this.permissionUserByName(player);

    optionalPermissionUser.ifPresent(permissionUser -> {
      permissionUser.properties().append(node, value);
      this.permissionManagement.updateUser(permissionUser);
    });
  }

  @Override
  public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(permissionGroup -> permissionGroup.properties().getInt(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setGroupInfoInteger(String world, String group, String node, int value) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    optionalPermissionGroup.ifPresent(permissionGroup -> {
      permissionGroup.properties().append(node, value);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

  @Override
  public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser -> permissionUser.properties().getDouble(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setPlayerInfoDouble(String world, String player, String node, double value) {
    var optionalPermissionUser = this.permissionUserByName(player);

    optionalPermissionUser.ifPresent(permissionUser -> {
      permissionUser.properties().append(node, value);
      this.permissionManagement.updateUser(permissionUser);
    });
  }

  @Override
  public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(permissionGroup -> permissionGroup.properties().getDouble(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setGroupInfoDouble(String world, String group, String node, double value) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    optionalPermissionGroup.ifPresent(permissionGroup -> {
      permissionGroup.properties().append(node, value);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

  @Override
  public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser -> permissionUser.properties().getBoolean(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
    var optionalPermissionUser = this.permissionUserByName(player);

    optionalPermissionUser.ifPresent(permissionUser -> {
      permissionUser.properties().append(node, value);
      this.permissionManagement.updateUser(permissionUser);
    });
  }

  @Override
  public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup
      .map(permissionGroup -> permissionGroup.properties().getBoolean(node, defaultValue)).orElse(defaultValue);
  }

  @Override
  public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    optionalPermissionGroup.ifPresent(permissionGroup -> {
      permissionGroup.properties().append(node, value);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

  @Override
  public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
    var optionalPermissionUser = this.permissionUserByName(player);

    return optionalPermissionUser.map(permissionUser -> permissionUser.properties().getString(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setPlayerInfoString(String world, String player, String node, String value) {
    var optionalPermissionUser = this.permissionUserByName(player);

    optionalPermissionUser.ifPresent(permissionUser -> {
      permissionUser.properties().append(node, value);
      this.permissionManagement.updateUser(permissionUser);
    });
  }

  @Override
  public String getGroupInfoString(String world, String group, String node, String defaultValue) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    return optionalPermissionGroup.map(permissionGroup -> permissionGroup.properties().getString(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setGroupInfoString(String world, String group, String node, String value) {
    var optionalPermissionGroup = this.permissionGroupByName(group);

    optionalPermissionGroup.ifPresent(permissionGroup -> {
      permissionGroup.properties().append(node, value);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

}
