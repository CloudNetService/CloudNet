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

package eu.cloudnetservice.cloudnet.driver.permission;

import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.common.function.ThrowableSupplier;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@RPCValidation
@SuppressWarnings("unused")
public interface PermissionManagement {

  /**
   * Get the child permission management or null if there is no child permission management.
   *
   * @return the child permission management or null if there is no child permission management.
   */
  @Nullable PermissionManagement childPermissionManagement();

  /**
   * Gets if this permission management can be overridden.
   *
   * @return if this permission management can be overridden, false otherwise.
   */
  boolean canBeOverwritten();

  /**
   * Gets the first permission user with the given name.
   *
   * @param name the name of the user to get.
   * @return the first permission user or null if no user with this name exists.
   */
  @Nullable PermissionUser firstUser(String name);

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
   * @return true if the reload was successful, false otherwise.
   */
  boolean reload();

  /**
   * Gets the highest permission group of the specified user, evaluated by using the group potency.
   *
   * @param permissionUser the user
   * @return the highest permission group.
   */
  @Nullable PermissionGroup highestPermissionGroup(@NonNull PermissionUser permissionUser);

  /**
   * Gets the default permission group.
   *
   * @return the default permission group.
   */
  @Nullable PermissionGroup defaultPermissionGroup();

  /**
   * Removes all timed-out permissions and groups of the given permission user.
   *
   * @param permissionUser the permission user to check.
   * @return true if at least one permission was removed, false otherwise.
   */
  boolean testPermissionUser(@Nullable PermissionUser permissionUser);

  /**
   * Removes the timed-out permissions of the given permissible.
   *
   * @param permissible the permissible to check.
   * @return true if at least one permission was removed, false otherwise.
   */
  boolean testPermissible(@Nullable Permissible permissible);

  /**
   * Adds a new permission user if it does not already exist.
   *
   * @param name     the name of the new user.
   * @param password the password of the new user.
   * @param potency  the potency of the new user.
   * @return the newly created permission user.
   */
  @NonNull PermissionUser addUser(@NonNull String name, @NonNull String password, int potency);

  /**
   * Adds a new permission group if it does not already exists.
   *
   * @param role    the case-sensitive name of the new group
   * @param potency the potency of the new group
   * @return the newly created permission group
   */
  @NonNull PermissionGroup addGroup(@NonNull String role, int potency);

  /**
   * Gets the extended groups of the specified group.
   *
   * @param permissible the permissible to get the extended groups of.
   * @return the extended groups of the given permissible.
   */
  @NonNull Collection<PermissionGroup> groupsOf(@Nullable Permissible permissible);

  /**
   * Checks if the given permissible has the given permission.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param permission  the permission to check.
   * @return the check result.
   * @see #permissionResult(Permissible, Permission)
   */
  default boolean hasPermission(@NonNull Permissible permissible, @NonNull Permission permission) {
    return this.permissionResult(permissible, permission).asBoolean();
  }

  /**
   * Checks if the given permissible has the given permission.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param group       the group to get the permissions on.
   * @param permission  the permission to check.
   * @return the check result.
   * @see #groupPermissionResult(Permissible, String, Permission)
   */
  default boolean hasGroupPermission(
    @NonNull Permissible permissible,
    @NonNull String group,
    @NonNull Permission permission
  ) {
    return this.groupPermissionResult(permissible, group, permission).asBoolean();
  }

  /**
   * Checks if the given permissible has the given permission.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param permission  the permission to check.
   * @return the check result. {@link PermissionCheckResult#DENIED} indicates that there was no allowing/forbidding
   * permission.
   */
  @NonNull PermissionCheckResult permissionResult(@NonNull Permissible permissible, @NonNull Permission permission);

  /**
   * Checks if the given permissible has the given permission.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param group       the group to get the permissions on.
   * @param permission  the permission to check.
   * @return the check result. {@link PermissionCheckResult#DENIED} indicates that there was no allowing/forbidding
   * permission.
   */
  @NonNull PermissionCheckResult groupPermissionResult(
    @NonNull Permissible permissible,
    @NonNull String group,
    @NonNull Permission permission);

