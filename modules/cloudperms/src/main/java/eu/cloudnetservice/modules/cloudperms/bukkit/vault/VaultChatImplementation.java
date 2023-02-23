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

package eu.cloudnetservice.modules.cloudperms.bukkit.vault;

import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import java.util.Optional;
import lombok.NonNull;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

public class VaultChatImplementation extends Chat {

  private final PermissionManagement permissionManagement;

  public VaultChatImplementation(@NonNull Permission permission, @NonNull PermissionManagement permissionManagement) {
    super(permission);
    this.permissionManagement = permissionManagement;
  }

  private @NonNull Optional<String> userPermissionGroupName(@NonNull String username) {
    return this.permissionUserByName(username)
      .map(this.permissionManagement::highestPermissionGroup)
      .map(PermissionGroup::name);
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
  public String getPlayerPrefix(String world, String player) {
    return this.userPermissionGroupName(player).map(groupName -> this.getGroupPrefix(world, groupName)).orElse(null);
  }

  @Override
  public void setPlayerPrefix(String world, String player, String prefix) {
    // unsupported
  }

  @Override
  public String getPlayerSuffix(String world, String player) {
    return this.userPermissionGroupName(player).map(groupName -> this.getGroupSuffix(world, groupName)).orElse(null);
  }

  @Override
  public void setPlayerSuffix(String world, String player, String suffix) {
    // unsupported
  }

  @Override
  public String getGroupPrefix(String world, String group) {
    return this.permissionGroupByName(group).map(PermissionGroup::display).orElse(null);
  }

  @Override
  public void setGroupPrefix(String world, String group, String prefix) {
    this.permissionManagement.modifyGroup(group, ($, builder) -> builder.prefix(prefix));
  }

  @Override
  public String getGroupSuffix(String world, String group) {
    return this.permissionGroupByName(group).map(PermissionGroup::suffix).orElse(null);
  }

  @Override
  public void setGroupSuffix(String world, String group, String suffix) {
    this.permissionManagement.modifyGroup(group, ($, builder) -> builder.suffix(suffix));
  }

  @Override
  public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
    return this.permissionUserByName(player)
      .map(permissionUser -> permissionUser.propertyHolder().getInt(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setPlayerInfoInteger(String world, String player, String node, int value) {
    this.permissionUserByName(player).ifPresent(permissionUser -> {
      permissionUser.propertyHolder().append(node, value);
      this.permissionManagement.updateUser(permissionUser);
    });
  }

  @Override
  public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
    return this.permissionGroupByName(group)
      .map(permissionGroup -> permissionGroup.propertyHolder().getInt(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setGroupInfoInteger(String world, String group, String node, int value) {
    this.permissionGroupByName(group).ifPresent(permissionGroup -> {
      permissionGroup.propertyHolder().append(node, value);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

  @Override
  public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
    return this.permissionUserByName(player)
      .map(permissionUser -> permissionUser.propertyHolder().getDouble(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setPlayerInfoDouble(String world, String player, String node, double value) {
    this.permissionUserByName(player).ifPresent(permissionUser -> {
      permissionUser.propertyHolder().append(node, value);
      this.permissionManagement.updateUser(permissionUser);
    });
  }

  @Override
  public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
    return this.permissionGroupByName(group)
      .map(permissionGroup -> permissionGroup.propertyHolder().getDouble(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setGroupInfoDouble(String world, String group, String node, double value) {
    this.permissionGroupByName(group).ifPresent(permissionGroup -> {
      permissionGroup.propertyHolder().append(node, value);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

  @Override
  public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
    return this.permissionUserByName(player)
      .map(permissionUser -> permissionUser.propertyHolder().getBoolean(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
    this.permissionUserByName(player).ifPresent(permissionUser -> {
      permissionUser.propertyHolder().append(node, value);
      this.permissionManagement.updateUser(permissionUser);
    });
  }

  @Override
  public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
    return this.permissionGroupByName(group)
      .map(permissionGroup -> permissionGroup.propertyHolder().getBoolean(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
    this.permissionGroupByName(group).ifPresent(permissionGroup -> {
      permissionGroup.propertyHolder().append(node, value);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }

  @Override
  public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
    return this.permissionUserByName(player)
      .map(permissionUser -> permissionUser.propertyHolder().getString(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setPlayerInfoString(String world, String player, String node, String value) {
    this.permissionUserByName(player).ifPresent(permissionUser -> {
      permissionUser.propertyHolder().append(node, value);
      this.permissionManagement.updateUser(permissionUser);
    });
  }

  @Override
  public String getGroupInfoString(String world, String group, String node, String defaultValue) {
    return this.permissionGroupByName(group)
      .map(permissionGroup -> permissionGroup.propertyHolder().getString(node, defaultValue))
      .orElse(defaultValue);
  }

  @Override
  public void setGroupInfoString(String world, String group, String node, String value) {
    this.permissionGroupByName(group).ifPresent(permissionGroup -> {
      permissionGroup.propertyHolder().append(node, value);
      this.permissionManagement.updateGroup(permissionGroup);
    });
  }
}
