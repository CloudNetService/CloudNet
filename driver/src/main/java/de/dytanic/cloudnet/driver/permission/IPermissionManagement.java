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

package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableSupplier;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IPermissionManagement {

  /**
   * Get the child permission management or {@code null} if there is no child permission management.
   *
   * @return the child permission management or {@code null} if there is no child permission management.
   */
  @Nullable
  IPermissionManagement getChildPermissionManagement();

  /**
   * Gets if this permission management can be overridden.
   *
   * @return if this permission management can be overridden.
   */
  boolean canBeOverwritten();

  /**
   * Gets the first permission user with the given {@code name}.
   *
   * @param name the name of the user to get.
   * @return the first permission user or {@code null} if no user with this name exists.
   */
  @Nullable PermissionUser getFirstUser(String name);

  /**
   * Initializes this permission management.
   */
  void init();

  /**
   * TODO
   */
  void close();

  /**
   * Reloads this permission management
   *
   * @return {@code true} if the reload was successful
   */
  boolean reload();

  /**
   * Gets the highest permission group of the specified user, evaluated by using the group potency.
   *
   * @param permissionUser the user
   * @return the highest permission group.
   */
  @Nullable PermissionGroup getHighestPermissionGroup(@NotNull PermissionUser permissionUser);

  /**
   * Gets the default permission group.
   *
   * @return the default permission group.
   */
  @Nullable PermissionGroup getDefaultPermissionGroup();

  /**
   * Removes all timed-out permissions and groups of the given {@code permissionUser}.
   *
   * @param permissionUser the permission user to check.
   * @return {@code true} if at least one permission was removed.
   */
  boolean testPermissionUser(@Nullable PermissionUser permissionUser);

  /**
   * Removes the timed-out permissions of the given {@code permissible}.
   *
   * @param permissible the permissible to check.
   * @return {@code true} if at least one permission was removed.
   */
  boolean testPermissible(@Nullable IPermissible permissible);

  /**
   * Adds a new permission user if it does not already exists.
   *
   * @param name     the name of the new user.
   * @param password the password of the new user.
   * @param potency  the potency of the new user.
   * @return the newly created permission user.
   */
  @NotNull PermissionUser addUser(@NotNull String name, @NotNull String password, int potency);

  /**
   * Adds a new permission group if it does not already exists.
   *
   * @param role    the case-sensitive name of the new group
   * @param potency the potency of the new group
   * @return the newly created permission group
   */
  @NotNull PermissionGroup addGroup(@NotNull String role, int potency);

  /**
   * Gets the extended groups of the specified {@code group}.
   *
   * @param permissible the permissible to get the extended groups of.
   * @return the extended groups of the given {@code permissible}.
   */
  @NotNull
  Collection<PermissionGroup> getGroupsOf(@Nullable IPermissible permissible);

  /**
   * Checks if the given {@code permissible} has the given {@code permission}.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param permission  the permission to check.
   * @return the check result.
   * @see #getPermissionResult(IPermissible, String)
   */
  default boolean hasPermission(@NotNull IPermissible permissible, @NotNull String permission) {
    return this.getPermissionResult(permissible, permission).asBoolean();
  }

  /**
   * Checks if the given {@code permissible} has the given {@code permission}.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param permission  the permission to check.
   * @return the check result.
   * @see #getPermissionResult(IPermissible, Permission)
   */
  default boolean hasPermission(@NotNull IPermissible permissible, @NotNull Permission permission) {
    return this.getPermissionResult(permissible, permission).asBoolean();
  }

  /**
   * Checks if the given {@code permissible} has the given {@code permission}.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param group       the group to get the permissions on.
   * @param permission  the permission to check.
   * @return the check result.
   * @see #getPermissionResult(IPermissible, String, Permission)
   */
  default boolean hasPermission(
    @NotNull IPermissible permissible,
    @NotNull String group,
    @NotNull Permission permission
  ) {
    return this.getPermissionResult(permissible, group, permission).asBoolean();
  }

  /**
   * Checks if the given {@code permissible} has the given {@code permission}.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param permission  the permission to check.
   * @return the check result. {@link PermissionCheckResult#DENIED} indicates that there was no allowing/forbidding
   * permission.
   */
  @NotNull
  PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String permission);

  /**
   * Checks if the given {@code permissible} has the given {@code permission}.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param permission  the permission to check.
   * @return the check result. {@link PermissionCheckResult#DENIED} indicates that there was no allowing/forbidding
   * permission.
   */
  @NotNull
  PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull Permission permission);

  /**
   * Checks if the given {@code permissible} has the given {@code permission}.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param group       the group to get the permissions on.
   * @param permission  the permission to check.
   * @return the check result. {@link PermissionCheckResult#DENIED} indicates that there was no allowing/forbidding
   * permission.
   */
  @NotNull
  PermissionCheckResult getPermissionResult(
    @NotNull IPermissible permissible,
    @NotNull String group,
    @NotNull Permission permission);

  /**
   * Checks if the given {@code permissible} has the given {@code permission}.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param groups      the groups to get the permissions on.
   * @param permission  the permission to check.
   * @return the check result. {@link PermissionCheckResult#DENIED} indicates that there was no allowing/forbidding
   * permission.
   */
  @NotNull
  PermissionCheckResult getPermissionResult(
    @NotNull IPermissible permissible,
    @NotNull Iterable<String> groups,
    @NotNull Permission permission);

  /**
   * Checks if the given {@code permissible} has the given {@code permission}.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param groups      the groups to get the permissions on.
   * @param permission  the permission to check.
   * @return the check result. {@link PermissionCheckResult#DENIED} indicates that there was no allowing/forbidding
   * permission.
   */
  @NotNull
  PermissionCheckResult getPermissionResult(
    @NotNull IPermissible permissible,
    @NotNull String[] groups,
    @NotNull Permission permission);

  /**
   * Finds the highest permission (sorted by the potency) in the given {@code permissions} array using the given {@code
   * permission} potency as the starting point.
   *
   * @param permissions the permissions to check through.
   * @param permission  the starting point for the check to run.
   * @return the highest permission in the given {@code permission} or {@code null} if there is no permission with a
   * higher potency in the given collection
   */
  @Nullable
  Permission findHighestPermission(@NotNull Collection<Permission> permissions, @NotNull Permission permission);

  /**
   * Gets all permission of the specified {@code permissible}
   *
   * @param permissible the permissible to get the permission of.
   * @return all permissions of the permissible
   */
  @NotNull
  Collection<Permission> getAllPermissions(@NotNull IPermissible permissible);

  /**
   * Gets all permission of the specified {@code permissible} on the given {@code group}.
   *
   * @param permissible the permissible to get the permission of.
   * @param group       the group to get the permission on or {@code null} if no specific group should be used.
   * @return all permissions of the permissible on the specified group if provided
   */
  @NotNull
  Collection<Permission> getAllPermissions(@NotNull IPermissible permissible, @Nullable String group);

  /**
   * Adds a new user to the database.
   *
   * @param permissionUser the user to be added
   * @return the new user
   */
  @NotNull PermissionUser addUser(@NotNull PermissionUser permissionUser);

  /**
   * Updates an already existing user in the database.
   *
   * @param permissionUser the user to be updated
   */
  void updateUser(@NotNull PermissionUser permissionUser);

  /**
   * Deletes all users in the database matching the given name. This method is case-sensitive.
   *
   * @param name the name of the users to be deleted
   */
  boolean deleteUser(@NotNull String name);

  /**
   * Deletes one user with the uniqueId of the given user.
   *
   * @param permissionUser the user to be deleted
   */
  boolean deleteUser(@NotNull PermissionUser permissionUser);

  /**
   * Checks if a user with the given uniqueId is stored in the database.
   *
   * @param uniqueId the uniqueId of the user
   * @return {@code true} if there is a user with that uniqueId, {@code false} otherwise
   */
  boolean containsUser(@NotNull UUID uniqueId);

  /**
   * Checks if at least one user with the given name is stored in the database. This method is case-sensitive.
   *
   * @param name the name of the user
   * @return {@code true} if there is a user with that name, {@code false} otherwise
   */
  boolean containsUser(@NotNull String name);

  /**
   * Gets a user with the given uniqueId out of the database.
   *
   * @param uniqueId the uniqueId of the user
   * @return the {@link PermissionUser} from the database or {@code null} if there is no user with that uniqueId stored
   */
  @Nullable PermissionUser getUser(@NotNull UUID uniqueId);

  /**
   * Gets a user with the given uniqueId out of the database or creates a new one if the database contains no such
   * entry.
   *
   * @param uniqueId the uniqueId of the user
   * @param name     the name of the permission user
   * @return the {@link PermissionUser} from the database or a newly created one.
   */
  @NotNull PermissionUser getOrCreateUser(@NotNull UUID uniqueId, @NotNull String name);

  /**
   * Gets a list of all users with the given name out of the database. This can only return null when the connection to
   * the database (or when it is executed in a Wrapper instance the connection to the cloud) times out.
   *
   * @param name the name of the users
   * @return a list of all {@link PermissionUser}s stored in the database or an empty list if there is no user with that
   * name stored.
   */
  @NotNull List<PermissionUser> getUsers(@NotNull String name);

  /**
   * Gets a list of all users stored in the database. This can only return null when the connection to the database (or
   * when it is executed in a Wrapper instance the connection to the cloud) times out.
   * <p>
   * This method shouldn't be used when there are many users stored in the database, because that takes a lot of
   * memory.
   *
   * @return a list of all {@link PermissionUser}s stored in the database or an empty list if there is no user with that
   * name stored.
   */
  @NotNull Collection<PermissionUser> getUsers();

  /**
   * Gets a list of all users stored in the database with the given group. This can only return null when the connection
   * to the database (or when it is executed in a Wrapper instance the connection to the cloud) times out.
   * <p>
   * This method shouldn't be used when there are many users with that group stored in the database, because that takes
   * a lot of memory.
   *
   * @return a list of all {@link PermissionUser}s stored in the database or an empty list if there is no user with that
   * name stored.
   */
  @NotNull Collection<PermissionUser> getUsersByGroup(@NotNull String group);

  /**
   * Adds a new permission group to the list of groups. If a group with that name already exists, it will be deleted and
   * created again.
   *
   * @param permissionGroup the {@link PermissionGroup} to be added
   * @return the new group
   */
  @NotNull PermissionGroup addGroup(@NotNull PermissionGroup permissionGroup);

  /**
   * Updates a permission group in the list of groups. If a group with that name doesn't exist, it will be created.
   *
   * @param permissionGroup the {@link PermissionGroup} to be updated
   */
  void updateGroup(@NotNull PermissionGroup permissionGroup);

  /**
   * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
   *
   * @param name the case-sensitive name of the group
   */
  boolean deleteGroup(@NotNull String name);

  /**
   * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
   *
   * @param permissionGroup the {@link PermissionGroup} to be deleted
   */
  boolean deleteGroup(@NotNull PermissionGroup permissionGroup);

  /**
   * Checks if a specific group exists.
   *
   * @param group the case-sensitive name of the group
   * @return {@code true} if the group exists, {@code false} otherwise
   */
  boolean containsGroup(@NotNull String group);

  /**
   * Gets a specific group by its name.
   *
   * @param name the case-sensitive name of the group
   * @return the {@link PermissionUser} if it exists, {@code null} otherwise
   */
  @Nullable PermissionGroup getGroup(@NotNull String name);

  /**
   * Gets the list of all groups in the Cloud.
   *
   * @return a list of {@link PermissionGroup}s registered in the cloud or an empty list if there is no group registered
   */
  @NotNull Collection<PermissionGroup> getGroups();

  /**
   * Clears all groups in the Cloud and sets given groups.
   *
   * @param groups the new groups
   */
  void setGroups(@Nullable Collection<? extends PermissionGroup> groups);

  /**
   * Gets the permission group with the given name by using {@link #getGroup(String)} and, if not null, puts it into the
   * consumer, after that, the group will be updated by using {@link #updateGroup(PermissionGroup)}.
   *
   * @param name     the name of the group
   * @param modifier the Consumer to modify the user
   * @return the modified group
   */
  @Nullable PermissionGroup modifyGroup(@NotNull String name, @NotNull Consumer<PermissionGroup> modifier);

  /**
   * Gets the permission user with the given uniqueId by using {@link #getUser(UUID)} and, if not null, puts them into
   * the consumer, after that, the user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param uniqueId the uniqueId of the user
   * @param modifier the Consumer to modify the user
   * @return the modified user
   */
  @Nullable PermissionUser modifyUser(@NotNull UUID uniqueId, @NotNull Consumer<PermissionUser> modifier);

  /**
   * Gets every user matching the given name by using {@link #getUsers(String)} and puts them into the consumer, after
   * that, every user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param name     the name of the users
   * @param modifier the Consumer to modify the available users
   * @return a list of all modified users
   */
  @NotNull List<PermissionUser> modifyUsers(@NotNull String name, @NotNull Consumer<PermissionUser> modifier);

  /**
   * Retrieves every permission group object of the specified {@code permissionUser}.
   *
   * @param permissionUser the user to get the groups of
   * @return a collection of all group objects the user is in
   */
  default @NotNull ITask<Collection<PermissionGroup>> getGroupsOfAsync(@Nullable PermissionUser permissionUser) {
    return CompletableTask.supplyAsync(() -> this.getGroupsOf(permissionUser));
  }

  /**
   * Adds a new user to the database.
   *
   * @param permissionUser the user to be added
   * @return the created permission user
   */
  default @NotNull ITask<PermissionUser> addUserAsync(@NotNull PermissionUser permissionUser) {
    return CompletableTask.supplyAsync(() -> this.addUser(permissionUser));
  }

  /**
   * Adds a new user to the database.
   *
   * @param name     the name of the new user
   * @param password the password of the new user
   * @param potency  the potency of the new user
   * @return the created permission user
   */
  default @NotNull ITask<PermissionUser> addUserAsync(@NotNull String name, @NotNull String password, int potency) {
    return CompletableTask.supplyAsync(() -> this.addUser(name, password, potency));
  }

  /**
   * Updates an already existing user in the database.
   *
   * @param permissionUser the user to be updated
   * @return a task completed when the operation was executed
   */
  default @NotNull ITask<Void> updateUserAsync(@NotNull PermissionUser permissionUser) {
    return CompletableTask.supplyAsync(() -> this.updateUser(permissionUser));
  }

  /**
   * Deletes all users in the database matching the given name. This method is case-sensitive.
   *
   * @param name the name of the users to be deleted
   * @return if the operation was successful
   */
  default @NotNull ITask<Boolean> deleteUserAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.deleteUser(name));
  }

  /**
   * Deletes one user with the uniqueId of the given user.
   *
   * @param permissionUser the user to be deleted
   * @return if the operation was successful
   */
  default @NotNull ITask<Boolean> deleteUserAsync(@NotNull PermissionUser permissionUser) {
    return CompletableTask.supplyAsync(() -> this.deleteUser(permissionUser));
  }

  /**
   * Checks if a user with the given uniqueId is stored in the database.
   *
   * @param uniqueId the uniqueId of the user
   * @return {@code true} if there is a user with that uniqueId, {@code false} otherwise
   */
  default @NotNull ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
    return CompletableTask.supplyAsync(() -> this.containsUser(uniqueId));
  }

  /**
   * Checks if at least one user with the given name is stored in the database. This method is case-sensitive.
   *
   * @param name the name of the user
   * @return {@code true} if there is a user with that name, {@code false} otherwise
   */
  default @NotNull ITask<Boolean> containsUserAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.containsUser(name));
  }

  /**
   * Gets a user with the given uniqueId out of the database.
   *
   * @param uniqueId the uniqueId of the user
   * @return the {@link PermissionUser} from the database or {@code null} if there is no user with that uniqueId stored
   */
  default @NotNull ITask<PermissionUser> getUserAsync(@NotNull UUID uniqueId) {
    return CompletableTask.supplyAsync(() -> this.getUser(uniqueId));
  }

  /**
   * Gets a user with the given uniqueId out of the database or creates a new one if the database contains no such
   * entry.
   *
   * @param uniqueId the uniqueId of the user
   * @param name     the name of the permission user
   * @return the {@link PermissionUser} from the database or a newly created one.
   */
  default @NotNull ITask<PermissionUser> getOrCreateUserAsync(@NotNull UUID uniqueId, @NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.getOrCreateUser(uniqueId, name));
  }

  /**
   * Gets a list of all users with the given name out of the database. This can only return null when the connection to
   * the database (or when it is executed in a Wrapper instance the connection to the cloud) times out.
   *
   * @param name the name of the users
   * @return a list of all {@link PermissionUser}s stored in the database or an empty list if there is no user with that
   * name stored.
   */
  default @NotNull ITask<List<PermissionUser>> getUsersAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.getUsers(name));
  }

  /**
   * Gets the first user with the specified {@code name}.
   *
   * @param name the name of the user to get.
   * @return the {@link PermissionUser} from the database or {@code null} if there is no user with that name stored
   */
  default @NotNull ITask<PermissionUser> getFirstUserAsync(String name) {
    return CompletableTask.supplyAsync(() -> this.getFirstUser(name));
  }

  /**
   * Gets a list of all users stored in the database. This can only return null when the connection to the database (or
   * when it is executed in a Wrapper instance the connection to the cloud) times out.
   * <p>
   * This method shouldn't be used when there are many users stored in the database, because that takes a lot of
   * memory.
   *
   * @return a list of all {@link PermissionUser}s stored in the database or an empty list if there is no user with that
   * name stored.
   */
  default @NotNull ITask<Collection<PermissionUser>> getUsersAsync() {
    return CompletableTask.supplyAsync((ThrowableSupplier<Collection<PermissionUser>, Throwable>) this::getUsers);
  }

  /**
   * Gets a list of all users stored in the database with the given group. This can only return null when the connection
   * to the database (or when it is executed in a Wrapper instance the connection to the cloud) times out.
   * <p>
   * This method shouldn't be used when there are many users with that group stored in the database, because that takes
   * a lot of memory.
   *
   * @param group the name of the group to get the users of.
   * @return a list of all {@link PermissionUser}s stored in the database or an empty list if there is no user with that
   * name stored.
   */
  default @NotNull ITask<Collection<PermissionUser>> getUsersByGroupAsync(@NotNull String group) {
    return CompletableTask.supplyAsync(() -> this.getUsersByGroup(group));
  }

  /**
   * Adds a new permission group to the list of groups. If a group with that name already exists, it will be deleted and
   * created again.
   *
   * @param permissionGroup the {@link PermissionGroup} to be added
   * @return the created permission group.
   */
  default @NotNull ITask<PermissionGroup> addGroupAsync(@NotNull PermissionGroup permissionGroup) {
    return CompletableTask.supplyAsync(() -> this.addGroup(permissionGroup));
  }

  /**
   * Adds a new permission group to the list of groups. If a group with that name already exists, it will be deleted and
   * created again.
   *
   * @param role    the name of the group to create.
   * @param potency the potency of the new group.
   * @return the created permission group.
   */
  default @NotNull ITask<PermissionGroup> addGroupAsync(@NotNull String role, int potency) {
    return CompletableTask.supplyAsync(() -> this.addGroup(role, potency));
  }

  /**
   * Updates a permission group in the list of groups. If a group with that name doesn't exist, it will be created.
   *
   * @param permissionGroup the {@link PermissionGroup} to be updated
   * @return a task completed when the operation was executed
   */
  default @NotNull ITask<Void> updateGroupAsync(@NotNull PermissionGroup permissionGroup) {
    return CompletableTask.supplyAsync(() -> this.updateGroup(permissionGroup));
  }

  /**
   * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
   *
   * @param name the case-sensitive name of the group
   * @return a task completed when the operation was executed
   */
  default @NotNull ITask<Boolean> deleteGroupAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.deleteGroup(name));
  }

  /**
   * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
   *
   * @param permissionGroup the {@link PermissionGroup} to be deleted
   * @return a task completed when the operation was executed
   */
  default @NotNull ITask<Boolean> deleteGroupAsync(@NotNull PermissionGroup permissionGroup) {
    return CompletableTask.supplyAsync(() -> this.deleteGroup(permissionGroup));
  }

  /**
   * Checks if a specific group exists.
   *
   * @param group the case-sensitive name of the group
   * @return {@code true} if the group exists, {@code false} otherwise
   */
  default @NotNull ITask<Boolean> containsGroupAsync(@NotNull String group) {
    return CompletableTask.supplyAsync(() -> this.containsGroup(group));
  }

  /**
   * Gets a specific group by its name.
   *
   * @param name the case-sensitive name of the group
   * @return the {@link PermissionUser} if it exists, {@code null} otherwise
   */
  default @NotNull ITask<PermissionGroup> getGroupAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.getGroup(name));
  }

  /**
   * Gets the default permission group.
   *
   * @return the default permission group.
   */
  default @NotNull ITask<PermissionGroup> getDefaultPermissionGroupAsync() {
    return CompletableTask.supplyAsync(this::getDefaultPermissionGroup);
  }

  /**
   * Gets the list of all groups in the Cloud.
   *
   * @return a list of {@link PermissionGroup}s registered in the cloud or an empty list if there is no group registered
   */
  default @NotNull ITask<Collection<PermissionGroup>> getGroupsAsync() {
    return CompletableTask.supplyAsync((ThrowableSupplier<Collection<PermissionGroup>, Throwable>) this::getGroups);
  }

  /**
   * Clears all groups in the Cloud and sets given groups.
   *
   * @param groups the new groups
   */
  default @NotNull ITask<Void> setGroupsAsync(@Nullable Collection<? extends PermissionGroup> groups) {
    return CompletableTask.supplyAsync(() -> this.setGroups(groups));
  }

  /**
   * Gets the permission group with the given name by using {@link #getGroupAsync(String)} and, if not null, puts it
   * into the consumer, after that, the group will be updated by using {@link #updateGroup(PermissionGroup)}.
   *
   * @param name     the name of the group
   * @param modifier the Consumer to modify the user
   * @return the modified user
   */
  default @NotNull ITask<PermissionGroup> modifyGroupAsync(
    @NotNull String name,
    @NotNull Consumer<PermissionGroup> modifier
  ) {
    return CompletableTask.supplyAsync(() -> this.modifyGroup(name, modifier));
  }

  /**
   * Gets the permission user with the given uniqueId by using {@link #getUserAsync(UUID)} and, if not null, puts them
   * into the consumer, after that, the user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param uniqueId the uniqueId of the user
   * @param modifier the Consumer to modify the user
   * @return the modified user
   */
  default @NotNull ITask<PermissionUser> modifyUserAsync(
    @NotNull UUID uniqueId,
    @NotNull Consumer<PermissionUser> modifier
  ) {
    return CompletableTask.supplyAsync(() -> this.modifyUser(uniqueId, modifier));
  }

  /**
   * Gets every user matching the given name by using {@link #getUsersAsync(String)} and puts them into the consumer,
   * after that, every user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param name     the name of the users
   * @param modifier the Consumer to modify the available users
   * @return a list of all modified users
   */
  default @NotNull ITask<List<PermissionUser>> modifyUsersAsync(
    @NotNull String name,
    @NotNull Consumer<PermissionUser> modifier
  ) {
    return CompletableTask.supplyAsync(() -> this.modifyUsers(name, modifier));
  }
}
