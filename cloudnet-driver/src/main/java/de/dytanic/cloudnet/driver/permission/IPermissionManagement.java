package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.concurrent.ITask;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface IPermissionManagement {

    IPermissionManagement getChildPermissionManagement();

    boolean canBeOverwritten();

    /**
     * @deprecated use {@link #getUsers(String)} instead
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.5")
    List<IPermissionUser> getUser(String name);

    IPermissionUser getFirstUser(String name);

    void init();

    boolean reload();


    IPermissionGroup getHighestPermissionGroup(@NotNull IPermissionUser permissionUser);

    IPermissionGroup getDefaultPermissionGroup();

    boolean testPermissionGroup(@Nullable IPermissionGroup permissionGroup);

    boolean testPermissionUser(@Nullable IPermissionUser permissionUser);

    boolean testPermissible(@Nullable IPermissible permissible);

    IPermissionUser addUser(@NotNull String name, @NotNull String password, int potency);

    IPermissionGroup addGroup(@NotNull String role, int potency);

    Collection<IPermissionGroup> getGroups(@Nullable IPermissionUser permissionUser);

    Collection<IPermissionGroup> getExtendedGroups(@Nullable IPermissionGroup group);

    default boolean hasPermission(@NotNull IPermissionUser permissionUser, @NotNull String permission) {
        return this.getPermissionResult(permissionUser, permission).asBoolean();
    }

    default boolean hasPermission(@NotNull IPermissionUser permissionUser, @NotNull Permission permission) {
        return this.getPermissionResult(permissionUser, permission).asBoolean();
    }

    default boolean hasPermission(@NotNull IPermissionUser permissionUser, @NotNull String group, @NotNull Permission permission) {
        return this.getPermissionResult(permissionUser, group, permission).asBoolean();
    }

    @NotNull
    PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull String permission);

    @NotNull
    PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull Permission permission);

    @NotNull
    PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull String group, @NotNull Permission permission);

    Collection<Permission> getAllPermissions(@NotNull IPermissible permissible);

    Collection<Permission> getAllPermissions(@NotNull IPermissible permissible, @Nullable String group);

    /**
     * Adds a new user to the database.
     *
     * @param permissionUser the user to be added
     * @return the new user
     */
    IPermissionUser addUser(@NotNull IPermissionUser permissionUser);

    /**
     * Updates an already existing user in the database.
     *
     * @param permissionUser the user to be updated
     */
    void updateUser(@NotNull IPermissionUser permissionUser);

    /**
     * Deletes all users in the database matching the given name.
     * This method is case-sensitive.
     *
     * @param name the name of the users to be deleted
     */
    boolean deleteUser(@NotNull String name);

    /**
     * Deletes one user with the uniqueId of the given user.
     *
     * @param permissionUser the user to be deleted
     */
    boolean deleteUser(@NotNull IPermissionUser permissionUser);

    /**
     * Checks if a user with the given uniqueId is stored in the database.
     *
     * @param uniqueId the uniqueId of the user
     * @return {@code true} if there is a user with that uniqueId, {@code false} otherwise
     */
    boolean containsUser(@NotNull UUID uniqueId);

    /**
     * Checks if at least one user with the given name is stored in the database.
     * This method is case-sensitive.
     *
     * @param name the name of the user
     * @return {@code true} if there is a user with that name, {@code false} otherwise
     */
    boolean containsUser(@NotNull String name);

    /**
     * Gets a user with the given uniqueId out of the database.
     *
     * @param uniqueId the uniqueId of the user
     * @return the {@link IPermissionUser} from the database or {@code null} if there is no user with that uniqueId stored
     */
    @Nullable
    IPermissionUser getUser(@NotNull UUID uniqueId);

    /**
     * Gets a list of all users with the given name out of the database.
     * This can only return null when the connection to the database (or when it is executed in a
     * Wrapper instance the connection to the cloud) times out.
     *
     * @param name the name of the users
     * @return a list of all {@link IPermissionUser}s stored in the database or an empty list if there is no user with that name stored.
     */
    @NotNull
    List<IPermissionUser> getUsers(@NotNull String name);

    /**
     * Gets a list of all users stored in the database.
     * This can only return null when the connection to the database (or when it is executed in a
     * Wrapper instance the connection to the cloud) times out.
     * <p>
     * This method shouldn't be used when there are many users stored in the database, because that takes a lot of memory.
     *
     * @return a list of all {@link IPermissionUser}s stored in the database or an empty list if there is no user with that name stored.
     */
    Collection<IPermissionUser> getUsers();

    /**
     * Clears all users stored in the database and inserts the given list.
     *
     * @param users the new {@link IPermissionUser}s to be stored in the database
     */
    void setUsers(@NotNull Collection<? extends IPermissionUser> users);

    /**
     * Gets a list of all users stored in the database with the given group.
     * This can only return null when the connection to the database (or when it is executed in a
     * Wrapper instance the connection to the cloud) times out.
     * <p>
     * This method shouldn't be used when there are many users with that group stored in the database, because that takes a lot of memory.
     *
     * @return a list of all {@link IPermissionUser}s stored in the database or an empty list if there is no user with that name stored.
     */
    Collection<IPermissionUser> getUsersByGroup(@NotNull String group);

    /**
     * Adds a new permission group to the list of groups. If a group with that name already exists,
     * it will be deleted and created again.
     *
     * @param permissionGroup the {@link IPermissionGroup} to be added
     * @return the new group
     */
    IPermissionGroup addGroup(@NotNull IPermissionGroup permissionGroup);

    /**
     * Updates a permission group in the list of groups. If a group with that name doesn't exist,
     * it will be created.
     *
     * @param permissionGroup the {@link IPermissionGroup} to be updated
     */
    void updateGroup(@NotNull IPermissionGroup permissionGroup);

    /**
     * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
     *
     * @param name the case-sensitive name of the group
     */
    void deleteGroup(@NotNull String name);

    /**
     * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
     *
     * @param permissionGroup the {@link IPermissionGroup} to be deleted
     */
    void deleteGroup(@NotNull IPermissionGroup permissionGroup);

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
     * @return the {@link IPermissionUser} if it exists, {@code null} otherwise
     */
    @Nullable
    IPermissionGroup getGroup(@NotNull String name);

    /**
     * Gets the list of all groups in the Cloud.
     *
     * @return a list of {@link IPermissionGroup}s registered in the cloud or an empty list if there is no group registered
     */
    Collection<IPermissionGroup> getGroups();

    /**
     * Clears all groups in the Cloud and sets given groups.
     *
     * @param groups the new groups
     */
    void setGroups(@Nullable Collection<? extends IPermissionGroup> groups);

    /**
     * Gets the permission group with the given name by using {@link #getGroup(String)} and, if not null, puts it into the consumer, after
     * that, the group will be updated by using {@link #updateGroup(IPermissionGroup)}.
     *
     * @param name     the name of the group
     * @param modifier the Consumer to modify the user
     * @return the modified user
     */
    IPermissionGroup modifyGroup(@NotNull String name, @NotNull Consumer<IPermissionGroup> modifier);

    /**
     * Gets the permission user with the given uniqueId by using {@link #getUser(UUID)} and, if not null, puts them into the consumer, after
     * that, the user will be updated by using {@link #updateUser(IPermissionUser)}.
     *
     * @param uniqueId the uniqueId of the user
     * @param modifier the Consumer to modify the user
     * @return the modified user
     */
    IPermissionUser modifyUser(@NotNull UUID uniqueId, @NotNull Consumer<IPermissionUser> modifier);

    /**
     * Gets every user matching the given name by using {@link #getUsers(String)} and puts them into the consumer, after
     * that, every user will be updated by using {@link #updateUser(IPermissionUser)}.
     *
     * @param name     the name of the users
     * @param modifier the Consumer to modify the available users
     * @return a list of all modified users
     */
    List<IPermissionUser> modifyUsers(@NotNull String name, @NotNull Consumer<IPermissionUser> modifier);

    @NotNull
    ITask<Collection<IPermissionGroup>> getGroupsAsync(@Nullable IPermissionUser permissionUser);

    /**
     * Adds a new user to the database.
     *
     * @param permissionUser the user to be added
     */
    @NotNull
    ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser);

    @NotNull
    ITask<IPermissionUser> addUserAsync(@NotNull String name, @NotNull String password, int potency);

    /**
     * Updates an already existing user in the database.
     *
     * @param permissionUser the user to be updated
     */
    @NotNull
    ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser);

    /**
     * Deletes all users in the database matching the given name.
     * This method is case-sensitive.
     *
     * @param name the name of the users to be deleted
     */
    @NotNull
    ITask<Boolean> deleteUserAsync(@NotNull String name);

    /**
     * Deletes one user with the uniqueId of the given user.
     *
     * @param permissionUser the user to be deleted
     */
    @NotNull
    ITask<Boolean> deleteUserAsync(@NotNull IPermissionUser permissionUser);

    /**
     * Checks if a user with the given uniqueId is stored in the database.
     *
     * @param uniqueId the uniqueId of the user
     * @return {@code true} if there is a user with that uniqueId, {@code false} otherwise
     */
    @NotNull
    ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId);

    /**
     * Checks if at least one user with the given name is stored in the database.
     * This method is case-sensitive.
     *
     * @param name the name of the user
     * @return {@code true} if there is a user with that name, {@code false} otherwise
     */
    @NotNull
    ITask<Boolean> containsUserAsync(@NotNull String name);

    /**
     * Gets a user with the given uniqueId out of the database.
     *
     * @param uniqueId the uniqueId of the user
     * @return the {@link IPermissionUser} from the database or {@code null} if there is no user with that uniqueId stored
     */
    @NotNull
    ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId);

    /**
     * Gets a list of all users with the given name out of the database.
     * This can only return null when the connection to the database (or when it is executed in a
     * Wrapper instance the connection to the cloud) times out.
     *
     * @param name the name of the users
     * @return a list of all {@link IPermissionUser}s stored in the database or an empty list if there is no user with that name stored.
     */
    @NotNull
    ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name);

    @NotNull
    ITask<IPermissionUser> getFirstUserAsync(String name);

    /**
     * Gets a list of all users stored in the database.
     * This can only return null when the connection to the database (or when it is executed in a
     * Wrapper instance the connection to the cloud) times out.
     * <p>
     * This method shouldn't be used when there are many users stored in the database, because that takes a lot of memory.
     *
     * @return a list of all {@link IPermissionUser}s stored in the database or an empty list if there is no user with that name stored.
     */
    @NotNull
    ITask<Collection<IPermissionUser>> getUsersAsync();

    /**
     * Clears all users stored in the database and inserts the given list.
     *
     * @param users the new {@link IPermissionUser}s to be stored in the database
     */
    @NotNull
    ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users);

    /**
     * Gets a list of all users stored in the database with the given group.
     * This can only return null when the connection to the database (or when it is executed in a
     * Wrapper instance the connection to the cloud) times out.
     * <p>
     * This method shouldn't be used when there are many users with that group stored in the database, because that takes a lot of memory.
     *
     * @return a list of all {@link IPermissionUser}s stored in the database or an empty list if there is no user with that name stored.
     */
    @NotNull
    ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group);

    /**
     * Adds a new permission group to the list of groups. If a group with that name already exists,
     * it will be deleted and created again.
     *
     * @param permissionGroup the {@link IPermissionGroup} to be added
     */
    @NotNull
    ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup);

    @NotNull
    ITask<IPermissionGroup> addGroupAsync(@NotNull String role, int potency);

    /**
     * Updates a permission group in the list of groups. If a group with that name doesn't exist,
     * it will be created.
     *
     * @param permissionGroup the {@link IPermissionGroup} to be updated
     */
    @NotNull
    ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup);

    /**
     * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
     *
     * @param name the case-sensitive name of the group
     */
    @NotNull
    ITask<Void> deleteGroupAsync(@NotNull String name);

    /**
     * Deletes a group by its name out of the list of groups. If a group with that name doesn't exist, nothing happens.
     *
     * @param permissionGroup the {@link IPermissionGroup} to be deleted
     */
    @NotNull
    ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup);

    /**
     * Checks if a specific group exists.
     *
     * @param group the case-sensitive name of the group
     * @return {@code true} if the group exists, {@code false} otherwise
     */
    @NotNull
    ITask<Boolean> containsGroupAsync(@NotNull String group);

    /**
     * Gets a specific group by its name.
     *
     * @param name the case-sensitive name of the group
     * @return the {@link IPermissionUser} if it exists, {@code null} otherwise
     */
    @NotNull
    ITask<IPermissionGroup> getGroupAsync(@NotNull String name);

    ITask<IPermissionGroup> getDefaultPermissionGroupAsync();

    /**
     * Gets the list of all groups in the Cloud.
     *
     * @return a list of {@link IPermissionGroup}s registered in the cloud or an empty list if there is no group registered
     */
    @NotNull
    ITask<Collection<IPermissionGroup>> getGroupsAsync();

    /**
     * Clears all groups in the Cloud and sets given groups.
     *
     * @param groups the new groups
     */
    @NotNull
    ITask<Void> setGroupsAsync(@Nullable Collection<? extends IPermissionGroup> groups);

    /**
     * Gets the permission group with the given name by using {@link #getGroupAsync(String)} and, if not null, puts it into the consumer, after
     * that, the group will be updated by using {@link #updateGroup(IPermissionGroup)}.
     *
     * @param name     the name of the group
     * @param modifier the Consumer to modify the user
     * @return the modified user
     */
    ITask<IPermissionGroup> modifyGroupAsync(@NotNull String name, @NotNull Consumer<IPermissionGroup> modifier);

    /**
     * Gets the permission user with the given uniqueId by using {@link #getUserAsync(UUID)} and, if not null, puts them into the consumer, after
     * that, the user will be updated by using {@link #updateUser(IPermissionUser)}.
     *
     * @param uniqueId the uniqueId of the user
     * @param modifier the Consumer to modify the user
     * @return the modified user
     */
    ITask<IPermissionUser> modifyUserAsync(@NotNull UUID uniqueId, @NotNull Consumer<IPermissionUser> modifier);

    /**
     * Gets every user matching the given name by using {@link #getUsersAsync(String)} and puts them into the consumer, after
     * that, every user will be updated by using {@link #updateUser(IPermissionUser)}.
     *
     * @param name     the name of the users
     * @param modifier the Consumer to modify the available users
     * @return a list of all modified users
     */
    ITask<List<IPermissionUser>> modifyUsersAsync(@NotNull String name, @NotNull Consumer<IPermissionUser> modifier);

}
