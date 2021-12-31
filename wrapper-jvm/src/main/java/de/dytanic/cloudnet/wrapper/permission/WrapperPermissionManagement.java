/*
 * Copyright 2019-2022 CloudNetService team & contributors
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
import de.dytanic.cloudnet.driver.permission.Permissible;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionCheckResult;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.wrapper.Wrapper;
import de.dytanic.cloudnet.wrapper.network.listener.message.PermissionChannelMessageListener;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class WrapperPermissionManagement extends DefaultCachedPermissionManagement implements PermissionManagement {

  private final Wrapper wrapper;
  private final RPCSender rpcSender;

  private final PermissionCacheListener cacheListener;
  private final PermissionChannelMessageListener channelMessageListener;

  public WrapperPermissionManagement(@NonNull Wrapper wrapper) {
    this.wrapper = wrapper;
    this.rpcSender = wrapper.rpcProviderFactory().providerForClass(
      wrapper.networkClient(),
      PermissionManagement.class);

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
  public @NonNull Collection<PermissionGroup> groups() {
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
  public @NonNull PermissionUser addUser(@NonNull String name, @NonNull String password, int potency) {
    return this.addPermissionUser(new PermissionUser(UUID.randomUUID(), name, password, potency));
  }

  @Override
  public @NonNull PermissionGroup addGroup(@NonNull String role, int potency) {
    return this.addPermissionGroup(new PermissionGroup(role, potency));
  }

  @Override
  public @NonNull PermissionCheckResult permissionResult(
    @NonNull Permissible permissible,
    @NonNull Permission permission
  ) {
    return this.groupsPermissionResult(
      permissible,
      this.wrapper.currentServiceInfo().configuration().groups().toArray(new String[0]),
      permission);
  }

  @Override
  public @NonNull PermissionUser addPermissionUser(@NonNull PermissionUser permissionUser) {
    return this.rpcSender.invokeMethod("addPermissionUser", permissionUser).fireSync();
  }

  @Override
  public void updateUser(@NonNull PermissionUser permissionUser) {
    this.rpcSender.invokeMethod("updateUser", permissionUser).fireSync();
  }

  @Override
  public boolean deleteUser(@NonNull String name) {
    return this.rpcSender.invokeMethod("deleteUser", name).fireSync();
  }

  @Override
  public boolean deletePermissionUser(@NonNull PermissionUser permissionUser) {
    return this.rpcSender.invokeMethod("deletePermissionUser", permissionUser).fireSync();
  }

  @Override
  public boolean containsUser(@NonNull UUID uniqueId) {
    var permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return true;
    }

    return this.rpcSender.invokeMethod("containsUser", uniqueId).fireSync();
  }

  @Override
  public boolean containsOneUser(@NonNull String name) {
    for (var permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.name().equals(name)) {
        return true;
      }
    }

    return this.rpcSender.invokeMethod("containsOneUser", name).fireSync();
  }

  @Override
  public @Nullable PermissionUser user(@NonNull UUID uniqueId) {
    var permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.rpcSender.invokeMethod("user", uniqueId).fireSync();
    this.permissionUserCache.put(permissionUser.uniqueId(), permissionUser);

    return permissionUser;
  }

  @Override
  public @NonNull PermissionUser getOrCreateUser(@NonNull UUID uniqueId, @NonNull String name) {
    var permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.rpcSender.invokeMethod("getOrCreateUser", uniqueId, name).fireSync();
    this.permissionUserCache.put(permissionUser.uniqueId(), permissionUser);

    return permissionUser;
  }

  @Override
  public @NonNull List<PermissionUser> usersByName(@NonNull String name) {
    return this.rpcSender.invokeMethod("usersByName", name).fireSync();
  }

  @Override
  public @NonNull Collection<PermissionUser> users() {
    return this.rpcSender.invokeMethod("users").fireSync();
  }

  @Override
  public @NonNull Collection<PermissionUser> usersByGroup(@NonNull String group) {
    return this.rpcSender.invokeMethod("usersByGroup", group).fireSync();
  }

  @Override
  public @NonNull PermissionGroup addPermissionGroup(@NonNull PermissionGroup permissionGroup) {
    return this.rpcSender.invokeMethod("addPermissionGroup", permissionGroup).fireSync();
  }

  @Override
  public void updateGroup(@NonNull PermissionGroup permissionGroup) {
    this.rpcSender.invokeMethod("updateGroup", permissionGroup).fireSync();
  }

  @Override
  public boolean deleteGroup(@NonNull String name) {
    return this.rpcSender.invokeMethod("deleteGroup", name).fireSync();
  }

  @Override
  public boolean deletePermissionGroup(@NonNull PermissionGroup permissionGroup) {
    return this.rpcSender.invokeMethod("deletePermissionGroup", permissionGroup).fireSync();
  }

  @Override
  public boolean containsGroup(@NonNull String group) {
    return this.permissionGroupCache.getIfPresent(group) != null;
  }

  @Override
  public @Nullable PermissionGroup group(@NonNull String name) {
    return this.permissionGroupCache.getIfPresent(name);
  }

  @Override
  public PermissionManagement childPermissionManagement() {
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

  protected @NonNull Collection<PermissionGroup> loadGroups() {
    return this.rpcSender.invokeMethod("groups").fireSync();
  }
}
