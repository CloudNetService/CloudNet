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

package de.dytanic.cloudnet.wrapper.permission;

import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.permission.DefaultCachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissible;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionCheckResult;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.wrapper.Wrapper;
import de.dytanic.cloudnet.wrapper.network.listener.message.PermissionChannelMessageListener;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrapperPermissionManagement extends DefaultCachedPermissionManagement implements IPermissionManagement {

  private final Wrapper wrapper;
  private final RPCSender rpcSender;

  private final PermissionCacheListener cacheListener;
  private final PermissionChannelMessageListener channelMessageListener;

  public WrapperPermissionManagement(@NotNull Wrapper wrapper) {
    this.wrapper = wrapper;
    this.rpcSender = wrapper.rpcProviderFactory().providerForClass(
      wrapper.networkClient(),
      IPermissionManagement.class);

    this.cacheListener = new PermissionCacheListener(this);
    this.channelMessageListener = new PermissionChannelMessageListener(wrapper.eventManager(), this);
  }

  @Override
  public void init() {
    var groups = this.loadGroups();
    if (!groups.isEmpty()) {
      for (var group : groups) {
        this.permissionGroupCache.put(group.name(), group);
      }
    }

    this.wrapper.eventManager().registerListeners(this.cacheListener, this.channelMessageListener);
  }

  @Override
  public void close() {
    this.wrapper.eventManager().unregisterListener(this.cacheListener, this.channelMessageListener);
  }

  @Override
  public boolean reload() {
    boolean success = this.rpcSender.invokeMethod("reload").fireSync();

    if (success) {
      var permissionGroups = this.loadGroups();

      this.permissionGroupLocks.clear();
      this.permissionGroupCache.invalidateAll();

      for (var group : permissionGroups) {
        this.permissionGroupCache.put(group.name(), group);
      }
    }

    return success;
  }

  @Override
  public @NotNull Collection<PermissionGroup> groups() {
    return this.permissionGroupCache.asMap().values();
  }

  @Override
  public void groups(@Nullable Collection<? extends PermissionGroup> groups) {
    this.rpcSender.invokeMethod("groups", groups).fireSync();
  }

  @Override
  public PermissionGroup defaultPermissionGroup() {
    return this.permissionGroupCache.asMap().values().stream()
      .filter(PermissionGroup::defaultGroup)
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NotNull PermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
    return this.addPermissionUser(new PermissionUser(UUID.randomUUID(), name, password, potency));
  }

  @Override
  public @NotNull PermissionGroup addGroup(@NotNull String role, int potency) {
    return this.addPermissionGroup(new PermissionGroup(role, potency));
  }

  @Override
  public @NotNull PermissionCheckResult permissionResult(
    @NotNull IPermissible permissible,
    @NotNull Permission permission
  ) {
    return this.groupsPermissionResult(
      permissible,
      this.wrapper.currentServiceInfo().configuration().groups().toArray(new String[0]),
      permission);
  }

  @Override
  public @NotNull PermissionUser addPermissionUser(@NotNull PermissionUser permissionUser) {
    return this.rpcSender.invokeMethod("addPermissionUser", permissionUser).fireSync();
  }

  @Override
  public void updateUser(@NotNull PermissionUser permissionUser) {
    this.rpcSender.invokeMethod("updateUser", permissionUser).fireSync();
  }

  @Override
  public boolean deleteUser(@NotNull String name) {
    return this.rpcSender.invokeMethod("deleteUser", name).fireSync();
  }

  @Override
  public boolean deletePermissionUser(@NotNull PermissionUser permissionUser) {
    return this.rpcSender.invokeMethod("deletePermissionUser", permissionUser).fireSync();
  }

  @Override
  public boolean containsUser(@NotNull UUID uniqueId) {
    var permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return true;
    }

    return this.rpcSender.invokeMethod("containsUser", uniqueId).fireSync();
  }

  @Override
  public boolean containsOneUser(@NotNull String name) {
    for (var permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.name().equals(name)) {
        return true;
      }
    }

    return this.rpcSender.invokeMethod("containsOneUser", name).fireSync();
  }

  @Override
  public @Nullable PermissionUser user(@NotNull UUID uniqueId) {
    var permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.rpcSender.invokeMethod("user", uniqueId).fireSync();
    this.permissionUserCache.put(permissionUser.uniqueId(), permissionUser);

    return permissionUser;
  }

  @Override
  public @NotNull PermissionUser getOrCreateUser(@NotNull UUID uniqueId, @NotNull String name) {
    var permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.rpcSender.invokeMethod("getOrCreateUser", uniqueId, name).fireSync();
    this.permissionUserCache.put(permissionUser.uniqueId(), permissionUser);

    return permissionUser;
  }

  @Override
  public @NotNull List<PermissionUser> usersByName(@NotNull String name) {
    return this.rpcSender.invokeMethod("usersByName", name).fireSync();
  }

  @Override
  public @NotNull Collection<PermissionUser> users() {
    return this.rpcSender.invokeMethod("users").fireSync();
  }

  @Override
  public @NotNull Collection<PermissionUser> usersByGroup(@NotNull String group) {
    return this.rpcSender.invokeMethod("usersByGroup", group).fireSync();
  }

  @Override
  public @NotNull PermissionGroup addPermissionGroup(@NotNull PermissionGroup permissionGroup) {
    return this.rpcSender.invokeMethod("addPermissionGroup", permissionGroup).fireSync();
  }

  @Override
  public void updateGroup(@NotNull PermissionGroup permissionGroup) {
    this.rpcSender.invokeMethod("updateGroup", permissionGroup).fireSync();
  }

  @Override
  public boolean deleteGroup(@NotNull String name) {
    return this.rpcSender.invokeMethod("deleteGroup", name).fireSync();
  }

  @Override
  public boolean deletePermissionGroup(@NotNull PermissionGroup permissionGroup) {
    return this.rpcSender.invokeMethod("deletePermissionGroup", permissionGroup).fireSync();
  }

  @Override
  public boolean containsGroup(@NotNull String group) {
    return this.permissionGroupCache.getIfPresent(group) != null;
  }

  @Override
  public @Nullable PermissionGroup group(@NotNull String name) {
    return this.permissionGroupCache.getIfPresent(name);
  }

  @Override
  public IPermissionManagement childPermissionManagement() {
    return null;
  }

  @Override
  public boolean canBeOverwritten() {
    return true;
  }

  @Override
  public PermissionUser firstUser(String name) {
    for (var permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.name().equals(name)) {
        return permissionUser;
      }
    }

    return this.rpcSender.invokeMethod("firstUser", name).fireSync();
  }

  protected @NotNull Collection<PermissionGroup> loadGroups() {
    return this.rpcSender.invokeMethod("groups").fireSync();
  }
}
