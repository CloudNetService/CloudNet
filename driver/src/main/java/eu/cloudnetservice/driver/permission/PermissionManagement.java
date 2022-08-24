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

package eu.cloudnetservice.driver.permission;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.function.ThrowableSupplier;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCValidation;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main api point for accessing and managing permissions, permission users and permission groups. This management
 * contains some helpful methods to check a permissible for a certain permission and removing outdated permissions.
 * Updating the permissible is handled by the permission management too.
 *
 * @since 4.0
 */
@RPCValidation
public interface PermissionManagement {

  /**
   * Gets the child permission management of this permission management. The child is null if there is no child
   * permission management.
   *
   * @return the child management, null if there is no child.
   */
  @Nullable PermissionManagement childPermissionManagement();

  /**
   * Gets if this permission management can be overridden. If this is the case the permission management is replaceable
   * using {@link CloudNetDriver#permissionManagement(PermissionManagement)}.
   *
   * @return if overriding is allowed.
   */
  boolean allowsOverride();

  /**
   * Initializes this permission management.
   */
  void init();

  /**
   * Closes the permission management.
   */
  void close();

  /**
   * Reloads this permission management.
   *
   * @return true if reloading was successful, false otherwise.
   */
  boolean reload();

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
   * Creates a new permission user and inserts it into the database. If the there already is a user in the database the
   * user is updated.
   *
   * @param name     the name of the new user.
   * @param password the password of the new user.
   * @param potency  the potency of the new user.
   * @return the newly created permission user.
   * @throws NullPointerException if the given name or password is null.
   */
  @NonNull PermissionUser addPermissionUser(@NonNull String name, @NonNull String password, int potency);

  /**
   * Inserts the given permission user into the database. If there already is a permission user with the same unique id
   * the old one is replaced by the given permission user.
   *
   * @param permissionUser the user to add.
   * @return the same instance that was given.
   * @throws NullPointerException if the given user is null.
   */
  @NonNull PermissionUser addPermissionUser(@NonNull PermissionUser permissionUser);

  /**
   * Gets all groups that are associated with the given permissible.
   *
   * @param permissible the permissible to retrieve the groups for.
   * @return all found groups.
   */
  @NonNull Collection<PermissionGroup> groupsOf(@Nullable Permissible permissible);

  /**
   * Checks if the given permissible or any group that is associated with the permissible has the permission, and it is
   * not overriden by another permission with a negative potency.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param permission  the permission to check for.
   * @return true if the permissible has the permission, false otherwise.
   * @throws NullPointerException if the given permissible or permission is null.
   * @see #permissionResult(Permissible, Permission)
   */
  default boolean hasPermission(@NonNull Permissible permissible, @NonNull Permission permission) {
    return this.permissionResult(permissible, permission).asBoolean();
  }

  /**
   * Checks if the given permissible has the given permission for the given group.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param group       the group that the permission is assigned to.
   * @param permission  the permission to check.
   * @return true if the permissible has the permission on the given group, false otherwise.
   * @throws NullPointerException if the given permissible, group or permission is null.
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
   * Checks if the given permissible or any group that is associated with the permissible has the permission, and it is
   * not overriden by another permission with a negative potency.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param permission  the permission to check.
   * @return the result of the permission check.
   * @throws NullPointerException if the given permissible or permission is null.
   */
  @NonNull PermissionCheckResult permissionResult(@NonNull Permissible permissible, @NonNull Permission permission);

  /**
   * Checks if the given permissible has the given permission for the given group.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param group       the group to get the permissions on.
   * @param permission  the permission to check.
   * @return the result of the permission check.
   * @throws NullPointerException if the given permissible, group or permission is null.
   */
  @NonNull PermissionCheckResult groupPermissionResult(
    @NonNull Permissible permissible,
    @NonNull String group,
    @NonNull Permission permission);

  /**
   * Checks if the given permissible has the given permission for the given groups.
   *
   * @param permissible the permissible to check if the permission is set.
   * @param groups      the groups to get the permissions on.
   * @param permission  the permission to check.
   * @return the result of the permission check.
   * @throws NullPointerException if the given permissible, groups or permission is null.
   */
  @NonNull PermissionCheckResult groupsPermissionResult(
    @NonNull Permissible permissible,
    @NonNull String[] groups,
    @NonNull Permission permission);

