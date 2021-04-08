package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "3.5")
public class CloudPermissionsManagement implements IPermissionManagement, CachedPermissionManagement {

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
        CloudPermissionsManagement management = new CloudPermissionsPermissionManagement(Objects.requireNonNull(CloudNetDriver.getInstance().getPermissionManagement()));
        CloudNetDriver.getInstance().setPermissionManagement(management);
        return management;
    }

    @Override
    public IPermissionManagement getChildPermissionManagement() {
        return wrapped.getChildPermissionManagement();
    }

    @Override
    public boolean canBeOverwritten() {
        return wrapped.canBeOverwritten();
    }

    @Override
    @ApiStatus.ScheduledForRemoval(inVersion = "3.5")
    @Deprecated
    public List<IPermissionUser> getUser(String name) {
        return wrapped.getUser(name);
    }

    @Override
    public IPermissionUser getFirstUser(String name) {
        return wrapped.getFirstUser(name);
    }

    @Override
    public void init() {
        this.wrapped.init();
    }

    @Override
    public boolean reload() {
        return wrapped.reload();
    }

    @Override
    public IPermissionGroup getHighestPermissionGroup(@NotNull IPermissionUser permissionUser) {
        return wrapped.getHighestPermissionGroup(permissionUser);
    }

    @Override
    public IPermissionGroup getDefaultPermissionGroup() {
        return wrapped.getDefaultPermissionGroup();
    }

    @Override
    public boolean testPermissionGroup(@Nullable IPermissionGroup permissionGroup) {
        return wrapped.testPermissionGroup(permissionGroup);
    }

    @Override
    public boolean testPermissionUser(@Nullable IPermissionUser permissionUser) {
        return wrapped.testPermissionUser(permissionUser);
    }

    @Override
    public boolean testPermissible(@Nullable IPermissible permissible) {
        return wrapped.testPermissible(permissible);
    }

    @Override
    public IPermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
        return wrapped.addUser(name, password, potency);
    }

    @Override
    public IPermissionGroup addGroup(@NotNull String role, int potency) {
        return wrapped.addGroup(role, potency);
    }

    @Override
    @NotNull
    public Collection<IPermissionGroup> getGroups(@Nullable IPermissible permissible) {
        return wrapped.getGroups(permissible);
    }

    @Override
    @Deprecated
    public Collection<IPermissionGroup> getExtendedGroups(@Nullable IPermissionGroup group) {
        return wrapped.getExtendedGroups(group);
    }

    @Override
    public boolean hasPermission(@NotNull IPermissible permissible, @NotNull String permission) {
        return wrapped.hasPermission(permissible, permission);
    }

    @Override
    public boolean hasPermission(@NotNull IPermissible permissible, @NotNull Permission permission) {
        return wrapped.hasPermission(permissible, permission);
    }

    @Override
    public boolean hasPermission(@NotNull IPermissible permissible, @NotNull String group, @NotNull Permission permission) {
        return wrapped.hasPermission(permissible, group, permission);
    }

    @Override
    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String permission) {
        return wrapped.getPermissionResult(permissible, permission);
    }

    @Override
    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull Permission permission) {
        return wrapped.getPermissionResult(permissible, permission);
    }

    @Override
    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String group, @NotNull Permission permission) {
        return wrapped.getPermissionResult(permissible, group, permission);
    }

    @Override
    public @NotNull Collection<Permission> getAllPermissions(@NotNull IPermissible permissible) {
        return wrapped.getAllPermissions(permissible);
    }

    @Override
    public @NotNull Collection<Permission> getAllPermissions(@NotNull IPermissible permissible, @Nullable String group) {
        return wrapped.getAllPermissions(permissible, group);
    }

    @Override
    public IPermissionUser addUser(@NotNull IPermissionUser permissionUser) {
        return wrapped.addUser(permissionUser);
    }

    @Override
    public void updateUser(@NotNull IPermissionUser permissionUser) {
        wrapped.updateUser(permissionUser);
    }

    @Override
    public boolean deleteUser(@NotNull String name) {
        return wrapped.deleteUser(name);
    }

    @Override
    public boolean deleteUser(@NotNull IPermissionUser permissionUser) {
        return wrapped.deleteUser(permissionUser);
    }

    @Override
    public boolean containsUser(@NotNull UUID uniqueId) {
        return wrapped.containsUser(uniqueId);
    }

    @Override
    public boolean containsUser(@NotNull String name) {
        return wrapped.containsUser(name);
    }

    @Override
    public @Nullable IPermissionUser getUser(@NotNull UUID uniqueId) {
        return wrapped.getUser(uniqueId);
    }

    @Override
    public @NotNull List<IPermissionUser> getUsers(@NotNull String name) {
        return wrapped.getUsers(name);
    }

    @Override
    public @NotNull Collection<IPermissionUser> getUsers() {
        return wrapped.getUsers();
    }

    @Override
    public void setUsers(@Nullable Collection<? extends IPermissionUser> users) {
        wrapped.setUsers(users == null ? Collections.emptyList() : users);
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
        return wrapped.getUsersByGroup(group);
    }

    @Override
    public IPermissionGroup addGroup(@NotNull IPermissionGroup permissionGroup) {
        return wrapped.addGroup(permissionGroup);
    }

    @Override
    public void updateGroup(@NotNull IPermissionGroup permissionGroup) {
        wrapped.updateGroup(permissionGroup);
    }

    @Override
    public void deleteGroup(@NotNull String name) {
        wrapped.deleteGroup(name);
    }

    @Override
    public void deleteGroup(@NotNull IPermissionGroup permissionGroup) {
        wrapped.deleteGroup(permissionGroup);
    }

    @Override
    public boolean containsGroup(@NotNull String group) {
        return wrapped.containsGroup(group);
    }

    @Override
    public @Nullable IPermissionGroup getGroup(@NotNull String name) {
        return wrapped.getGroup(name);
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return wrapped.getGroups();
    }

    @Override
    public void setGroups(@Nullable Collection<? extends IPermissionGroup> groups) {
        wrapped.setGroups(groups);
    }

    @Override
    public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync(@Nullable IPermissionUser permissionUser) {
        return wrapped.getGroupsAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
        return wrapped.addUserAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull String name, @NotNull String password, int potency) {
        return wrapped.addUserAsync(name, password, potency);
    }

    @Override
    public @NotNull ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        return wrapped.updateUserAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<Boolean> deleteUserAsync(@NotNull String name) {
        return wrapped.deleteUserAsync(name);
    }

    @Override
    public @NotNull ITask<Boolean> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        return wrapped.deleteUserAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
        return wrapped.containsUserAsync(uniqueId);
    }

    @Override
    public @NotNull ITask<Boolean> containsUserAsync(@NotNull String name) {
        return wrapped.containsUserAsync(name);
    }

    @Override
    public @NotNull ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId) {
        return wrapped.getUserAsync(uniqueId);
    }

    @Override
    public @NotNull ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name) {
        return wrapped.getUsersAsync(name);
    }

    @Override
    public @NotNull ITask<IPermissionUser> getFirstUserAsync(String name) {
        return wrapped.getFirstUserAsync(name);
    }

    @Override
    public @NotNull ITask<Collection<IPermissionUser>> getUsersAsync() {
        return wrapped.getUsersAsync();
    }

    @Override
    public @NotNull ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users) {
        return wrapped.setUsersAsync(users);
    }

    @Override
    public @NotNull ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group) {
        return wrapped.getUsersByGroupAsync(group);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return wrapped.addGroupAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull String role, int potency) {
        return wrapped.addGroupAsync(role, potency);
    }

    @Override
    public @NotNull ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return wrapped.updateGroupAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Void> deleteGroupAsync(@NotNull String name) {
        return wrapped.deleteGroupAsync(name);
    }

    @Override
    public @NotNull ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return wrapped.deleteGroupAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Boolean> containsGroupAsync(@NotNull String group) {
        return wrapped.containsGroupAsync(group);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> getGroupAsync(@NotNull String name) {
        return wrapped.getGroupAsync(name);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> getDefaultPermissionGroupAsync() {
        return wrapped.getDefaultPermissionGroupAsync();
    }

    @Override
    public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return wrapped.getGroupsAsync();
    }

    @Override
    public @NotNull ITask<Void> setGroupsAsync(@Nullable Collection<? extends IPermissionGroup> groups) {
        return wrapped.setGroupsAsync(groups);
    }

    @Override
    public Map<UUID, IPermissionUser> getCachedPermissionUsers() {
        return this.wrapped instanceof CachedPermissionManagement ? ((CachedPermissionManagement) this.wrapped).getCachedPermissionUsers() : null;
    }

    @Override
    public Map<String, IPermissionGroup> getCachedPermissionGroups() {
        return this.wrapped instanceof CachedPermissionManagement ? ((CachedPermissionManagement) this.wrapped).getCachedPermissionGroups() : null;
    }

    @Override
    public IPermissionGroup modifyGroup(@NotNull String name, @NotNull Consumer<IPermissionGroup> modifier) {
        return wrapped.modifyGroup(name, modifier);
    }

    @Override
    public IPermissionUser modifyUser(@NotNull UUID uniqueId, @NotNull Consumer<IPermissionUser> modifier) {
        return wrapped.modifyUser(uniqueId, modifier);
    }

    @Override
    public List<IPermissionUser> modifyUsers(@NotNull String name, @NotNull Consumer<IPermissionUser> modifier) {
        return wrapped.modifyUsers(name, modifier);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> modifyGroupAsync(@NotNull String name, @NotNull Consumer<IPermissionGroup> modifier) {
        return wrapped.modifyGroupAsync(name, modifier);
    }

    @Override
    public @NotNull ITask<IPermissionUser> modifyUserAsync(@NotNull UUID uniqueId, @NotNull Consumer<IPermissionUser> modifier) {
        return wrapped.modifyUserAsync(uniqueId, modifier);
    }

    @Override
    public @NotNull ITask<List<IPermissionUser>> modifyUsersAsync(@NotNull String name, @NotNull Consumer<IPermissionUser> modifier) {
        return wrapped.modifyUsersAsync(name, modifier);
    }

}