  /**
   * Checks if the given permissible has the given permission.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param groups      the groups to get the permissions on.
   * @param permission  the permission to check.
   * @return the check result. {@link PermissionCheckResult#DENIED} indicates that there was no allowing/forbidding
   * permission.
   */
  @NonNull PermissionCheckResult groupsPermissionResult(
    @NonNull Permissible permissible,
    @NonNull String[] groups,
    @NonNull Permission permission);

  /**
   * Finds the highest permission (sorted by the potency) in the given permissions array using the given permission
   * potency as the starting point.
   *
   * @param permissions the permissions to check through.
   * @param permission  the starting point for the check to run.
   * @return the highest permission in the given permission or null if there is no permission with a higher potency in
   * the given collection
   */
  @Nullable Permission findHighestPermission(@NonNull Collection<Permission> permissions,
    @NonNull Permission permission);

  /**
   * Gets all permission of the specified permissible
   *
   * @param permissible the permissible to get the permission of.
   * @return all permissions of the permissible
   */
  @NonNull Collection<Permission> allPermissions(@NonNull Permissible permissible);

  /**
   * Gets all permission of the specified permissible on the given group.
   *
   * @param permissible the permissible to get the permission of.
   * @param group       the group to get the permission on or null if no specific group should be used.
   * @return all permissions of the permissible on the specified group if provided
   */
  @NonNull Collection<Permission> allGroupPermissions(@NonNull Permissible permissible, @Nullable String group);

  /**
   * Adds a new user to the database.
   *
   * @param permissionUser the user to be added
   * @return the new user
   */
  @NonNull PermissionUser addPermissionUser(@NonNull PermissionUser permissionUser);

  /**
   * Updates an already existing user in the database.
   *
   * @param permissionUser the user to be updated
   */
  void updateUser(@NonNull PermissionUser permissionUser);

  /**
   * Deletes all users in the database matching the given name. This method is case-sensitive.
   *
   * @param name the name of the users to be deleted
   */
  boolean deleteUser(@NonNull String name);

  /**
   * Deletes one user with the uniqueId of the given user.
   *
   * @param permissionUser the user to be deleted
   */
  boolean deletePermissionUser(@NonNull PermissionUser permissionUser);

  /**
   * Checks if a user with the given uniqueId is stored in the database.
   *
   * @param uniqueId the uniqueId of the user
   * @return true if there is a user with that uniqueId, false otherwise
   */
  boolean containsUser(@NonNull UUID uniqueId);

  /**
   * Checks if at least one user with the given name is stored in the database. This method is case-sensitive.
   *
   * @param name the name of the user
   * @return true if there is a user with that name, false otherwise
   */
  boolean containsOneUser(@NonNull String name);

  /**
   * Gets a user with the given uniqueId out of the database.
   *
   * @param uniqueId the uniqueId of the user
   * @return the {@link PermissionUser} from the database or null if there is no user with that uniqueId stored
   */
  @Nullable PermissionUser user(@NonNull UUID uniqueId);

  /**
   * Gets a user with the given uniqueId out of the database or creates a new one if the database contains no such
   * entry.
   *
   * @param uniqueId the uniqueId of the user
   * @param name     the name of the permission user
   * @return the {@link PermissionUser} from the database or a newly created one.
   */
  @NonNull PermissionUser getOrCreateUser(@NonNull UUID uniqueId, @NonNull String name);

  /**
   * Gets a list of all users with the given name out of the database. This can only return null when the connection to
   * the database (or when it is executed in a Wrapper instance the connection to the cloud) times out.
   *
   * @param name the name of the users
   * @return a list of all {@link PermissionUser}s stored in the database or an empty list if there is no user with that
   * name stored.
   */
  @NonNull List<PermissionUser> usersByName(@NonNull String name);

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
  @NonNull Collection<PermissionUser> users();

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
  @NonNull Collection<PermissionUser> usersByGroup(@NonNull String group);

  /**
   * Adds a new permission group to the list of groups. If a group with that name already exists, it will be deleted and
   * created again.
   *
   * @param permissionGroup the {@link PermissionGroup} to be added
   * @return the new group
   */
  @NonNull PermissionGroup addPermissionGroup(@NonNull PermissionGroup permissionGroup);

  /**
   * Updates a permission group in the list of groups. If a group with that name doesn't exist, it will be created.
   *
   * @param permissionGroup the {@link PermissionGroup} to be updated
   */
  void updateGroup(@NonNull PermissionGroup permissionGroup);

