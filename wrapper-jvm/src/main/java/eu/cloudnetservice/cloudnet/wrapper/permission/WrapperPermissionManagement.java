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

package eu.cloudnetservice.cloudnet.wrapper.permission;

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.permission.DefaultCachedPermissionManagement;
import eu.cloudnetservice.cloudnet.driver.permission.Permissible;
import eu.cloudnetservice.cloudnet.driver.permission.Permission;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionCheckResult;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionGroup;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionManagement;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionUser;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.wrapper.network.listener.message.PermissionChannelMessageListener;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class WrapperPermissionManagement extends DefaultCachedPermissionManagement {

  private final RPCSender rpcSender;
  private final EventManager eventManager;

  private final PermissionCacheListener cacheListener;
  private final PermissionChannelMessageListener channelMessageListener;

  public WrapperPermissionManagement(@NonNull RPCSender sender) {
    this.rpcSender = sender;
    this.eventManager = CloudNetDriver.instance().eventManager();

    this.cacheListener = new PermissionCacheListener(this);
    this.channelMessageListener = new PermissionChannelMessageListener(this.eventManager, this);
  }

  @Override
  public void init() {
    var groups = this.loadGroups();
    if (!groups.isEmpty()) {
      for (var group : groups) {
        this.permissionGroupCache.put(group.name(), group);
      }
    }

    this.eventManager.registerListeners(this.cacheListener, this.channelMessageListener);
  }

  @Override
  public void close() {
    this.eventManager.unregisterListener(this.cacheListener, this.channelMessageListener);
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
  public @Nullable PermissionGroup defaultPermissionGroup() {
    return this.permissionGroupCache.asMap().values().stream()
      .filter(PermissionGroup::defaultGroup)
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NonNull PermissionUser addPermissionUser(@NonNull String name, @NonNull String password, int potency) {
    return this.addPermissionUser(PermissionUser.builder()
      .name(name)
      .uniqueId(UUID.randomUUID())
      .password(password)
      .potency(potency)
      .build());
  }

  @Override
  public @NonNull PermissionGroup addPermissionGroup(@NonNull String name, int potency) {
    return this.addPermissionGroup(PermissionGroup.builder().name(name).potency(potency).build());
  }

  @Override
  public @NonNull PermissionCheckResult permissionResult(
    @NonNull Permissible permissible,
    @NonNull Permission permission
  ) {
    return this.groupsPermissionResult(
      permissible,
      Wrapper.instance().currentServiceInfo().configuration().groups().toArray(new String[0]),
      permission);
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
  public boolean containsGroup(@NonNull String group) {
    return this.permissionGroupCache.getIfPresent(group) != null;
  }

  @Override
  public @Nullable PermissionGroup group(@NonNull String name) {
    return this.permissionGroupCache.getIfPresent(name);
  }

  @Override
  public @Nullable PermissionManagement childPermissionManagement() {
    return null;
  }

  @Override
  public boolean allowsOverride() {
    return true;
  }

  @Override
  public @Nullable PermissionUser firstUser(@NonNull String name) {
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