  /**
   * Finds the highest permission (sorted by the potency) in the given permission collection using the given permission
   * potency as the starting point.
   *
   * @param permissions the permissions to check through.
   * @param permission  the starting point for the check to run.
   * @return the highest permission in the given permission or null if there is no permission with a higher potency in
   * the given collection.
   * @throws NullPointerException if the given permissions or permission is null.
   */
  @Nullable Permission findHighestPermission(@NonNull Collection<Permission> permissions,
    @NonNull Permission permission);

  /**
   * Gets all permission of the specified permissible.
   *
   * @param permissible the permissible to get the permission of.
   * @return all permissions of the permissible.
   * @throws NullPointerException if the given permissible is null.
   */
  @NonNull Collection<Permission> allPermissions(@NonNull Permissible permissible);

  /**
   * Gets all permission of the specified permissible on the given group.
   *
   * @param permissible the permissible to get the permission of.
   * @param group       the group to get the permission on or null if no specific group should be used.
   * @return all permissions of the permissible on the specified group if provided.
   * @throws NullPointerException if the given permissible or group is null.
   */
  @NonNull Collection<Permission> allGroupPermissions(@NonNull Permissible permissible, @Nullable String group);

  /**
   * Updates the given permission user in the database. If there is no user with the unique id the user is just inserted
   * into the database.
   *
   * @param permissionUser the user to update in the database.
   * @throws NullPointerException if the given user is null.
   */
  void updateUser(@NonNull PermissionUser permissionUser);

  /**
   * Deletes all users in the database matching the given name. The given name is case-sensitive.
   *
   * @param name the name of the users to be deleted
   * @return true if any user was deleted, false if none was deleted.
   * @throws NullPointerException if the given name is null.
   */
  boolean deleteUser(@NonNull String name);

  /**
   * Deletes the given user from the database.
   *
   * @param permissionUser the user to delete from the database.
   * @return if there was a user with the unique id of the given user, false otherwise.
   * @throws NullPointerException if the given user is null.
   */
  boolean deletePermissionUser(@NonNull PermissionUser permissionUser);

  /**
   * Checks if a user with the given unique id is stored in the database.
   *
   * @param uniqueId the unique id of the user.
   * @return true if there is a user with the given unique id, false otherwise.
   * @throws NullPointerException if the given unique id is null.
   */
  boolean containsUser(@NonNull UUID uniqueId);

  /**
   * Checks if at least one user with the given name is stored in the database. This given name is case-sensitive.
   *
   * @param name the name of the user
   * @return true if there is a user with that name, false otherwise
   * @throws NullPointerException if the given name is null.
   */
  boolean containsOneUser(@NonNull String name);

  /**
   * Searches for the user with the given unique id.
   *
   * @param uniqueId the unique id of the user.
   * @return the found permission user, null if no user was found.
   * @throws NullPointerException if the given unique id is null.
   */
  @Nullable PermissionUser user(@NonNull UUID uniqueId);

  /**
   * Searches the database for a permission user with the given unique id. If no user with the given id is found a new
   * user is stored in the database.
   * <p>
   * The given name is not used to search for the user.
   *
   * @param uniqueId the uniqueId of the user.
   * @param name     the name of the user.
   * @return the found or created permission user.
   * @throws NullPointerException if the given unique id or name is null.
   */
  @NonNull PermissionUser getOrCreateUser(@NonNull UUID uniqueId, @NonNull String name);

  /**
   * Searches all users matching the given name and selecting the first one.
   *
   * @param name the name of the user to search.
   * @return the found user or null if no user with this name exists.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable PermissionUser firstUser(@NonNull String name);

  /**
   * Gets a list of all users with the given name out of the database.
   *
   * @param name the name to search in the database for.
   * @return all found users that have the given name.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull List<PermissionUser> usersByName(@NonNull String name);

  /**
   * Gets a list of all stored permission users in the database.
   * <p>
   * Note: If there are many users in the database this might lead to memory issues as all of them are loaded into
   * memory.
   *
   * @return all permission users.
   */
  @NonNull Collection<PermissionUser> users();