  /**
   * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
   *
   * @param name the case-sensitive name of the group
   */
  boolean deleteGroup(@NonNull String name);

  /**
   * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
   *
   * @param permissionGroup the {@link PermissionGroup} to be deleted
   */
  boolean deletePermissionGroup(@NonNull PermissionGroup permissionGroup);

  /**
   * Checks if a specific group exists.
   *
   * @param group the case-sensitive name of the group
   * @return true if the group exists, false otherwise
   */
  boolean containsGroup(@NonNull String group);

  /**
   * Gets a specific group by its name.
   *
   * @param name the case-sensitive name of the group
   * @return the {@link PermissionUser} if it exists, null otherwise
   */
  @Nullable PermissionGroup group(@NonNull String name);

  /**
   * Gets the list of all groups in the Cloud.
   *
   * @return a list of {@link PermissionGroup}s registered in the cloud or an empty list if there is no group registered
   */
  @NonNull Collection<PermissionGroup> groups();

  /**
   * Clears all groups in the Cloud and sets given groups.
   *
   * @param groups the new groups
   */
  void groups(@Nullable Collection<? extends PermissionGroup> groups);

  /**
   * Gets the permission group with the given name by using {@link #group(String)} and, if not null, puts it into the
   * consumer, after that, the group will be updated by using {@link #updateGroup(PermissionGroup)}.
   *
   * @param name     the name of the group
   * @param modifier the Consumer to modify the user
   * @return the modified group
   */
  @Nullable PermissionGroup modifyGroup(
    @NonNull String name,
    @NonNull BiConsumer<PermissionGroup, PermissionGroup.Builder> modifier);

  /**
   * Gets the permission user with the given uniqueId by using {@link #user(UUID)} and, if not null, puts them into the
   * consumer, after that, the user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param uniqueId the uniqueId of the user
   * @param modifier the Consumer to modify the user
   * @return the modified user
   */
  @Nullable PermissionUser modifyUser(
    @NonNull UUID uniqueId,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier);

  /**
   * Gets every user matching the given name by using {@link #usersByName(String)} and puts them into the consumer,
   * after that, every user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param name     the name of the users
   * @param modifier the Consumer to modify the available users
   * @return a list of all modified users
   */
  @NonNull List<PermissionUser> modifyUsers(
    @NonNull String name,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier);

  /**
   * Sends a command line to the node the method is called on or the node the wrapper is connected to. The command line
   * is executed and all resulting messages are collected in the collection of strings.
   *
   * @param user        the user to execute the command line with
   * @param commandLine the command line to execute on the node
   * @return all messages regarding the command execution
   */
  @NonNull Collection<String> sendCommandLine(@NonNull PermissionUser user, @NonNull String commandLine);

  /**
   * Retrieves every permission group object of the specified permissionUser.
   *
   * @param permissionUser the user to get the groups of
   * @return a collection of all group objects the user is in
   */
  default @NonNull Task<Collection<PermissionGroup>> groupsOfAsync(@Nullable PermissionUser permissionUser) {
    return Task.supply(() -> this.groupsOf(permissionUser));
  }

  /**
   * Adds a new user to the database.
   *
   * @param permissionUser the user to be added
   * @return the created permission user
   */
  default @NonNull Task<PermissionUser> addPermissionUserAsync(@NonNull PermissionUser permissionUser) {
    return Task.supply(() -> this.addPermissionUser(permissionUser));
  }

  /**
   * Adds a new user to the database.
   *
   * @param name     the name of the new user
   * @param password the password of the new user
   * @param potency  the potency of the new user
   * @return the created permission user
   */
  default @NonNull Task<PermissionUser> addUserAsync(@NonNull String name, @NonNull String password, int potency) {
    return Task.supply(() -> this.addUser(name, password, potency));
  }

  /**
   * Updates an already existing user in the database.
   *
   * @param permissionUser the user to be updated
   * @return a task completed when the operation was executed
   */
  default @NonNull Task<Void> updateUserAsync(@NonNull PermissionUser permissionUser) {
    return Task.supply(() -> this.updateUser(permissionUser));
  }

