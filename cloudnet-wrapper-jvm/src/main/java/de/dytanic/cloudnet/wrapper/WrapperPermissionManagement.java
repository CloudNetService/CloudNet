package de.dytanic.cloudnet.wrapper;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.PacketQueryProvider;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.permission.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class WrapperPermissionManagement implements DefaultPermissionManagement {

    private static final Function<Pair<JsonDocument, byte[]>, Void> VOID_FUNCTION = documentPair -> null;

    private PacketQueryProvider packetQueryProvider;

    public WrapperPermissionManagement(PacketQueryProvider packetQueryProvider) {
        this.packetQueryProvider = packetQueryProvider;
    }

    @Override
    public IPermissionUser addUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        try {
            return this.addUserAsync(permissionUser).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void updateUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        try {
            this.updateUserAsync(permissionUser).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void deleteUser(@NotNull String name) {
        Preconditions.checkNotNull(name);

        try {
            this.deleteUserAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void deleteUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        try {
            this.deleteUserAsync(permissionUser).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean containsUser(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        try {
            return this.containsUserAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean containsUser(@NotNull String name) {
        Preconditions.checkNotNull(name);

        try {
            return this.containsUserAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    @Nullable
    @Override
    public IPermissionUser getUser(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        try {
            return this.getUserAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public List<IPermissionUser> getUsers(@NotNull String name) {
        Preconditions.checkNotNull(name);

        try {
            return this.getUsersAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        try {
            return this.getUsersAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public void setUsers(@Nullable Collection<? extends IPermissionUser> users) {
        Preconditions.checkNotNull(users);

        try {
            this.setUsersAsync(users).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
        Preconditions.checkNotNull(group);

        try {
            return this.getUsersByGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public IPermissionGroup addGroup(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        try {
            return this.addGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateGroup(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        try {
            this.updateGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void deleteGroup(@NotNull String group) {
        Preconditions.checkNotNull(group);

        try {
            this.deleteGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void deleteGroup(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        try {
            this.deleteGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean containsGroup(@NotNull String group) {
        Preconditions.checkNotNull(group);

        try {
            return this.containsGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    @Nullable
    @Override
    public IPermissionGroup getGroup(@NotNull String name) {
        Preconditions.checkNotNull(name);

        try {
            return this.getGroupAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        try {
            return this.getGroupsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public void setGroups(@NotNull Collection<? extends IPermissionGroup> groups) {
        Preconditions.checkNotNull(groups);

        try {
            this.setGroupsAsync(groups).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean reload() {
        try {
            return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_reload"), null,
                    pair -> pair.getSecond()[0] == 1).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    @Override
    @NotNull
    public ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_add_user").append("permissionUser", permissionUser), null,
                pair -> permissionUser);
    }

    @Override
    @NotNull
    public ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_update_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Void> deleteUserAsync(@NotNull String name) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_user_with_name").append("name", name), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Void> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_user_with_uuid").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    @Override
    @NotNull
    public ITask<Boolean> containsUserAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_user_with_name").append("name", name), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    @Override
    @NotNull
    public ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_user_by_uuid").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("permissionUser", PermissionUser.TYPE));
    }

    @Override
    @NotNull
    public ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_user_by_name").append("name", name), null,
                documentPair -> new ArrayList<>(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                }.getType())));
    }

    @Override
    @NotNull
    public ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_users"), null,
                documentPair -> new ArrayList<>(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                }.getType())));
    }

    @Override
    @NotNull
    public ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_set_users").append("permissionUsers", users), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group) {
        Preconditions.checkNotNull(group);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_users_by_group").append("group", group), null,
                documentPair -> new ArrayList<>(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                }.getType())));
    }

    @Override
    @NotNull
    public ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_add_group").append("permissionGroup", permissionGroup), null,
                pair -> permissionGroup);
    }

    @Override
    @NotNull
    public ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_update_group").append("permissionGroup", permissionGroup), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Void> deleteGroupAsync(@NotNull String name) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_group_with_name").append("name", name), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_group").append("permissionGroup", permissionGroup), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Boolean> containsGroupAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_group").append("name", name), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    @Override
    @NotNull
    public ITask<IPermissionGroup> getGroupAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_group").append("name", name), null,
                documentPair -> documentPair.getFirst().get("permissionGroup", PermissionGroup.TYPE));
    }

    @Override
    @NotNull
    public ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_groups"), null,
                documentPair -> new ArrayList<>(documentPair.getFirst().get("permissionGroups", new TypeToken<List<PermissionGroup>>() {
                }.getType(), new ArrayList<>())));
    }

    @Override
    @NotNull
    public ITask<Void> setGroupsAsync(@NotNull Collection<? extends IPermissionGroup> groups) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_set_groups").append("permissionGroups", groups), null,
                VOID_FUNCTION);
    }

}
