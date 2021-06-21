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

package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.DefaultCachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissible;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionCheckResult;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "3.5")
public class CloudPermissionsManagement extends DefaultCachedPermissionManagement implements IPermissionManagement,
  CachedPermissionManagement {

  private final IPermissionManagement wrapped;

  public CloudPermissionsManagement(IPermissionManagement wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * @deprecated use {@link CloudNetDriver#getPermissionManagement()} instead.
   */
  @Deprecated
  public static CloudPermissionsManagement getInstance() {
    return (CloudPermissionsManagement) CloudNetDriver.getInstance().getPermissionManagement();
  }

  public static CloudPermissionsManagement newInstance() {
    CloudPermissionsManagement management = new CloudPermissionsPermissionManagement(
      Objects.requireNonNull(CloudNetDriver.getInstance().getPermissionManagement()));
    CloudNetDriver.getInstance().setPermissionManagement(management);
    return management;
  }

  @Override
  public IPermissionManagement getChildPermissionManagement() {
    return this.wrapped.getChildPermissionManagement();
  }

  @Override
  public boolean canBeOverwritten() {
    return this.wrapped.canBeOverwritten();
  }

  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "3.5")
  @Deprecated
  public List<IPermissionUser> getUser(String name) {
    return this.wrapped.getUser(name);
  }

  @Override
  public IPermissionUser getFirstUser(String name) {
    return this.wrapped.getFirstUser(name);
  }

  @Override
  public void init() {
    this.wrapped.init();
  }

  @Override
  public boolean reload() {
    return this.wrapped.reload();
  }

  @Override
  public IPermissionGroup getHighestPermissionGroup(@NotNull IPermissionUser permissionUser) {
    return this.wrapped.getHighestPermissionGroup(permissionUser);
  }

  @Override
  public IPermissionGroup getDefaultPermissionGroup() {
    return this.wrapped.getDefaultPermissionGroup();
  }

  @Override
  public boolean testPermissionGroup(@Nullable IPermissionGroup permissionGroup) {
    return this.wrapped.testPermissionGroup(permissionGroup);
  }

  @Override
  public boolean testPermissionUser(@Nullable IPermissionUser permissionUser) {
    return this.wrapped.testPermissionUser(permissionUser);
  }

  @Override
  public boolean testPermissible(@Nullable IPermissible permissible) {
    return this.wrapped.testPermissible(permissible);
  }

  @Override
  public IPermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
    return this.wrapped.addUser(name, password, potency);
  }

  @Override
  public IPermissionGroup addGroup(@NotNull String role, int potency) {
    return this.wrapped.addGroup(role, potency);
  }

  @Override
  @NotNull
  public Collection<IPermissionGroup> getGroups(@Nullable IPermissible permissible) {
    return this.wrapped.getGroups(permissible);
  }

  @Override
  @Deprecated
  public Collection<IPermissionGroup> getExtendedGroups(@Nullable IPermissionGroup group) {
    return this.wrapped.getExtendedGroups(group);
  }

  @Override
  public boolean hasPermission(@NotNull IPermissible permissible, @NotNull String permission) {
    return this.wrapped.hasPermission(permissible, permission);
  }

  @Override
  public boolean hasPermission(@NotNull IPermissible permissible, @NotNull Permission permission) {
    return this.wrapped.hasPermission(permissible, permission);
  }

  @Override
  public boolean hasPermission(@NotNull IPermissible permissible, @NotNull String group,
    @NotNull Permission permission) {
    return this.wrapped.hasPermission(permissible, group, permission);
  }

  @Override
  @NotNull
  public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String permission) {
    return this.wrapped.getPermissionResult(permissible, permission);
  }

  @Override
  @NotNull
  public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull Permission permission) {
    return this.wrapped.getPermissionResult(permissible, permission);
  }

  @Override
  @NotNull
  public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String group,
    @NotNull Permission permission) {
    return this.wrapped.getPermissionResult(permissible, group, permission);
  }

  @Override
  public @NotNull PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible,
    @NotNull Iterable<String> groups, @NotNull Permission permission) {
    return this.wrapped.getPermissionResult(permissible, groups, permission);
  }

  @Override
  public @NotNull PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String[] groups,
    @NotNull Permission permission) {
    return this.wrapped.getPermissionResult(permissible, groups, permission);
  }

  @Override
  public @Nullable Permission findHighestPermission(@NotNull Collection<Permission> permissions,
    @NotNull Permission permission) {
    return this.wrapped.findHighestPermission(permissions, permission);
  }

  @Override
  public @NotNull Collection<Permission> getAllPermissions(@NotNull IPermissible permissible) {
    return this.wrapped.getAllPermissions(permissible);
  }

  @Override
  public @NotNull Collection<Permission> getAllPermissions(@NotNull IPermissible permissible, @Nullable String group) {
    return this.wrapped.getAllPermissions(permissible, group);
  }

  @Override
  public IPermissionUser addUser(@NotNull IPermissionUser permissionUser) {
    return this.wrapped.addUser(permissionUser);
  }

  @Override
  public void updateUser(@NotNull IPermissionUser permissionUser) {
    this.wrapped.updateUser(permissionUser);
  }

  @Override
  public boolean deleteUser(@NotNull String name) {
    return this.wrapped.deleteUser(name);
  }

  @Override
  public boolean deleteUser(@NotNull IPermissionUser permissionUser) {
    return this.wrapped.deleteUser(permissionUser);
  }

  @Override
  public boolean containsUser(@NotNull UUID uniqueId) {
    return this.wrapped.containsUser(uniqueId);
  }

  @Override
  public boolean containsUser(@NotNull String name) {
    return this.wrapped.containsUser(name);
  }

  @Override
  public @Nullable IPermissionUser getUser(@NotNull UUID uniqueId) {
    return this.wrapped.getUser(uniqueId);
  }

  @Override
  public @NotNull IPermissionUser getOrCreateUser(@NotNull UUID uniqueId, @NotNull String name) {
    return this.wrapped.getOrCreateUser(uniqueId, name);
  }

  @Override
  public @NotNull List<IPermissionUser> getUsers(@NotNull String name) {
    return this.wrapped.getUsers(name);
  }

  @Override
  public @NotNull Collection<IPermissionUser> getUsers() {
    return this.wrapped.getUsers();
  }

  @Override
  public void setUsers(@Nullable Collection<? extends IPermissionUser> users) {
    this.wrapped.setUsers(users == null ? Collections.emptyList() : users);
  }

  @Override
  public Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
    return this.wrapped.getUsersByGroup(group);
  }

  @Override
  public IPermissionGroup addGroup(@NotNull IPermissionGroup permissionGroup) {
    return this.wrapped.addGroup(permissionGroup);
  }

  @Override
  public void updateGroup(@NotNull IPermissionGroup permissionGroup) {
    this.wrapped.updateGroup(permissionGroup);
  }

  @Override
  public void deleteGroup(@NotNull String name) {
    this.wrapped.deleteGroup(name);
  }

  @Override
  public void deleteGroup(@NotNull IPermissionGroup permissionGroup) {
    this.wrapped.deleteGroup(permissionGroup);
  }

  @Override
  public boolean containsGroup(@NotNull String group) {
    return this.wrapped.containsGroup(group);
  }

  @Override
  public @Nullable IPermissionGroup getGroup(@NotNull String name) {
    return this.wrapped.getGroup(name);
  }

  @Override
  public Collection<IPermissionGroup> getGroups() {
    return this.wrapped.getGroups();
  }

  @Override
  public void setGroups(@Nullable Collection<? extends IPermissionGroup> groups) {
    this.wrapped.setGroups(groups);
  }

  @Override
  public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync(@Nullable IPermissionUser permissionUser) {
    return this.wrapped.getGroupsAsync(permissionUser);
  }

  @Override
  public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
    return this.wrapped.addUserAsync(permissionUser);
  }

  @Override
  public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull String name, @NotNull String password, int potency) {
    return this.wrapped.addUserAsync(name, password, potency);
  }

  @Override
  public @NotNull ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
    return this.wrapped.updateUserAsync(permissionUser);
  }

  @Override
  public @NotNull ITask<Boolean> deleteUserAsync(@NotNull String name) {
    return this.wrapped.deleteUserAsync(name);
  }

  @Override
  public @NotNull ITask<Boolean> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
    return this.wrapped.deleteUserAsync(permissionUser);
  }

  @Override
  public @NotNull ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
    return this.wrapped.containsUserAsync(uniqueId);
  }

  @Override
  public @NotNull ITask<Boolean> containsUserAsync(@NotNull String name) {
    return this.wrapped.containsUserAsync(name);
  }

  @Override
  public @NotNull ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId) {
    return this.wrapped.getUserAsync(uniqueId);
  }

  @Override
  public @NotNull ITask<IPermissionUser> getOrCreateUserAsync(@NotNull UUID uniqueId, @NotNull String name) {
    return this.wrapped.getOrCreateUserAsync(uniqueId, name);
  }

  @Override
  public @NotNull ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name) {
    return this.wrapped.getUsersAsync(name);
  }

  @Override
  public @NotNull ITask<IPermissionUser> getFirstUserAsync(String name) {
    return this.wrapped.getFirstUserAsync(name);
  }

  @Override
  public @NotNull ITask<Collection<IPermissionUser>> getUsersAsync() {
    return this.wrapped.getUsersAsync();
  }

  @Override
  public @NotNull ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users) {
    return this.wrapped.setUsersAsync(users);
  }

  @Override
  public @NotNull ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group) {
    return this.wrapped.getUsersByGroupAsync(group);
  }

  @Override
  public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
    return this.wrapped.addGroupAsync(permissionGroup);
  }

  @Override
  public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull String role, int potency) {
    return this.wrapped.addGroupAsync(role, potency);
  }

  @Override
  public @NotNull ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
    return this.wrapped.updateGroupAsync(permissionGroup);
  }

  @Override
  public @NotNull ITask<Void> deleteGroupAsync(@NotNull String name) {
    return this.wrapped.deleteGroupAsync(name);
  }

  @Override
  public @NotNull ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
    return this.wrapped.deleteGroupAsync(permissionGroup);
  }

  @Override
  public @NotNull ITask<Boolean> containsGroupAsync(@NotNull String group) {
    return this.wrapped.containsGroupAsync(group);
  }

  @Override
  public @NotNull ITask<IPermissionGroup> getGroupAsync(@NotNull String name) {
    return this.wrapped.getGroupAsync(name);
  }

  @Override
  public @NotNull ITask<IPermissionGroup> getDefaultPermissionGroupAsync() {
    return this.wrapped.getDefaultPermissionGroupAsync();
  }

  @Override
  public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync() {
    return this.wrapped.getGroupsAsync();
  }

  @Override
  public @NotNull ITask<Void> setGroupsAsync(@Nullable Collection<? extends IPermissionGroup> groups) {
    return this.wrapped.setGroupsAsync(groups);
  }

  @Override
  public Map<UUID, IPermissionUser> getCachedPermissionUsers() {
    return this.wrapped instanceof CachedPermissionManagement ? ((CachedPermissionManagement) this.wrapped)
      .getCachedPermissionUsers() : null;
  }

  @Override
  public Map<String, IPermissionGroup> getCachedPermissionGroups() {
    return this.wrapped instanceof CachedPermissionManagement ? ((CachedPermissionManagement) this.wrapped)
      .getCachedPermissionGroups() : null;
  }

  @Override
  public IPermissionGroup modifyGroup(@NotNull String name, @NotNull Consumer<IPermissionGroup> modifier) {
    return this.wrapped.modifyGroup(name, modifier);
  }

  @Override
  public IPermissionUser modifyUser(@NotNull UUID uniqueId, @NotNull Consumer<IPermissionUser> modifier) {
    return this.wrapped.modifyUser(uniqueId, modifier);
  }

  @Override
  public List<IPermissionUser> modifyUsers(@NotNull String name, @NotNull Consumer<IPermissionUser> modifier) {
    return this.wrapped.modifyUsers(name, modifier);
  }

  @Override
  public @NotNull ITask<IPermissionGroup> modifyGroupAsync(@NotNull String name,
    @NotNull Consumer<IPermissionGroup> modifier) {
    return this.wrapped.modifyGroupAsync(name, modifier);
  }

  @Override
  public @NotNull ITask<IPermissionUser> modifyUserAsync(@NotNull UUID uniqueId,
    @NotNull Consumer<IPermissionUser> modifier) {
    return this.wrapped.modifyUserAsync(uniqueId, modifier);
  }

  @Override
  public @NotNull ITask<List<IPermissionUser>> modifyUsersAsync(@NotNull String name,
    @NotNull Consumer<IPermissionUser> modifier) {
    return this.wrapped.modifyUsersAsync(name, modifier);
  }

}
