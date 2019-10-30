package de.dytanic.cloudnet.wrapper.provider;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.provider.PermissionProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class WrapperPermissionProvider implements PermissionProvider {

    private static final Function<Pair<JsonDocument, byte[]>, Void> VOID_FUNCTION = documentPair -> null;

    private Wrapper wrapper;

    public WrapperPermissionProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public void addUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        try {
            this.addUserAsync(permissionUser).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        try {
            this.updateUserAsync(permissionUser).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteUser(String name) {
        Validate.checkNotNull(name);

        try {
            this.deleteUserAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        try {
            this.deleteUserAsync(permissionUser).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean containsUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return this.containsUserAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean containsUser(String name) {
        Validate.checkNotNull(name);

        try {
            return this.containsUserAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return this.getUserAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<IPermissionUser> getUsers(String name) {
        Validate.checkNotNull(name);

        try {
            return this.getUsersAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        try {
            return this.getUsersAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setUsers(Collection<? extends IPermissionUser> users) {
        Validate.checkNotNull(users);

        try {
            this.setUsersAsync(users).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(String group) {
        Validate.checkNotNull(group);

        try {
            return this.getUsersByGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void addGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        try {
            this.addGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        try {
            this.updateGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteGroup(String group) {
        Validate.checkNotNull(group);

        try {
            this.deleteGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        try {
            this.deleteGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean containsGroup(String group) {
        Validate.checkNotNull(group);

        try {
            return this.containsGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public IPermissionGroup getGroup(String name) {
        Validate.checkNotNull(name);

        try {
            return this.getGroupAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        try {
            return this.getGroupsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setGroups(Collection<? extends IPermissionGroup> groups) {
        Validate.checkNotNull(groups);

        try {
            this.setGroupsAsync(groups).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }


    @Override
    public ITask<Void> addUserAsync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_add_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> updateUserAsync(IPermissionUser permissionUser) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_update_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> deleteUserAsync(String name) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_user_with_name").append("name", name), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> deleteUserAsync(IPermissionUser permissionUser) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Boolean> containsUserAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_user_with_uuid").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    @Override
    public ITask<Boolean> containsUserAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_user_with_name").append("name", name), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    @Override
    public ITask<IPermissionUser> getUserAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_user_by_uuid").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("permissionUser", PermissionUser.TYPE));
    }

    @Override
    public ITask<List<IPermissionUser>> getUsersAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_user_by_name").append("name", name), null,
                documentPair -> {
                    List<IPermissionUser> collection = Iterables.newArrayList();
                    collection.addAll(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                    }.getType()));

                    return collection;
                });
    }

    @Override
    public ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_users"), null,
                documentPair -> {
                    Collection<IPermissionUser> collection = Iterables.newArrayList();
                    collection.addAll(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                    }.getType()));

                    return collection;
                });
    }

    @Override
    public ITask<Void> setUsersAsync(Collection<? extends IPermissionUser> users) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_set_users").append("permissionUsers", users), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Collection<IPermissionUser>> getUsersByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_users_by_group").append("group", group), null,
                documentPair -> {
                    List<IPermissionUser> collection = Iterables.newArrayList();
                    collection.addAll(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                    }.getType()));

                    return collection;
                });
    }

    @Override
    public ITask<Void> addGroupAsync(IPermissionGroup permissionGroup) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_add_group").append("permissionGroup", permissionGroup), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> updateGroupAsync(IPermissionGroup permissionGroup) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_update_group").append("permissionGroup", permissionGroup), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> deleteGroupAsync(String name) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_group_with_name").append("name", name), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> deleteGroupAsync(IPermissionGroup permissionGroup) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_group").append("permissionGroup", permissionGroup), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Boolean> containsGroupAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_group").append("name", name), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    @Override
    public ITask<IPermissionGroup> getGroupAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_group").append("name", name), null,
                documentPair -> documentPair.getFirst().get("permissionGroup", PermissionGroup.TYPE));
    }

    @Override
    public ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_groups"), null,
                documentPair -> {
                    List<IPermissionGroup> collection = Iterables.newArrayList();
                    collection.addAll(documentPair.getFirst().get("permissionGroups", new TypeToken<List<PermissionGroup>>() {
                    }.getType(), Iterables.newArrayList()));

                    return collection;
                });
    }

    @Override
    public ITask<Void> setGroupsAsync(Collection<? extends IPermissionGroup> groups) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_set_groups").append("permissionGroups", groups), null,
                VOID_FUNCTION);
    }

}