  /**
   * Gets a list of all stored permission users in the database that are in the given group.
   * <p>
   * Note: If there are many users in the database this might lead to memory issues as all of them are loaded into
   * memory.
   *
   * @return all permission users in the given group.
   * @throws NullPointerException if the given group is null.
   */
  @NonNull Collection<PermissionUser> usersByGroup(@NonNull String group);

  /**
   * Gets the default permission group. The default group is determined by the {@link PermissionGroup#defaultGroup()}
   * property. If multiple groups are the default group this method might return another result for each call.
   *
   * @return the default permission group, null if no group is marked as default.
   */
  @Nullable PermissionGroup defaultPermissionGroup();

  /**
   * Gets the highest permission group for the given user. The highest permission group is determined by the potency of
   * the group. A higher potency results in the highest group.
   *
   * @param permissionUser the user to get the group for.
   * @return the highest permission group, null if the user is not in any group and there is no default group.
   * @throws NullPointerException if the given user is null.
   */
  @Nullable PermissionGroup highestPermissionGroup(@NonNull PermissionUser permissionUser);

  /**
   * Creates a new permission group with the given name and potency. If there already is group with the given name the
   * old group is replaced by the newly created one.
   *
   * @param name    the case-sensitive name of the new group.
   * @param potency the potency of the new group.
   * @return the newly created permission group.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull PermissionGroup addPermissionGroup(@NonNull String name, int potency);

  /**
   * Stores the given permission group. If there already is a group with the same name the old group is replaced by the
   * given one.
   *
   * @param permissionGroup the group to add.
   * @return the same instance that was given.
   * @throws NullPointerException if the given permission group is null.
   */
  @NonNull PermissionGroup addPermissionGroup(@NonNull PermissionGroup permissionGroup);

  /**
   * Updates the given permission group. If there is no group with the given name the group is added.
   *
   * @param permissionGroup the group to update.
   * @throws NullPointerException if the given permission group is null.
   */
  void updateGroup(@NonNull PermissionGroup permissionGroup);

  /**
   * Deletes the group by the given name.
   *
   * @param name the name of the group to delete.
   * @return true if a group with the name was found and deleted, false otherwise.
   * @throws NullPointerException if the given name is null.
   */
  boolean deleteGroup(@NonNull String name);

  /**
   * Deletes the given permission group.
   *
   * @param permissionGroup the permission group to delete.
   * @return always true as the group is already present.
   * @throws NullPointerException if the given group is null.
   */
  boolean deletePermissionGroup(@NonNull PermissionGroup permissionGroup);

  /**
   * Checks if the given group exists.
   *
   * @param group the case-sensitive name of the group.
   * @return true if the group exists, false otherwise.
   * @throws NullPointerException if the given group is null.
   */
  boolean containsGroup(@NonNull String group);

  /**
   * Gets the permission group by the given name.
   *
   * @param name the case-sensitive name of the group.
   * @return the permission group with the given name, null if there is no group with the given name.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable PermissionGroup group(@NonNull String name);

  /**
   * Gets all known permission groups.
   *
   * @return all permissions groups.
   */
  @NonNull Collection<PermissionGroup> groups();

  /**
   * Overwrites all known groups with the given collection of groups. Null is allowed and results in the removal of all
   * groups without a replacement.
   *
   * @param groups the groups to add after clearing all other groups.
   */
  void groups(@Nullable Collection<PermissionGroup> groups);

  /**
   * Gets the permission group with the given name by using {@link #group(String)} and, if not null, puts it into the
   * consumer, after that, the group will be updated by using {@link #updateGroup(PermissionGroup)}.
   *
   * @param name     the name of the group.
   * @param modifier the consumer to modify the user.
   * @return the modified group, null if no group was found with the given name.
   * @throws NullPointerException if the given name or consumer is null.
   */
  @Nullable PermissionGroup modifyGroup(@NonNull String name,
    @NonNull BiConsumer<PermissionGroup, PermissionGroup.Builder> modifier);

  /**
   * Gets the permission user with the given unique id using {@link #user(UUID)} and, if not null, puts them into the
   * consumer, after that, the user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param uniqueId the unique id of the user
   * @param modifier the consumer to modify the user
   * @return the modified user, null if no user was found with the given unique id.
   * @throws NullPointerException if the given unique id or consumer is null.
   */
  @Nullable PermissionUser modifyUser(
    @NonNull UUID uniqueId,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier);

