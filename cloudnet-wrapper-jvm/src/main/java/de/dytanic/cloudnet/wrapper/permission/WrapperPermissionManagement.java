package de.dytanic.cloudnet.wrapper.permission;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.DefaultPermissionManagement;
import de.dytanic.cloudnet.driver.permission.DefaultSynchronizedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissible;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionCheckResult;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WrapperPermissionManagement extends DefaultPermissionManagement
        implements DefaultSynchronizedPermissionManagement, IPermissionManagement, CachedPermissionManagement, DriverAPIUser {

    private final Wrapper wrapper;

    private final Map<UUID, IPermissionUser> cachedPermissionUsers = new ConcurrentHashMap<>();
    private final Map<String, IPermissionGroup> cachedPermissionGroups = new ConcurrentHashMap<>();

    public WrapperPermissionManagement(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public void init() {
        Collection<IPermissionGroup> groups = this.loadGroupsAsync().getDef(null);
        if (groups != null && !groups.isEmpty()) {
            for (IPermissionGroup group : groups) {
                this.cachedPermissionGroups.put(group.getName(), group);
            }
        }

        CloudNetDriver.getInstance().getEventManager().registerListener(new PermissionCacheListener(this));
    }

    @Override
    public boolean reload() {
        boolean success = this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_RELOAD,
                packet -> packet.getBuffer().readBoolean()
        ).get(5, TimeUnit.SECONDS, false);

        if (success) {
            Collection<IPermissionGroup> permissionGroups = this.loadGroupsAsync().getDef(null);

            this.cachedPermissionGroups.clear();

            for (IPermissionGroup group : permissionGroups) {
                this.cachedPermissionGroups.put(group.getName(), group);
            }
        }

        return success;
    }

    @Override
    public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync(@Nullable IPermissionUser permissionUser) {
        if (permissionUser == null) {
            return CompletedTask.create(Collections.emptyList());
        }
        return CompletedTask.create(this.getGroups(permissionUser));

    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return this.cachedPermissionGroups.values();
    }

    @Override
    @NotNull
    public ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_ADD_USER,
                buffer -> buffer.writeObject(permissionUser),
                packet -> packet.getBuffer().readObject(PermissionUser.class)
        );
    }

    @Override
    public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull String name, @NotNull String password, int potency) {
        return this.addUserAsync(new PermissionUser(UUID.randomUUID(), name, password, potency));
    }

    @Override
    @NotNull
    public ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_UPDATE_USER,
                buffer -> buffer.writeObject(permissionUser),
                packet -> null
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> deleteUserAsync(@NotNull String name) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_DELETE_USERS_BY_NAME,
                buffer -> buffer.writeString(name),
                packet -> packet.getBuffer().readBoolean()
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_DELETE_USER,
                buffer -> buffer.writeObject(permissionUser),
                packet -> packet.getBuffer().readBoolean()
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        if (this.cachedPermissionUsers.containsKey(uniqueId)) {
            return CompletedTask.create(true);
        }

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_CONTAINS_USER_BY_UNIQUE_ID,
                buffer -> buffer.writeUUID(uniqueId),
                packet -> packet.getBuffer().readBoolean()
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> containsUserAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        if (this.cachedPermissionUsers.values().stream().anyMatch(permissionUser -> permissionUser.getName().equals(name))) {
            return CompletedTask.create(true);
        }

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_CONTAINS_USER_BY_NAME,
                buffer -> buffer.writeString(name),
                packet -> packet.getBuffer().readBoolean()
        );
    }

    @Override
    @NotNull
    public ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        if (this.cachedPermissionUsers.containsKey(uniqueId)) {
            return CompletedTask.create(this.cachedPermissionUsers.get(uniqueId));
        }

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_USER_BY_UNIQUE_ID,
                buffer -> buffer.writeUUID(uniqueId),
                packet -> packet.getBuffer().readOptionalObject(PermissionUser.class)
        );
    }

    @Override
    @NotNull
    public ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_USERS_BY_NAME,
                buffer -> buffer.writeString(name),
                packet -> Arrays.asList(packet.getBuffer().readObjectArray(PermissionUser.class))
        );
    }

    @Override
    public @NotNull ITask<IPermissionUser> getFirstUserAsync(String name) {
        Preconditions.checkNotNull(name);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_FIRST_USER_BY_NAME,
                buffer -> buffer.writeString(name),
                packet -> packet.getBuffer().readOptionalObject(PermissionUser.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_USERS,
                packet -> Arrays.asList(packet.getBuffer().readObjectArray(PermissionUser.class))
        );
    }

    @Override
    @NotNull
    public ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users) {
        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_SET_USERS,
                buffer -> buffer.writeObjectCollection(users)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group) {
        Preconditions.checkNotNull(group);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_USERS_BY_GROUP,
                buffer -> buffer.writeString(group),
                packet -> Arrays.asList(packet.getBuffer().readObjectArray(PermissionUser.class))
        );
    }

    @Override
    @NotNull
    public ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_ADD_GROUP,
                buffer -> buffer.writeObject(permissionGroup),
                packet -> packet.getBuffer().readObject(PermissionGroup.class)
        );
    }

    @Override
    public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull String role, int potency) {
        return this.addGroupAsync(new PermissionGroup(role, potency));
    }

    @Override
    @NotNull
    public ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_UPDATE_GROUP,
                buffer -> buffer.writeObject(permissionGroup)
        );
    }

    @Override
    @NotNull
    public ITask<Void> deleteGroupAsync(@NotNull String name) {
        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_DELETE_GROUP_BY_NAME,
                buffer -> buffer.writeString(name)
        );
    }

    @Override
    @NotNull
    public ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_DELETE_GROUP,
                buffer -> buffer.writeObject(permissionGroup)
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> containsGroupAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return CompletedTask.create(this.cachedPermissionGroups.containsKey(name));
    }

    @Override
    @NotNull
    public ITask<IPermissionGroup> getGroupAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return CompletedTask.create(this.cachedPermissionGroups.get(name));
    }

    @Override
    public @NotNull ITask<IPermissionGroup> getDefaultPermissionGroupAsync() {
        return CompletedTask.create(this.getDefaultPermissionGroup());
    }

    @Override
    public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return CompletedTask.create(this.cachedPermissionGroups.values());
    }

    @Override
    public IPermissionGroup getDefaultPermissionGroup() {
        return this.cachedPermissionGroups.values().stream()
                .filter(IPermissionGroup::isDefaultGroup)
                .findFirst()
                .orElse(null);
    }

    private ITask<Collection<IPermissionGroup>> loadGroupsAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_GROUPS,
                packet -> Arrays.asList(packet.getBuffer().readObjectArray(PermissionGroup.class))
        );
    }

    @Override
    @NotNull
    public ITask<Void> setGroupsAsync(Collection<? extends IPermissionGroup> groups) {
        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.PERMISSION_MANAGEMENT_SET_GROUPS,
                buffer -> buffer.writeObjectCollection(groups)
        );
    }

    @Override
    public @NotNull PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull Permission permission) {
        for (String group : this.wrapper.getCurrentServiceInfoSnapshot().getConfiguration().getGroups()) {
            PermissionCheckResult result = this.getPermissionResult(permissible, group, permission);
            if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
                return result;
            }
        }

        return super.getPermissionResult(permissible, permission);
    }

    @Override
    public IPermissionManagement getChildPermissionManagement() {
        return null;
    }

    @Override
    public boolean canBeOverwritten() {
        return true;
    }

    public Map<UUID, IPermissionUser> getCachedPermissionUsers() {
        return this.cachedPermissionUsers;
    }

    public Map<String, IPermissionGroup> getCachedPermissionGroups() {
        return this.cachedPermissionGroups;
    }

    @Override
    public INetworkChannel getNetworkChannel() {
        return this.wrapper.getNetworkChannel();
    }
}
