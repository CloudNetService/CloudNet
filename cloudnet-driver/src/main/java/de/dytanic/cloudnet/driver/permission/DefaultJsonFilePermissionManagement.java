package de.dytanic.cloudnet.driver.permission;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class DefaultJsonFilePermissionManagement implements ClusterSynchronizedPermissionManagement {

    private final File file;

    private final Map<UUID, IPermissionUser> permissionUsers = new ConcurrentHashMap<>();

    private final Map<String, IPermissionGroup> permissionGroups = new ConcurrentHashMap<>();

    private IPermissionManagementHandler permissionManagementHandler;

    public DefaultJsonFilePermissionManagement(File file) {
        Preconditions.checkNotNull(file);

        this.file = file;
        this.file.getParentFile().mkdirs();

        this.load();
    }

    @Override
    public IPermissionUser addUserWithoutClusterSync(IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        this.deleteUserWithoutClusterSync(permissionUser);
        this.permissionUsers.put(permissionUser.getUniqueId(), permissionUser);
        this.save();

        return permissionUser;
    }

    @Override
    public void updateUserWithoutClusterSync(IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleUpdateUser(this, permissionUser);
        }

        testPermissionUser(permissionUser);
        this.permissionUsers.put(permissionUser.getUniqueId(), permissionUser);
        this.save();
    }

    @Override
    public void deleteUserWithoutClusterSync(String name) {
        Preconditions.checkNotNull(name);

        for (IPermissionUser permissionUser : this.permissionUsers.values().stream().filter(permissionUser -> permissionUser.getName().equals(name)).collect(Collectors.toList())) {
            this.permissionUsers.remove(permissionUser.getUniqueId());
        }

        this.save();
    }

    @Override
    public void deleteUserWithoutClusterSync(IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        this.permissionUsers.remove(permissionUser.getUniqueId());
        this.save();
    }

    @Override
    public boolean containsUser(UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.permissionUsers.containsKey(uniqueId);
    }

    @Override
    public boolean containsUser(String name) {
        Preconditions.checkNotNull(name);

        return this.permissionUsers.values().stream().anyMatch(permissionUser -> permissionUser.getName().equalsIgnoreCase(name));
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);
        IPermissionUser permissionUser = this.permissionUsers.get(uniqueId);

        if (testPermissionUser(permissionUser)) {
            updateUser(permissionUser);
        }

        return permissionUser;
    }

    @Override
    public List<IPermissionUser> getUsers(String name) {
        Preconditions.checkNotNull(name);

        List<IPermissionUser> permissionUsers = this.permissionUsers.values().stream().filter(permissionUser -> permissionUser.getName().equals(name)).collect(Collectors.toList());

        for (IPermissionUser user : permissionUsers) {
            if (this.testPermissionUser(user)) {
                this.updateUser(user);
            }
        }

        return permissionUsers;
    }

    @Override
    public boolean reload() {
        load();

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleReloaded(this);
        }

        return true;
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        Collection<IPermissionUser> permissionUsers = this.permissionUsers.values();

        for (IPermissionUser permissionUser : permissionUsers) {
            if (testPermissionUser(permissionUser)) {
                updateUser(permissionUser);
            }
        }

        return permissionUsers;
    }

    @Override
    public void setUsersWithoutClusterSync(Collection<? extends IPermissionUser> users) {
        Preconditions.checkNotNull(users);

        this.permissionGroups.clear();

        for (IPermissionUser permissionUser : users) {
            testPermissionUser(permissionUser);
            this.permissionUsers.put(permissionUser.getUniqueId(), permissionUser);
        }

        this.save();
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(String group) {
        Preconditions.checkNotNull(group);

        return this.permissionUsers.values().stream().filter(permissionUser -> permissionUser.inGroup(group)).collect(Collectors.toList());
    }

    @Override
    public IPermissionGroup addGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleAddGroup(this, permissionGroup);
        }

        this.permissionGroups.put(permissionGroup.getName(), permissionGroup);
        this.save();

        return permissionGroup;
    }

    @Override
    public void updateGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleUpdateGroup(this, permissionGroup);
        }

        testPermissionGroup(permissionGroup);
        this.permissionGroups.put(permissionGroup.getName(), permissionGroup);
        this.save();
    }

    @Override
    public void deleteGroupWithoutClusterSync(String group) {
        Preconditions.checkNotNull(group);
        IPermissionGroup permissionGroup = this.permissionGroups.remove(group);
        this.save();

        if (permissionGroup != null) {
            if (permissionManagementHandler != null) {
                permissionManagementHandler.handleDeleteGroup(this, permissionGroup);
            }
        }
    }

    @Override
    public void deleteGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleDeleteGroup(this, permissionGroup);
        }

        this.permissionGroups.remove(permissionGroup.getName());
        this.save();
    }

    @Override
    public boolean containsGroup(String name) {
        Preconditions.checkNotNull(name);

        return permissionGroups.containsKey(name);
    }

    @Override
    public IPermissionGroup getGroup(String name) {
        Preconditions.checkNotNull(name);
        IPermissionGroup permissionGroup = this.permissionGroups.get(name);

        if (permissionGroup != null && testPermissionGroup(permissionGroup)) {
            updateGroup(permissionGroup);
        }

        return permissionGroup;
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        Collection<IPermissionGroup> permissionGroups = this.permissionGroups.values();

        for (IPermissionGroup permissionGroup : permissionGroups) {
            if (testPermissionGroup(permissionGroup)) {
                updateGroup(permissionGroup);
            }
        }

        return permissionGroups;
    }

    @Override
    public void setGroupsWithoutClusterSync(Collection<? extends IPermissionGroup> permissionGroups) {
        if (permissionGroups == null) {
            return;
        }

        this.permissionGroups.clear();

        for (IPermissionGroup permissionGroup : permissionGroups) {
            testPermissionGroup(permissionGroup);
            this.permissionGroups.put(permissionGroup.getName(), permissionGroup);
        }

        this.save();
    }

    private void save() {
        new JsonDocument()
                .append("groups", this.permissionGroups.values())
                .append("users", this.permissionUsers.values())
                .write(this.file);
    }

    private void load() {
        JsonDocument document = JsonDocument.newDocument(this.file);

        setUsers(document.get("users", new TypeToken<Collection<PermissionUser>>() {
        }.getType()));

        setGroups(document.get("groups", new TypeToken<Collection<PermissionGroup>>() {
        }.getType()));
    }

    public IPermissionManagementHandler getPermissionManagementHandler() {
        return this.permissionManagementHandler;
    }

    public void setPermissionManagementHandler(IPermissionManagementHandler permissionManagementHandler) {
        this.permissionManagementHandler = permissionManagementHandler;
    }
}