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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrapperPermissionManagement extends DefaultCachedPermissionManagement implements IPermissionManagement {

  private final Wrapper wrapper;
  private final RPCSender rpcSender;
  private final PermissionCacheListener listener;

  public WrapperPermissionManagement(@NotNull Wrapper wrapper) {
    this.wrapper = wrapper;
    this.rpcSender = wrapper.getRPCProviderFactory().providerForClass(
      wrapper.getNetworkClient(),
      IPermissionManagement.class);
    this.listener = new PermissionCacheListener(this);
  }

  @Override
  public void init() {
    Collection<PermissionGroup> groups = this.getGroups();
    if (!groups.isEmpty()) {
      for (PermissionGroup group : groups) {
        this.permissionGroupCache.put(group.getName(), group);
      }
    }

    this.wrapper.getEventManager().registerListener(this.listener);
  }

  @Override
  public void close() {
    this.wrapper.getEventManager().unregisterListener(this.listener);
  }

  @Override
  public boolean reload() {
    boolean success = this.rpcSender.invokeMethod("reload").fireSync();

    if (success) {
      Collection<PermissionGroup> permissionGroups = this.getGroups();

      this.permissionGroupLocks.clear();
      this.permissionGroupCache.invalidateAll();

      for (PermissionGroup group : permissionGroups) {
        this.permissionGroupCache.put(group.getName(), group);
      }
    }

    return success;
  }

  @Override
  public @NotNull Collection<PermissionGroup> getGroups() {
    return this.permissionGroupCache.asMap().values();
  }

  @Override
  public void setGroups(@Nullable Collection<? extends PermissionGroup> groups) {
    this.rpcSender.invokeMethod("setGroups", groups).fireSync();
  }

  @Override
  public PermissionGroup getDefaultPermissionGroup() {
    return this.permissionGroupCache.asMap().values().stream()
      .filter(PermissionGroup::isDefaultGroup)
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NotNull PermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
    return this.addUser(new PermissionUser(UUID.randomUUID(), name, password, potency));
  }

  @Override
  public @NotNull PermissionGroup addGroup(@NotNull String role, int potency) {
    return this.addGroup(new PermissionGroup(role, potency));
  }

  @Override
  public @NotNull PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible,
    @NotNull Permission permission) {
    return this.getPermissionResult(permissible,
      this.wrapper.getCurrentServiceInfoSnapshot().getConfiguration().getGroups(), permission);
  }

  @Override
  public @NotNull PermissionUser addUser(@NotNull PermissionUser permissionUser) {
    return this.rpcSender.invokeMethod("addUser", permissionUser).fireSync();
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
  public boolean deleteUser(@NotNull PermissionUser permissionUser) {
    return this.rpcSender.invokeMethod("deleteUser", permissionUser).fireSync();
  }

  @Override
  public boolean containsUser(@NotNull UUID uniqueId) {
    PermissionUser permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return true;
    }

    return this.rpcSender.invokeMethod("containsUser", uniqueId).fireSync();
  }

  @Override
  public boolean containsUser(@NotNull String name) {
    for (PermissionUser permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.getName().equals(name)) {
        return true;
      }
    }

    return this.rpcSender.invokeMethod("containsUser", name).fireSync();
  }

  @Override
  public @Nullable PermissionUser getUser(@NotNull UUID uniqueId) {
    PermissionUser permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.rpcSender.invokeMethod("getUser", uniqueId).fireSync();
    this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);

    return permissionUser;
  }

  @Override
  public @NotNull PermissionUser getOrCreateUser(@NotNull UUID uniqueId, @NotNull String name) {
    PermissionUser permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.rpcSender.invokeMethod("getOrCreateUser", uniqueId, name).fireSync();
    this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);

    return permissionUser;
  }

  @Override
  public @NotNull List<PermissionUser> getUsers(@NotNull String name) {
    return this.rpcSender.invokeMethod("getUsers", name).fireSync();
  }

  @Override
  public @NotNull Collection<PermissionUser> getUsers() {
    return this.rpcSender.invokeMethod("getUsers").fireSync();
  }

  @Override
  public @NotNull Collection<PermissionUser> getUsersByGroup(@NotNull String group) {
    return this.rpcSender.invokeMethod("getUsersByGroup", group).fireSync();
  }

  @Override
  public @NotNull PermissionGroup addGroup(@NotNull PermissionGroup permissionGroup) {
    return this.rpcSender.invokeMethod("addGroup", permissionGroup).fireSync();
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
  public boolean deleteGroup(@NotNull PermissionGroup permissionGroup) {
    return this.rpcSender.invokeMethod("deleteGroup", permissionGroup).fireSync();
  }

  @Override
  public boolean containsGroup(@NotNull String group) {
    return this.permissionGroupCache.getIfPresent(group) != null;
  }

  @Override
  public @Nullable PermissionGroup getGroup(@NotNull String name) {
    return this.permissionGroupCache.getIfPresent(name);
  }

  @Override
  public IPermissionManagement getChildPermissionManagement() {
    return null;
  }

  @Override
  public boolean canBeOverwritten() {
    return true;
  }

  @Override
  public PermissionUser getFirstUser(String name) {
    for (PermissionUser permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.getName().equals(name)) {
        return permissionUser;
      }
    }

    return this.rpcSender.invokeMethod("getFirstUser", name).fireSync();
  }
}
