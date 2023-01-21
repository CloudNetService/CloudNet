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

package eu.cloudnetservice.wrapper.permission;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.permission.DefaultCachedPermissionManagement;
import eu.cloudnetservice.driver.permission.Permissible;
import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionCheckResult;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.wrapper.network.listener.message.PermissionChannelMessageListener;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class WrapperPermissionManagement extends DefaultCachedPermissionManagement {

  private final RPCSender rpcSender;
  private final EventManager eventManager;
  private final ServiceConfiguration serviceConfiguration;

  private final PermissionCacheListener cacheListener;
  private final PermissionChannelMessageListener channelMessageListener;

  public WrapperPermissionManagement(
    @NonNull RPCSender sender,
    @NonNull EventManager eventManager,
    @NonNull ServiceConfiguration serviceConfiguration
  ) {
    this.rpcSender = sender;
    this.eventManager = eventManager;
    this.serviceConfiguration = serviceConfiguration;

    this.channelMessageListener = new PermissionChannelMessageListener();
    this.cacheListener = new PermissionCacheListener(this);
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
      this.serviceConfiguration.groups().toArray(new String[0]),
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
    // get the permission user from the node
    permissionUser = this.rpcSender.invokeMethod("user", uniqueId).fireSync();
    if (permissionUser != null) {
      // only cache the permission user if the user exists
      this.permissionUserCache.put(permissionUser.uniqueId(), permissionUser);
    }

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