  /**
   * Gets every user matching the given name using {@link #usersByName(String)} and puts them into the consumer, after
   * that, every user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param name     the name of the users.
   * @param modifier the consumer to modify the available users.
   * @return a list containing all modified users.
   * @throws NullPointerException if the given name or consumer is null.
   */
  @NonNull List<PermissionUser> modifyUsers(
    @NonNull String name,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier);

  /**
   * Sends a command line to the node the method is called on or the node the wrapper is connected to. The command line
   * is executed and all resulting messages are collected in the collection of strings.
   *
   * @param user        the user to execute the command line with.
   * @param commandLine the command line to execute on the node.
   * @return all messages regarding the command execution.
   * @throws NullPointerException if the given user or command line is null.
   */
  @NonNull Collection<String> sendCommandLine(@NonNull PermissionUser user, @NonNull String commandLine);

  /**
   * Gets all groups that are associated with the given permissible.
   *
   * @param permissible the permissible to retrieve the groups for.
   * @return a task containing all found groups.
   */
  default @NonNull Task<Collection<PermissionGroup>> groupsOfAsync(@Nullable Permissible permissible) {
    return Task.supply(() -> this.groupsOf(permissible));
  }

  /**
   * Inserts the given permission user into the database. If there already is a permission user with the same unique id
   * the old one is replaced by the given permission user.
   *
   * @param permissionUser the user to add.
   * @return a task containing the same instance that was given.
   * @throws NullPointerException if the given user is null.
   */
  default @NonNull Task<PermissionUser> addPermissionUserAsync(@NonNull PermissionUser permissionUser) {
    return Task.supply(() -> this.addPermissionUser(permissionUser));
  }

  /**
   * Creates a new permission user and inserts it into the database. If the there already is a user in the database the
   * user is updated.
   *
   * @param name     the name of the new user.
   * @param password the password of the new user.
   * @param potency  the potency of the new user.
   * @return a task containing the newly created permission user.
   * @throws NullPointerException if the given name or password is null.
   */
  default @NonNull Task<PermissionUser> addUserAsync(@NonNull String name, @NonNull String password, int potency) {
    return Task.supply(() -> this.addPermissionUser(name, password, potency));
  }

  /**
   * Updates the given permission user in the database. If there is no user with the unique id the user is just inserted
   * into the database.
   *
   * @param permissionUser the user to update in the database.
   * @return a task that completes when the user was updated.
   * @throws NullPointerException if the given user is null.
   */
  default @NonNull Task<Void> updateUserAsync(@NonNull PermissionUser permissionUser) {
    return Task.supply(() -> this.updateUser(permissionUser));
  }

  /**
   * Deletes all users in the database matching the given name. The given name is case-sensitive.
   *
   * @param name the name of the users to be deleted
   * @return a task containing true if any user was deleted, false if none was deleted.
   * @throws NullPointerException if the given name is null.
   */
  default @NonNull Task<Boolean> deleteUserAsync(@NonNull String name) {
    return Task.supply(() -> this.deleteUser(name));
  }

  /**
   * Deletes the given user from the database.
   *
   * @param permissionUser the user to delete from the database.
   * @return a task containing if there was a user with the unique id of the given user, false otherwise.
   * @throws NullPointerException if the given user is null.
   */
  default @NonNull Task<Boolean> deletePermissionUserAsync(@NonNull PermissionUser permissionUser) {
    return Task.supply(() -> this.deletePermissionUser(permissionUser));
  }

  /**
   * Checks if a user with the given unique id is stored in the database.
   *
   * @param uniqueId the unique id of the user.
   * @return a task containing true if there is a user with the given unique id, false otherwise.
   * @throws NullPointerException if the given unique id is null.
   */
  default @NonNull Task<Boolean> containsUserAsync(@NonNull UUID uniqueId) {
    return Task.supply(() -> this.containsUser(uniqueId));
  }

  /**
   * Checks if at least one user with the given name is stored in the database. This given name is case-sensitive.
   *
   * @param name the name of the user
   * @return a task containing true if there is a user with that name, false otherwise
   * @throws NullPointerException if the given name is null.
   */
  default @NonNull Task<Boolean> containsOneUserAsync(@NonNull String name) {
    return Task.supply(() -> this.containsOneUser(name));
  }

