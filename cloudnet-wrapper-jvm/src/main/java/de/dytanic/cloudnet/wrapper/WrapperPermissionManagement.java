package de.dytanic.cloudnet.wrapper;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.CountingTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.PacketQueryProvider;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.permission.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class WrapperPermissionManagement implements DefaultSynchronizedPermissionManagement, DefaultPermissionManagement, IPermissionManagement {

    private static final Function<Pair<JsonDocument, byte[]>, Void> VOID_FUNCTION = documentPair -> null;

    private final PacketQueryProvider packetQueryProvider;

    public WrapperPermissionManagement(PacketQueryProvider packetQueryProvider) {
        this.packetQueryProvider = packetQueryProvider;
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
    public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync(@Nullable IPermissionUser permissionUser) {
        if (permissionUser == null) {
            return CompletedTask.create(Collections.emptyList());
        }

        Collection<IPermissionGroup> groups = new ArrayList<>();
        CountingTask<Collection<IPermissionGroup>> task = new CountingTask<>(groups, 0);

        permissionUser.getGroups().stream()
                .map(PermissionUserGroupInfo::getGroup)
                .map(this::getGroupAsync)
                .forEach(groupTask -> {
                    task.incrementCount();
                    groupTask.onComplete(group -> {
                        groups.add(group);
                        task.countDown();
                    })
                            .onCancelled(iPermissionGroupITask -> task.countDown())
                            .onFailure(throwable -> task.countDown());
                });

        return task;
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return this.getGroupsAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Override
    @NotNull
    public ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_add_user").append("permissionUser", permissionUser), null,
                pair -> permissionUser);
    }

    @Override
    public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull String name, @NotNull String password, int potency) {
        return this.addUserAsync(new PermissionUser(UUID.randomUUID(), name, password, potency));
    }

    @Override
    @NotNull
    public ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_update_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Boolean> deleteUserAsync(@NotNull String name) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_user_with_name").append("name", name), null,
                pair -> pair.getSecond()[0] == 1);
    }

    @Override
    @NotNull
    public ITask<Boolean> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_user").append("permissionUser", permissionUser), null,
                pair -> pair.getSecond()[0] == 1);
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
    public @NotNull ITask<IPermissionUser> getFirstUserAsync(String name) {
        Preconditions.checkNotNull(name);

        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_first_user").append("name", name), null,
                documentPair -> documentPair.getFirst().get("permissionUser", PermissionUser.TYPE));
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
    public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull String role, int potency) {
        return this.addGroupAsync(new PermissionGroup(role, potency));
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
    public ITask<IPermissionGroup> getDefaultPermissionGroupAsync() {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_default_group"), null,
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
    public ITask<Void> setGroupsAsync(Collection<? extends IPermissionGroup> groups) {
        return this.packetQueryProvider.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_set_groups").append("permissionGroups", groups), null,
                VOID_FUNCTION);
    }

    @Override
    public IPermissionManagement getChildPermissionManagement() {
        return null;
    }

    @Override
    public boolean canBeOverwritten() {
        return true;
    }
}