  /**
   * Deletes all users in the database matching the given name. This method is case-sensitive.
   *
   * @param name the name of the users to be deleted
   * @return if the operation was successful
   */
  default @NonNull Task<Boolean> deleteUserAsync(@NonNull String name) {
    return Task.supply(() -> this.deleteUser(name));
  }

  /**
   * Deletes one user with the uniqueId of the given user.
   *
   * @param permissionUser the user to be deleted
   * @return if the operation was successful
   */
  default @NonNull Task<Boolean> deletePermissionUserAsync(@NonNull PermissionUser permissionUser) {
    return Task.supply(() -> this.deletePermissionUser(permissionUser));
  }

  /**
   * Checks if a user with the given uniqueId is stored in the database.
   *
   * @param uniqueId the uniqueId of the user
   * @return true if there is a user with that uniqueId, false otherwise
   */
  default @NonNull Task<Boolean> containsUserAsync(@NonNull UUID uniqueId) {
    return Task.supply(() -> this.containsUser(uniqueId));
  }

  /**
   * Checks if at least one user with the given name is stored in the database. This method is case-sensitive.
   *
   * @param name the name of the user
   * @return true if there is a user with that name, false otherwise
   */
  default @NonNull Task<Boolean> containsOneUserAsync(@NonNull String name) {
    return Task.supply(() -> this.containsOneUser(name));
  }

  /**
   * Gets a user with the given uniqueId out of the database.
   *
   * @param uniqueId the uniqueId of the user
   * @return the {@link PermissionUser} from the database or null if there is no user with that uniqueId stored
   */
  default @NonNull Task<PermissionUser> userAsync(@NonNull UUID uniqueId) {
    return Task.supply(() -> this.user(uniqueId));
  }

  /**
   * Gets a user with the given uniqueId out of the database or creates a new one if the database contains no such
   * entry.
   *
   * @param uniqueId the uniqueId of the user
   * @param name     the name of the permission user
   * @return the {@link PermissionUser} from the database or a newly created one.
   */
  default @NonNull Task<PermissionUser> getOrCreateUserAsync(@NonNull UUID uniqueId, @NonNull String name) {
    return Task.supply(() -> this.getOrCreateUser(uniqueId, name));
  }

  /**
   * Gets a list of all users with the given name out of the database. This can only return null when the connection to
   * the database (or when it is executed in a Wrapper instance the connection to the cloud) times out.
   *
   * @param name the name of the users
   * @return a list of all {@link PermissionUser}s stored in the database or an empty list if there is no user with that
   * name stored.
   */
  default @NonNull Task<List<PermissionUser>> usersByNameAsync(@NonNull String name) {
    return Task.supply(() -> this.usersByName(name));
  }