  /**
   * Searches for the user with the given unique id.
   *
   * @param uniqueId the unique id of the user.
   * @return a task containing the found permission user, null if no user was found.
   * @throws NullPointerException if the given unique id is null.
   */
  default @NonNull Task<PermissionUser> userAsync(@NonNull UUID uniqueId) {
    return Task.supply(() -> this.user(uniqueId));
  }

  /**
   * Searches the database for a permission user with the given unique id. If no user with the given id is found a new
   * user is stored in the database.
   * <p>
   * The given name is not used to search for the user.
   *
   * @param uniqueId the uniqueId of the user.
   * @param name     the name of the user.
   * @return a task containing the found or created permission user.
   * @throws NullPointerException if the given unique id or name is null.
   */
  default @NonNull Task<PermissionUser> getOrCreateUserAsync(@NonNull UUID uniqueId, @NonNull String name) {
    return Task.supply(() -> this.getOrCreateUser(uniqueId, name));
  }

  /**
   * Gets a list of all users with the given name out of the database.
   *
   * @param name the name to search in the database for.
   * @return a task containing all found users that have the given name.
   * @throws NullPointerException if the given name is null.
   */
  default @NonNull Task<List<PermissionUser>> usersByNameAsync(@NonNull String name) {
    return Task.supply(() -> this.usersByName(name));
  }

  /**
   * Searches all users matching the given name and selecting the first one.
   *
   * @param name the name of the user to search.
   * @return a task containing the found user or null if no user with this name exists.
   * @throws NullPointerException if the given name is null.
   */
  default @NonNull Task<PermissionUser> firstUserAsync(String name) {
    return Task.supply(() -> this.firstUser(name));
  }

  /**
   * Gets a list of all stored permission users in the database.
   * <p>
   * Note: If there are many users in the database this might lead to memory issues as all of them are loaded into
   * memory.
   *
   * @return a task containing all permission users.
   */
  default @NonNull Task<Collection<PermissionUser>> usersAsync() {
    return Task.supply(this::users);
  }

  /**
   * Gets a list of all stored permission users in the database that are in the given group.
   * <p>
   * Note: If there are many users in the database this might lead to memory issues as all of them are loaded into
   * memory.
   *
   * @return a task containing all permission users in the given group.
   * @throws NullPointerException if the given group is null.
   */
  default @NonNull Task<Collection<PermissionUser>> usersByGroupAsync(@NonNull String group) {
    return Task.supply(() -> this.usersByGroup(group));
  }

  /**
   * Stores the given permission group. If there already is a group with the same name the old group is replaced by the
   * given one.
   *
   * @param permissionGroup the group to add.
   * @return a task containing the same instance that was given.
   * @throws NullPointerException if the given permission group is null.
   */
  default @NonNull Task<PermissionGroup> addPermissionGroupAsync(@NonNull PermissionGroup permissionGroup) {
    return Task.supply(() -> this.addPermissionGroup(permissionGroup));
  }

  /**
   * Creates a new permission group with the given name and potency. If there already is group with the given name the
   * old group is replaced by the newly created one.
   *
   * @param name    the case-sensitive name of the new group.
   * @param potency the potency of the new group.
   * @return a task containing the newly created permission group.
   * @throws NullPointerException if the given name is null.
   */
  default @NonNull Task<PermissionGroup> addGroupAsync(@NonNull String name, int potency) {
    return Task.supply(() -> this.addPermissionGroup(name, potency));
  }

  /**
   * Updates the given permission group. If there is no group with the given name the group is added.
   *
   * @param permissionGroup the group to update.
   * @return a task that completes after the group is updated.
   * @throws NullPointerException if the given permission group is null.
   */
  default @NonNull Task<Void> updateGroupAsync(@NonNull PermissionGroup permissionGroup) {
    return Task.supply(() -> this.updateGroup(permissionGroup));
  }

  /**
   * Deletes the group by the given name.
   *
   * @param name the name of the group to delete.
   * @return a task containing true if a group with the name was found and deleted, false otherwise.
   * @throws NullPointerException if the given name is null.
   */
  default @NonNull Task<Boolean> deleteGroupAsync(@NonNull String name) {
    return Task.supply(() -> this.deleteGroup(name));
  }

