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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.permission.DefaultCachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissible;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
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

  public WrapperPermissionManagement(Wrapper wrapper) {
    this.wrapper = wrapper;
    this.rpcSender = wrapper.getRPCProviderFactory()
      .providerForClass(wrapper.getNetworkClient(), IPermissionManagement.class);
  }

  @Override
  public void init() {
    Collection<IPermissionGroup> groups = this.loadGroups();
    if (!groups.isEmpty()) {
      for (IPermissionGroup group : groups) {
        this.permissionGroupCache.put(group.getName(), group);
      }
    }

    CloudNetDriver.getInstance().getEventManager().registerListener(new PermissionCacheListener(this));
  }

  @Override
  public boolean reload() {
    boolean success = this.rpcSender.invokeMethod("reload").fireSync();

    if (success) {
      Collection<IPermissionGroup> permissionGroups = this.loadGroups();

      this.permissionGroupLocks.clear();
      this.permissionGroupCache.invalidateAll();

      for (IPermissionGroup group : permissionGroups) {
        this.permissionGroupCache.put(group.getName(), group);
      }
    }

    return success;
  }

  private Collection<IPermissionGroup> loadGroups() {
    return this.rpcSender.invokeMethod("getGroups").fireSync();
  }

  @Override
  public Collection<IPermissionGroup> getGroups() {
    return this.permissionGroupCache.asMap().values();
  }

  @Override
  public void setGroups(@Nullable Collection<? extends IPermissionGroup> groups) {
    this.rpcSender.invokeMethod("setGroups", groups).fireSync();
  }

  @Override
  public IPermissionGroup getDefaultPermissionGroup() {
    return this.permissionGroupCache.asMap().values().stream()
      .filter(IPermissionGroup::isDefaultGroup)
      .findFirst()
      .orElse(null);
  }

  @Override
  public IPermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
    return this.addUser(new PermissionUser(UUID.randomUUID(), name, password, potency));
  }

  @Override
  public IPermissionGroup addGroup(@NotNull String role, int potency) {
    return this.addGroup(new PermissionGroup(role, potency));
  }

  @Override
  public @NotNull PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible,
    @NotNull Permission permission) {
    return this.getPermissionResult(permissible,
      this.wrapper.getCurrentServiceInfoSnapshot().getConfiguration().getGroups(), permission);
  }

  @Override
  public IPermissionUser addUser(@NotNull IPermissionUser permissionUser) {
    Preconditions.checkNotNull(permissionUser);

    return this.rpcSender.invokeMethod("addUser", permissionUser).fireSync();
  }

  @Override
  public void updateUser(@NotNull IPermissionUser permissionUser) {
    this.rpcSender.invokeMethod("updateUser", permissionUser).fireSync();
  }

  @Override
  public boolean deleteUser(@NotNull String name) {
    return this.rpcSender.invokeMethod("deleteUser", name).fireSync();
  }

  @Override
  public boolean deleteUser(@NotNull IPermissionUser permissionUser) {
    return this.rpcSender.invokeMethod("deleteUser", permissionUser).fireSync();
  }

  @Override
  public boolean containsUser(@NotNull UUID uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    IPermissionUser permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return true;
    }

    return this.rpcSender.invokeMethod("containsUser", uniqueId).fireSync();
  }

  @Override
  public boolean containsUser(@NotNull String name) {
    Preconditions.checkNotNull(name);

    for (IPermissionUser permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.getName().equals(name)) {
        return true;
      }
    }

    return this.rpcSender.invokeMethod("containsUser", name).fireSync();
  }

  @Override
  public @Nullable IPermissionUser getUser(@NotNull UUID uniqueId) {
    Preconditions.checkNotNull(uniqueId, "uniqueId");

    IPermissionUser permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.rpcSender.invokeMethod("getUser", uniqueId).fireSync();
    this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);

    return permissionUser;
  }

  @Override
  public @NotNull IPermissionUser getOrCreateUser(@NotNull UUID uniqueId, @NotNull String name) {
    Preconditions.checkNotNull(uniqueId, "uniqueId");
    Preconditions.checkNotNull(name, "name");

    IPermissionUser permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.rpcSender.invokeMethod("getOrCreateUser", uniqueId, name).fireSync();
    this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);

    return permissionUser;
  }

  @Override
  public @NotNull List<IPermissionUser> getUsers(@NotNull String name) {
    Preconditions.checkNotNull(name);

    return this.rpcSender.invokeMethod("getUsers", name).fireSync();
  }

  @Override
  public @NotNull Collection<IPermissionUser> getUsers() {
    return this.rpcSender.invokeMethod("getUsers").fireSync();
  }

  @Override
  public void setUsers(@NotNull Collection<? extends IPermissionUser> users) {
    this.rpcSender.invokeMethod("setUsers", users).fireSync();
  }

  @Override
  public Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
    Preconditions.checkNotNull(group);

    return this.rpcSender.invokeMethod("getUsersByGroup", group).fireSync();
  }

  @Override
  public IPermissionGroup addGroup(@NotNull IPermissionGroup permissionGroup) {
    return this.rpcSender.invokeMethod("addGroup", permissionGroup).fireSync();
  }

  @Override
  public void updateGroup(@NotNull IPermissionGroup permissionGroup) {
    this.rpcSender.invokeMethod("updateGroup", permissionGroup).fireSync();
  }

  @Override
  public void deleteGroup(@NotNull String name) {
    this.rpcSender.invokeMethod("deleteGroup", name).fireSync();
  }

  @Override
  public void deleteGroup(@NotNull IPermissionGroup permissionGroup) {
    this.rpcSender.invokeMethod("deleteGroup", permissionGroup).fireSync();
  }

  @Override
  public boolean containsGroup(@NotNull String group) {
    return this.permissionGroupCache.getIfPresent(group) != null;
  }

  @Override
  public @Nullable IPermissionGroup getGroup(@NotNull String name) {
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
  public IPermissionUser getFirstUser(String name) {
    Preconditions.checkNotNull(name);

    for (IPermissionUser permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.getName().equals(name)) {
        return permissionUser;
      }
    }

    return this.rpcSender.invokeMethod("getFirstUser", name).fireSync();
  }
}