  /**
   * Gets the first user with the specified name.
   *
   * @param name the name of the user to get.
   * @return the {@link PermissionUser} from the database or null if there is no user with that name stored
   */
  default @NonNull Task<PermissionUser> firstUserAsync(String name) {
    return Task.supply(() -> this.firstUser(name));
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
  default @NonNull Task<Collection<PermissionUser>> usersAsync() {
    return Task.supply(this::users);
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
  default @NonNull Task<Collection<PermissionUser>> usersByGroupAsync(@NonNull String group) {
    return Task.supply(() -> this.usersByGroup(group));
  }

  /**
   * Adds a new permission group to the list of groups. If a group with that name already exists, it will be deleted and
   * created again.
   *
   * @param permissionGroup the {@link PermissionGroup} to be added
   * @return the created permission group.
   */
  default @NonNull Task<PermissionGroup> addPermissionGroupAsync(@NonNull PermissionGroup permissionGroup) {
    return Task.supply(() -> this.addPermissionGroup(permissionGroup));
  }

  /**
   * Adds a new permission group to the list of groups. If a group with that name already exists, it will be deleted and
   * created again.
   *
   * @param role    the name of the group to create.
   * @param potency the potency of the new group.
   * @return the created permission group.
   */
  default @NonNull Task<PermissionGroup> addGroupAsync(@NonNull String role, int potency) {
    return Task.supply(() -> this.addGroup(role, potency));
  }

  /**
   * Updates a permission group in the list of groups. If a group with that name doesn't exist, it will be created.
   *
   * @param permissionGroup the {@link PermissionGroup} to be updated
   * @return a task completed when the operation was executed
   */
  default @NonNull Task<Void> updateGroupAsync(@NonNull PermissionGroup permissionGroup) {
    return Task.supply(() -> this.updateGroup(permissionGroup));
  }

  /**
   * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
   *
   * @param name the case-sensitive name of the group
   * @return a task completed when the operation was executed
   */
  default @NonNull Task<Boolean> deleteGroupAsync(@NonNull String name) {
    return Task.supply(() -> this.deleteGroup(name));
  }

  /**
   * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
   *
   * @param permissionGroup the {@link PermissionGroup} to be deleted
   * @return a task completed when the operation was executed
   */
  default @NonNull Task<Boolean> deletePermissionGroupAsync(@NonNull PermissionGroup permissionGroup) {
    return Task.supply(() -> this.deletePermissionGroup(permissionGroup));
  }

  /**
   * Checks if a specific group exists.
   *
   * @param group the case-sensitive name of the group
   * @return true if the group exists, false otherwise
   */
  default @NonNull Task<Boolean> containsGroupAsync(@NonNull String group) {
    return Task.supply(() -> this.containsGroup(group));
  }

  /**
   * Gets a specific group by its name.
   *
   * @param name the case-sensitive name of the group
   * @return the {@link PermissionUser} if it exists, null otherwise
   */
  default @NonNull Task<PermissionGroup> groupAsync(@NonNull String name) {
    return Task.supply(() -> this.group(name));
  }

  /**
   * Gets the default permission group.
   *
   * @return the default permission group.
   */
  default @NonNull Task<PermissionGroup> defaultPermissionGroupAsync() {
    return Task.supply(this::defaultPermissionGroup);
  }

  /**
   * Gets the list of all groups in the Cloud.
   *
   * @return a list of {@link PermissionGroup}s registered in the cloud or an empty list if there is no group registered
   */
  default @NonNull Task<Collection<PermissionGroup>> groupsAsync() {
    return Task.supply((ThrowableSupplier<Collection<PermissionGroup>, Throwable>) this::groups);
  }

  /**
   * Clears all groups in the Cloud and sets given groups.
   *
   * @param groups the new groups
   */
  default @NonNull Task<Void> groupsAsync(@Nullable Collection<? extends PermissionGroup> groups) {
    return Task.supply(() -> this.groups(groups));
  }

  /**
   * Gets the permission group with the given name by using {@link #groupAsync(String)} and, if not null, puts it into
   * the consumer, after that, the group will be updated by using {@link #updateGroup(PermissionGroup)}.
   *
   * @param name     the name of the group
   * @param modifier the Consumer to modify the user
   * @return the modified user
   */
  default @NonNull Task<PermissionGroup> modifyGroupAsync(
    @NonNull String name,
    @NonNull BiConsumer<PermissionGroup, PermissionGroup.Builder> modifier
  ) {
    return Task.supply(() -> this.modifyGroup(name, modifier));
  }

  /**
   * Gets the permission user with the given uniqueId by using {@link #userAsync(UUID)} and, if not null, puts them into
   * the consumer, after that, the user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param uniqueId the uniqueId of the user
   * @param modifier the Consumer to modify the user
   * @return the modified user
   */
  default @NonNull Task<PermissionUser> modifyUserAsync(
    @NonNull UUID uniqueId,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier
  ) {
    return Task.supply(() -> this.modifyUser(uniqueId, modifier));
  }

  /**
   * Gets every user matching the given name by using {@link #usersByNameAsync(String)} and puts them into the consumer,
   * after that, every user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param name     the name of the users
   * @param modifier the Consumer to modify the available users
   * @return a list of all modified users
   */
  default @NonNull Task<List<PermissionUser>> modifyUsersAsync(
    @NonNull String name,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier
  ) {
    return Task.supply(() -> this.modifyUsers(name, modifier));
  }

  /**
   * Sends a command line to the node the method is called on or the node the wrapper is connected to. The command line
   * is executed and all resulting messages are collected in the collection of strings.
   *
   * @param user        the user to execute the command line with
   * @param commandLine the command line to execute on the node
   * @return all messages regarding the command execution
   */
  default @NonNull Task<Collection<String>> sendCommandLineAsync(
    @NonNull PermissionUser user,
    @NonNull String commandLine
  ) {
    return Task.supply(() -> this.sendCommandLine(user, commandLine));
  }
}