  /**
   * Deletes the given permission group.
   *
   * @param permissionGroup the permission group to delete.
   * @return a task containing always true as the group is already present.
   * @throws NullPointerException if the given group is null.
   */
  default @NonNull Task<Boolean> deletePermissionGroupAsync(@NonNull PermissionGroup permissionGroup) {
    return Task.supply(() -> this.deletePermissionGroup(permissionGroup));
  }

  /**
   * Checks if the given group exists.
   *
   * @param group the case-sensitive name of the group.
   * @return a task containing true if the group exists, false otherwise.
   * @throws NullPointerException if the given group is null.
   */
  default @NonNull Task<Boolean> containsGroupAsync(@NonNull String group) {
    return Task.supply(() -> this.containsGroup(group));
  }

  /**
   * Gets the permission group by the given name.
   *
   * @param name the case-sensitive name of the group.
   * @return a task containing the permission group with the given name, null if there is no group with the given name.
   * @throws NullPointerException if the given name is null.
   */
  default @NonNull Task<PermissionGroup> groupAsync(@NonNull String name) {
    return Task.supply(() -> this.group(name));
  }

  /**
   * Gets the default permission group. The default group is determined by the {@link PermissionGroup#defaultGroup()}
   * property. If multiple groups are the default group this method might return another result for each call.
   *
   * @return a task containing the default permission group, null if no group is marked as default.
   */
  default @NonNull Task<PermissionGroup> defaultPermissionGroupAsync() {
    return Task.supply(this::defaultPermissionGroup);
  }

  /**
   * Gets all known permission groups.
   *
   * @return a task containing all permissions groups.
   */
  default @NonNull Task<Collection<PermissionGroup>> groupsAsync() {
    return Task.supply((ThrowableSupplier<Collection<PermissionGroup>, Throwable>) this::groups);
  }

  /**
   * Overwrites all known groups with the given collection of groups. Null is allowed and results in the removal of all
   * groups without a replacement.
   *
   * @param groups the groups to add after clearing all other groups.
   * @return a task that completes after the groups were replaced.
   */
  default @NonNull Task<Void> groupsAsync(@Nullable Collection<PermissionGroup> groups) {
    return Task.supply(() -> this.groups(groups));
  }

  /**
   * Gets the permission group with the given name by using {@link #group(String)} and, if not null, puts it into the
   * consumer, after that, the group will be updated by using {@link #updateGroup(PermissionGroup)}.
   *
   * @param name     the name of the group.
   * @param modifier the consumer to modify the user.
   * @return a task containing the modified group, null if no group was found with the given name.
   * @throws NullPointerException if the given name or consumer is null.
   */
  default @NonNull Task<PermissionGroup> modifyGroupAsync(
    @NonNull String name,
    @NonNull BiConsumer<PermissionGroup, PermissionGroup.Builder> modifier
  ) {
    return Task.supply(() -> this.modifyGroup(name, modifier));
  }

  /**
   * Gets the permission user with the given unique id using {@link #user(UUID)} and, if not null, puts them into the
   * consumer, after that, the user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param uniqueId the unique id of the user
   * @param modifier the consumer to modify the user
   * @return a task containing the modified user, null if no user was found with the given unique id.
   * @throws NullPointerException if the given unique id or consumer is null.
   */
  default @NonNull Task<PermissionUser> modifyUserAsync(
    @NonNull UUID uniqueId,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier
  ) {
    return Task.supply(() -> this.modifyUser(uniqueId, modifier));
  }

  /**
   * Gets every user matching the given name using {@link #usersByName(String)} and puts them into the consumer, after
   * that, every user will be updated by using {@link #updateUser(PermissionUser)}.
   *
   * @param name     the name of the users.
   * @param modifier the consumer to modify the available users.
   * @return a task containing a list containing all modified users.
   * @throws NullPointerException if the given name or consumer is null.
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
   * @param user        the user to execute the command line with.
   * @param commandLine the command line to execute on the node.
   * @return a task containing all messages regarding the command execution.
   * @throws NullPointerException if the given user or command line is null.
   */
  default @NonNull Task<Collection<String>> sendCommandLineAsync(
    @NonNull PermissionUser user,
    @NonNull String commandLine
  ) {
    return Task.supply(() -> this.sendCommandLine(user, commandLine));
  }
}
