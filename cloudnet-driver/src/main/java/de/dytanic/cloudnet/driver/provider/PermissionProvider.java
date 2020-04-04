package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * This class provides methods to get and modify the permission users and groups in CloudNet's database.
 */
public interface PermissionProvider {

    /**
     * Adds a new user to the database.
     *
     * @param permissionUser the user to be added
     */
    void addUser(@NotNull IPermissionUser permissionUser);

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
    void deleteUser(@NotNull String name);

    /**
     * Deletes one user with the uniqueId of the given user.
     *
     * @param permissionUser the user to be deleted
     */
    void deleteUser(@NotNull IPermissionUser permissionUser);

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
    void setUsers(@Nullable Collection<? extends IPermissionUser> users);

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
     */
    void addGroup(@NotNull IPermissionGroup permissionGroup);

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
    void setGroups(@NotNull Collection<? extends IPermissionGroup> groups);

    /**
     * Adds a new user to the database.
     *
     * @param permissionUser the user to be added
     */
    @NotNull
    ITask<Void> addUserAsync(@NotNull IPermissionUser permissionUser);

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
    ITask<Void> deleteUserAsync(@NotNull String name);

    /**
     * Deletes one user with the uniqueId of the given user.
     *
     * @param permissionUser the user to be deleted
     */
    @NotNull
    ITask<Void> deleteUserAsync(@NotNull IPermissionUser permissionUser);

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
    ITask<Void> addGroupAsync(@NotNull IPermissionGroup permissionGroup);

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
    ITask<Void> setGroupsAsync(@NotNull Collection<? extends IPermissionGroup> groups);

}
